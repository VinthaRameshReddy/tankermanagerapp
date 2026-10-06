package com.tankermanager.app.data.model

data class AuthResponse(
    val token: String?,
    val userId: Long?,
    val fullName: String?,
    val phone: String?,
    val role: String?,
    val operatorId: Long?,
    val operatorName: String?,
    val mpinEnabled: Boolean? = false,
    val biometricEnabled: Boolean? = false,
    val securePayload: String? = null
)

data class LoginRequest(val phone: String, val password: String)

data class EncryptedPayloadRequest(val payload: String)

data class BiometricChallengeRequest(val phone: String, val deviceId: String)

data class BiometricChallengeResponse(
    val challengeId: String?,
    val nonce: String?,
    val expiresAt: String?,
    val securePayload: String? = null
)

data class BiometricRegisterResponse(
    val registered: Boolean? = null,
    val deviceId: String? = null,
    val biometricEnabled: Boolean? = null
)

data class CreateStaffRequest(
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val password: String,
    val role: String,
    val licenseNumber: String? = null,
    val monthlySalary: Double? = null
)

data class CreateOperatorRequest(
    val name: String,
    val code: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val plan: String? = "BASIC",
    val ownerName: String? = null,
    val ownerPhone: String,
    val ownerPassword: String
)

data class OperatorResponse(
    val id: Long?,
    val name: String?,
    val code: String?,
    val phone: String?,
    val email: String?,
    val plan: String?,
    val active: Boolean?,
    val subscriptionExpiresOn: String?
)

data class StaffResponse(
    val id: Long?,
    val fullName: String?,
    val phone: String?,
    val role: String?,
    val active: Boolean?
)

data class DashboardResponse(
    val totalTankers: Long = 0,
    val availableTankers: Long = 0,
    val onTripTankers: Long = 0,
    val totalDrivers: Long = 0,
    val activeTrips: Long = 0,
    val completedTrips: Long = 0,
    val totalExpenses: Double? = 0.0,
    val totalBoreExpenses: Double? = 0.0
)

data class TankerResponse(
    val id: Long,
    val vehicleNumber: String,
    val model: String?,
    val capacityLitres: Int?,
    val status: String?,
    val lastKnownLat: Double?,
    val lastKnownLng: Double?,
    val active: Boolean?
)

data class TankerRequest(
    val vehicleNumber: String,
    val model: String? = null,
    val capacityLitres: Int? = null
)

data class DriverResponse(
    val id: Long,
    val userId: Long?,
    val fullName: String?,
    val phone: String?,
    val licenseNumber: String?,
    val monthlySalary: Double?,
    val totalTripsCompleted: Int?,
    val performanceScore: Double?,
    val available: Boolean?,
    val active: Boolean?
)

data class CustomerLocationResponse(
    val id: Long,
    val label: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val mapsLink: String?,
    val tripRate: Double? = null
)

data class CustomerLocationRequest(
    val label: String? = null,
    val address: String? = null,
    val mapsLink: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tripRate: Double? = null
)

data class CustomerResponse(
    val id: Long,
    val name: String?,
    val phone: String?,
    val defaultAddress: String?,
    val defaultLat: Double?,
    val defaultLng: Double?,
    val locations: List<CustomerLocationResponse>? = null
)

data class CustomerRequest(
    val name: String,
    val phone: String,
    val defaultAddress: String? = null,
    val defaultLat: Double? = null,
    val defaultLng: Double? = null,
    val mapsLink: String? = null,
    val locationLabel: String? = null
)

data class BoreResponse(
    val id: Long,
    val name: String?,
    val address: String?,
    val mapsLink: String? = null,
    val latitude: Double?,
    val longitude: Double?,
    val primaryBore: Boolean?,
    val active: Boolean? = true
)

data class BoreRequest(
    val name: String,
    val address: String? = null,
    val mapsLink: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val primaryBore: Boolean = true
)

data class DriverStatusRequest(val active: Boolean)

data class SwapTripRequest(val otherTripId: Long)

data class DistancePreviewRequest(
    val boreId: Long? = null,
    val customerLocationId: Long? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    val mapsLink: String? = null
)

data class DistancePreviewResponse(
    val boreId: Long?,
    val boreName: String?,
    val distanceKm: Double?,
    val etaMinutes: Int?
)

