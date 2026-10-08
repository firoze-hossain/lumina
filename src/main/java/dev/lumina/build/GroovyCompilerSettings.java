package dev.lumina.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Build, Execution, Deployment > Compiler > Groovy Compiler.
 * Matches standard IDE settings 1:1 (media_1791430028667.png):
 *  - Path to configscript
 *  - Invoke dynamic support
 *  - Exclude from stub generation (list of exclude paths with recursive flag)
 */
public class GroovyCompilerSettings implements Cloneable {

    private String configScriptPath = "";
    private boolean invokeDynamicSupport = false;
    private List<CompilerExcludeEntry> stubGenerationExcludes = new ArrayList<>();

    public GroovyCompilerSettings() {
    }

    public GroovyCompilerSettings(GroovyCompilerSettings other) {
        if (other != null) {
            this.configScriptPath = other.configScriptPath;
            this.invokeDynamicSupport = other.invokeDynamicSupport;
            if (other.stubGenerationExcludes != null) {
                this.stubGenerationExcludes = new ArrayList<>();
                for (CompilerExcludeEntry e : other.stubGenerationExcludes) {
                    this.stubGenerationExcludes.add(e.clone());
                }
            }
        }
    }

    public String getConfigScriptPath() {
        return configScriptPath != null ? configScriptPath : "";
    }

    public void setConfigScriptPath(String configScriptPath) {
        this.configScriptPath = configScriptPath != null ? configScriptPath.trim() : "";
    }

    public boolean isInvokeDynamicSupport() {
        return invokeDynamicSupport;
    }

    public void setInvokeDynamicSupport(boolean invokeDynamicSupport) {
        this.invokeDynamicSupport = invokeDynamicSupport;
    }

    public List<CompilerExcludeEntry> getStubGenerationExcludes() {
        return stubGenerationExcludes != null ? stubGenerationExcludes : new ArrayList<>();
    }

    public void setStubGenerationExcludes(List<CompilerExcludeEntry> stubGenerationExcludes) {
        if (stubGenerationExcludes != null) {
            this.stubGenerationExcludes = new ArrayList<>(stubGenerationExcludes);
        } else {
            this.stubGenerationExcludes = new ArrayList<>();
        }
    }

    @Override
    public GroovyCompilerSettings clone() {
        return new GroovyCompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroovyCompilerSettings that = (GroovyCompilerSettings) o;
        return invokeDynamicSupport == that.invokeDynamicSupport &&
                Objects.equals(configScriptPath, that.configScriptPath) &&
                Objects.equals(stubGenerationExcludes, that.stubGenerationExcludes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configScriptPath, invokeDynamicSupport, stubGenerationExcludes);
    }
}
