package org.uj.project.tidarobot.stats.dto;

public record UserRankingEntry(
        Long userId,
        String username,
        Long reservationCount
) {}
