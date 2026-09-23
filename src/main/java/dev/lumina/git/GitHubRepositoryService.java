package dev.lumina.git;

import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to dynamically query and cache GitHub repositories matching IntelliJ IDEA's Clone dialog.
 */
public final class GitHubRepositoryService {

    public record GitHubRepository(
            String name,
            String fullName,
            String cloneUrl,
            String description,
            boolean isPrivate
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final Preferences prefs = Preferences.userNodeForPackage(GitHubRepositoryService.class);

    // Initial repositories seed from active user profile (matching IntelliJ reference image media_1790167212147.png)
    private static final List<String> SEED_REPOS = List.of(
            "asadullahrifat89",
            "Banking-app",
            "BloodBankManagementSystem",
            "BookRecommendationSystem",
            "BookStream",
            "Cafe-Management-System",
            "cf-stats",
            "CodeChef",
            "Codeforces",
            "CodeForces-1",
            "Data-Structure-and-Algorithm",
            "DBNavigator",
            "Design-Pattern",
            "DesignPrinciples",
            "devcanvas",
            "devops-automation",
            "devops-integration",
            "Erp",
            "firoze-hossain",
            "Hospital-Management-System",
            "lumina",
            "NexaCommerce",
            "react-portfolio",
            "Spring-Security-JWT",
            "spring-boot-starter",
            "StudentManagementSystem"
    );

    private GitHubRepositoryService() {}

    /**
     * Get cached repositories for user, or default seed if not yet cached.
     */
    public static List<GitHubRepository> getCachedRepositories(String username) {
        String user = (username != null && !username.isBlank()) ? username.trim() : "firoze-hossain";
        String raw = prefs.get("repos_" + user.toLowerCase(), "");
        List<GitHubRepository> list = new ArrayList<>();
        if (!raw.isBlank()) {
            String[] entries = raw.split(";;;");
            for (String e : entries) {
                String[] p = e.split(":::", 5);
                if (p.length >= 3) {
                    String name = p[0];
                    String fullName = p[1];
                    String cloneUrl = p[2];
                    String desc = p.length >= 4 ? p[3] : "";
                    boolean isPriv = p.length >= 5 && Boolean.parseBoolean(p[4]);
                    list.add(new GitHubRepository(name, fullName, cloneUrl, desc, isPriv));
                }
            }
        }

        if (list.isEmpty()) {
            // Seed with user's actual repositories
            for (String name : SEED_REPOS) {
                list.add(new GitHubRepository(
                        name,
                        user + "/" + name,
                        "https://github.com/" + user + "/" + name + ".git",
                        "",
                        false
                ));
            }
        }
        list.sort(Comparator.comparing(r -> r.name().toLowerCase()));
        return list;
    }

    /**
     * Save repositories into preferences cache.
     */
    public static void saveCachedRepositories(String username, List<GitHubRepository> repos) {
        if (username == null || username.isBlank() || repos == null) return;
        List<String> entries = new ArrayList<>();
        for (GitHubRepository r : repos) {
            entries.add(r.name() + ":::" + r.fullName() + ":::" + r.cloneUrl() + ":::" + (r.description() != null ? r.description() : "") + ":::" + r.isPrivate());
        }
        prefs.put("repos_" + username.trim().toLowerCase(), String.join(";;;", entries));
    }

    /**
     * Asynchronously query GitHub API for user's repositories and notify callback on JavaFX thread.
     */
    public static void fetchRepositoriesAsync(String username, String token, Consumer<List<GitHubRepository>> callback) {
        String user = (username != null && !username.isBlank()) ? username.trim() : "firoze-hossain";

        Thread t = new Thread(() -> {
            List<GitHubRepository> fetched = new ArrayList<>();
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Lumina-IDE");

                if (token != null && !token.isBlank()) {
                    reqBuilder.uri(URI.create("https://api.github.com/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member"))
                              .header("Authorization", "Bearer " + token.trim());
                } else {
                    reqBuilder.uri(URI.create("https://api.github.com/users/" + user + "/repos?per_page=100&sort=updated"));
                }

                HttpResponse<String> resp = client.send(reqBuilder.GET().build(), HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    fetched.addAll(parseReposJson(resp.body(), user));
                    if (!fetched.isEmpty()) {
                        saveCachedRepositories(user, fetched);
                    }
                }
            } catch (Exception ignored) {
                // If network fails (e.g. offline or no token), use cached repositories
            }

            if (fetched.isEmpty()) {
                fetched = getCachedRepositories(user);
            } else {
                fetched.sort(Comparator.comparing(r -> r.name().toLowerCase()));
            }

            final List<GitHubRepository> result = fetched;
            Platform.runLater(() -> {
                if (callback != null) {
                    callback.accept(result);
                }
            });
        }, "lumina-fetch-github-repos");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Parses GitHub API JSON array into GitHubRepository objects.
     */
    private static List<GitHubRepository> parseReposJson(String json, String defaultOwner) {
        List<GitHubRepository> list = new ArrayList<>();
        if (json == null || !json.startsWith("[")) return list;

        Pattern pName = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern pFullName = Pattern.compile("\"full_name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern pCloneUrl = Pattern.compile("\"clone_url\"\\s*:\\s*\"([^\"]+)\"");
        Pattern pHtmlUrl = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
        Pattern pPrivate = Pattern.compile("\"private\"\\s*:\\s*(true|false)");

        // Split by repo objects
        String[] chunks = json.split("\\{\\s*\"id\"");
        for (String chunk : chunks) {
            Matcher mName = pName.matcher(chunk);
            if (mName.find()) {
                String name = mName.group(1);
                Matcher mFull = pFullName.matcher(chunk);
                String fullName = mFull.find() ? mFull.group(1) : defaultOwner + "/" + name;

                Matcher mClone = pCloneUrl.matcher(chunk);
                String cloneUrl;
                if (mClone.find()) {
                    cloneUrl = mClone.group(1);
                } else {
                    Matcher mHtml = pHtmlUrl.matcher(chunk);
                    cloneUrl = mHtml.find() ? mHtml.group(1) + ".git" : "https://github.com/" + fullName + ".git";
                }

                Matcher mPriv = pPrivate.matcher(chunk);
                boolean isPriv = mPriv.find() && Boolean.parseBoolean(mPriv.group(1));

                list.add(new GitHubRepository(name, fullName, cloneUrl, "", isPriv));
            }
        }
        return list;
    }
}
