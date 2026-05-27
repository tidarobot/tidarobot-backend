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

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.uj.project.tidarobot.config.CacheConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParkingReservationService {

    // Calendar days to subtract from the target date to get the trigger date.
    // Friday opens on Monday (4 days before); all other weekdays open on their
    // preceding Tuesday/Wednesday/Thursday/Friday, which spans the weekend (+6 days).
    private static final Map<DayOfWeek, Integer> TRIGGER_OFFSET_BY_TARGET_DAY = Map.of(
            DayOfWeek.MONDAY,    6,
            DayOfWeek.TUESDAY,   6,
            DayOfWeek.WEDNESDAY, 6,
            DayOfWeek.THURSDAY,  6,
            DayOfWeek.FRIDAY,    4,
            DayOfWeek.SATURDAY,  4,
            DayOfWeek.SUNDAY,    5
    );

    private final ParkingReservationRepository reservationRepository;

    @Value("${parking.trigger-hour}")
    private int triggerHour;

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.STATS_DAYS,   allEntries = true),
            @CacheEvict(value = CacheConfig.STATS_USERS,  allEntries = true),
            @CacheEvict(value = CacheConfig.STATS_FLOORS, allEntries = true),
            @CacheEvict(value = CacheConfig.USER_LATEST_RESERVATIONS, key = "#user.id")
    })
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

    private LocalDateTime calculateScheduledFor(LocalDate targetDate) {
        int offset = TRIGGER_OFFSET_BY_TARGET_DAY.get(targetDate.getDayOfWeek());
        LocalDateTime triggerTime = targetDate.minusDays(offset).atTime(triggerHour, 0);
        LocalDateTime now = LocalDateTime.now();
        return triggerTime.isAfter(now) ? triggerTime : now;
    }

    @Cacheable(value = CacheConfig.USER_LATEST_RESERVATIONS, key = "#user.id")
    public List<ParkingReservationResponse> getLatestReservations(User user) {
        return new ArrayList<>(reservationRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList());
    }

    public Page<ParkingReservationResponse> getReservationHistory(User user, Pageable pageable) {
        return switch (user.getRole()){
            case ADMIN -> reservationRepository.findAll(pageable).map(this::toResponse);
            case USER -> reservationRepository.findAllByUserId(user.getId(), pageable).map(this::toResponse);
        };
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.STATS_DAYS,   allEntries = true),
            @CacheEvict(value = CacheConfig.STATS_USERS,  allEntries = true),
            @CacheEvict(value = CacheConfig.STATS_FLOORS, allEntries = true),
            @CacheEvict(value = CacheConfig.USER_LATEST_RESERVATIONS, key = "#user.id")
    })
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
