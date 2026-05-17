package org.uj.project.tidarobot.parking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParkingReservationRepository extends JpaRepository<ParkingReservation, Long> {

    List<ParkingReservation> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ParkingReservation> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM ParkingReservation r JOIN FETCH r.user WHERE r.status = :status AND r.scheduledFor < :threshold")
    List<ParkingReservation> findAllByStatusAndScheduledForBefore(@Param("status") ReservationStatus status, @Param("threshold") LocalDateTime threshold);
}
