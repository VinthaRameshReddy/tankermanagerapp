package com.tankermanager.service;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.entity.*;
import com.tankermanager.enums.TankerStatus;
import com.tankermanager.enums.TripStatus;
import com.tankermanager.exception.BadRequestException;
import com.tankermanager.exception.ResourceNotFoundException;
import com.tankermanager.repository.*;
import com.tankermanager.security.SecurityUtils;
import com.tankermanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final TankerRepository tankerRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLocationRepository customerLocationRepository;
    private final BoreLocationRepository boreRepository;
    private final OperatorRepository operatorRepository;
    private final ExpenseRepository expenseRepository;
    private final BoreExpenseRepository boreExpenseRepository;
    private final DriverSalaryRepository salaryRepository;
    private final TripRepository tripRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public TankerResponse addTanker(TankerRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        if (tankerRepository.existsByOperatorIdAndVehicleNumber(operatorId, req.getVehicleNumber())) {
            throw new BadRequestException("Vehicle number already exists");
        }
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        Tanker tanker = tankerRepository.save(Tanker.builder()
                .operator(op)
                .vehicleNumber(req.getVehicleNumber().toUpperCase())
                .model(req.getModel())
                .capacityLitres(req.getCapacityLitres())
                .status(TankerStatus.AVAILABLE)
                .active(true)
                .build());
        return toTanker(tanker);
    }

    public List<TankerResponse> listTankers() {
        return tankerRepository.findByOperatorIdAndActiveTrue(SecurityUtils.requireOperatorId())
                .stream().map(this::toTanker).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers() {
        return driverRepository.findByOperatorIdAndActiveTrue(SecurityUtils.requireOperatorId())
                .stream().map(this::toDriver).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> availableDrivers() {
        return driverRepository.findByOperatorIdAndAvailableTrueAndActiveTrue(SecurityUtils.requireOperatorId())
                .stream().map(this::toDriver).collect(Collectors.toList());
    }

    public List<StaffResponse> listManagers() {
        return userAccountRepository
                .findByOperatorIdAndRole(SecurityUtils.requireOperatorId(), com.tankermanager.enums.Role.MANAGER)
                .stream()
                .map(u -> StaffResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .role(u.getRole().name())
                        .active(u.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerResponse upsertCustomer(CustomerRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        Customer customer = customerRepository.findByOperatorIdAndPhone(operatorId, req.getPhone())
                .orElse(Customer.builder().operator(op).phone(req.getPhone()).build());
        customer.setName(req.getName());
        customer.setDefaultAddress(req.getDefaultAddress());
        customer.setDefaultLat(req.getDefaultLat());
        customer.setDefaultLng(req.getDefaultLng());
        customer.setActive(true);

        if (req.getMapsLink() != null && !req.getMapsLink().isBlank()) {
            var coords = com.tankermanager.util.MapsLinkParser.parse(req.getMapsLink());
            customer.setDefaultLat(coords.latitude());
            customer.setDefaultLng(coords.longitude());
            if (customer.getDefaultAddress() == null || customer.getDefaultAddress().isBlank()) {
                customer.setDefaultAddress(req.getDefaultAddress() != null && !req.getDefaultAddress().isBlank()
                        ? req.getDefaultAddress()
                        : "Shared Maps location");
            }
        }

        customer = customerRepository.save(customer);

        if (req.getMapsLink() != null && !req.getMapsLink().isBlank()) {
            var coords = com.tankermanager.util.MapsLinkParser.parse(req.getMapsLink());
            String label = (req.getLocationLabel() != null && !req.getLocationLabel().isBlank())
                    ? req.getLocationLabel().trim()
                    : "Delivery";
            String address = (req.getDefaultAddress() != null && !req.getDefaultAddress().isBlank())
                    ? req.getDefaultAddress().trim()
                    : label;
            customerLocationRepository.save(CustomerLocation.builder()
                    .customer(customer)
                    .label(label)
                    .address(address)
                    .latitude(coords.latitude())
                    .longitude(coords.longitude())
                    .mapsLink(req.getMapsLink().trim())
                    .active(true)
                    .build());
        } else if (req.getDefaultLat() != null && req.getDefaultLng() != null) {
            String label = (req.getLocationLabel() != null && !req.getLocationLabel().isBlank())
                    ? req.getLocationLabel().trim()
                    : "Delivery";
            String address = (req.getDefaultAddress() != null && !req.getDefaultAddress().isBlank())
                    ? req.getDefaultAddress().trim()
                    : label;
            customerLocationRepository.save(CustomerLocation.builder()
                    .customer(customer)
                    .label(label)
                    .address(address)
                    .latitude(req.getDefaultLat())
                    .longitude(req.getDefaultLng())
                    .active(true)
                    .build());
        }

        return toCustomer(customer);
    }

    @Transactional
    public CustomerLocationResponse addCustomerLocation(Long customerId, CustomerLocationRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!customer.getOperator().getId().equals(operatorId)) {
            throw new BadRequestException("Customer not in your operator");
        }

        BigDecimal lat = req.getLatitude();
        BigDecimal lng = req.getLongitude();
        String mapsLink = req.getMapsLink();
        if (mapsLink != null && !mapsLink.isBlank()) {
            var coords = com.tankermanager.util.MapsLinkParser.parse(mapsLink);
            lat = coords.latitude();
            lng = coords.longitude();
        }
        if (lat == null || lng == null) {
            throw new BadRequestException("Provide a Google Maps link or latitude/longitude");
        }

        String label = (req.getLabel() != null && !req.getLabel().isBlank()) ? req.getLabel().trim() : "Delivery";
        String address = (req.getAddress() != null && !req.getAddress().isBlank())
                ? req.getAddress().trim()
                : label;

        CustomerLocation loc = customerLocationRepository.save(CustomerLocation.builder()
                .customer(customer)
                .label(label)
                .address(address)
                .latitude(lat)
                .longitude(lng)
                .mapsLink(mapsLink != null ? mapsLink.trim() : null)
                .active(true)
                .build());

        // Keep customer defaults in sync with latest location
        customer.setDefaultAddress(address);
        customer.setDefaultLat(lat);
        customer.setDefaultLng(lng);
        customerRepository.save(customer);

        return toLocation(loc);
    }

    public List<CustomerLocationResponse> listCustomerLocations(Long customerId) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!customer.getOperator().getId().equals(operatorId)) {
            throw new BadRequestException("Customer not in your operator");
        }
        return customerLocationRepository.findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId)
                .stream().map(this::toLocation).collect(Collectors.toList());
    }

    public List<CustomerResponse> listCustomers() {
        return customerRepository.findByOperatorIdAndActiveTrue(SecurityUtils.requireOperatorId())
                .stream().map(this::toCustomer).collect(Collectors.toList());
    }

    public CustomerResponse findByPhone(String phone) {
        return customerRepository.findByOperatorIdAndPhone(SecurityUtils.requireOperatorId(), phone)
                .map(this::toCustomer)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for phone: " + phone));
    }

    @Transactional
    public BoreResponse addBore(BoreRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        if (req.isPrimaryBore()) {
            boreRepository.findByOperatorIdAndActiveTrue(operatorId).forEach(b -> {
                b.setPrimaryBore(false);
                boreRepository.save(b);
            });
        }
        BoreLocation bore = boreRepository.save(BoreLocation.builder()
                .operator(op)
                .name(req.getName())
                .address(req.getAddress())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .primaryBore(req.isPrimaryBore())
                .active(true)
                .build());
        return toBore(bore);
    }

    public List<BoreResponse> listBores() {
        return boreRepository.findByOperatorIdAndActiveTrue(SecurityUtils.requireOperatorId())
                .stream().map(this::toBore).collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse addExpense(ExpenseRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        Tanker tanker = tankerRepository.findByIdAndOperatorId(req.getTankerId(), operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tanker not found"));
        UserAccount by = userAccountRepository.findById(SecurityUtils.currentUser().getId()).orElse(null);
        Expense e = expenseRepository.save(Expense.builder()
                .operator(op)
                .tanker(tanker)
                .type(req.getType())
                .amount(req.getAmount())
                .expenseDate(req.getExpenseDate() != null ? req.getExpenseDate() : LocalDate.now())
                .description(req.getDescription())
                .recordedBy(by)
                .build());
        return toExpense(e);
    }

    public List<ExpenseResponse> listExpenses() {
        return expenseRepository.findByOperatorIdOrderByExpenseDateDesc(SecurityUtils.requireOperatorId())
                .stream().map(this::toExpense).collect(Collectors.toList());
    }

    @Transactional
    public BoreExpenseResponse addBoreExpense(BoreExpenseRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        BoreLocation bore = boreRepository.findByIdAndOperatorId(req.getBoreId(), operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Bore not found"));
        UserAccount by = userAccountRepository.findById(SecurityUtils.currentUser().getId()).orElse(null);
        BoreExpense e = boreExpenseRepository.save(BoreExpense.builder()
                .operator(op)
                .bore(bore)
                .type(req.getType())
                .amount(req.getAmount())
                .expenseDate(req.getExpenseDate() != null ? req.getExpenseDate() : LocalDate.now())
                .description(req.getDescription())
                .recordedBy(by)
                .build());
        return toBoreExpense(e);
    }

    public List<BoreExpenseResponse> listBoreExpenses() {
        return boreExpenseRepository.findByOperatorIdOrderByExpenseDateDesc(SecurityUtils.requireOperatorId())
                .stream().map(this::toBoreExpense).collect(Collectors.toList());
    }

    @Transactional
    public SalaryResponse addSalary(SalaryRequest req) {
        Long operatorId = SecurityUtils.requireOperatorId();
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        Driver driver = driverRepository.findByIdAndOperatorId(req.getDriverId(), operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        BigDecimal bonus = req.getBonus() != null ? req.getBonus() : BigDecimal.ZERO;
        BigDecimal deductions = req.getDeductions() != null ? req.getDeductions() : BigDecimal.ZERO;
        BigDecimal net = req.getBaseAmount().add(bonus).subtract(deductions);

        DriverSalary salary = salaryRepository.findByDriverIdAndSalaryMonth(driver.getId(), req.getSalaryMonth())
                .orElse(DriverSalary.builder().operator(op).driver(driver).salaryMonth(req.getSalaryMonth()).build());
        salary.setBaseAmount(req.getBaseAmount());
        salary.setBonus(bonus);
        salary.setDeductions(deductions);
        salary.setNetAmount(net);
        salary.setNotes(req.getNotes());
        if (req.isMarkPaid()) {
            salary.setPaid(true);
            salary.setPaidAt(Instant.now());
        }
        return toSalary(salaryRepository.save(salary));
    }

    @Transactional(readOnly = true)
    public List<SalaryResponse> listSalaries() {
        return salaryRepository.findByOperatorIdOrderBySalaryMonthDesc(SecurityUtils.requireOperatorId())
                .stream().map(this::toSalary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Long operatorId = SecurityUtils.requireOperatorId();
        List<Tanker> tankers = tankerRepository.findByOperatorIdAndActiveTrue(operatorId);
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to = LocalDate.now();
        return DashboardResponse.builder()
                .totalTankers(tankers.size())
                .availableTankers(tankers.stream().filter(t -> t.getStatus() == TankerStatus.AVAILABLE).count())
                .onTripTankers(tankers.stream().filter(t -> t.getStatus() == TankerStatus.ON_TRIP).count())
                .totalDrivers(driverRepository.findByOperatorIdAndActiveTrue(operatorId).size())
                .activeTrips(tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.EN_ROUTE).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.ASSIGNED).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.GOING_FOR_LOADING).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.LOADING).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.LOADING_COMPLETED).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.ARRIVED).size()
                        + tripRepository.findByOperatorIdAndStatus(operatorId, TripStatus.UNLOADING).size())
                .completedTrips(tripRepository.countByOperatorIdAndStatus(operatorId, TripStatus.COMPLETED))
                .totalExpenses(expenseRepository.sumByOperatorAndDateRange(operatorId, from, to))
                .totalBoreExpenses(boreExpenseRepository.sumByOperatorAndDateRange(operatorId, from, to))
                .build();
    }

    public List<VehicleReportResponse> vehicleReports() {
        Long operatorId = SecurityUtils.requireOperatorId();
        return tankerRepository.findByOperatorIdAndActiveTrue(operatorId).stream()
                .map(t -> VehicleReportResponse.builder()
                        .tankerId(t.getId())
                        .vehicleNumber(t.getVehicleNumber())
                        .completedTrips(tripRepository.countByTankerIdAndStatus(t.getId(), TripStatus.COMPLETED))
                        .totalExpenses(expenseRepository.sumByTanker(t.getId()))
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private TankerResponse toTanker(Tanker t) {
        return TankerResponse.builder()
                .id(t.getId()).vehicleNumber(t.getVehicleNumber()).model(t.getModel())
                .capacityLitres(t.getCapacityLitres()).status(t.getStatus())
                .lastKnownLat(t.getLastKnownLat()).lastKnownLng(t.getLastKnownLng())
                .active(t.isActive()).build();
    }

    private DriverResponse toDriver(Driver d) {
        return DriverResponse.builder()
                .id(d.getId()).userId(d.getUser().getId())
                .fullName(d.getUser().getFullName()).phone(d.getUser().getPhone())
                .licenseNumber(d.getLicenseNumber()).monthlySalary(d.getMonthlySalary())
                .totalTripsCompleted(d.getTotalTripsCompleted()).performanceScore(d.getPerformanceScore())
                .available(d.isAvailable()).active(d.isActive()).build();
    }

    private CustomerResponse toCustomer(Customer c) {
        List<CustomerLocationResponse> locs = customerLocationRepository
                .findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(c.getId())
                .stream().map(this::toLocation).collect(Collectors.toList());
        return CustomerResponse.builder()
                .id(c.getId()).name(c.getName()).phone(c.getPhone())
                .defaultAddress(c.getDefaultAddress()).defaultLat(c.getDefaultLat())
                .defaultLng(c.getDefaultLng())
                .locations(locs)
                .build();
    }

    private CustomerLocationResponse toLocation(CustomerLocation loc) {
        return CustomerLocationResponse.builder()
                .id(loc.getId())
                .label(loc.getLabel())
                .address(loc.getAddress())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .mapsLink(loc.getMapsLink())
                .build();
    }

    private BoreResponse toBore(BoreLocation b) {
        return BoreResponse.builder()
                .id(b.getId()).name(b.getName()).address(b.getAddress())
                .latitude(b.getLatitude()).longitude(b.getLongitude())
                .primaryBore(b.isPrimaryBore()).build();
    }

    private ExpenseResponse toExpense(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId()).tankerId(e.getTanker().getId())
                .vehicleNumber(e.getTanker().getVehicleNumber()).type(e.getType())
                .amount(e.getAmount()).expenseDate(e.getExpenseDate())
                .description(e.getDescription()).build();
    }

    private BoreExpenseResponse toBoreExpense(BoreExpense e) {
        return BoreExpenseResponse.builder()
                .id(e.getId()).boreId(e.getBore().getId()).boreName(e.getBore().getName())
                .type(e.getType()).amount(e.getAmount()).expenseDate(e.getExpenseDate())
                .description(e.getDescription()).build();
    }

    private SalaryResponse toSalary(DriverSalary s) {
        return SalaryResponse.builder()
                .id(s.getId()).driverId(s.getDriver().getId())
                .driverName(s.getDriver().getUser().getFullName())
                .salaryMonth(s.getSalaryMonth()).baseAmount(s.getBaseAmount())
                .bonus(s.getBonus()).deductions(s.getDeductions()).netAmount(s.getNetAmount())
                .paid(s.isPaid()).build();
    }
}
