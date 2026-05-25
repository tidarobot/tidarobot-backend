package org.uj.project.tidarobot.parking.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.parking.service.ParkingBotService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSchedulerTest {

    @Mock ParkingReservationRepository reservationRepository;
    @Mock ParkingBotService parkingBotService;
    @InjectMocks ParkingScheduler scheduler;

    @Test
    void triggerDueReservations_noDueReservations_botNeverCalled() {
        when(reservationRepository.findAllByStatusAndScheduledForBefore(eq(ReservationStatus.SCHEDULED), any()))
                .thenReturn(List.of());

        scheduler.triggerDueReservations();

        verify(parkingBotService, never()).execute(any());
    }

    @Test
    void triggerDueReservations_twoDueReservations_botCalledOnceForEach() {
        ParkingReservation r1 = ParkingReservation.builder().id(1L).status(ReservationStatus.SCHEDULED).build();
        ParkingReservation r2 = ParkingReservation.builder().id(2L).status(ReservationStatus.SCHEDULED).build();

        when(reservationRepository.findAllByStatusAndScheduledForBefore(eq(ReservationStatus.SCHEDULED), any()))
                .thenReturn(List.of(r1, r2));

        scheduler.triggerDueReservations();

        verify(parkingBotService).execute(r1);
        verify(parkingBotService).execute(r2);
        verifyNoMoreInteractions(parkingBotService);
    }
}
