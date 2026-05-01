package org.uj.project.tidarobot.stats.dto;

import java.time.LocalDate;

public record DayRankingEntry(
        LocalDate targetDate,
        Long reservationCount
) {}
