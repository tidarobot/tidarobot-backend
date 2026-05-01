package org.uj.project.tidarobot.parking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.parking.service.ParkingBotService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingScheduler {

    private final ParkingReservationRepository reservationRepository;
    private final ParkingBotService parkingBotService;

    @Scheduled(fixedDelay = 60_000)
    public void triggerDueReservations() {
        List<ParkingReservation> due = reservationRepository
                .findAllByStatusAndScheduledForBefore(ReservationStatus.SCHEDULED, LocalDateTime.now());

        if (!due.isEmpty()) {
            log.info("Found {} reservation(s) due for execution", due.size());
        }

        for (ParkingReservation reservation : due) {
            parkingBotService.execute(reservation);
        }
    }
}
