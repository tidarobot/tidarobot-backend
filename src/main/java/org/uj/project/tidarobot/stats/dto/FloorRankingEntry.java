package org.uj.project.tidarobot.stats.dto;

import org.uj.project.tidarobot.parking.entity.Floor;

public record FloorRankingEntry(
        Floor floor,
        Long reservationCount
) {}
