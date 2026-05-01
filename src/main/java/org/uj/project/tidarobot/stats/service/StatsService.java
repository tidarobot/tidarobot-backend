package org.uj.project.tidarobot.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.parking.entity.City;
import org.uj.project.tidarobot.parking.entity.ReservationStatus;
import org.uj.project.tidarobot.stats.dto.DayRankingEntry;
import org.uj.project.tidarobot.stats.dto.FloorRankingEntry;
import org.uj.project.tidarobot.stats.dto.UserRankingEntry;
import org.uj.project.tidarobot.stats.repository.StatsRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Map<String, Comparator<DayRankingEntry>> DAY_COMPARATORS = Map.of(
            "targetDate", Comparator.comparing(DayRankingEntry::targetDate),
            "reservationCount", Comparator.comparing(DayRankingEntry::reservationCount)
    );

    private static final Map<String, Comparator<UserRankingEntry>> USER_COMPARATORS = Map.of(
            "username", Comparator.comparing(UserRankingEntry::username),
            "reservationCount", Comparator.comparing(UserRankingEntry::reservationCount)
    );

    private static final Map<String, Comparator<FloorRankingEntry>> FLOOR_COMPARATORS = Map.of(
            "floor", Comparator.comparing(e -> e.floor().name()),
            "reservationCount", Comparator.comparing(FloorRankingEntry::reservationCount)
    );

    private final StatsRepository statsRepository;

    public Page<DayRankingEntry> getDayRanking(
            Pageable pageable, City city, ReservationStatus status, LocalDate dateFrom, LocalDate dateTo
    ) {
        List<DayRankingEntry> all = statsRepository.findDayRanking(city, status, dateFrom, dateTo);
        return sortAndPaginate(all, pageable, DAY_COMPARATORS);
    }

    public Page<UserRankingEntry> getUserRanking(
            Pageable pageable, City city, ReservationStatus status, LocalDate dateFrom, LocalDate dateTo
    ) {
        List<UserRankingEntry> all = statsRepository.findUserRanking(city, status, dateFrom, dateTo);
        return sortAndPaginate(all, pageable, USER_COMPARATORS);
    }

    public Page<FloorRankingEntry> getFloorRanking(
            Pageable pageable, City city, ReservationStatus status, LocalDate dateFrom, LocalDate dateTo
    ) {
        List<FloorRankingEntry> all = statsRepository.findFloorRanking(city, status, dateFrom, dateTo);
        return sortAndPaginate(all, pageable, FLOOR_COMPARATORS);
    }

    private <T> Page<T> sortAndPaginate(List<T> all, Pageable pageable, Map<String, Comparator<T>> comparators) {
        List<T> sorted = applySorting(all, pageable.getSort(), comparators);
        return applyPagination(sorted, pageable);
    }

    private <T> List<T> applySorting(List<T> list, Sort sort, Map<String, Comparator<T>> comparators) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<T> combined = null;
        for (Sort.Order order : sort) {
            Comparator<T> comparator = comparators.get(order.getProperty());
            if (comparator == null) {
                continue; // ignore unknown sort fields
            }
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
            combined = combined == null ? comparator : combined.thenComparing(comparator);
        }

        if (combined == null) {
            return list;
        }

        return list.stream().sorted(combined).toList();
    }

    private <T> Page<T> applyPagination(List<T> list, Pageable pageable) {
        int total = list.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<T> page = start >= total ? List.of() : list.subList(start, end);
        return new PageImpl<>(page, pageable, total);
    }
}
