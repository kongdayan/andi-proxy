package com.andi.proxy;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class SearchQueryTest {
    @Test public void matchesWordsAcrossTitleAndDetailRegardlessOfCaseOrOrder() {
        SearchQuery query = new SearchQuery("  ANDROID\t设置\u3000 ");
        assertTrue(query.rank("设置", "com.android.settings") >= 0);
        assertTrue(new SearchQuery("设置 android").rank("设置", "com.android.settings") >= 0);
        assertEquals(-1, query.rank("设置", "com.example.settings"));
    }

    @Test public void ranksTitleMatchesAheadOfSecondaryContent() {
        SearchQuery query = new SearchQuery("mercury");
        assertTrue(query.rank("Mercury", "com.example") < query.rank("Mercury Browser", "com.example"));
        assertTrue(query.rank("Mercury Browser", "com.example") < query.rank("Travel", "com.mercury"));
    }

    @Test public void requiresEveryWordWithoutRequiringAContiguousPhrase() {
        SearchQuery query = new SearchQuery("blue oslo");
        assertTrue(query.rank("Oslo", "a blue lantern") >= 0);
        assertEquals(-1, query.rank("Oslo", "a red lantern"));
    }

    @Test public void emptyAndWhitespaceQueriesRestoreAllEntriesWithoutHighlights() {
        SearchQuery query = new SearchQuery(" \t\n\u3000");
        assertTrue(query.isEmpty()); assertEquals(0, query.rank("设置", "com.android.settings"));
        assertTrue(query.ranges("设置").isEmpty());
    }

    @Test public void highlightOffsetsStayCorrectAroundUnicodeAndEmoji() {
        List<SearchQuery.Range> ranges = new SearchQuery("设置 ANDROID").ranges("📱设置 ANDROID");
        assertEquals(2, ranges.size());
        assertEquals(2, ranges.get(0).start); assertEquals(4, ranges.get(0).end);
        assertEquals(5, ranges.get(1).start); assertEquals(12, ranges.get(1).end);
        List<SearchQuery.Range> turkish = new SearchQuery("i").ranges("İstanbul");
        assertEquals(0, turkish.get(0).start); assertEquals(1, turkish.get(0).end);
    }

    @Test public void overlappingAndRepeatedTermsMergeIntoOneHighlight() {
        List<SearchQuery.Range> ranges = new SearchQuery("ana nana ana").ranges("Banana");
        assertEquals(1, ranges.size()); assertEquals(1, ranges.get(0).start); assertEquals(6, ranges.get(0).end);
    }
}
