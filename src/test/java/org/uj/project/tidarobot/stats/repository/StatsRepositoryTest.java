package org.uj.project.tidarobot.stats.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.uj.project.tidarobot.config.JpaTestConfig;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.Floor;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.stats.dto.DayRankingEntry;
import org.uj.project.tidarobot.stats.dto.FloorRankingEntry;
import org.uj.project.tidarobot.stats.dto.UserRankingEntry;
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
class StatsRepositoryTest {

    @Autowired StatsRepository statsRepository;
    @Autowired ParkingReservationRepository reservationRepository;
    @Autowired UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .username("user1").email("u1@test.com").passwordHash("hash")
                .role(Role.USER).status(Status.APPROVED).createdAt(LocalDateTime.now()).build());
        user2 = userRepository.save(User.builder()
                .username("user2").email("u2@test.com").passwordHash("hash")
                .role(Role.USER).status(Status.APPROVED).createdAt(LocalDateTime.now()).build());
    }

    // --- findDayRanking ---

    @Test
    void findDayRanking_noFilters_groupsByDateWithCorrectCounts() {
        LocalDate day1 = LocalDate.of(2026, 6, 1);
        LocalDate day2 = LocalDate.of(2026, 6, 2);

        save(user1, City.CRACOW, Floor.MINUS_1, day1, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.MINUS_1, day1, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.MINUS_1, day2, ReservationStatus.COMPLETED);

        List<DayRankingEntry> result = statsRepository.findDayRanking(null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(e -> {
            assertThat(e.targetDate()).isEqualTo(day1);
            assertThat(e.reservationCount()).isEqualTo(2L);
        });
        assertThat(result).anySatisfy(e -> {
            assertThat(e.targetDate()).isEqualTo(day2);
            assertThat(e.reservationCount()).isEqualTo(1L);
        });
    }

    @Test
    void findDayRanking_filterByCity_excludesOtherCities() {
        LocalDate day = LocalDate.of(2026, 6, 1);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user1, City.WARSAW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);

        List<DayRankingEntry> result = statsRepository.findDayRanking(City.CRACOW, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reservationCount()).isEqualTo(1L);
    }

    @Test
    void findDayRanking_filterByStatus_excludesOtherStatuses() {
        LocalDate day = LocalDate.of(2026, 6, 1);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.FAILED);

        List<DayRankingEntry> result = statsRepository.findDayRanking(null, ReservationStatus.COMPLETED, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reservationCount()).isEqualTo(1L);
    }

    @Test
    void findDayRanking_filterByDateRange_includesBoundaryDatesExcludesOutsideRange() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to   = LocalDate.of(2026, 6, 3);

        save(user1, City.CRACOW, Floor.MINUS_1, from.minusDays(1), ReservationStatus.COMPLETED); // before range
        save(user1, City.CRACOW, Floor.MINUS_1, from,              ReservationStatus.COMPLETED); // boundary
        save(user1, City.CRACOW, Floor.MINUS_1, to,                ReservationStatus.COMPLETED); // boundary
        save(user1, City.CRACOW, Floor.MINUS_1, to.plusDays(1),   ReservationStatus.COMPLETED); // after range

        List<DayRankingEntry> result = statsRepository.findDayRanking(null, null, from, to);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DayRankingEntry::targetDate)
                .containsExactlyInAnyOrder(from, to);
    }

    // --- findUserRanking ---

    @Test
    void findUserRanking_groupsByUserWithCorrectCounts() {
        LocalDate day = LocalDate.of(2026, 6, 1);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user2, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);

        List<UserRankingEntry> result = statsRepository.findUserRanking(null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(e -> {
            assertThat(e.username()).isEqualTo("user1");
            assertThat(e.reservationCount()).isEqualTo(2L);
        });
        assertThat(result).anySatisfy(e -> {
            assertThat(e.username()).isEqualTo("user2");
            assertThat(e.reservationCount()).isEqualTo(1L);
        });
    }

    // --- findFloorRanking ---

    @Test
    void findFloorRanking_groupsByFloorWithCorrectCounts() {
        LocalDate day = LocalDate.of(2026, 6, 1);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.MINUS_1, day, ReservationStatus.COMPLETED);
        save(user1, City.CRACOW, Floor.OUTDOOR, day, ReservationStatus.COMPLETED);

        List<FloorRankingEntry> result = statsRepository.findFloorRanking(null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(e -> {
            assertThat(e.floor()).isEqualTo(Floor.MINUS_1);
            assertThat(e.reservationCount()).isEqualTo(2L);
        });
        assertThat(result).anySatisfy(e -> {
            assertThat(e.floor()).isEqualTo(Floor.OUTDOOR);
            assertThat(e.reservationCount()).isEqualTo(1L);
        });
    }

    // --- helper ---

    private void save(User user, City city, Floor floor, LocalDate targetDate, ReservationStatus status) {
        reservationRepository.save(ParkingReservation.builder()
                .user(user)
                .city(city)
                .floor(floor)
                .targetDate(targetDate)
                .scheduledFor(LocalDateTime.now())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
