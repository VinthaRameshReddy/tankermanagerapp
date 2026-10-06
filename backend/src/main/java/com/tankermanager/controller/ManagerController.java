package com.tankermanager.controller;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.service.FleetService;
import com.tankermanager.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final FleetService fleetService;
    private final TripService tripService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return fleetService.dashboard();
    }

    @GetMapping("/reports/vehicles")
    public List<VehicleReportResponse> vehicleReports() {
        return fleetService.vehicleReports();
    }

    // Tankers
    @PostMapping("/tankers")
    @ResponseStatus(HttpStatus.CREATED)
    public TankerResponse addTanker(@Valid @RequestBody TankerRequest request) {
        return fleetService.addTanker(request);
    }

    @PatchMapping("/tankers/{id}")
    public TankerResponse updateTanker(@PathVariable Long id, @Valid @RequestBody TankerRequest request) {
        return fleetService.updateTanker(id, request);
    }

    @DeleteMapping("/tankers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTanker(@PathVariable Long id) {
        fleetService.deleteTanker(id);
    }

    @GetMapping("/tankers")
    public List<TankerResponse> listTankers() {
        return fleetService.listTankers();
    }

    // Drivers
    @GetMapping("/drivers")
    public List<DriverResponse> listDrivers(
            @RequestParam(defaultValue = "false") boolean includeResigned) {
        return fleetService.listDrivers(includeResigned);
    }

    @GetMapping("/drivers/available")
    public List<DriverResponse> availableDrivers() {
        return fleetService.availableDrivers();
    }

    @PatchMapping("/drivers/{id}/status")
    public DriverResponse setDriverStatus(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusRequest request) {
        return fleetService.setDriverActive(id, Boolean.TRUE.equals(request.getActive()));
    }

    @GetMapping("/managers")
    public List<StaffResponse> listManagers() {
        return fleetService.listManagers();
    }

    // Customers (Owner + Manager)
    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse upsertCustomer(@Valid @RequestBody CustomerRequest request) {
        return fleetService.upsertCustomer(request);
    }

    @GetMapping("/customers")
    public List<CustomerResponse> listCustomers() {
        return fleetService.listCustomers();
    }

    @GetMapping("/customers/by-phone/{phone}")
    public CustomerResponse findCustomer(@PathVariable String phone) {
        return fleetService.findByPhone(phone);
    }

    @GetMapping("/customers/{id}/locations")
    public List<CustomerLocationResponse> listCustomerLocations(@PathVariable Long id) {
        return fleetService.listCustomerLocations(id);
    }

    @PostMapping("/customers/{id}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerLocationResponse addCustomerLocation(
            @PathVariable Long id,
            @RequestBody CustomerLocationRequest request) {
        return fleetService.addCustomerLocation(id, request);
    }

    @PatchMapping("/customers/{id}/locations/{locationId}")
    public CustomerLocationResponse updateCustomerLocation(
            @PathVariable Long id,
            @PathVariable Long locationId,
            @RequestBody CustomerLocationRequest request) {
        return fleetService.updateCustomerLocation(id, locationId, request);
    }

    @GetMapping("/customers/{id}/dues")
    public CustomerDuesResponse customerDues(@PathVariable Long id) {
        return tripService.customerDues(id);
    }

    // Bores
    @PostMapping("/bores")
    @ResponseStatus(HttpStatus.CREATED)
    public BoreResponse addBore(@Valid @RequestBody BoreRequest request) {
        return fleetService.addBore(request);
    }

    @PatchMapping("/bores/{id}")
    public BoreResponse updateBore(@PathVariable Long id, @Valid @RequestBody BoreRequest request) {
        return fleetService.updateBore(id, request);
    }

    @DeleteMapping("/bores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBore(@PathVariable Long id) {
        fleetService.deleteBore(id);
    }

    @GetMapping("/bores")
    public List<BoreResponse> listBores() {
        return fleetService.listBores();
    }

    // Trips
    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse bookTrip(@Valid @RequestBody BookTripRequest request) {
        return tripService.bookTrip(request);
    }

    @GetMapping("/trips")
    public List<TripResponse> listTrips(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String tankerIds) {
        List<Long> ids = parseIds(tankerIds);
        return tripService.listForOperator(period, ids);
    }

    @GetMapping("/trips/{id}")
    public TripResponse getTrip(@PathVariable Long id) {
        return tripService.getTrip(id);
    }

    @PostMapping("/trips/{id}/swap")
    public List<TripResponse> swapTrips(
            @PathVariable Long id,
            @Valid @RequestBody SwapTripRequest request) {
        return tripService.swapTrips(id, request.getOtherTripId());
    }

    @PostMapping("/trips/distance-preview")
    public DistancePreviewResponse distancePreview(@RequestBody DistancePreviewRequest request) {
        return tripService.previewDistance(request);
    }

    @PatchMapping("/trips/{id}/status")
    public TripResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTripStatusRequest request) {
        return tripService.updateStatus(id, request, false);
    }

    @PostMapping("/trips/{id}/payments")
    public TripResponse recordPayment(@PathVariable Long id, @Valid @RequestBody TripPaymentRequest request) {
        return tripService.recordPayment(id, request);
    }

    @PatchMapping("/trips/{id}/amount")
    public TripResponse updateTripAmount(@PathVariable Long id, @Valid @RequestBody UpdateTripAmountRequest request) {
        return tripService.updateTripAmount(id, request.getTripAmount());
    }

    // Expenses
    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseRequest request) {
        return fleetService.addExpense(request);
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> listExpenses() {
        return fleetService.listExpenses();
    }

    @PostMapping("/bore-expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public BoreExpenseResponse addBoreExpense(@Valid @RequestBody BoreExpenseRequest request) {
        return fleetService.addBoreExpense(request);
    }

    @GetMapping("/bore-expenses")
    public List<BoreExpenseResponse> listBoreExpenses() {
        return fleetService.listBoreExpenses();
    }

    // Salaries
    @PostMapping("/salaries")
    @ResponseStatus(HttpStatus.CREATED)
    public SalaryResponse addSalary(@Valid @RequestBody SalaryRequest request) {
        return fleetService.addSalary(request);
    }

    @GetMapping("/salaries")
    public List<SalaryResponse> listSalaries() {
        return fleetService.listSalaries();
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
