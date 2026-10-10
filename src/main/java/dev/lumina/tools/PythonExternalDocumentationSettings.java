package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Python External Documentation configuration settings in Lumina IDE.
 */
public class PythonExternalDocumentationSettings implements Cloneable {

    private List<PythonDocUrlEntry> entries = new ArrayList<>();

    public PythonExternalDocumentationSettings() {
        initDefaults();
    }

    private void initDefaults() {
        entries.clear();
        entries.add(new PythonDocUrlEntry("pyramid", "http://docs.pylonsproject.org/projects/pyramid/en/latest/api/{module.basename}.html#{element.qname}"));
        entries.add(new PythonDocUrlEntry("pandas", "https://pandas.pydata.org/pandas-docs/stable/generated/{element.qname}.html"));
        entries.add(new PythonDocUrlEntry("PySide", "http://pyside.github.io/docs/pyside/{module.name.slashes}/{class.name}.html#{module.name}.{element.qname}"));
        entries.add(new PythonDocUrlEntry("PyQt5", "http://doc.qt.io/qt-5/{class.name.lower}.html#{functionToProperty.name}{functionIsProperty?-prop}"));
        entries.add(new PythonDocUrlEntry("kivy", "http://kivy.org/docs/api/{module.name}.html"));
        entries.add(new PythonDocUrlEntry("PyQt4", "http://pyqt.sourceforge.net/Docs/PyQt4/{class.name.lower}.html#{function.name}"));
        entries.add(new PythonDocUrlEntry("flask", "http://flask.pocoo.org/docs/latest/api/#{element.qname}"));
        entries.add(new PythonDocUrlEntry("matplotlib", "http://matplotlib.org/api/{module.basename}_api.html#{element.qname}"));
        entries.add(new PythonDocUrlEntry("wx", "http://www.wxpython.org/docs/api/{module.name}.{class.name}-class.html#{function.name}"));
        entries.add(new PythonDocUrlEntry("gtk", "http://library.gnome.org/devel/pygtk/stable/class-gtk{class.name.lower}.html#method-gtk{class.name.lower}--{function.name}"));
    }

    public List<PythonDocUrlEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PythonDocUrlEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public PythonExternalDocumentationSettings copy() {
        return clone();
    }

    @Override
    public PythonExternalDocumentationSettings clone() {
        try {
            PythonExternalDocumentationSettings copy = (PythonExternalDocumentationSettings) super.clone();
            copy.entries = new ArrayList<>();
            for (PythonDocUrlEntry e : this.entries) {
                copy.entries.add(e.clone());
            }
            return copy;
        } catch (CloneNotSupportedException ex) {
            PythonExternalDocumentationSettings copy = new PythonExternalDocumentationSettings();
            copy.entries = new ArrayList<>();
            for (PythonDocUrlEntry e : this.entries) {
                copy.entries.add(e.clone());
            }
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonExternalDocumentationSettings that = (PythonExternalDocumentationSettings) o;
        return Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }
}
