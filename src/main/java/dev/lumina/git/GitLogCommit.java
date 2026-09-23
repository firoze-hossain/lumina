package dev.lumina.git;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable domain model representing a Git commit parsed for the Git Log view.
 */
public record GitLogCommit(
        String hash,
        String shortHash,
        List<String> parents,
        String authorName,
        String authorEmail,
        long timestamp,
        String subject,
        String fullMessage,
        List<String> refs,
        int graphLane
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d/M/yy, h:mm a");

    public GitLogCommit withGraphLane(int lane) {
        return new GitLogCommit(
                hash, shortHash, parents, authorName, authorEmail,
                timestamp, subject, fullMessage, refs, lane
        );
    }

    /**
     * Formats timestamp into authentic format:
     * - "Today 7:46 AM" if timestamp is today
     * - "Yesterday 2:51 PM" if timestamp is yesterday
     * - "21/9/26, 5:55 PM" otherwise
     */
    public String getFormattedDate() {
        return formatCommitTimestamp(timestamp, Instant.now());
    }

    public static String formatCommitTimestamp(long commitTimestampSeconds, Instant nowInstant) {
        if (commitTimestampSeconds <= 0) return "";
        ZoneId zone = ZoneId.systemDefault();
        Instant commitInstant = Instant.ofEpochSecond(commitTimestampSeconds);
        LocalDate commitDate = commitInstant.atZone(zone).toLocalDate();
        LocalDate nowDate = nowInstant.atZone(zone).toLocalDate();

        String timeStr = commitInstant.atZone(zone).format(TIME_FORMAT);

        if (commitDate.equals(nowDate)) {
            return "Today " + timeStr;
        } else if (commitDate.equals(nowDate.minusDays(1))) {
            return "Yesterday " + timeStr;
        } else {
            return commitInstant.atZone(zone).format(DATE_TIME_FORMAT);
        }
    }

    /**
     * Parses ref strings into clean branch and tag names.
     * E.g. "HEAD -> master, origin/master, tag: v1.0"
     */
    public static List<String> parseRefs(String refStr) {
        List<String> list = new ArrayList<>();
        if (refStr == null || refStr.isBlank()) return list;

        for (String part : refStr.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("tag: ")) {
                list.add(trimmed.substring(5).trim());
            } else if (trimmed.contains(" -> ")) {
                list.add(trimmed.substring(trimmed.indexOf(" -> ") + 4).trim());
            } else {
                list.add(trimmed);
            }
        }
        return list;
    }

    /**
     * Checks if this commit matches a search query (case-sensitive or insensitive).
     */
    public boolean matches(String query, boolean caseSensitive) {
        if (query == null || query.isBlank()) return true;
        String q = caseSensitive ? query.trim() : query.trim().toLowerCase();
        String subj = caseSensitive ? (subject != null ? subject : "") : (subject != null ? subject.toLowerCase() : "");
        String h = caseSensitive ? (hash != null ? hash : "") : (hash != null ? hash.toLowerCase() : "");
        String author = caseSensitive ? (authorName != null ? authorName : "") : (authorName != null ? authorName.toLowerCase() : "");

        return subj.contains(q) || h.contains(q) || author.contains(q);
    }
}
