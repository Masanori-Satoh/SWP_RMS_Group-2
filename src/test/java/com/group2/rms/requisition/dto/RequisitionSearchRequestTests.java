package com.group2.rms.requisition.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequisitionSearchRequestTests {

    @Test
    void activeFilterCount_shouldIgnoreKeywordSortPaginationAndEmptyFilters() {
        var request = new RequisitionSearchRequest(
                "developer", List.of(), "  ", null, null, null, null,
                "newest", 2, 20, "  ");

        assertEquals(0, request.activeFilterCount());
    }

    @Test
    void activeFilterCount_shouldCountEachFilterGroupOnce() {
        var request = new RequisitionSearchRequest(
                null, List.of("Draft", "Approved"), "published", 1, 2,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                null, null, null, "Full-time");

        assertEquals(6, request.activeFilterCount());
    }

    @Test
    void activeFilterCount_shouldCountEitherDateBoundaryAsOneFilter() {
        var fromOnly = new RequisitionSearchRequest(null, null, null, null, null,
                LocalDate.of(2026, 1, 1), null, null, null, null, null);
        var toOnly = new RequisitionSearchRequest(null, null, null, null, null,
                null, LocalDate.of(2026, 1, 31), null, null, null, null);

        assertEquals(1, fromOnly.activeFilterCount());
        assertEquals(1, toOnly.activeFilterCount());
    }

    @Test
    void hasStatus_shouldDistinguishAbsentEmptySelectedAndUnselectedStatuses() {
        var absent = new RequisitionSearchRequest(null, null, null, null, null,
                null, null, null, null, null, null);
        var empty = new RequisitionSearchRequest(null, List.of(), null, null, null,
                null, null, null, null, null, null);
        var selected = new RequisitionSearchRequest(null, List.of("Draft", "Approved"), null,
                null, null, null, null, null, null, null, null);

        assertFalse(absent.hasStatus("Draft"));
        assertFalse(empty.hasStatus("Draft"));
        assertTrue(selected.hasStatus("Approved"));
        assertFalse(selected.hasStatus("Rejected"));
    }
}
