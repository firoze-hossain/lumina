package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Async Stack Traces (Image 2).
 */
public class AsyncStackTraceSettings implements Cloneable {

    private boolean instrumentingAgent = true;
    private List<String> configuredAnnotations = new ArrayList<>();
    private List<AsyncStackTraceRule> rules = new ArrayList<>();
    private boolean captureLocalVariables = false;

    public AsyncStackTraceSettings() {
        initDefaults();
    }

    public AsyncStackTraceSettings(AsyncStackTraceSettings other) {
        if (other != null) {
            this.instrumentingAgent = other.instrumentingAgent;
            this.configuredAnnotations = new ArrayList<>(other.configuredAnnotations);
            this.rules = new ArrayList<>();
            for (AsyncStackTraceRule r : other.rules) {
                this.rules.add(r.clone());
            }
            this.captureLocalVariables = other.captureLocalVariables;
        }
    }

    public void initDefaults() {
        configuredAnnotations = new ArrayList<>(List.of(
                "org.jetbrains.annotations.Async.Schedule",
                "org.jetbrains.annotations.Async.Execute",
                "org.springframework.scheduling.annotation.Async"
        ));

        rules = new ArrayList<>(List.of(
                new AsyncStackTraceRule(true,
                        "java.util.concurrent.CompletableFuture", "supplyAsync", "this",
                        "java.util.concurrent.CompletableFuture$AsyncSupply", "run", "this"),
                new AsyncStackTraceRule(true,
                        "java.util.concurrent.CompletableFuture", "runAsync", "this",
                        "java.util.concurrent.CompletableFuture$AsyncRun", "run", "this"),
                new AsyncStackTraceRule(true,
                        "java.lang.Thread", "start", "this",
                        "java.lang.Runnable", "run", "this"),
                new AsyncStackTraceRule(true,
                        "java.util.concurrent.Executor", "execute", "command",
                        "java.lang.Runnable", "run", "this"),
                new AsyncStackTraceRule(true,
                        "java.util.concurrent.ForkJoinTask", "fork", "this",
                        "java.util.concurrent.ForkJoinTask", "exec", "this"),
                new AsyncStackTraceRule(true,
                        "kotlinx.coroutines.BuildersKt", "launch", "block",
                        "kotlin.coroutines.Continuation", "resumeWith", "this")
        ));
    }

    public boolean isInstrumentingAgent() {
        return instrumentingAgent;
    }

    public void setInstrumentingAgent(boolean instrumentingAgent) {
        this.instrumentingAgent = instrumentingAgent;
    }

    public List<String> getConfiguredAnnotations() {
        return configuredAnnotations;
    }

    public void setConfiguredAnnotations(List<String> configuredAnnotations) {
        this.configuredAnnotations = configuredAnnotations != null ? new ArrayList<>(configuredAnnotations) : new ArrayList<>();
    }

    public List<AsyncStackTraceRule> getRules() {
        return rules;
    }

    public void setRules(List<AsyncStackTraceRule> rules) {
        this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    public boolean isCaptureLocalVariables() {
        return captureLocalVariables;
    }

    public void setCaptureLocalVariables(boolean captureLocalVariables) {
        this.captureLocalVariables = captureLocalVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsyncStackTraceSettings that)) return false;
        return instrumentingAgent == that.instrumentingAgent &&
                captureLocalVariables == that.captureLocalVariables &&
                Objects.equals(configuredAnnotations, that.configuredAnnotations) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentingAgent, configuredAnnotations, rules, captureLocalVariables);
    }

    @Override
    public AsyncStackTraceSettings clone() {
        return new AsyncStackTraceSettings(this);
    }
}
