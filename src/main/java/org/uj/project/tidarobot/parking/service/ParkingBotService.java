package org.uj.project.tidarobot.parking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingBotService {

    private final ParkingReservationRepository reservationRepository;

    public void execute(ParkingReservation reservation) {
        reservation.setStatus(ReservationStatus.IN_PROGRESS);
        reservationRepository.save(reservation);

        try {
            log.info("[MOCK] Executing parking bot — user: {}, city: {}, floor: {}, date: {}",
                    reservation.getUser().getUsername(),
                    reservation.getCity(),
                    reservation.getFloor(),
                    reservation.getTargetDate());

            // TODO: invoke Python script here, e.g.:
            // ProcessBuilder pb = new ProcessBuilder("python", "reserve.py", ...args);
            // pb.start().waitFor();

            reservation.setStatus(ReservationStatus.COMPLETED);
            log.info("[MOCK] Reservation {} completed successfully", reservation.getId());

        } catch (Exception e) {
            reservation.setStatus(ReservationStatus.FAILED);
            log.error("[MOCK] Reservation {} failed: {}", reservation.getId(), e.getMessage());
        }

        reservationRepository.save(reservation);
    }
}
