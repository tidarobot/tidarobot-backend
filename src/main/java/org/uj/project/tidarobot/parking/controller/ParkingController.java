package org.uj.project.tidarobot.parking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.uj.project.tidarobot.parking.dto.ParkingReservationRequest;
import org.uj.project.tidarobot.parking.dto.ParkingReservationResponse;
import org.uj.project.tidarobot.parking.service.ParkingReservationService;
import org.uj.project.tidarobot.user.entity.User;

import java.util.List;

@RestController
@RequestMapping("/parking/reservations")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingReservationService reservationService;

    @PostMapping
    public ResponseEntity<ParkingReservationResponse> createReservation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ParkingReservationRequest request
    ) {
        ParkingReservationResponse response = reservationService.createReservation(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ParkingReservationResponse>> getLatestReservations(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(reservationService.getLatestReservations(user));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<ParkingReservationResponse>> getReservationHistory(
            @AuthenticationPrincipal User user,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reservationService.getReservationHistory(user, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        reservationService.cancelReservation(user, id);
        return ResponseEntity.noContent().build();
    }
}
