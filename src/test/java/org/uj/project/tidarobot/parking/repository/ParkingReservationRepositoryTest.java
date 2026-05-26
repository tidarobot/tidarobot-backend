package org.uj.project.tidarobot.parking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.uj.project.tidarobot.config.JpaTestConfig;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.Floor;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;
import org.uj.project.tidarobot.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JpaTestConfig.class)
@Transactional
class ParkingReservationRepositoryTest {

    @Autowired ParkingReservationRepository reservationRepository;
    @Autowired UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .username("user1").email("user1@test.com").passwordHash("hash")
                .role(Role.USER).status(Status.APPROVED).createdAt(LocalDateTime.now()).build());
        user2 = userRepository.save(User.builder()
                .username("user2").email("user2@test.com").passwordHash("hash")
                .role(Role.USER).status(Status.APPROVED).createdAt(LocalDateTime.now()).build());
    }

    // --- findTop5ByUserIdOrderByCreatedAtDesc ---

    @Test
    void findTop5_sixReservationsForUser_returnsOnlyFiveMostRecent() {
        for (int i = 1; i <= 6; i++) {
            reservationRepository.save(reservation(user1, ReservationStatus.SCHEDULED,
                    LocalDateTime.now().plusMinutes(i)));
        }
        reservationRepository.save(reservation(user2, ReservationStatus.SCHEDULED, LocalDateTime.now()));

        List<ParkingReservation> result = reservationRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user1.getId());

        assertThat(result).hasSize(5);
        assertThat(result).allMatch(r -> r.getUser().getId().equals(user1.getId()));
    }

    @Test
    void findTop5_orderedByCreatedAtDescending() {
        for (int i = 1; i <= 3; i++) {
            reservationRepository.save(reservation(user1, ReservationStatus.SCHEDULED,
                    LocalDateTime.now().plusMinutes(i)));
        }

        List<ParkingReservation> result = reservationRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user1.getId());

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getCreatedAt())
                    .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    // --- findAllByUserId ---

    @Test
    void findAllByUserId_returnsOnlyThatUsersReservations() {
        reservationRepository.save(reservation(user1, ReservationStatus.SCHEDULED, LocalDateTime.now()));
        reservationRepository.save(reservation(user1, ReservationStatus.COMPLETED, LocalDateTime.now()));
        reservationRepository.save(reservation(user2, ReservationStatus.SCHEDULED, LocalDateTime.now()));

        Page<ParkingReservation> result = reservationRepository
                .findAllByUserId(user1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(r -> r.getUser().getId().equals(user1.getId()));
    }

    @Test
    void findAllByUserId_paginationRespected() {
        for (int i = 0; i < 5; i++) {
            reservationRepository.save(reservation(user1, ReservationStatus.SCHEDULED, LocalDateTime.now()));
        }

        Page<ParkingReservation> page0 = reservationRepository
                .findAllByUserId(user1.getId(), PageRequest.of(0, 3));
        Page<ParkingReservation> page1 = reservationRepository
                .findAllByUserId(user1.getId(), PageRequest.of(1, 3));

        assertThat(page0.getContent()).hasSize(3);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
    }

    // --- findAllByStatusAndScheduledForBefore ---

    @Test
    void findAllByStatusAndScheduledForBefore_filtersCorrectlyByStatusAndTime() {
        LocalDateTime threshold = LocalDateTime.now();

        // should be returned: SCHEDULED + before threshold
        reservationRepository.save(reservationWithScheduledFor(
                user1, ReservationStatus.SCHEDULED, threshold.minusHours(1)));
        // should NOT be returned: SCHEDULED but after threshold
        reservationRepository.save(reservationWithScheduledFor(
                user1, ReservationStatus.SCHEDULED, threshold.plusHours(1)));
        // should NOT be returned: COMPLETED + before threshold
        reservationRepository.save(reservationWithScheduledFor(
                user1, ReservationStatus.COMPLETED, threshold.minusHours(1)));

        List<ParkingReservation> result = reservationRepository
                .findAllByStatusAndScheduledForBefore(ReservationStatus.SCHEDULED, threshold);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(ReservationStatus.SCHEDULED);
    }

    @Test
    void findAllByStatusAndScheduledForBefore_userIsJoinFetched() {
        reservationRepository.save(reservationWithScheduledFor(
                user1, ReservationStatus.SCHEDULED, LocalDateTime.now().minusMinutes(1)));

        List<ParkingReservation> result = reservationRepository
                .findAllByStatusAndScheduledForBefore(ReservationStatus.SCHEDULED, LocalDateTime.now());

        assertThat(result.getFirst().getUser().getUsername()).isEqualTo("user1");
    }

    // --- helpers ---

    private ParkingReservation reservation(User user, ReservationStatus status, LocalDateTime createdAt) {
        return ParkingReservation.builder()
                .user(user)
                .city(City.CRACOW)
                .floor(Floor.MINUS_1)
                .targetDate(LocalDate.now().plusDays(7))
                .scheduledFor(LocalDateTime.now().plusDays(1))
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private ParkingReservation reservationWithScheduledFor(User user, ReservationStatus status,
                                                           LocalDateTime scheduledFor) {
        return ParkingReservation.builder()
                .user(user)
                .city(City.CRACOW)
                .floor(Floor.MINUS_1)
                .targetDate(LocalDate.now().plusDays(7))
                .scheduledFor(scheduledFor)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
