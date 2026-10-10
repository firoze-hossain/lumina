package dev.lumina.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings configuration model for Database User Parameters in Lumina IDE.
 */
public class DatabaseUserParametersSettings implements Cloneable {

    private boolean enableInConsolesAndSqlFiles = true;
    private boolean enableInLiteralsWithSqlInjection = true;
    private boolean substituteInsideSqlStrings = false;
    private List<DatabaseUserParameterPattern> patterns = new ArrayList<>();

    public DatabaseUserParametersSettings() {
        initDefaultPatterns();
    }

    private void initDefaultPatterns() {
        patterns.clear();
        patterns.add(new DatabaseUserParameterPattern("\"#name#\"", "everywhere", "XML"));
        patterns.add(new DatabaseUserParameterPattern("\"$a.b.c$?\"", "everywhere", "All excl. SQL"));
        patterns.add(new DatabaseUserParameterPattern("\"#{a.b.c}?\"", "everywhere", "All excl. SQL"));
        patterns.add(new DatabaseUserParameterPattern("\"%(name)s\"", "everywhere", "Python"));
        patterns.add(new DatabaseUserParameterPattern("\"%name\"", "everywhere", "JAVA, PHP, Python"));
        patterns.add(new DatabaseUserParameterPattern("\":'name'\"", "everywhere", "PostgreSQL"));
        patterns.add(new DatabaseUserParameterPattern("\"${name}\"", "everywhere", ""));
        patterns.add(new DatabaseUserParameterPattern("\"$(name)\"", "everywhere", ""));
    }

    public boolean isEnableInConsolesAndSqlFiles() {
        return enableInConsolesAndSqlFiles;
    }

    public void setEnableInConsolesAndSqlFiles(boolean enableInConsolesAndSqlFiles) {
        this.enableInConsolesAndSqlFiles = enableInConsolesAndSqlFiles;
    }

    public boolean isEnableInLiteralsWithSqlInjection() {
        return enableInLiteralsWithSqlInjection;
    }

    public void setEnableInLiteralsWithSqlInjection(boolean enableInLiteralsWithSqlInjection) {
        this.enableInLiteralsWithSqlInjection = enableInLiteralsWithSqlInjection;
    }

    public boolean isSubstituteInsideSqlStrings() {
        return substituteInsideSqlStrings;
    }

    public void setSubstituteInsideSqlStrings(boolean substituteInsideSqlStrings) {
        this.substituteInsideSqlStrings = substituteInsideSqlStrings;
    }

    public List<DatabaseUserParameterPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<DatabaseUserParameterPattern> patterns) {
        this.patterns = patterns != null ? new ArrayList<>(patterns) : new ArrayList<>();
    }

    @Override
    public DatabaseUserParametersSettings clone() {
        try {
            DatabaseUserParametersSettings copy = (DatabaseUserParametersSettings) super.clone();
            copy.patterns = new ArrayList<>();
            for (DatabaseUserParameterPattern p : this.patterns) {
                copy.patterns.add(p.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseUserParametersSettings that = (DatabaseUserParametersSettings) o;
        return enableInConsolesAndSqlFiles == that.enableInConsolesAndSqlFiles &&
                enableInLiteralsWithSqlInjection == that.enableInLiteralsWithSqlInjection &&
                substituteInsideSqlStrings == that.substituteInsideSqlStrings &&
                Objects.equals(patterns, that.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableInConsolesAndSqlFiles, enableInLiteralsWithSqlInjection, substituteInsideSqlStrings, patterns);
    }
}
