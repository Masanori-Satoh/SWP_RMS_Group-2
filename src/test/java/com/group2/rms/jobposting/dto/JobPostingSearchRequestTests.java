package com.group2.rms.jobposting.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPostingSearchRequestTests {

    @Test
    void activeFilterCount_shouldIgnoreKeywordSortPaginationAndEmptyFilters() {
        var request = new JobPostingSearchRequest(
                "developer", List.of(), null, null, "  ", null, null,
                "newest", 2, 20);

        assertEquals(0, request.activeFilterCount());
    }

    @Test
    void activeFilterCount_shouldCountEachFilterGroupOnce() {
        var request = new JobPostingSearchRequest(
                null, List.of("Draft", "Published"), 1, 2, "open",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                null, null, null);

        assertEquals(5, request.activeFilterCount());
    }

    @Test
    void activeFilterCount_shouldCountEitherDateBoundaryAsOneFilter() {
        var fromOnly = new JobPostingSearchRequest(null, null, null, null, null,
                LocalDate.of(2026, 1, 1), null, null, null, null);
        var toOnly = new JobPostingSearchRequest(null, null, null, null, null,
                null, LocalDate.of(2026, 1, 31), null, null, null);

        assertEquals(1, fromOnly.activeFilterCount());
        assertEquals(1, toOnly.activeFilterCount());
    }

    @Test
    void hasStatus_shouldDistinguishAbsentEmptySelectedAndUnselectedStatuses() {
        var absent = new JobPostingSearchRequest(null, null, null, null, null,
                null, null, null, null, null);
        var empty = new JobPostingSearchRequest(null, List.of(), null, null, null,
                null, null, null, null, null);
        var selected = new JobPostingSearchRequest(null, List.of("Draft", "Published"),
                null, null, null, null, null, null, null, null);

        assertFalse(absent.hasStatus("Draft"));
        assertFalse(empty.hasStatus("Draft"));
        assertTrue(selected.hasStatus("Published"));
        assertFalse(selected.hasStatus("Closed"));
    }
}
