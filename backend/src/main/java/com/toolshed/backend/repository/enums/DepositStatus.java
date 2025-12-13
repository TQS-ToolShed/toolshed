package com.toolshed.backend.repository.enums;

/**
 * Represents the status of a security deposit for a booking.
 */
public enum DepositStatus {
    NOT_REQUIRED, // No damage reported, no deposit needed
    REQUIRED, // Damage reported, deposit must be paid
    PAID // Deposit has been paid by the renter
}
