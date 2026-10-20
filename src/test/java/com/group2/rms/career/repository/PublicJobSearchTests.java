package com.group2.rms.career.repository;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tách từ khóa và thoát ký tự đại diện của LIKE (hàm thuần, không cần DB). */
class PublicJobSearchTests {

    @Test
    void blankKeywordHasNoTerms() {
        assertEquals(List.of(), PublicJobSearch.terms(null));
        assertEquals(List.of(), PublicJobSearch.terms("   "));
    }

    @Test
    void keywordIsSplitOnWhitespaceWithoutDuplicates() {
        assertEquals(List.of("Kế", "toán"), PublicJobSearch.terms("  Kế   toán  "));
        assertEquals(List.of("Java", "spring"), PublicJobSearch.terms("Java spring JAVA"));
    }

    @Test
    void atMostFiveTermsAreUsed() {
        assertEquals(List.of("a1", "a2", "a3", "a4", "a5"), PublicJobSearch.terms("a1 a2 a3 a4 a5 a6 a7"));
    }

    @Test
    void likeWildcardsInTheKeywordAreEscaped() {
        assertEquals("%java%", PublicJobSearch.containsPattern("java"));
        assertEquals("%\\%%", PublicJobSearch.containsPattern("%"));
        assertEquals("%a\\_b%", PublicJobSearch.containsPattern("a_b"));
        assertEquals("%\\[x]%", PublicJobSearch.containsPattern("[x]"));
        assertEquals("%c\\\\d%", PublicJobSearch.containsPattern("c\\d"));
    }
}
