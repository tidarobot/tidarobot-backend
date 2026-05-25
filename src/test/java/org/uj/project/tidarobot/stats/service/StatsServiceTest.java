package org.uj.project.tidarobot.stats.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.uj.project.tidarobot.stats.dto.DayRankingEntry;
import org.uj.project.tidarobot.stats.repository.StatsRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock StatsRepository statsRepository;
    @InjectMocks StatsService statsService;

    private List<DayRankingEntry> threeDayEntries() {
        return List.of(
                new DayRankingEntry(LocalDate.of(2026, 1, 3), 5L),
                new DayRankingEntry(LocalDate.of(2026, 1, 1), 10L),
                new DayRankingEntry(LocalDate.of(2026, 1, 2), 3L)
        );
    }

    @Test
    void getDayRanking_noSort_preservesRepositoryOrder() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(0, 10), null, null, null, null);

        assertThat(result.getContent()).extracting(DayRankingEntry::reservationCount)
                .containsExactly(5L, 10L, 3L);
    }

    @Test
    void getDayRanking_sortByReservationCountAscending_returnsSortedAsc() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(0, 10, Sort.by("reservationCount").ascending()), null, null, null, null);

        assertThat(result.getContent()).extracting(DayRankingEntry::reservationCount)
                .containsExactly(3L, 5L, 10L);
    }

    @Test
    void getDayRanking_sortByReservationCountDescending_returnsSortedDesc() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(0, 10, Sort.by("reservationCount").descending()), null, null, null, null);

        assertThat(result.getContent()).extracting(DayRankingEntry::reservationCount)
                .containsExactly(10L, 5L, 3L);
    }

    @Test
    void getDayRanking_unknownSortField_doesNotThrowAndReturnsAllEntries() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(0, 10, Sort.by("unknownField").ascending()), null, null, null, null);

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void getDayRanking_paginationFirstPage_returnsCorrectSlice() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(0, 2), null, null, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getDayRanking_paginationLastPage_returnsRemainingEntry() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(1, 2), null, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getDayRanking_pageOffsetBeyondTotal_returnsEmptyContentWithCorrectTotal() {
        when(statsRepository.findDayRanking(any(), any(), any(), any())).thenReturn(threeDayEntries());

        Page<DayRankingEntry> result = statsService.getDayRanking(
                PageRequest.of(5, 10), null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}
