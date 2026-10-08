package dev.lumina.kubernetes;

import java.util.Objects;

/**
 * Model representing a custom cluster argument for kubectl proxy/command execution.
 */
public class KubernetesCustomArg implements Cloneable {

    private String cluster;
    private String parameter;

    public KubernetesCustomArg() {
        this("", "");
    }

    public KubernetesCustomArg(String cluster, String parameter) {
        this.cluster = cluster;
        this.parameter = parameter;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public KubernetesCustomArg clone() {
        try {
            return (KubernetesCustomArg) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KubernetesCustomArg(this.cluster, this.parameter);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesCustomArg that = (KubernetesCustomArg) o;
        return Objects.equals(cluster, that.cluster) &&
                Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cluster, parameter);
    }
}
