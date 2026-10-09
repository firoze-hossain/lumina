package dev.lumina.kubernetes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > Kubernetes in Lumina IDE.
 * Dynamically discovers active clusters from kubeconfig files.
 */
public class KubernetesLanguageSettings {

    public static final String DEFAULT_CLUSTER = "<Cluster not configured>";
    public static final String DEFAULT_API_VERSION = "<Latest Version>";
    public static final String DEFAULT_KUSTOMIZE_VERSION = "5.x";

    public static final List<String> AVAILABLE_API_VERSIONS = List.of(
            "<Latest Version>",
            "1.32",
            "1.31",
            "1.30",
            "1.29",
            "1.28",
            "1.27",
            "1.26",
            "1.25"
    );

    public static final List<String> AVAILABLE_KUSTOMIZE_VERSIONS = List.of(
            "5.x",
            "4.x",
            "3.x",
            "2.x"
    );

    private String currentCluster = DEFAULT_CLUSTER;
    private String apiVersion = DEFAULT_API_VERSION;
    private boolean useApiSchemaFromActiveCluster = false;
    private String kustomizeVersion = DEFAULT_KUSTOMIZE_VERSION;
    private List<KubernetesResourceSpec> specifications = new ArrayList<>();

    public KubernetesLanguageSettings() {
    }

    public KubernetesLanguageSettings(KubernetesLanguageSettings other) {
        if (other != null) {
            this.currentCluster = other.currentCluster;
            this.apiVersion = other.apiVersion;
            this.useApiSchemaFromActiveCluster = other.useApiSchemaFromActiveCluster;
            this.kustomizeVersion = other.kustomizeVersion;
            if (other.specifications != null) {
                for (KubernetesResourceSpec spec : other.specifications) {
                    this.specifications.add(spec.copy());
                }
            }
        }
    }

    public KubernetesLanguageSettings copy() {
        return new KubernetesLanguageSettings(this);
    }

    public String getCurrentCluster() {
        return currentCluster;
    }

    public void setCurrentCluster(String currentCluster) {
        this.currentCluster = currentCluster != null ? currentCluster : DEFAULT_CLUSTER;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion != null ? apiVersion : DEFAULT_API_VERSION;
    }

    public boolean isUseApiSchemaFromActiveCluster() {
        return useApiSchemaFromActiveCluster;
    }

    public void setUseApiSchemaFromActiveCluster(boolean useApiSchemaFromActiveCluster) {
        this.useApiSchemaFromActiveCluster = useApiSchemaFromActiveCluster;
    }

    public String getKustomizeVersion() {
        return kustomizeVersion;
    }

    public void setKustomizeVersion(String kustomizeVersion) {
        this.kustomizeVersion = kustomizeVersion != null ? kustomizeVersion : DEFAULT_KUSTOMIZE_VERSION;
    }

    public List<KubernetesResourceSpec> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<KubernetesResourceSpec> specifications) {
        this.specifications.clear();
        if (specifications != null) {
            for (KubernetesResourceSpec s : specifications) {
                this.specifications.add(s.copy());
            }
        }
    }

    /**
     * Dynamically detects available clusters from system kubeconfig files.
     */
    public static List<String> detectAvailableClusters() {
        List<String> clusters = new ArrayList<>();
        clusters.add(DEFAULT_CLUSTER);

        String kubeconfigEnv = System.getenv("KUBECONFIG");
        List<Path> candidatePaths = new ArrayList<>();
        if (kubeconfigEnv != null && !kubeconfigEnv.isBlank()) {
            for (String part : kubeconfigEnv.split(File.pathSeparator)) {
                candidatePaths.add(Path.of(part.trim()));
            }
        }
        candidatePaths.add(Path.of(System.getProperty("user.home"), ".kube", "config"));

        for (Path p : candidatePaths) {
            try {
                if (Files.exists(p) && Files.isReadable(p)) {
                    List<String> lines = Files.readAllLines(p);
                    boolean inContexts = false;
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("contexts:")) {
                            inContexts = true;
                        } else if (inContexts && (trimmed.startsWith("clusters:") || trimmed.startsWith("users:"))) {
                            inContexts = false;
                        } else if (inContexts && trimmed.startsWith("name:")) {
                            String name = trimmed.substring("name:".length()).trim();
                            if (!name.isEmpty() && !clusters.contains(name)) {
                                clusters.add(name);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return clusters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesLanguageSettings that = (KubernetesLanguageSettings) o;
        return useApiSchemaFromActiveCluster == that.useApiSchemaFromActiveCluster &&
                Objects.equals(currentCluster, that.currentCluster) &&
                Objects.equals(apiVersion, that.apiVersion) &&
                Objects.equals(kustomizeVersion, that.kustomizeVersion) &&
                Objects.equals(specifications, that.specifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCluster, apiVersion, useApiSchemaFromActiveCluster, kustomizeVersion, specifications);
    }
}
