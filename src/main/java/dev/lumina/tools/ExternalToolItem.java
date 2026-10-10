package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing an External Tool configuration in Lumina IDE.
 */
public class ExternalToolItem implements Cloneable {

    private String name;
    private String group = "External Tools";
    private String description = "";
    private String program = "";
    private String arguments = "";
    private String workingDirectory = "";

    private boolean synchronizeFiles = true;
    private boolean openConsole = true;
    private boolean makeActiveOnStdout = false;
    private boolean makeActiveOnStderr = false;
    private String outputFilters = "";

    public ExternalToolItem() {
        this("", "External Tools");
    }

    public ExternalToolItem(String name, String group) {
        this.name = name != null ? name : "";
        this.group = group != null ? group : "External Tools";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group != null ? group : "External Tools";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program != null ? program : "";
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments != null ? arguments : "";
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory != null ? workingDirectory : "";
    }

    public boolean isSynchronizeFiles() {
        return synchronizeFiles;
    }

    public void setSynchronizeFiles(boolean synchronizeFiles) {
        this.synchronizeFiles = synchronizeFiles;
    }

    public boolean isOpenConsole() {
        return openConsole;
    }

    public void setOpenConsole(boolean openConsole) {
        this.openConsole = openConsole;
    }

    public boolean isMakeActiveOnStdout() {
        return makeActiveOnStdout;
    }

    public void setMakeActiveOnStdout(boolean makeActiveOnStdout) {
        this.makeActiveOnStdout = makeActiveOnStdout;
    }

    public boolean isMakeActiveOnStderr() {
        return makeActiveOnStderr;
    }

    public void setMakeActiveOnStderr(boolean makeActiveOnStderr) {
        this.makeActiveOnStderr = makeActiveOnStderr;
    }

    public String getOutputFilters() {
        return outputFilters;
    }

    public void setOutputFilters(String outputFilters) {
        this.outputFilters = outputFilters != null ? outputFilters : "";
    }

    @Override
    public ExternalToolItem clone() {
        try {
            return (ExternalToolItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalToolItem that = (ExternalToolItem) o;
        return synchronizeFiles == that.synchronizeFiles &&
                openConsole == that.openConsole &&
                makeActiveOnStdout == that.makeActiveOnStdout &&
                makeActiveOnStderr == that.makeActiveOnStderr &&
                Objects.equals(name, that.name) &&
                Objects.equals(group, that.group) &&
                Objects.equals(description, that.description) &&
                Objects.equals(program, that.program) &&
                Objects.equals(arguments, that.arguments) &&
                Objects.equals(workingDirectory, that.workingDirectory) &&
                Objects.equals(outputFilters, that.outputFilters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, group, description, program, arguments, workingDirectory,
                synchronizeFiles, openConsole, makeActiveOnStdout, makeActiveOnStderr, outputFilters);
    }
}
