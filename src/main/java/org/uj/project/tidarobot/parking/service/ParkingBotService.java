package org.uj.project.tidarobot.parking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.parking.entity.Floor;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.parking.repository.ParkingReservationRepository;
import org.uj.project.tidarobot.security.EncryptionService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingBotService {

    private static final Map<Floor, String> CRACOW_FLOOR_TO_AREA = Map.of(
            Floor.MINUS_1, "CIB_Kraków -1",
            Floor.MINUS_2, "CIB_Kraków -2",
            Floor.OUTDOOR, "CIB_Kraków_Outdoor"
    );

    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");

    @Value("${parking.python-executable:python3}")
    private String pythonExecutable;

    @Value("${parking.python-script-path}")
    private String scriptPath;

    private final ParkingReservationRepository reservationRepository;
    private final EncryptionService encryptionService;

    public void execute(ParkingReservation reservation) {
        reservation.setStatus(ReservationStatus.IN_PROGRESS);
        reservationRepository.save(reservation);

        try {
            String parkingArea = CRACOW_FLOOR_TO_AREA.get(reservation.getFloor());
            String day   = reservation.getTargetDate().format(DAY_FMT);
            String month = reservation.getTargetDate().format(MONTH_FMT);

            log.info("Executing parking bot — user: {}, city: {}, floor: {}, date: {}",
                    reservation.getUser().getUsername(),
                    reservation.getCity(),
                    reservation.getFloor(),
                    reservation.getTargetDate());

            String encryptedTidaroPassword = reservation.getUser().getPasswordTidaro();
            String decryptedTidaroPassword = encryptionService.decrypt(encryptedTidaroPassword);


            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable, scriptPath,
                    "--email",        reservation.getUser().getLoginTidaro(),
                    "--password",     decryptedTidaroPassword,
                    "--parking-area", parkingArea,
                    "--day",          day,
                    "--month",        month
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> log.info("[bot] {}", line));
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                reservation.setStatus(ReservationStatus.COMPLETED);
                log.info("Reservation {} completed — spot booked", reservation.getId());
            } else if (exitCode == 1) {
                reservation.setStatus(ReservationStatus.WAITLISTED);
                log.info("Reservation {} waitlisted — no free spots", reservation.getId());
            } else {
                reservation.setStatus(ReservationStatus.FAILED);
                log.error("Reservation {} failed — bot exited with code {}", reservation.getId(), exitCode);
            }

        } catch (Exception e) {
            reservation.setStatus(ReservationStatus.FAILED);
            log.error("Reservation {} failed: {}", reservation.getId(), e.getMessage());
        }

        reservationRepository.save(reservation);
    }
}
