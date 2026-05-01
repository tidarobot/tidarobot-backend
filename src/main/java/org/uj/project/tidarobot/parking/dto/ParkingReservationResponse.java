package org.uj.project.tidarobot.parking.dto;

import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.Floor;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ParkingReservationResponse(
        Long id,
        String username,
        City city,
        Floor floor,
        LocalDate targetDate,
        LocalDateTime scheduledFor,
        ReservationStatus status,
        LocalDateTime createdAt
) {}
