package org.uj.project.tidarobot.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParkingReservationRepository extends JpaRepository<ParkingReservation, Long> {

    List<ParkingReservation> findAllByUserId(Long userId);

    List<ParkingReservation> findAllByStatusAndScheduledForBefore(ReservationStatus status, LocalDateTime threshold);
}
