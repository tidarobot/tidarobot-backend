package org.uj.project.tidarobot.parking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.uj.project.tidarobot.exception.InvalidReservationException;
import org.uj.project.tidarobot.exception.UserNotFoundException;
import org.uj.project.tidarobot.parking.dto.ParkingReservationRequest;
import org.uj.project.tidarobot.parking.dto.ParkingReservationResponse;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.Floor;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingReservationServiceTest {

    @Mock ParkingReservationRepository reservationRepository;
    @InjectMocks ParkingReservationService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "daysBefore", 6);
        ReflectionTestUtils.setField(service, "triggerHour", 16);

        user = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .status(Status.APPROVED)
                .build();
    }

    // --- createReservation ---

    @Test
    void createReservation_pastDate_throwsInvalidReservationException() {
        ParkingReservationRequest req = new ParkingReservationRequest(
                City.CRACOW, Floor.MINUS_1, LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.createReservation(user, req))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("past date");
    }

    @Test
    void createReservation_warsawCity_throwsInvalidReservationException() {
        ParkingReservationRequest req = new ParkingReservationRequest(
                City.WARSAW, Floor.MINUS_1, LocalDate.now().plusDays(10));

        assertThatThrownBy(() -> service.createReservation(user, req))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("Warsaw");
    }

    @Test
    void createReservation_triggerTimeInFuture_scheduledForEqualsCalculatedTime() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        LocalDateTime expectedTrigger = targetDate.minusDays(6).atTime(16, 0);

        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ParkingReservation r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ParkingReservationResponse response = service.createReservation(user,
                new ParkingReservationRequest(City.CRACOW, Floor.MINUS_1, targetDate));

        assertThat(response.scheduledFor()).isEqualTo(expectedTrigger);
    }

    @Test
    void createReservation_triggerTimeInPast_scheduledForFallsBackToNow() {
        // targetDate = today → trigger would have been 6 days ago → falls back to now
        LocalDate targetDate = LocalDate.now();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ParkingReservation r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ParkingReservationResponse response = service.createReservation(user,
                new ParkingReservationRequest(City.CRACOW, Floor.MINUS_1, targetDate));

        assertThat(response.scheduledFor())
                .isAfter(before)
                .isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void createReservation_happyPath_savesAndReturnsCorrectResponse() {
        LocalDate targetDate = LocalDate.now().plusDays(30);

        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ParkingReservation r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        ParkingReservationResponse response = service.createReservation(user,
                new ParkingReservationRequest(City.CRACOW, Floor.MINUS_2, targetDate));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.city()).isEqualTo(City.CRACOW);
        assertThat(response.floor()).isEqualTo(Floor.MINUS_2);
        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.status()).isEqualTo(ReservationStatus.SCHEDULED);
        verify(reservationRepository).save(any(ParkingReservation.class));
    }

    // --- cancelReservation ---

    @Test
    void cancelReservation_reservationNotFound_throwsUserNotFoundException() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelReservation(user, 99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void cancelReservation_belongsToAnotherUser_throwsInvalidReservationException() {
        User other = User.builder().id(2L).username("other").role(Role.USER).status(Status.APPROVED).build();
        ParkingReservation reservation = ParkingReservation.builder()
                .id(1L).user(other).status(ReservationStatus.SCHEDULED).build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancelReservation(user, 1L))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("own reservations");
    }

    @Test
    void cancelReservation_statusNotScheduled_throwsInvalidReservationException() {
        ParkingReservation reservation = ParkingReservation.builder()
                .id(1L).user(user).status(ReservationStatus.COMPLETED).build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancelReservation(user, 1L))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("SCHEDULED");
    }

    @Test
    void cancelReservation_happyPath_setsStatusCancelledAndSaves() {
        ParkingReservation reservation = ParkingReservation.builder()
                .id(1L).user(user).status(ReservationStatus.SCHEDULED).build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelReservation(user, 1L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }

    // --- getLatestReservations ---

    @Test
    void getLatestReservations_delegatesToRepositoryWithUserId() {
        when(reservationRepository.findTop5ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<ParkingReservationResponse> result = service.getLatestReservations(user);

        assertThat(result).isEmpty();
        verify(reservationRepository).findTop5ByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getLatestReservations_mapsReservationsToResponse() {
        LocalDate targetDate = LocalDate.now().plusDays(5);
        LocalDateTime now = LocalDateTime.now();
        ParkingReservation r = ParkingReservation.builder()
                .id(10L)
                .user(user)
                .city(City.CRACOW)
                .floor(Floor.MINUS_1)
                .targetDate(targetDate)
                .scheduledFor(now)
                .status(ReservationStatus.SCHEDULED)
                .createdAt(now)
                .build();

        when(reservationRepository.findTop5ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r));

        List<ParkingReservationResponse> result = service.getLatestReservations(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).username()).isEqualTo("testuser");
        assertThat(result.get(0).city()).isEqualTo(City.CRACOW);
        assertThat(result.get(0).floor()).isEqualTo(Floor.MINUS_1);
        assertThat(result.get(0).targetDate()).isEqualTo(targetDate);
        assertThat(result.get(0).status()).isEqualTo(ReservationStatus.SCHEDULED);
    }

    // --- getReservationHistory ---

    @Test
    void getReservationHistory_userRole_queriesOnlyOwnReservations() {
        when(reservationRepository.findAllByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getReservationHistory(user, PageRequest.of(0, 10));

        verify(reservationRepository).findAllByUserId(eq(1L), any(Pageable.class));
        verify(reservationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getReservationHistory_adminRole_queriesAllReservations() {
        User admin = User.builder().id(2L).username("admin").role(Role.ADMIN).status(Status.APPROVED).build();
        when(reservationRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.getReservationHistory(admin, PageRequest.of(0, 10));

        verify(reservationRepository).findAll(any(Pageable.class));
        verify(reservationRepository, never()).findAllByUserId(any(), any());
    }
}