data class BookTripRequest(
    val customerPhone: String,
    val customerName: String? = null,
    val tankerId: Long,
    val driverId: Long,
    val boreId: Long? = null,
    val customerLocationId: Long? = null,
    val dropAddress: String? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    val mapsLink: String? = null,
    val tripAmount: Double? = null,
    val notes: String? = null
)

data class StatusHistoryItem(
    val fromStatus: String?,
    val toStatus: String?,
    val note: String?,
    val smsSent: Boolean?,
    val changedAt: String?
)

data class TripResponse(
    val id: Long,
    val tripCode: String?,
    val status: String?,
    val queuePosition: Int? = null,
    val customerName: String?,
    val customerPhone: String?,
    val customerId: Long? = null,
    val customerLocationId: Long? = null,
    val tankerId: Long? = null,
    val tankerNumber: String?,
    val driverId: Long?,
    val driverName: String?,
    val driverPhone: String?,
    val boreId: Long? = null,
    val boreName: String?,
    val dropAddress: String?,
    val dropLat: Double?,
    val dropLng: Double?,
    val boreLat: Double?,
    val boreLng: Double?,
    val etaMinutes: Int?,
    val distanceKm: Double? = null,
    val mapsNavigateUrl: String? = null,
    val tripAmount: Double? = null,
    val amountPaid: Double? = null,
    val amountDue: Double? = null,
    val trackingToken: String?,
    val trackingEnabled: Boolean?,
    val assignedAt: String?,
    val startedAt: String?,
    val completedAt: String?,
    val createdAt: String? = null,
    val notes: String?,
    val history: List<StatusHistoryItem>? = null
)

data class TripPaymentRequest(
    val amount: Double,
    val note: String? = null
)

data class UpdateTripAmountRequest(
    val tripAmount: Double
)

data class TripDueItem(
    val tripId: Long?,
    val tripCode: String?,
    val dropAddress: String?,
    val status: String?,
    val tripAmount: Double?,
    val amountPaid: Double?,
    val amountDue: Double?
)

data class CustomerDuesResponse(
    val customerId: Long?,
    val customerName: String?,
    val customerPhone: String?,
    val totalBilled: Double?,
    val totalPaid: Double?,
    val totalDue: Double?,
    val trips: List<TripDueItem>? = null
)

data class UpdateTripStatusRequest(val status: String, val note: String? = null)

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float? = null,
    val heading: Float? = null
)

data class TrackingResponse(
    val tripCode: String?,
    val status: String?,
    val trackingEnabled: Boolean?,
    val etaMinutes: Int?,
    val vehicleLat: Double?,
    val vehicleLng: Double?,
    val dropLat: Double?,
    val dropLng: Double?,
    val dropAddress: String?,
    val message: String?
)

data class ExpenseRequest(
    val tankerId: Long,
    val type: String,
    val amount: Double,
    val expenseDate: String? = null,
    val description: String? = null
)

data class ExpenseResponse(
    val id: Long,
    val tankerId: Long?,
    val vehicleNumber: String?,
    val type: String?,
    val amount: Double?,
    val expenseDate: String?,
    val description: String?
)

data class BoreExpenseRequest(
    val boreId: Long,
    val type: String,
    val amount: Double,
    val expenseDate: String? = null,
    val description: String? = null
)

data class BoreExpenseResponse(
    val id: Long,
    val boreId: Long?,
    val boreName: String?,
    val type: String?,
    val amount: Double?,
    val expenseDate: String?,
    val description: String?
)

data class SalaryRequest(
    val driverId: Long,
    val salaryMonth: String,
    val baseAmount: Double,
    val bonus: Double? = 0.0,
    val deductions: Double? = 0.0,
    val notes: String? = null,
    val markPaid: Boolean = false
)

data class SalaryResponse(
    val id: Long,
    val driverId: Long?,
    val driverName: String?,
    val salaryMonth: String?,
    val baseAmount: Double?,
    val bonus: Double?,
    val deductions: Double?,
    val netAmount: Double?,
    val paid: Boolean?
)

data class VehicleReportResponse(
    val tankerId: Long?,
    val vehicleNumber: String?,
    val completedTrips: Long?,
    val totalExpenses: Double?,
    val status: String?
)

data class ApiError(val error: String?, val message: String?, val status: Int?)
