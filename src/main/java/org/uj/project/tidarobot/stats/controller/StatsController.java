package org.uj.project.tidarobot.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.stats.dto.DayRankingEntry;
import org.uj.project.tidarobot.stats.dto.FloorRankingEntry;
import org.uj.project.tidarobot.stats.dto.UserRankingEntry;
import org.uj.project.tidarobot.stats.service.StatsService;

import java.time.LocalDate;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * Ranking of days with the most reservations.
     * Sort fields: targetDate, reservationCount
     */
    @GetMapping("/days")
    public ResponseEntity<Page<DayRankingEntry>> getDayRanking(
            @PageableDefault(size = 20, sort = "reservationCount", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) City city,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(statsService.getDayRanking(pageable, city, status, dateFrom, dateTo));
    }

    /**
     * Ranking of users with the most reservations.
     * Sort fields: username, reservationCount
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserRankingEntry>> getUserRanking(
            @PageableDefault(size = 20, sort = "reservationCount", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) City city,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(statsService.getUserRanking(pageable, city, status, dateFrom, dateTo));
    }

    /**
     * Ranking of parking floors by popularity.
     * Sort fields: floor, reservationCount
     */
    @GetMapping("/floors")
    public ResponseEntity<Page<FloorRankingEntry>> getFloorRanking(
            @PageableDefault(size = 20, sort = "reservationCount", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) City city,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(statsService.getFloorRanking(pageable, city, status, dateFrom, dateTo));
    }
}
