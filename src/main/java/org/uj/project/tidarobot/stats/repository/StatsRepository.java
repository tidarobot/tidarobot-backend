package org.uj.project.tidarobot.stats.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.ParkingReservation;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.stats.dto.DayRankingEntry;
import org.uj.project.tidarobot.stats.dto.FloorRankingEntry;
import org.uj.project.tidarobot.stats.dto.UserRankingEntry;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Repository
public interface StatsRepository extends Repository<ParkingReservation, Long> {

    @Query("""
            SELECT new org.uj.project.tidarobot.stats.dto.DayRankingEntry(r.targetDate, COUNT(r))
            FROM ParkingReservation r
            WHERE (:city IS NULL OR r.city = :city)
              AND (:status IS NULL OR r.status = :status)
              AND (:dateFrom IS NULL OR r.targetDate >= :dateFrom)
              AND (:dateTo IS NULL OR r.targetDate <= :dateTo)
            GROUP BY r.targetDate
            """)
    List<DayRankingEntry> findDayRanking(
            @Param("city") City city,
            @Param("status") ReservationStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    @Query("""
            SELECT new org.uj.project.tidarobot.stats.dto.UserRankingEntry(r.user.id, r.user.username, COUNT(r))
            FROM ParkingReservation r
            WHERE (:city IS NULL OR r.city = :city)
              AND (:status IS NULL OR r.status = :status)
              AND (:dateFrom IS NULL OR r.targetDate >= :dateFrom)
              AND (:dateTo IS NULL OR r.targetDate <= :dateTo)
            GROUP BY r.user.id, r.user.username
            """)
    List<UserRankingEntry> findUserRanking(
            @Param("city") City city,
            @Param("status") ReservationStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    @Query("""
            SELECT new org.uj.project.tidarobot.stats.dto.FloorRankingEntry(r.floor, COUNT(r))
            FROM ParkingReservation r
            WHERE (:city IS NULL OR r.city = :city)
              AND (:status IS NULL OR r.status = :status)
              AND (:dateFrom IS NULL OR r.targetDate >= :dateFrom)
              AND (:dateTo IS NULL OR r.targetDate <= :dateTo)
            GROUP BY r.floor
            """)
    List<FloorRankingEntry> findFloorRanking(
            @Param("city") City city,
            @Param("status") ReservationStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}
