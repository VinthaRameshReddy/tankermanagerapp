package com.tankermanager.app.data.api

import com.tankermanager.app.data.model.*
import retrofit2.http.*

interface TankerApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(): AuthResponse

    @POST("api/auth/staff")
    suspend fun createStaff(@Body body: CreateStaffRequest): AuthResponse

    @GET("api/admin/operators")
    suspend fun listOperators(): List<OperatorResponse>

    @POST("api/admin/operators")
    suspend fun createOperator(@Body body: CreateOperatorRequest): OperatorResponse

    @GET("api/manager/dashboard")
    suspend fun dashboard(): DashboardResponse

    @GET("api/manager/managers")
    suspend fun managers(): List<StaffResponse>

    @GET("api/manager/reports/vehicles")
    suspend fun vehicleReports(): List<VehicleReportResponse>

    @GET("api/manager/tankers")
    suspend fun tankers(): List<TankerResponse>

    @POST("api/manager/tankers")
    suspend fun addTanker(@Body body: TankerRequest): TankerResponse

    @GET("api/manager/drivers")
    suspend fun drivers(): List<DriverResponse>

    @GET("api/manager/drivers/available")
    suspend fun availableDrivers(): List<DriverResponse>

    @GET("api/manager/customers")
    suspend fun customers(): List<CustomerResponse>

    @POST("api/manager/customers")
    suspend fun upsertCustomer(@Body body: CustomerRequest): CustomerResponse

    @GET("api/manager/customers/by-phone/{phone}")
    suspend fun customerByPhone(@Path("phone") phone: String): CustomerResponse

    @GET("api/manager/bores")
    suspend fun bores(): List<BoreResponse>

    @POST("api/manager/bores")
    suspend fun addBore(@Body body: BoreRequest): BoreResponse

    @GET("api/manager/trips")
    suspend fun trips(): List<TripResponse>

    @GET("api/manager/trips/{id}")
    suspend fun trip(@Path("id") id: Long): TripResponse

    @POST("api/manager/trips")
    suspend fun bookTrip(@Body body: BookTripRequest): TripResponse

    @PATCH("api/manager/trips/{id}/status")
    suspend fun managerUpdateStatus(
        @Path("id") id: Long,
        @Body body: UpdateTripStatusRequest
    ): TripResponse

    @GET("api/manager/expenses")
    suspend fun expenses(): List<ExpenseResponse>

    @POST("api/manager/expenses")
    suspend fun addExpense(@Body body: ExpenseRequest): ExpenseResponse

    @GET("api/manager/bore-expenses")
    suspend fun boreExpenses(): List<BoreExpenseResponse>

    @POST("api/manager/bore-expenses")
    suspend fun addBoreExpense(@Body body: BoreExpenseRequest): BoreExpenseResponse

    @GET("api/manager/salaries")
    suspend fun salaries(): List<SalaryResponse>

    @POST("api/manager/salaries")
    suspend fun addSalary(@Body body: SalaryRequest): SalaryResponse

    @GET("api/driver/trips")
    suspend fun driverTrips(): List<TripResponse>

    @GET("api/driver/trips/active")
    suspend fun driverActiveTrips(): List<TripResponse>

    @PATCH("api/driver/trips/{id}/status")
    suspend fun driverUpdateStatus(
        @Path("id") id: Long,
        @Body body: UpdateTripStatusRequest
    ): TripResponse

    @POST("api/driver/trips/{id}/location")
    suspend fun updateLocation(
        @Path("id") id: Long,
        @Body body: LocationUpdateRequest
    )

    @GET("api/public/track/{token}")
    suspend fun track(@Path("token") token: String): TrackingResponse
}
