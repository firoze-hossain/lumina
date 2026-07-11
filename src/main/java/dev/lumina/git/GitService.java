package dev.lumina.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Thin wrapper around the git CLI (uses the user's installed git + credentials). */
public final class GitService {

    public record Result(int code, String output) {
        public boolean ok() {
            return code == 0;
        }
    }

    private GitService() {
    }

    public static boolean isAvailable() {
        try {
            return exec(Path.of("."), "--version").ok();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isRepository(Path dir) {
        return dir != null && Files.isDirectory(dir.resolve(".git"));
    }

    /** Current branch name, or null when not a repo / detached lookup fails. */
    public static String currentBranch(Path dir) {
        if (!isRepository(dir)) return null;
        Result r = exec(dir, "rev-parse", "--abbrev-ref", "HEAD");
        return r.ok() ? r.output().trim() : null;
    }

    public static List<String> localBranches(Path dir) {
        List<String> branches = new ArrayList<>();
        Result r = exec(dir, "branch", "--format=%(refname:short)");
        if (r.ok()) {
            for (String line : r.output().split("\\R")) {
                if (!line.isBlank()) branches.add(line.trim());
            }
        }
        return branches;
    }

    public static Result checkout(Path dir, String branch) {
        return exec(dir, "checkout", branch);
    }

    public static Result createBranch(Path dir, String branch) {
        return exec(dir, "checkout", "-b", branch);
    }

    public static Result init(Path dir) {
        return exec(dir, "init");
    }

    /** Short porcelain status, e.g. to show change counts. */
    public static Result status(Path dir) {
        return exec(dir, "status", "--short", "--branch");
    }

    // ----------------------------------------------------------------- blame

    public record BlameLine(String author, String date, String summary) {
        public String gutter() {
            String a = author == null ? "" : author;
            if (a.length() > 14) a = a.substring(0, 13) + "\u2026";
            return date + "  " + a;
        }
    }

    /**
     * Per-line annotations from `git blame --line-porcelain`, in file order.
     * Returns null when the file isn't tracked in a git repository.
     */
    public static List<BlameLine> blame(Path file) {
        Path dir = file.getParent();
        if (dir == null) return null;
        Result r = exec(dir, "blame", "--line-porcelain", "--",
                file.getFileName().toString());
        if (!r.ok()) return null;

        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("M/d/yy");
        List<BlameLine> out = new ArrayList<>();
        String author = null, date = null, summary = null;
        for (String line : r.output().split("\n", -1)) {
            if (line.startsWith("author ")) {
                author = line.substring(7);
            } else if (line.startsWith("author-time ")) {
                try {
                    long epoch = Long.parseLong(line.substring(12).trim());
                    date = java.time.Instant.ofEpochSecond(epoch)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate().format(fmt);
                } catch (NumberFormatException e) {
                    date = "";
                }
            } else if (line.startsWith("summary ")) {
                summary = line.substring(8);
            } else if (line.startsWith("\t")) {
                out.add(new BlameLine(
                        author == null ? "" : author,
                        date == null ? "" : date,
                        summary == null ? "" : summary));
            }
        }
        return out.isEmpty() ? null : out;
    }

    /** origin URL normalized to https for opening in a browser, or null. */
    public static String remoteBrowserUrl(Path dir) {
        Result r = exec(dir, "remote", "get-url", "origin");
        if (!r.ok()) return null;
        String url = r.output().trim();
        if (url.startsWith("git@")) {
            // git@github.com:user/repo.git -> https://github.com/user/repo
            url = "https://" + url.substring(4).replaceFirst(":", "/");
        }
        if (url.endsWith(".git")) url = url.substring(0, url.length() - 4);
        return url.startsWith("http") ? url : null;
    }

    public static Result exec(Path dir, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(cmd)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            return new Result(code, out);
        } catch (IOException e) {
            return new Result(-1, "git not available: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(-1, "interrupted");
        }
    }
}
