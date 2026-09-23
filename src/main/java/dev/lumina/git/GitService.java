package dev.lumina.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static Result createBranch(Path dir, String branch, String startPoint, boolean checkout, boolean overwrite) {
        List<String> args = new ArrayList<>();
        if (checkout) {
            args.add("checkout");
            args.add(overwrite ? "-B" : "-b");
            args.add(branch);
            if (startPoint != null && !startPoint.isBlank() && !"HEAD".equalsIgnoreCase(startPoint)) {
                args.add(startPoint);
            }
        } else {
            args.add("branch");
            if (overwrite) {
                args.add("-f");
            }
            args.add(branch);
            if (startPoint != null && !startPoint.isBlank() && !"HEAD".equalsIgnoreCase(startPoint)) {
                args.add(startPoint);
            }
        }
        return exec(dir, args.toArray(new String[0]));
    }

    public static List<String> remoteBranches(Path dir) {
        List<String> branches = new ArrayList<>();
        Result r = exec(dir, "branch", "-r", "--format=%(refname:short)");
        if (r.ok()) {
            for (String line : r.output().split("\\R")) {
                line = line.trim();
                if (!line.isBlank() && !line.contains("->") && !line.endsWith("/HEAD")) {
                    branches.add(line);
                }
            }
        }
        return branches;
    }

    public static int unpushedCommitsCount(Path dir, String branch) {
        if (!isRepository(dir)) return 0;
        String upstream = getUpstreamBranch(dir);
        Result r;
        if (upstream != null) {
            r = exec(dir, "rev-list", upstream + "..HEAD", "--count");
        } else {
            r = exec(dir, "rev-list", "origin/" + branch + "..HEAD", "--count");
        }
        if (r.ok()) {
            try {
                return Integer.parseInt(r.output().trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public static Result updateProject(Path dir, boolean rebase) {
        if (rebase) {
            return exec(dir, "pull", "--rebase");
        } else {
            return exec(dir, "pull", "--no-rebase");
        }
    }

    public static Result renameBranch(Path dir, String oldName, String newName) {
        return exec(dir, "branch", "-m", oldName, newName);
    }

    public static Result deleteBranch(Path dir, String branch, boolean force) {
        return exec(dir, "branch", force ? "-D" : "-d", branch);
    }

    public static Result deleteRemoteBranch(Path dir, String remote, String branch) {
        return exec(dir, "push", remote, "--delete", branch);
    }

    public static Result mergeIntoCurrent(Path dir, String branch) {
        return exec(dir, "merge", branch);
    }

    public static Result rebaseOnto(Path dir, String branch) {
        return exec(dir, "rebase", branch);
    }

    public static Result pullIntoCurrent(Path dir, String remoteBranch, boolean rebase) {
        if (remoteBranch.contains("/")) {
            String remote = remoteBranch.substring(0, remoteBranch.indexOf('/'));
            String branch = remoteBranch.substring(remoteBranch.indexOf('/') + 1);
            if (rebase) {
                return exec(dir, "pull", "--rebase", remote, branch);
            } else {
                return exec(dir, "pull", "--no-rebase", remote, branch);
            }
        }
        return exec(dir, "pull", remoteBranch);
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
        return push(dir, false, false, "All");
    }

    public static Result push(Path dir, boolean forceWithLease, boolean pushTags, String tagMode) {
        List<String> args = new ArrayList<>();
        args.add("push");
        if (forceWithLease) {
            args.add("--force-with-lease");
        }
        if (pushTags) {
            if ("All".equalsIgnoreCase(tagMode)) {
                args.add("--tags");
            } else {
                args.add("--follow-tags");
            }
        }
        String upstream = getUpstreamBranch(dir);
        if (upstream == null) {
            String branch = currentBranch(dir);
            if (branch != null && !branch.isBlank()) {
                args.add("-u");
                args.add("origin");
                args.add(branch);
            }
        }
        return exec(dir, args.toArray(new String[0]));
    }

    public static String getUpstreamBranch(Path dir) {
        if (!isRepository(dir)) return null;
        Result r = exec(dir, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}");
        if (r.ok() && !r.output().isBlank()) {
            return r.output().trim();
        }
        return null;
    }

    public record CommitFile(String relativePath, String fileName, String dirPath, String statusPrefix) {
        public static CommitFile fromLine(String line) {
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
            return new CommitFile(path, fName, dPath, status);
        }
    }

    public record OutgoingCommit(String hash, String shortHash, String subject, String author, String date, List<CommitFile> files) {}

    public static List<OutgoingCommit> outgoingCommits(Path dir) {
        List<OutgoingCommit> commits = new ArrayList<>();
        if (!isRepository(dir)) return commits;

        String currentBranch = currentBranch(dir);
        if (currentBranch == null) return commits;

        String upstream = getUpstreamBranch(dir);
        Result logResult;
        if (upstream != null) {
            logResult = exec(dir, "log", upstream + "..HEAD", "--pretty=format:%H%x00%h%x00%s%x00%an%x00%cr");
        } else {
            Result check = exec(dir, "rev-parse", "--verify", "origin/" + currentBranch);
            if (check.ok()) {
                logResult = exec(dir, "log", "origin/" + currentBranch + "..HEAD", "--pretty=format:%H%x00%h%x00%s%x00%an%x00%cr");
            } else {
                logResult = exec(dir, "log", "-10", "--pretty=format:%H%x00%h%x00%s%x00%an%x00%cr");
            }
        }

        if (logResult.ok() && !logResult.output().isBlank()) {
            for (String line : logResult.output().split("\\R")) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\0");
                if (parts.length >= 3) {
                    String hash = parts[0].trim();
                    String shortHash = parts[1].trim();
                    String subject = parts[2].trim();
                    String author = parts.length > 3 ? parts[3].trim() : "";
                    String date = parts.length > 4 ? parts[4].trim() : "";

                    List<CommitFile> files = new ArrayList<>();
                    Result diffTree = exec(dir, "diff-tree", "--no-commit-id", "--name-status", "-r", hash);
                    if (diffTree.ok() && !diffTree.output().isBlank()) {
                        for (String fLine : diffTree.output().split("\\R")) {
                            CommitFile cf = CommitFile.fromLine(fLine);
                            if (cf != null) files.add(cf);
                        }
                    }
                    commits.add(new OutgoingCommit(hash, shortHash, subject, author, date, files));
                }
            }
        }

        return commits;
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
        return stash(dir, message, false);
    }

    public static Result stash(Path dir, String message, boolean keepIndex) {
        List<String> args = new ArrayList<>(List.of("stash", "push", "-u"));
        if (keepIndex) {
            args.add("--keep-index");
        }
        if (message != null && !message.isBlank()) {
            args.add("-m");
            args.add(message.trim());
        }
        return exec(dir, args.toArray(new String[0]));
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

    public static Result createPatch(Path dir, Path patchFile, boolean reverse) {
        List<String> args = new ArrayList<>(List.of("diff", "HEAD"));
        if (reverse) {
            args.add("-R");
        }
        Result r = exec(dir, args.toArray(new String[0]));
        if (!r.ok()) return r;
        try {
            if (patchFile.getParent() != null) {
                Files.createDirectories(patchFile.getParent());
            }
            Files.writeString(patchFile, r.output(), StandardCharsets.UTF_8);
            return new Result(0, "Patch created at " + patchFile.toAbsolutePath());
        } catch (IOException e) {
            return new Result(-1, "Failed to write patch: " + e.getMessage());
        }
    }

    public static Result applyPatch(Path dir, Path patchFile) {
        if (!Files.isRegularFile(patchFile)) {
            return new Result(-1, "Patch file not found: " + patchFile);
        }
        return exec(dir, "apply", "--ignore-space-change", "--whitespace=nowarn", patchFile.toAbsolutePath().toString());
    }

    public static Result applyPatchFromText(Path dir, String patchContent) {
        if (patchContent == null || patchContent.isBlank()) {
            return new Result(-1, "Clipboard does not contain patch content.");
        }
        try {
            Path tmp = Files.createTempFile("lumina-patch-", ".patch");
            Files.writeString(tmp, patchContent, StandardCharsets.UTF_8);
            Result r = applyPatch(dir, tmp);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {}
            return r;
        } catch (IOException e) {
            return new Result(-1, "Failed to apply clipboard patch: " + e.getMessage());
        }
    }

    public static Result rollbackAll(Path dir) {
        exec(dir, "restore", "--staged", ".");
        Result r = exec(dir, "restore", ".");
        if (!r.ok()) {
            r = exec(dir, "checkout", "--", ".");
        }
        return r;
    }

    public static Result shelve(Path dir, String shelfName) {
        Path shelfDir = VcsShelfSettingsManager.getInstance().getShelfDir(dir);
        try {
            Files.createDirectories(shelfDir);
            String safeName = shelfName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isBlank()) safeName = "shelf_" + System.currentTimeMillis();
            Path patchFile = shelfDir.resolve(safeName + ".patch");
            Result r = createPatch(dir, patchFile, false);
            if (!r.ok()) return r;
            rollbackAll(dir);
            return new Result(0, "Shelved changes to " + safeName);
        } catch (IOException e) {
            return new Result(-1, "Failed to create shelf: " + e.getMessage());
        }
    }

    public static List<Path> shelfList(Path dir) {
        Path shelfDir = VcsShelfSettingsManager.getInstance().getShelfDir(dir);
        List<Path> list = new ArrayList<>();
        if (Files.isDirectory(shelfDir)) {
            try (var stream = Files.list(shelfDir)) {
                stream.filter(p -> p.toString().endsWith(".patch")).forEach(list::add);
            } catch (IOException ignored) {}
        }
        return list;
    }

    /** {@code git log}, one line per commit, most recent first. */
    public static Result log(Path dir, int maxCount) {
        return exec(dir, "log", "-" + maxCount,
                "--date=relative", "--pretty=format:%h\u0001%an\u0001%ad\u0001%s");
    }

    public static List<GitLogCommit> getLogCommits(Path dir, int maxCount, String branchFilter) {
        List<GitLogCommit> list = new ArrayList<>();
        if (!isRepository(dir)) return list;

        List<String> args = new ArrayList<>();
        args.add("log");
        if (maxCount > 0) {
            args.add("-" + maxCount);
        }
        // format: hash, shortHash, parents, authorName, authorEmail, timestamp(sec), subject, fullBody, refNames
        args.add("--pretty=format:%H%x1f%h%x1f%P%x1f%an%x1f%ae%x1f%at%x1f%s%x1f%B%x1f%D%x1e");

        if (branchFilter != null && !branchFilter.isBlank() && !"All".equalsIgnoreCase(branchFilter)) {
            if ("HEAD".equalsIgnoreCase(branchFilter) || branchFilter.startsWith("HEAD")) {
                args.add("HEAD");
            } else {
                args.add(branchFilter);
            }
        } else {
            args.add("--all");
        }

        Result r = exec(dir, args.toArray(new String[0]));
        if (!r.ok() || r.output().isBlank()) {
            if (args.contains("--all")) {
                args.remove("--all");
                r = exec(dir, args.toArray(new String[0]));
            }
        }

        if (r.ok() && !r.output().isBlank()) {
            String[] records = r.output().split("\u001e");
            for (String rec : records) {
                if (rec == null || rec.isBlank()) continue;
                String[] fields = rec.split("\u001f", -1);
                if (fields.length >= 7) {
                    String hash = fields[0].trim();
                    String shortHash = fields[1].trim();
                    String parentsStr = fields[2].trim();
                    List<String> parents = parentsStr.isEmpty() ? List.of() : List.of(parentsStr.split("\\s+"));
                    String authorName = fields[3].trim();
                    String authorEmail = fields[4].trim();
                    long timestamp = 0;
                    try {
                        timestamp = Long.parseLong(fields[5].trim());
                    } catch (NumberFormatException ignored) {}
                    String subject = fields[6].trim();
                    String fullBody = fields.length > 7 ? fields[7].trim() : subject;
                    String refStr = fields.length > 8 ? fields[8].trim() : "";
                    List<String> refs = GitLogCommit.parseRefs(refStr);

                    list.add(new GitLogCommit(
                            hash, shortHash, parents, authorName, authorEmail,
                            timestamp, subject, fullBody, refs, 0
                    ));
                }
            }
        }

        return list;
    }

    public static List<CommitFile> getCommitFiles(Path dir, String hash) {
        List<CommitFile> files = new ArrayList<>();
        if (!isRepository(dir) || hash == null || hash.isBlank()) return files;

        Result diffTree = exec(dir, "diff-tree", "--no-commit-id", "--name-status", "-r", hash);
        if (diffTree.ok() && !diffTree.output().isBlank()) {
            for (String fLine : diffTree.output().split("\\R")) {
                CommitFile cf = CommitFile.fromLine(fLine);
                if (cf != null) files.add(cf);
            }
        }
        return files;
    }

    public static String getFileContentAtCommit(Path dir, String hash, String relPath) {
        if (!isRepository(dir) || hash == null || hash.isBlank() || relPath == null || relPath.isBlank()) {
            return "";
        }
        String cleanPath = relPath.replace('\\', '/');
        Result r = exec(dir, "show", hash + ":" + cleanPath);
        return r.ok() ? r.output() : "";
    }

    public static List<String> getBranchesContaining(Path dir, String hash) {
        List<String> branches = new ArrayList<>();
        if (!isRepository(dir) || hash == null || hash.isBlank()) return branches;

        Result r = exec(dir, "branch", "-a", "--contains", hash);
        if (r.ok() && !r.output().isBlank()) {
            for (String line : r.output().split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.contains("->")) continue;
                if (trimmed.startsWith("* ")) {
                    trimmed = trimmed.substring(2).trim();
                }
                if (trimmed.startsWith("remotes/")) {
                    trimmed = trimmed.substring(8).trim();
                }
                if (!branches.contains(trimmed)) {
                    branches.add(trimmed);
                }
            }
        }
        return branches;
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

    public static List<String> remotes(Path dir) {
        List<String> list = new ArrayList<>();
        Result r = exec(dir, "remote");
        if (r.ok()) {
            for (String line : r.output().split("\\R")) {
                line = line.trim();
                if (!line.isBlank()) list.add(line);
            }
        }
        return list;
    }

    public static List<String> remoteBranchesForRemote(Path dir, String remote) {
        List<String> branches = new ArrayList<>();
        String prefix = (remote != null && !remote.isBlank()) ? remote + "/" : "";
        for (String rb : remoteBranches(dir)) {
            if (prefix.isEmpty()) {
                branches.add(rb);
            } else if (rb.startsWith(prefix)) {
                branches.add(rb.substring(prefix.length()));
            }
        }
        return branches;
    }

    public static Process startProcess(Path dir, Map<String, String> env, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(GitSettingsManager.getInstance().getEffectiveGitExecutable());
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(dir.toFile())
                .redirectErrorStream(true);
        if (env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }
        return pb.start();
    }

    public static Result execWithEnv(Path dir, Map<String, String> env, String... args) {
        try {
            Process p = startProcess(dir, env, args);
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

    public static Result fetch(Path dir, String remote, Map<String, String> env) {
        if (remote != null && !remote.isBlank()) {
            return execWithEnv(dir, env, "fetch", remote);
        }
        return execWithEnv(dir, env, "fetch", "--all", "--prune");
    }

    public static Result pull(Path dir, String remote, String branch, List<String> options, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add("pull");
        if (options != null) {
            for (String opt : options) {
                if (opt != null && !opt.isBlank()) args.add(opt.trim());
            }
        }
        if (remote != null && !remote.isBlank()) {
            args.add(remote.trim());
        }
        if (branch != null && !branch.isBlank()) {
            args.add(branch.trim());
        }
        return execWithEnv(dir, env, args.toArray(new String[0]));
    }

    public static List<String> allBranches(Path dir) {
        List<String> list = new ArrayList<>();
        for (String b : localBranches(dir)) {
            if (!b.isBlank() && !list.contains(b)) {
                list.add(b);
            }
        }
        for (String rb : remoteBranches(dir)) {
            if (!rb.isBlank() && !list.contains(rb)) {
                list.add(rb);
            }
        }
        return list;
    }

    public static Result merge(Path dir, String branch, List<String> options, String commitMessage, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add("merge");
        if (options != null) {
            for (String opt : options) {
                if (opt != null && !opt.isBlank()) args.add(opt.trim());
            }
        }
        if (commitMessage != null && !commitMessage.isBlank()) {
            args.add("-m");
            args.add(commitMessage.trim());
        }
        if (branch != null && !branch.isBlank()) {
            args.add(branch.trim());
        }
        return execWithEnv(dir, env, args.toArray(new String[0]));
    }

    public static Result rebase(Path dir, String branchOrHash, String ontoBranch, List<String> options, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add("rebase");
        if (options != null) {
            for (String opt : options) {
                if (opt != null && !opt.isBlank()) args.add(opt.trim());
            }
        }
        if (ontoBranch != null && !ontoBranch.isBlank()) {
            args.add("--onto");
            args.add(ontoBranch.trim());
        }
        if (branchOrHash != null && !branchOrHash.isBlank()) {
            args.add(branchOrHash.trim());
        }
        return execWithEnv(dir, env, args.toArray(new String[0]));
    }

    public static Result validateRevision(Path dir, String revision) {
        if (dir == null || revision == null || revision.isBlank()) {
            return new Result(-1, "Revision cannot be blank");
        }
        return exec(dir, "rev-parse", "--verify", revision.trim());
    }

    public static Result createTag(Path dir, String tagName, String commit, String message, boolean force) {
        return createTag(dir, tagName, commit, message, force, null);
    }

    public static Result createTag(Path dir, String tagName, String commit, String message, boolean force, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add("tag");
        if (force) {
            args.add("-f");
        }
        if (message != null && !message.isBlank()) {
            args.add("-a");
            args.add("-m");
            args.add(message.trim());
        }
        if (tagName != null && !tagName.isBlank()) {
            args.add(tagName.trim());
        }
        if (commit != null && !commit.isBlank()) {
            args.add(commit.trim());
        }
        return execWithEnv(dir, env, args.toArray(new String[0]));
    }

    public static Result resetHead(Path dir, String resetType, String commit) {
        return resetHead(dir, resetType, commit, null);
    }

    public static Result resetHead(Path dir, String resetType, String commit, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add("reset");
        String type = (resetType != null && !resetType.isBlank()) ? resetType.trim().toLowerCase() : "mixed";
        if ("hard".equals(type)) {
            args.add("--hard");
        } else if ("soft".equals(type)) {
            args.add("--soft");
        } else {
            args.add("--mixed");
        }
        String target = (commit != null && !commit.isBlank()) ? commit.trim() : "HEAD";
        args.add(target);
        return execWithEnv(dir, env, args.toArray(new String[0]));
    }

    public static Result exec(Path dir, String... args) {
        return execWithEnv(dir, null, args);
    }
}