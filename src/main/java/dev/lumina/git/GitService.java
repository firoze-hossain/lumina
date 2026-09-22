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

    /** Stage the given repo-relative paths (or all changes if empty). */
    public static Result add(Path dir, List<String> paths) {
        List<String> args = new ArrayList<>(List.of("add"));
        if (paths.isEmpty()) {
            args.add("-A");
        } else {
            args.add("--");
            args.addAll(paths);
        }
        return exec(dir, args.toArray(new String[0]));
    }

    public static Result commit(Path dir, String message) {
        return exec(dir, "commit", "-m", message);
    }

    public static Result commitAmend(Path dir, String message) {
        return exec(dir, "commit", "--amend", "-m", message);
    }

    public static String lastCommitMessage(Path dir) {
        Result r = exec(dir, "log", "-1", "--pretty=%B");
        return r.ok() ? r.output().trim() : "";
    }

    public static List<String> recentCommitMessages(Path dir, int limit) {
        List<String> list = new ArrayList<>();
        Result r = exec(dir, "log", "-" + limit, "--pretty=%s");
        if (r.ok()) {
            for (String line : r.output().split("\\R")) {
                if (!line.isBlank() && !list.contains(line.trim())) {
                    list.add(line.trim());
                }
            }
        }
        return list;
    }

    public static Result rollback(Path dir, List<String> paths) {
        if (paths == null || paths.isEmpty()) return new Result(0, "");
        List<String> unstageArgs = new ArrayList<>(List.of("restore", "--staged", "--"));
        unstageArgs.addAll(paths);
        exec(dir, unstageArgs.toArray(new String[0]));

        List<String> restoreArgs = new ArrayList<>(List.of("restore", "--"));
        restoreArgs.addAll(paths);
        Result r = exec(dir, restoreArgs.toArray(new String[0]));
        if (!r.ok()) {
            List<String> checkoutArgs = new ArrayList<>(List.of("checkout", "--"));
            checkoutArgs.addAll(paths);
            r = exec(dir, checkoutArgs.toArray(new String[0]));
        }
        return r;
    }

    public static Result statusDetailed(Path dir, boolean showIgnored) {
        if (showIgnored) {
            return exec(dir, "status", "--porcelain=v1", "-uall", "--ignored=matching");
        } else {
            return exec(dir, "status", "--porcelain=v1", "-uall");
        }
    }

    public static Result push(Path dir) {
        return exec(dir, "push");
    }

    public record StashEntry(int index, String ref, String branch, String message, String date) {}

    public record StashFile(String relativePath, String fileName, String dirPath, String statusPrefix) {
        public static StashFile fromLine(String line) {
            if (line == null || line.isBlank()) return null;
            String status = "M";
            String path = line.trim();
            if (line.contains("\t")) {
                int tabIdx = line.indexOf('\t');
                status = line.substring(0, tabIdx).trim();
                path = line.substring(tabIdx + 1).trim();
            } else if (line.length() >= 2 && Character.isLetter(line.charAt(0)) && Character.isWhitespace(line.charAt(1))) {
                status = line.substring(0, 1);
                path = line.substring(1).trim();
            }
            if (path.startsWith("\"") && path.endsWith("\"") && path.length() >= 2) {
                path = path.substring(1, path.length() - 1);
            }
            if (path.contains(" -> ")) {
                path = path.substring(path.indexOf(" -> ") + 4).trim();
            }
            int slash = path.lastIndexOf('/');
            String fName = slash >= 0 ? path.substring(slash + 1) : path;
            String dPath = slash >= 0 ? path.substring(0, slash) : "";
            return new StashFile(path, fName, dPath, status);
        }
    }

    public static Result stash(Path dir, String message) {
        if (message == null || message.isBlank()) {
            return exec(dir, "stash");
        }
        return exec(dir, "stash", "push", "-u", "-m", message);
    }

    public static Result stashList(Path dir) {
        return exec(dir, "stash", "list");
    }

    public static List<StashEntry> stashListDetailed(Path dir) {
        List<StashEntry> list = new ArrayList<>();
        Result r = exec(dir, "stash", "list", "--pretty=format:%gd%x00%cr%x00%gs");
        if (!r.ok() || r.output().isBlank()) {
            // Fallback to standard stash list
            Result fallback = exec(dir, "stash", "list");
            if (!fallback.ok() || fallback.output().isBlank()) return list;
            int idx = 0;
            for (String line : fallback.output().split("\\R")) {
                if (line.isBlank()) continue;
                String ref = "stash@{" + idx + "}";
                String branch = "master";
                String msg = line;
                int colon = line.indexOf(':');
                if (colon >= 0) {
                    ref = line.substring(0, colon).trim();
                    msg = line.substring(colon + 1).trim();
                }
                if (msg.startsWith("WIP on ") || msg.startsWith("On ")) {
                    int start = msg.indexOf("on ") >= 0 ? msg.indexOf("on ") + 3 : 3;
                    int endColon = msg.indexOf(':', start);
                    if (endColon > start) {
                        branch = msg.substring(start, endColon).trim();
                        msg = msg.substring(endColon + 1).trim();
                    }
                }
                msg = msg.replaceFirst("^[0-9a-fA-F]{7,40}\\s+", "").trim();
                list.add(new StashEntry(idx, ref, branch, msg, ""));
                idx++;
            }
            return list;
        }

        int index = 0;
        for (String record : r.output().split("\\R")) {
            if (record.isBlank()) continue;
            String[] parts = record.split("\0", -1);
            String ref = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : "stash@{" + index + "}";
            String date = parts.length > 1 ? parts[1].trim() : "";
            String subject = parts.length > 2 ? parts[2].trim() : "";

            String branch = "master";
            String msg = subject;
            if (subject.startsWith("WIP on ") || subject.startsWith("On ")) {
                int start = subject.indexOf("on ") >= 0 ? subject.indexOf("on ") + 3 : 3;
                int endColon = subject.indexOf(':', start);
                if (endColon > start) {
                    branch = subject.substring(start, endColon).trim();
                    msg = subject.substring(endColon + 1).trim();
                }
            }
            msg = msg.replaceFirst("^[0-9a-fA-F]{7,40}\\s+", "").trim();
            list.add(new StashEntry(index, ref, branch, msg, date));
            index++;
        }
        return list;
    }

    public static List<StashFile> stashFiles(Path dir, String stashRef) {
        List<StashFile> files = new ArrayList<>();
        if (stashRef == null || stashRef.isBlank()) stashRef = "stash@{0}";

        Result r = exec(dir, "diff", "--name-status", stashRef + "^1", stashRef);
        if (!r.ok() || r.output().isBlank()) {
            r = exec(dir, "stash", "show", "--name-status", stashRef);
        }
        if (!r.ok() || r.output().isBlank()) return files;

        for (String line : r.output().split("\\R")) {
            StashFile sf = StashFile.fromLine(line);
            if (sf != null && !sf.relativePath().isBlank()) {
                files.add(sf);
            }
        }
        return files;
    }

    public static String stashShowFile(Path dir, String stashRef, String relativePath) {
        if (stashRef == null || stashRef.isBlank()) stashRef = "stash@{0}";
        Result r = exec(dir, "show", stashRef + ":" + relativePath);
        return r.ok() ? r.output() : "";
    }

    public static String headShowFile(Path dir, String relativePath) {
        if (dir == null || relativePath == null || relativePath.isBlank()) return null;
        String gitPath = relativePath.replace('\\', '/');
        Result r = exec(dir, "show", "HEAD:" + gitPath);
        return r.ok() ? r.output() : null;
    }

    public static Result stashApply(Path dir) {
        return stashApply(dir, null);
    }

    public static Result stashApply(Path dir, String stashRef) {
        if (stashRef == null || stashRef.isBlank()) return exec(dir, "stash", "apply");
        return exec(dir, "stash", "apply", stashRef);
    }

    public static Result stashPop(Path dir) {
        return stashPop(dir, null);
    }

    public static Result stashPop(Path dir, String stashRef) {
        if (stashRef == null || stashRef.isBlank()) return exec(dir, "stash", "pop");
        return exec(dir, "stash", "pop", stashRef);
    }

    public static Result stashDrop(Path dir) {
        return stashDrop(dir, null);
    }

    public static Result stashDrop(Path dir, String stashRef) {
        if (stashRef == null || stashRef.isBlank()) return exec(dir, "stash", "drop");
        return exec(dir, "stash", "drop", stashRef);
    }

    /** {@code git log}, one line per commit, most recent first. */
    public static Result log(Path dir, int maxCount) {
        return exec(dir, "log", "-" + maxCount,
                "--date=relative", "--pretty=format:%h\u0001%an\u0001%ad\u0001%s");
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