package dev.lumina.scala;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Misc settings.
 * Matches Image 5:
 *  - ScalaTest default super class: default "org.scalatest.funsuite.AnyFunSuiteLike"
 *  - Trailing commas: default "Auto" ("Auto", "Always", "Never")
 *  - Language Injection Settings for Interpolated Strings (19 default rules)
 */
public class ScalaMiscSettings {

    public static final String DEFAULT_SCALATEST_SUPER_CLASS = "org.scalatest.funsuite.AnyFunSuiteLike";
    public static final String DEFAULT_TRAILING_COMMAS = "Auto";

    private String scalaTestDefaultSuperClass = DEFAULT_SCALATEST_SUPER_CLASS;
    private String trailingCommas = DEFAULT_TRAILING_COMMAS;
    private List<ScalaInterpolatedStringInjection> injections = createDefaultInjections();

    public static List<ScalaInterpolatedStringInjection> createDefaultInjections() {
        List<ScalaInterpolatedStringInjection> list = new ArrayList<>();
        list.add(new ScalaInterpolatedStringInjection("css", "CSS"));
        list.add(new ScalaInterpolatedStringInjection("fr", "SQL"));
        list.add(new ScalaInterpolatedStringInjection("fr0", "SQL"));
        list.add(new ScalaInterpolatedStringInjection("html", "HTML"));
        list.add(new ScalaInterpolatedStringInjection("java", "JAVA"));
        list.add(new ScalaInterpolatedStringInjection("js", "JavaScript"));
        list.add(new ScalaInterpolatedStringInjection("json", "JSON"));
        list.add(new ScalaInterpolatedStringInjection("json5", "JSON5"));
        list.add(new ScalaInterpolatedStringInjection("less", "LESS"));
        list.add(new ScalaInterpolatedStringInjection("md", "Markdown"));
        list.add(new ScalaInterpolatedStringInjection("protobuf", "protobuf"));
        list.add(new ScalaInterpolatedStringInjection("scala", "Scala"));
        list.add(new ScalaInterpolatedStringInjection("sql", "SQL"));
        list.add(new ScalaInterpolatedStringInjection("sqlu", "SQL"));
        list.add(new ScalaInterpolatedStringInjection("svg", "SVG"));
        list.add(new ScalaInterpolatedStringInjection("xhtml", "XHTML"));
        list.add(new ScalaInterpolatedStringInjection("xml", "XML"));
        list.add(new ScalaInterpolatedStringInjection("xpath", "XPath"));
        list.add(new ScalaInterpolatedStringInjection("yaml", "yaml"));
        return list;
    }

    public ScalaMiscSettings() {
    }

    public ScalaMiscSettings(String scalaTestDefaultSuperClass, String trailingCommas, List<ScalaInterpolatedStringInjection> injections) {
        this.scalaTestDefaultSuperClass = scalaTestDefaultSuperClass != null ? scalaTestDefaultSuperClass : DEFAULT_SCALATEST_SUPER_CLASS;
        this.trailingCommas = trailingCommas != null ? trailingCommas : DEFAULT_TRAILING_COMMAS;
        this.injections = new ArrayList<>();
        if (injections != null) {
            for (ScalaInterpolatedStringInjection rule : injections) {
                this.injections.add(rule != null ? rule.copy() : new ScalaInterpolatedStringInjection());
            }
        }
    }

    public String getScalaTestDefaultSuperClass() {
        return scalaTestDefaultSuperClass;
    }

    public void setScalaTestDefaultSuperClass(String scalaTestDefaultSuperClass) {
        this.scalaTestDefaultSuperClass = scalaTestDefaultSuperClass != null ? scalaTestDefaultSuperClass : DEFAULT_SCALATEST_SUPER_CLASS;
    }

    public String getTrailingCommas() {
        return trailingCommas;
    }

    public void setTrailingCommas(String trailingCommas) {
        this.trailingCommas = trailingCommas != null ? trailingCommas : DEFAULT_TRAILING_COMMAS;
    }

    public List<ScalaInterpolatedStringInjection> getInjections() {
        return injections;
    }

    public void setInjections(List<ScalaInterpolatedStringInjection> injections) {
        this.injections = new ArrayList<>();
        if (injections != null) {
            for (ScalaInterpolatedStringInjection rule : injections) {
                this.injections.add(rule != null ? rule.copy() : new ScalaInterpolatedStringInjection());
            }
        }
    }

    public ScalaMiscSettings copy() {
        return new ScalaMiscSettings(scalaTestDefaultSuperClass, trailingCommas, injections);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaMiscSettings that = (ScalaMiscSettings) o;
        return Objects.equals(scalaTestDefaultSuperClass, that.scalaTestDefaultSuperClass) &&
                Objects.equals(trailingCommas, that.trailingCommas) &&
                Objects.equals(injections, that.injections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scalaTestDefaultSuperClass, trailingCommas, injections);
    }

    @Override
    public String toString() {
        return "ScalaMiscSettings{" +
                "scalaTestDefaultSuperClass='" + scalaTestDefaultSuperClass + '\'' +
                ", trailingCommas='" + trailingCommas + '\'' +
                ", injections=" + injections +
                '}';
    }
}
