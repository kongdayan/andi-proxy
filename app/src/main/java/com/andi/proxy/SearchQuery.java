package com.andi.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Local text search shared by matching, ranking, and highlighting. */
public final class SearchQuery {
    private final String phrase;
    private final String[] terms;

    public SearchQuery(String value) {
        phrase = value == null ? "" : value.replaceAll("[\\s\\p{Z}]+", " ").trim();
        terms = phrase.isEmpty() ? new String[0] : phrase.split(" ");
    }

    public boolean isEmpty() { return terms.length == 0; }

    /** Lower rank is better; -1 means a required word was not found. */
    public int rank(String title, String detail) {
        if (isEmpty()) return 0;
        boolean allTitle = true, allDetail = true;
        for (String term : terms) {
            boolean inTitle = contains(title, term), inDetail = contains(detail, term);
            if (!inTitle && !inDetail) return -1;
            allTitle &= inTitle;
            allDetail &= inDetail;
        }
        if (title.equalsIgnoreCase(phrase)) return 0;
        if (title.regionMatches(true, 0, phrase, 0, phrase.length())) return 1;
        if (allTitle) return 2;
        return allDetail ? 3 : 4;
    }

    private static boolean contains(String text, String term) {
        for (int i = 0; i <= text.length() - term.length(); i++)
            if (text.regionMatches(true, i, term, 0, term.length())) return true;
        return false;
    }

    /** Original UTF-16 offsets; no lowercasing that could shift Unicode offsets. */
    public List<Range> ranges(String text) {
        List<Range> matches = new ArrayList<>();
        for (String term : terms) {
            for (int i = 0; i <= text.length() - term.length(); i++)
                if (text.regionMatches(true, i, term, 0, term.length()))
                    matches.add(new Range(i, i + term.length()));
        }
        matches.sort(Comparator.comparingInt(r -> r.start));
        List<Range> merged = new ArrayList<>();
        for (Range match : matches) {
            if (!merged.isEmpty() && match.start <= merged.get(merged.size() - 1).end) {
                Range last = merged.remove(merged.size() - 1);
                merged.add(new Range(last.start, Math.max(last.end, match.end)));
            } else merged.add(match);
        }
        return merged;
    }

    public static final class Range {
        public final int start, end;
        public Range(int start, int end) { this.start = start; this.end = end; }
    }
}
