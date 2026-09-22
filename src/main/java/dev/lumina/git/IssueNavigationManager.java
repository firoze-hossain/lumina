package dev.lumina.git;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages issue navigation patterns and link resolution matching IntelliJ IDEA's
 * "Version Control > Issue Navigation".
 *
 * Scans check-in comments / commit messages for issue IDs (e.g. ABC-123, #42)
 * and resolves them into clickable issue tracker URLs.
 */
public class IssueNavigationManager {

    public static final class IssueNavigationLink {
        private String issuePattern;
        private String linkUrl;

        public IssueNavigationLink(String issuePattern, String linkUrl) {
            this.issuePattern = issuePattern != null ? issuePattern.trim() : "";
            this.linkUrl = linkUrl != null ? linkUrl.trim() : "";
        }

        public String getIssuePattern() { return issuePattern; }
        public void setIssuePattern(String issuePattern) { this.issuePattern = issuePattern != null ? issuePattern.trim() : ""; }

        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl != null ? linkUrl.trim() : ""; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IssueNavigationLink that)) return false;
            return Objects.equals(issuePattern, that.issuePattern) && Objects.equals(linkUrl, that.linkUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issuePattern, linkUrl);
        }
    }

    public record IssueMatch(int start, int end, String issueKey, String targetUrl) {}

    private static final IssueNavigationManager INSTANCE = new IssueNavigationManager();

    private final Preferences prefs = Preferences.userNodeForPackage(IssueNavigationManager.class);
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<IssueNavigationLink> links = new ArrayList<>();

    private IssueNavigationManager() {
        loadPreferences();
    }

    public static IssueNavigationManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<IssueNavigationLink> getLinks() {
        return Collections.unmodifiableList(new ArrayList<>(links));
    }

    public synchronized void setLinks(List<IssueNavigationLink> newLinks) {
        links.clear();
        if (newLinks != null) {
            links.addAll(newLinks);
        }
        savePreferences();
        notifyListeners();
    }

    public synchronized void addLink(String issuePattern, String linkUrl) {
        if (issuePattern == null || issuePattern.isBlank()) return;
        links.add(new IssueNavigationLink(issuePattern, linkUrl));
        savePreferences();
        notifyListeners();
    }

    public synchronized void addLink(IssueNavigationLink link) {
        if (link == null || link.getIssuePattern().isBlank()) return;
        links.add(link);
        savePreferences();
        notifyListeners();
    }

    public synchronized void updateLink(int index, String issuePattern, String linkUrl) {
        if (index >= 0 && index < links.size()) {
            IssueNavigationLink item = links.get(index);
            item.setIssuePattern(issuePattern);
            item.setLinkUrl(linkUrl);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeLink(int index) {
        if (index >= 0 && index < links.size()) {
            links.remove(index);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeLink(IssueNavigationLink link) {
        if (links.remove(link)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void clearLinks() {
        links.clear();
        savePreferences();
        notifyListeners();
    }

    /**
     * Creates and adds a YouTrack issue navigation pattern matching IntelliJ's template:
     * Pattern: [A-Z]+-\d+
     * Link: <serverUrl>/issue/$0
     */
    public synchronized void addYouTrackPattern(String serverUrl) {
        String normalized = normalizeUrl(serverUrl);
        if (normalized.isEmpty()) return;
        String linkUrl = normalized + "/issue/$0";
        addLink(new IssueNavigationLink("[A-Z]+-\\d+", linkUrl));
    }

    /**
     * Creates and adds a JIRA issue navigation pattern matching IntelliJ's template:
     * Pattern: [A-Z]+-\d+
     * Link: <serverUrl>/browse/$0
     */
    public synchronized void addJiraPattern(String serverUrl) {
        String normalized = normalizeUrl(serverUrl);
        if (normalized.isEmpty()) return;
        String linkUrl = normalized + "/browse/$0";
        addLink(new IssueNavigationLink("[A-Z]+-\\d+", linkUrl));
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.isEmpty() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * Scans the given commit message / text and finds all issue matches with their resolved URLs.
     */
    public synchronized List<IssueMatch> findIssueMatches(String text) {
        if (text == null || text.isBlank() || links.isEmpty()) {
            return Collections.emptyList();
        }

        List<IssueMatch> matches = new ArrayList<>();
        for (IssueNavigationLink link : links) {
            String patternStr = link.getIssuePattern();
            String linkTemplate = link.getLinkUrl();
            if (patternStr.isBlank() || linkTemplate.isBlank()) continue;

            try {
                Pattern p = Pattern.compile(patternStr);
                Matcher m = p.matcher(text);
                while (m.find()) {
                    int start = m.start();
                    int end = m.end();
                    String issueKey = m.group(0);

                    // Substitute $0, $1, etc.
                    String resolvedUrl = linkTemplate.replace("$0", issueKey);
                    int groupCount = m.groupCount();
                    for (int g = 1; g <= groupCount; g++) {
                        String groupVal = m.group(g);
                        resolvedUrl = resolvedUrl.replace("$" + g, groupVal != null ? groupVal : "");
                    }

                    matches.add(new IssueMatch(start, end, issueKey, resolvedUrl));
                }
            } catch (Exception ignored) {
                // Ignore invalid regex patterns safely
            }
        }

        // Sort by start position
        matches.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return matches;
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        String raw = prefs.get("vcs_issue_navigation_links", "");
        links.clear();
        if (!raw.isBlank()) {
            String[] entries = raw.split(";;;");
            for (String e : entries) {
                String[] parts = e.split(":::", 2);
                if (parts.length == 2) {
                    links.add(new IssueNavigationLink(parts[0], parts[1]));
                }
            }
        }
    }

    private void savePreferences() {
        List<String> entries = new ArrayList<>();
        for (IssueNavigationLink l : links) {
            entries.add(l.getIssuePattern() + ":::" + l.getLinkUrl());
        }
        prefs.put("vcs_issue_navigation_links", String.join(";;;", entries));
    }
}
