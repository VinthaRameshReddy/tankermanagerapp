package com.tankermanager.controller;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.service.FleetService;
import com.tankermanager.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/tankers")
    public List<TankerResponse> listTankers() {
        return fleetService.listTankers();
    }

    // Drivers
    @GetMapping("/drivers")
    public List<DriverResponse> listDrivers() {
        return fleetService.listDrivers();
    }

    @GetMapping("/drivers/available")
    public List<DriverResponse> availableDrivers() {
        return fleetService.availableDrivers();
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

    // Bores
    @PostMapping("/bores")
    @ResponseStatus(HttpStatus.CREATED)
    public BoreResponse addBore(@Valid @RequestBody BoreRequest request) {
        return fleetService.addBore(request);
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
    public List<TripResponse> listTrips() {
        return tripService.listForOperator();
    }

    @GetMapping("/trips/{id}")
    public TripResponse getTrip(@PathVariable Long id) {
        return tripService.getTrip(id);
    }

    @PatchMapping("/trips/{id}/status")
    public TripResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTripStatusRequest request) {
        return tripService.updateStatus(id, request, false);
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
}
