package com.tankermanager.service;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.entity.*;
import com.tankermanager.enums.TankerStatus;
import com.tankermanager.enums.TripStatus;
import com.tankermanager.exception.BadRequestException;
import com.tankermanager.exception.ForbiddenException;
import com.tankermanager.exception.ResourceNotFoundException;
import com.tankermanager.repository.*;
import com.tankermanager.security.SecurityUtils;
import com.tankermanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final Set<TripStatus> ACTIVE = EnumSet.of(
            TripStatus.ASSIGNED, TripStatus.GOING_FOR_LOADING, TripStatus.LOADING,
            TripStatus.LOADING_COMPLETED, TripStatus.EN_ROUTE, TripStatus.ARRIVED, TripStatus.UNLOADING);

    private final TripRepository tripRepository;
    private final TripStatusHistoryRepository historyRepository;
    private final VehicleLocationRepository locationRepository;
    private final CustomerRepository customerRepository;
    private final TankerRepository tankerRepository;
    private final DriverRepository driverRepository;
    private final BoreLocationRepository boreRepository;
    private final OperatorRepository operatorRepository;
    private final UserAccountRepository userAccountRepository;
    private final SmsService smsService;
    private final EtaService etaService;

    @Transactional
    public TripResponse bookTrip(BookTripRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        UserPrincipal current = SecurityUtils.currentUser();

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));

        Customer customer = customerRepository.findByOperatorIdAndPhone(operatorId, req.getCustomerPhone())
                .orElseGet(() -> {
                    if (req.getCustomerName() == null || req.getCustomerName().isBlank()) {
                        throw new BadRequestException("Customer name required for new phone number");
                    }
                    return customerRepository.save(Customer.builder()
                            .operator(operator)
                            .name(req.getCustomerName())
                            .phone(req.getCustomerPhone())
                            .defaultAddress(req.getDropAddress())
                            .defaultLat(req.getDropLat())
                            .defaultLng(req.getDropLng())
                            .build());
                });

        Tanker tanker = tankerRepository.findByIdAndOperatorId(req.getTankerId(), operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tanker not found"));
        if (tanker.getStatus() != TankerStatus.AVAILABLE) {
            throw new BadRequestException("Tanker is not available");
        }

        Driver driver = driverRepository.findByIdAndOperatorId(req.getDriverId(), operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (!driver.isAvailable() || !driver.isActive()) {
            throw new BadRequestException("Driver is not available");
        }

        BoreLocation bore;
        if (req.getBoreId() != null) {
            bore = boreRepository.findByIdAndOperatorId(req.getBoreId(), operatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bore not found"));
        } else {
            bore = boreRepository.findFirstByOperatorIdAndPrimaryBoreTrueAndActiveTrue(operatorId)
                    .or(() -> boreRepository.findByOperatorIdAndActiveTrue(operatorId).stream().findFirst())
                    .orElseThrow(() -> new BadRequestException("No bore location configured. Add a bore first."));
        }

        UserAccount bookedBy = userAccountRepository.findById(current.getId()).orElse(null);

        int eta = etaService.estimateMinutes(bore.getLatitude(), bore.getLongitude(), req.getDropLat(), req.getDropLng());

        Trip trip = Trip.builder()
                .tripCode("TRP-" + Instant.now().getEpochSecond() + "-" + (int) (Math.random() * 900 + 100))
                .operator(operator)
                .customer(customer)
                .tanker(tanker)
                .driver(driver)
                .bore(bore)
                .bookedBy(bookedBy)
                .status(TripStatus.ASSIGNED)
                .dropAddress(req.getDropAddress())
                .dropLat(req.getDropLat())
                .dropLng(req.getDropLng())
                .etaMinutes(eta)
                .assignedAt(Instant.now())
                .trackingToken(UUID.randomUUID().toString().replace("-", ""))
                .trackingEnabled(true)
                .notes(req.getNotes())
                .build();

        trip = tripRepository.save(trip);

        tanker.setStatus(TankerStatus.ON_TRIP);
        tankerRepository.save(tanker);
        driver.setAvailable(false);
        driverRepository.save(driver);

        recordHistory(trip, TripStatus.ASSIGNED, TripStatus.ASSIGNED, bookedBy, "Trip booked and assigned", true);
        smsService.sendTripSms(trip, TripStatus.ASSIGNED);

        return toResponse(trip, true);
    }

    @Transactional
    public TripResponse updateStatus(Long tripId, UpdateTripStatusRequest req, boolean driverOnly) {
        Trip trip = loadTripForActor(tripId, driverOnly);
        TripStatus from = trip.getStatus();
        TripStatus to = req.getStatus();

        if (from == TripStatus.COMPLETED || from == TripStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of finished trip");
        }
        if (!isValidTransition(from, to)) {
            throw new BadRequestException("Invalid status transition: " + from + " -> " + to);
        }

        UserAccount changer = userAccountRepository.findById(SecurityUtils.currentUser().getId()).orElse(null);
        trip.setStatus(to);

        if (to == TripStatus.EN_ROUTE && trip.getStartedAt() == null) {
            trip.setStartedAt(Instant.now());
            int eta = etaService.estimateMinutes(
                    trip.getBore().getLatitude(), trip.getBore().getLongitude(),
                    trip.getDropLat(), trip.getDropLng());
            trip.setEtaMinutes(eta);
        }

        if (to == TripStatus.COMPLETED || to == TripStatus.CANCELLED) {
            trip.setCompletedAt(Instant.now());
            trip.setTrackingEnabled(false);
            trip.getTanker().setStatus(TankerStatus.AVAILABLE);
            trip.getDriver().setAvailable(true);
            if (to == TripStatus.COMPLETED) {
                Driver d = trip.getDriver();
                d.setTotalTripsCompleted(d.getTotalTripsCompleted() + 1);
            }
            tankerRepository.save(trip.getTanker());
            driverRepository.save(trip.getDriver());
        }

        tripRepository.save(trip);
        boolean sms = smsService.sendTripSms(trip, to);
        recordHistory(trip, from, to, changer, req.getNote(), sms);

        return toResponse(trip, true);
    }

    @Transactional
    public void updateLocation(Long tripId, LocationUpdateRequest req) {
        Trip trip = loadTripForActor(tripId, true);
        if (!ACTIVE.contains(trip.getStatus())) {
            throw new BadRequestException("Cannot update location for inactive trip");
        }

        locationRepository.save(VehicleLocation.builder()
                .trip(trip)
                .tanker(trip.getTanker())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .speedKmh(req.getSpeedKmh())
                .heading(req.getHeading())
                .build());

        Tanker tanker = trip.getTanker();
        tanker.setLastKnownLat(req.getLatitude());
        tanker.setLastKnownLng(req.getLongitude());
        tankerRepository.save(tanker);

        if (trip.getStatus() == TripStatus.EN_ROUTE || trip.getStatus() == TripStatus.LOADING_COMPLETED) {
            int eta = etaService.estimateMinutes(req.getLatitude(), req.getLongitude(), trip.getDropLat(), trip.getDropLng());
            trip.setEtaMinutes(eta);
            tripRepository.save(trip);
        }
    }

    public List<TripResponse> listForOperator() {
        Long operatorId = SecurityUtils.requireOperatorId();
        return tripRepository.findByOperatorIdOrderByCreatedAtDesc(operatorId).stream()
                .map(t -> toResponse(t, false))
                .collect(Collectors.toList());
    }

    public List<TripResponse> listForDriver() {
        Long userId = SecurityUtils.currentUser().getId();
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return tripRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(t -> toResponse(t, false))
                .collect(Collectors.toList());
    }

    public List<TripResponse> listActiveForDriver() {
        Long userId = SecurityUtils.currentUser().getId();
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return tripRepository.findByDriverIdAndStatusNotIn(driver.getId(),
                        List.of(TripStatus.COMPLETED, TripStatus.CANCELLED)).stream()
                .map(t -> toResponse(t, false))
                .collect(Collectors.toList());
    }

    public TripResponse getTrip(Long id) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Trip trip = tripRepository.findByIdAndOperatorId(id, operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        return toResponse(trip, true);
    }

    public TrackingResponse publicTrack(String token) {
        Trip trip = tripRepository.findByTrackingToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid tracking link"));

        if (!trip.isTrackingEnabled() || trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            return TrackingResponse.builder()
                    .tripCode(trip.getTripCode())
                    .status(trip.getStatus())
                    .trackingEnabled(false)
                    .message("Trip completed. Vehicle location is no longer available.")
                    .dropAddress(trip.getDropAddress())
                    .dropLat(trip.getDropLat())
                    .dropLng(trip.getDropLng())
                    .build();
        }

        var latest = locationRepository.findFirstByTripIdOrderByRecordedAtDesc(trip.getId());
        return TrackingResponse.builder()
                .tripCode(trip.getTripCode())
                .status(trip.getStatus())
                .trackingEnabled(true)
                .etaMinutes(trip.getEtaMinutes())
                .vehicleLat(latest.map(VehicleLocation::getLatitude).orElse(trip.getTanker().getLastKnownLat()))
                .vehicleLng(latest.map(VehicleLocation::getLongitude).orElse(trip.getTanker().getLastKnownLng()))
                .dropLat(trip.getDropLat())
                .dropLng(trip.getDropLng())
                .dropAddress(trip.getDropAddress())
                .message("Live tracking active")
                .build();
    }

    private Trip loadTripForActor(Long tripId, boolean driverOnly) {
        UserPrincipal current = SecurityUtils.currentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (driverOnly) {
            Driver driver = driverRepository.findByUserId(current.getId())
                    .orElseThrow(() -> new ForbiddenException("Not a driver"));
            if (!trip.getDriver().getId().equals(driver.getId())) {
                throw new ForbiddenException("Not your trip");
            }
        } else {
            if (current.getOperatorId() == null || !trip.getOperator().getId().equals(current.getOperatorId())) {
                throw new ForbiddenException("Trip not in your operator");
            }
        }
        return trip;
    }

    private boolean isValidTransition(TripStatus from, TripStatus to) {
        if (to == TripStatus.CANCELLED) return from != TripStatus.COMPLETED;
        return switch (from) {
            case ASSIGNED -> to == TripStatus.GOING_FOR_LOADING || to == TripStatus.LOADING;
            case GOING_FOR_LOADING -> to == TripStatus.LOADING;
            case LOADING -> to == TripStatus.LOADING_COMPLETED;
            case LOADING_COMPLETED -> to == TripStatus.EN_ROUTE;
            case EN_ROUTE -> to == TripStatus.ARRIVED;
            case ARRIVED -> to == TripStatus.UNLOADING;
            case UNLOADING -> to == TripStatus.COMPLETED;
            default -> false;
        };
    }

    private void recordHistory(Trip trip, TripStatus from, TripStatus to, UserAccount by, String note, boolean sms) {
        historyRepository.save(TripStatusHistory.builder()
                .trip(trip)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(by)
                .note(note)
                .smsSent(sms)
                .build());
    }

    private TripResponse toResponse(Trip trip, boolean withHistory) {
        TripResponse.TripResponseBuilder b = TripResponse.builder()
                .id(trip.getId())
                .tripCode(trip.getTripCode())
                .status(trip.getStatus())
                .customerName(trip.getCustomer().getName())
                .customerPhone(trip.getCustomer().getPhone())
                .tankerNumber(trip.getTanker().getVehicleNumber())
                .driverId(trip.getDriver().getId())
                .driverName(trip.getDriver().getUser().getFullName())
                .driverPhone(trip.getDriver().getUser().getPhone())
                .boreName(trip.getBore().getName())
                .dropAddress(trip.getDropAddress())
                .dropLat(trip.getDropLat())
                .dropLng(trip.getDropLng())
                .boreLat(trip.getBore().getLatitude())
                .boreLng(trip.getBore().getLongitude())
                .etaMinutes(trip.getEtaMinutes())
                .trackingToken(trip.getTrackingToken())
                .trackingEnabled(trip.isTrackingEnabled())
                .assignedAt(trip.getAssignedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .notes(trip.getNotes());

        if (withHistory) {
            b.history(historyRepository.findByTripIdOrderByChangedAtAsc(trip.getId()).stream()
                    .map(h -> StatusHistoryItem.builder()
                            .fromStatus(h.getFromStatus())
                            .toStatus(h.getToStatus())
                            .note(h.getNote())
                            .smsSent(h.isSmsSent())
                            .changedAt(h.getChangedAt())
                            .build())
                    .collect(Collectors.toList()));
        }
        return b.build();
    }
}
