package org.uj.project.tidarobot.parking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.exception.InvalidReservationException;
import org.uj.project.tidarobot.exception.UserNotFoundException;
import org.uj.project.tidarobot.parking.dto.ParkingReservationRequest;
import org.uj.project.tidarobot.parking.dto.ParkingReservationResponse;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.user.entity.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingReservationService {

    private final ParkingReservationRepository reservationRepository;

    @Value("${parking.days-before}")
    private int daysBefore;

    @Value("${parking.trigger-hour}")
    private int triggerHour;

    public ParkingReservationResponse createReservation(User user, ParkingReservationRequest request) {
        if (isWeekend(request.targetDate())) {
            throw new InvalidReservationException("Target date must be a working day (Monday–Friday)");
        }

        validateFloorForCity(request.city(), request.floor().name());

        LocalDate triggerDate = subtractWorkingDays(request.targetDate(), daysBefore);
        LocalDateTime scheduledFor = triggerDate.atTime(triggerHour, 0);

        if (!scheduledFor.isAfter(LocalDateTime.now())) {
            throw new InvalidReservationException(
                    "Registration window has already opened for this date (trigger time: " + scheduledFor + ")");
        }

        ParkingReservation reservation = ParkingReservation.builder()
                .user(user)
                .city(request.city())
                .floor(request.floor())
                .targetDate(request.targetDate())
                .scheduledFor(scheduledFor)
                .status(ReservationStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public List<ParkingReservationResponse> getUserReservations(User user) {
        return reservationRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void cancelReservation(User user, Long reservationId) {
        ParkingReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new UserNotFoundException("Reservation " + reservationId + " not found"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new InvalidReservationException("You can only cancel your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.SCHEDULED) {
            throw new InvalidReservationException(
                    "Only SCHEDULED reservations can be cancelled (current status: " + reservation.getStatus() + ")");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    private void validateFloorForCity(City city, String floor) {
        if (city == City.WARSAW) {
            throw new InvalidReservationException("Warsaw floor configuration is not yet available");
        }
        // Cracow supports all current Floor values — no additional check needed
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private LocalDate subtractWorkingDays(LocalDate date, int workingDays) {
        LocalDate result = date;
        int subtracted = 0;
        while (subtracted < workingDays) {
            result = result.minusDays(1);
            if (!isWeekend(result)) {
                subtracted++;
            }
        }
        return result;
    }

    private ParkingReservationResponse toResponse(ParkingReservation r) {
        return new ParkingReservationResponse(
                r.getId(),
                r.getUser().getUsername(),
                r.getCity(),
                r.getFloor(),
                r.getTargetDate(),
                r.getScheduledFor(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
