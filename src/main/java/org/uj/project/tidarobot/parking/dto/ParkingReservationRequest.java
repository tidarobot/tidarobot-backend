package org.uj.project.tidarobot.parking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.Floor;

import java.time.LocalDate;

public record ParkingReservationRequest(
        @NotNull(message = "City is required")
        City city,

        @NotNull(message = "Floor is required")
        Floor floor,

        @NotNull(message = "Target date is required")
        @Future(message = "Target date must be in the future")
        LocalDate targetDate
) {}
