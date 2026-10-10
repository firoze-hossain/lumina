package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Kotlin Notebook > Settings for New Notebooks in Lumina IDE.
 */
public class KotlinNotebookNewNotebooksSettings implements Cloneable {

    private boolean addProjectLibrariesToClasspath = false;

    public KotlinNotebookNewNotebooksSettings() {
    }

    public boolean isAddProjectLibrariesToClasspath() {
        return addProjectLibrariesToClasspath;
    }

    public void setAddProjectLibrariesToClasspath(boolean addProjectLibrariesToClasspath) {
        this.addProjectLibrariesToClasspath = addProjectLibrariesToClasspath;
    }

    public KotlinNotebookNewNotebooksSettings copy() {
        return clone();
    }

    @Override
    public KotlinNotebookNewNotebooksSettings clone() {
        try {
            return (KotlinNotebookNewNotebooksSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            KotlinNotebookNewNotebooksSettings copy = new KotlinNotebookNewNotebooksSettings();
            copy.addProjectLibrariesToClasspath = this.addProjectLibrariesToClasspath;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinNotebookNewNotebooksSettings that = (KotlinNotebookNewNotebooksSettings) o;
        return addProjectLibrariesToClasspath == that.addProjectLibrariesToClasspath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addProjectLibrariesToClasspath);
    }
}
