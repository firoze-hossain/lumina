package dev.lumina.git;

import java.util.*;

/**
 * Computes topological graph lanes and rendering metadata for git log commit visualization.
 */
public final class GitLogGraph {

    public static final String[] LANE_COLORS = {
            "#56A8F5", // Blue
            "#59A869", // Green
            "#BA68C8", // Purple
            "#E5C07B", // Gold / Yellow
            "#E06C75", // Red / Coral
            "#4EC9B0", // Teal
            "#FF9800", // Orange
            "#64B5F6"  // Light Blue
    };

    public record Connection(int fromLane, int toLane) {}

    public record RowGraph(
            int commitLane,
            Set<Integer> activePassingLanes,
            List<Connection> downConnections,
            List<Connection> upConnections
    ) {
        public String getCommitColor() {
            return getLaneColor(commitLane);
        }
    }

    public static String getLaneColor(int lane) {
        if (lane < 0) return LANE_COLORS[0];
        return LANE_COLORS[lane % LANE_COLORS.length];
    }

    /**
     * Analyzes a list of commits in topological order and assigns lanes and connections.
     * Returns a map of commit hash -> RowGraph.
     */
    public static Map<String, RowGraph> compute(List<GitLogCommit> commits) {
        Map<String, RowGraph> result = new HashMap<>();
        if (commits == null || commits.isEmpty()) return result;

        List<String> activeTracks = new ArrayList<>();
        Map<String, Integer> commitLanes = new HashMap<>();

        // First pass: assign lanes to each commit
        for (GitLogCommit c : commits) {
            String hash = c.hash();
            int lane = activeTracks.indexOf(hash);
            if (lane == -1) {
                // Find first null or empty slot in activeTracks
                int freeSlot = activeTracks.indexOf(null);
                if (freeSlot >= 0) {
                    lane = freeSlot;
                    activeTracks.set(lane, hash);
                } else {
                    lane = activeTracks.size();
                    activeTracks.add(hash);
                }
            }
            commitLanes.put(hash, lane);

            // Replace current commit with its first parent in the same lane
            List<String> parents = c.parents();
            if (parents != null && !parents.isEmpty()) {
                String firstParent = parents.get(0);
                activeTracks.set(lane, firstParent);

                // Additional parents (merge commit) get assigned to other lanes
                for (int i = 1; i < parents.size(); i++) {
                    String p = parents.get(i);
                    if (!activeTracks.contains(p)) {
                        int pSlot = activeTracks.indexOf(null);
                        if (pSlot >= 0) {
                            activeTracks.set(pSlot, p);
                        } else {
                            activeTracks.add(p);
                        }
                    }
                }
            } else {
                activeTracks.set(lane, null); // Root commit in branch
            }
        }

        // Second pass: compute connections and passing lanes for each commit
        activeTracks.clear();
        for (int i = 0; i < commits.size(); i++) {
            GitLogCommit c = commits.get(i);
            String hash = c.hash();
            int lane = commitLanes.getOrDefault(hash, 0);

            Set<Integer> passingLanes = new HashSet<>();
            for (int t = 0; t < activeTracks.size(); t++) {
                String tracked = activeTracks.get(t);
                if (tracked != null && !tracked.equals(hash)) {
                    passingLanes.add(t);
                }
            }

            List<Connection> downConnections = new ArrayList<>();
            List<String> parents = c.parents();
            if (parents != null) {
                for (String p : parents) {
                    Integer pLane = commitLanes.get(p);
                    if (pLane != null) {
                        downConnections.add(new Connection(lane, pLane));
                    }
                }
            }

            // Up connections from previously examined children
            List<Connection> upConnections = new ArrayList<>();
            for (int t = 0; t < activeTracks.size(); t++) {
                if (hash.equals(activeTracks.get(t))) {
                    upConnections.add(new Connection(t, lane));
                }
            }

            result.put(hash, new RowGraph(lane, passingLanes, downConnections, upConnections));

            // Update activeTracks for next iteration
            while (activeTracks.remove(hash)) {}
            if (parents != null) {
                for (String p : parents) {
                    if (!activeTracks.contains(p)) {
                        activeTracks.add(p);
                    }
                }
            }
        }

        return result;
    }
}
