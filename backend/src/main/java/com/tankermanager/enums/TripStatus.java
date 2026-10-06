package com.tankermanager.enums;

public enum TripStatus {
    /** Waiting in tanker queue — not started until previous trip finishes. */
    QUEUED,
    ASSIGNED,
    GOING_FOR_LOADING,
    LOADING,
    LOADING_COMPLETED,
    EN_ROUTE,
    ARRIVED,
    UNLOADING,
    COMPLETED,
    CANCELLED
}
