package com.tankermanager.app.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Auth : Routes("auth")
    data object Admin : Routes("admin")
    data object Manager : Routes("manager")
    data object Driver : Routes("driver")
    data object Track : Routes("track?token={token}") {
        fun create(token: String? = null) =
            if (token.isNullOrBlank()) "track?token=" else "track?token=$token"
    }
    data object TripDetail : Routes("trip/{id}") {
        fun create(id: Long) = "trip/$id"
    }
}
