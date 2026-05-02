package org.uj.project.tidarobot.parking.entity;

public enum ReservationStatus {
    SCHEDULED,    // waiting for the trigger time
    IN_PROGRESS,  // bot is currently running
    COMPLETED,    // spot successfully booked
    WAITLISTED,   // no spot available; bot entered the Tidaro reserve list
    FAILED,       // bot execution failed
    CANCELLED     // cancelled by the user before trigger
}
