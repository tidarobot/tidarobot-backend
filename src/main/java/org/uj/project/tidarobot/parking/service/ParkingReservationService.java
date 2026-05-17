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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
        if (request.targetDate().isBefore(LocalDate.now())) {
            throw new InvalidReservationException("Cannot reserve a parking space for a past date");
        }

        validateFloorForCity(request.city(), request.floor().name());

        LocalDateTime scheduledFor = calculateScheduledFor(request.targetDate());

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

    // Trigger opens 6 calendar days before the target date at triggerHour.
    // If that moment is already past, the bot should fire immediately.
    private LocalDateTime calculateScheduledFor(LocalDate targetDate) {
        LocalDateTime triggerTime = targetDate.minusDays(6).atTime(triggerHour, 0);
        LocalDateTime now = LocalDateTime.now();
        return triggerTime.isAfter(now) ? triggerTime : now;
    }

    public List<ParkingReservationResponse> getLatestReservations(User user) {
        return reservationRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<ParkingReservationResponse> getReservationHistory(User user, Pageable pageable) {
        return switch (user.getRole()){
            case ADMIN -> reservationRepository.findAll(pageable).map(this::toResponse);
            case USER -> reservationRepository.findAllByUserId(user.getId(), pageable).map(this::toResponse);
        };
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
