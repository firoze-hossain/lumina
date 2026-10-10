package dev.lumina.typescript;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > TypeScript settings in Lumina IDE.
 * Matches reference screenshot media_1791602860333_b0b06ef7.png:
 *  - Node interpreter
 *  - TypeScript package
 *  - TypeScript language service (Show project errors, Show suggestions, Enable service-powered type engine, Recompile on changes)
 *  - Options
 */
public class TypeScriptSettings implements Cloneable {

    public static final String DEFAULT_NODE = "Project node (/usr/local/bin/node)   24.11.1";
    public static final String DEFAULT_TYPESCRIPT = "Bundled   5.7.3";

    private String nodeInterpreter = DEFAULT_NODE;
    private String typeScriptPackage = DEFAULT_TYPESCRIPT;
    private boolean useLanguageService = true;
    private boolean showProjectErrors = true;
    private boolean showSuggestions = true;
    private boolean enableServicePoweredTypeEngine = false;
    private boolean recompileOnChanges = false;
    private String options = "";

    public TypeScriptSettings() {
    }

    public TypeScriptSettings(TypeScriptSettings other) {
        if (other != null) {
            this.nodeInterpreter = other.nodeInterpreter;
            this.typeScriptPackage = other.typeScriptPackage;
            this.useLanguageService = other.useLanguageService;
            this.showProjectErrors = other.showProjectErrors;
            this.showSuggestions = other.showSuggestions;
            this.enableServicePoweredTypeEngine = other.enableServicePoweredTypeEngine;
            this.recompileOnChanges = other.recompileOnChanges;
            this.options = other.options;
        }
    }

    public String getNodeInterpreter() {
        return nodeInterpreter;
    }

    public void setNodeInterpreter(String nodeInterpreter) {
        this.nodeInterpreter = nodeInterpreter != null ? nodeInterpreter : DEFAULT_NODE;
    }

    public String getTypeScriptPackage() {
        return typeScriptPackage;
    }

    public void setTypeScriptPackage(String typeScriptPackage) {
        this.typeScriptPackage = typeScriptPackage != null ? typeScriptPackage : DEFAULT_TYPESCRIPT;
    }

    public boolean isUseLanguageService() {
        return useLanguageService;
    }

    public void setUseLanguageService(boolean useLanguageService) {
        this.useLanguageService = useLanguageService;
    }

    public boolean isShowProjectErrors() {
        return showProjectErrors;
    }

    public void setShowProjectErrors(boolean showProjectErrors) {
        this.showProjectErrors = showProjectErrors;
    }

    public boolean isShowSuggestions() {
        return showSuggestions;
    }

    public void setShowSuggestions(boolean showSuggestions) {
        this.showSuggestions = showSuggestions;
    }

    public boolean isEnableServicePoweredTypeEngine() {
        return enableServicePoweredTypeEngine;
    }

    public void setEnableServicePoweredTypeEngine(boolean enableServicePoweredTypeEngine) {
        this.enableServicePoweredTypeEngine = enableServicePoweredTypeEngine;
    }

    public boolean isRecompileOnChanges() {
        return recompileOnChanges;
    }

    public void setRecompileOnChanges(boolean recompileOnChanges) {
        this.recompileOnChanges = recompileOnChanges;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options != null ? options : "";
    }

    @Override
    public TypeScriptSettings clone() {
        return new TypeScriptSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeScriptSettings that = (TypeScriptSettings) o;
        return useLanguageService == that.useLanguageService &&
                showProjectErrors == that.showProjectErrors &&
                showSuggestions == that.showSuggestions &&
                enableServicePoweredTypeEngine == that.enableServicePoweredTypeEngine &&
                recompileOnChanges == that.recompileOnChanges &&
                Objects.equals(nodeInterpreter, that.nodeInterpreter) &&
                Objects.equals(typeScriptPackage, that.typeScriptPackage) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeInterpreter, typeScriptPackage, useLanguageService,
                showProjectErrors, showSuggestions, enableServicePoweredTypeEngine, recompileOnChanges, options);
    }
}
