package dev.lumina.git;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub sign-in via personal access token: the browser opens a pre-filled
 * token-creation page, the user pastes the token, we verify it against the
 * GitHub API, and git push/pull/clone authenticate through a GIT_ASKPASS
 * helper that reads the token from an environment variable (so the token
 * never lands in git config or remote URLs).
 */
public final class GitHubAuth {

    /** Pre-filled token creation page (repo scope, named for the IDE). */
    public static final String TOKEN_URL =
            "https://github.com/settings/tokens/new?scopes=repo&description=Lumina%20IDE";

    private static final Pattern LOGIN = Pattern.compile("\"login\"\\s*:\\s*\"([^\"]+)\"");

    private GitHubAuth() {
    }

    /** Validate a token against api.github.com; returns the login or null. */
    public static String validate(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.github.com/user"))
                    .header("Authorization", "Bearer " + token.trim())
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            Matcher m = LOGIN.matcher(response.body());
            return m.find() ? m.group(1) : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Ensure the askpass helper exists and return its path, or null on
     * failure. The helper just echoes $LUMINA_GH_TOKEN, which we set per
     * git invocation — GitHub accepts the token for both prompts.
     */
    public static Path ensureAskpass() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path dir = Path.of(System.getProperty("user.home"), ".lumina");
        Path script = dir.resolve(windows ? "askpass.bat" : "askpass.sh");
        try {
            Files.createDirectories(dir);
            if (!Files.isRegularFile(script)) {
                Files.writeString(script, windows
                        ? "@echo off\r\necho %LUMINA_GH_TOKEN%\r\n"
                        : "#!/bin/sh\necho \"$LUMINA_GH_TOKEN\"\n");
            }
            if (!windows && !script.toFile().canExecute()
                    && !script.toFile().setExecutable(true)) {
                return null;
            }
            return script;
        } catch (IOException e) {
            return null;
        }
    }
}
