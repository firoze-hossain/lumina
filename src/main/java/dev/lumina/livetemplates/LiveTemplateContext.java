package dev.lumina.livetemplates;

import java.util.*;

/**
 * Supported syntax and language contexts for Live Template applicability.
 */
public final class LiveTemplateContext {

    public static final String EVERYWHERE = "EVERYWHERE";

    // Java
    public static final String JAVA = "JAVA";
    public static final String JAVA_DECLARATION = "JAVA_DECLARATION";
    public static final String JAVA_STATEMENT = "JAVA_STATEMENT";
    public static final String JAVA_EXPRESSION = "JAVA_EXPRESSION";
    public static final String JAVA_COMMENT = "JAVA_COMMENT";
    public static final String JAVA_STRING = "JAVA_STRING";

    // Kotlin
    public static final String KOTLIN = "KOTLIN";
    public static final String KOTLIN_TOP_LEVEL = "KOTLIN_TOP_LEVEL";
    public static final String KOTLIN_CLASS = "KOTLIN_CLASS";
    public static final String KOTLIN_STATEMENT = "KOTLIN_STATEMENT";
    public static final String KOTLIN_EXPRESSION = "KOTLIN_EXPRESSION";

    // XML
    public static final String XML = "XML";
    public static final String XML_TEXT = "XML_TEXT";
    public static final String XML_TAG = "XML_TAG";
    public static final String XML_ATTRIBUTE = "XML_ATTRIBUTE";
    public static final String XML_XSL_TEXT = "XML_XSL_TEXT";

    // HTML
    public static final String HTML = "HTML";
    public static final String HTML_TEXT = "HTML_TEXT";
    public static final String HTML_TAG = "HTML_TAG";
    public static final String HTML_ATTRIBUTE = "HTML_ATTRIBUTE";

    // Other Languages
    public static final String JAVASCRIPT = "JAVASCRIPT";
    public static final String TYPESCRIPT = "TYPESCRIPT";
    public static final String JSON = "JSON";
    public static final String CSS = "CSS";
    public static final String SQL = "SQL";
    public static final String SHELL = "SHELL";
    public static final String YAML = "YAML";

    public static final class ContextNode {
        public final String id;
        public final String displayName;
        public final List<ContextNode> children = new ArrayList<>();

        public ContextNode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public ContextNode addChild(ContextNode child) {
            children.add(child);
            return this;
        }
    }

    public static List<ContextNode> getContextTree() {
        List<ContextNode> roots = new ArrayList<>();

        roots.add(new ContextNode(EVERYWHERE, "Everywhere"));

        ContextNode java = new ContextNode(JAVA, "Java")
                .addChild(new ContextNode(JAVA_DECLARATION, "Declaration"))
                .addChild(new ContextNode(JAVA_STATEMENT, "Statement"))
                .addChild(new ContextNode(JAVA_EXPRESSION, "Expression"))
                .addChild(new ContextNode(JAVA_COMMENT, "Comment"))
                .addChild(new ContextNode(JAVA_STRING, "String"));
        roots.add(java);

        ContextNode kotlin = new ContextNode(KOTLIN, "Kotlin")
                .addChild(new ContextNode(KOTLIN_TOP_LEVEL, "Top level"))
                .addChild(new ContextNode(KOTLIN_CLASS, "Class"))
                .addChild(new ContextNode(KOTLIN_STATEMENT, "Statement"))
                .addChild(new ContextNode(KOTLIN_EXPRESSION, "Expression"));
        roots.add(kotlin);

        ContextNode xml = new ContextNode(XML, "XML")
                .addChild(new ContextNode(XML_TEXT, "XML Text"))
                .addChild(new ContextNode(XML_TAG, "XML Tag"))
                .addChild(new ContextNode(XML_ATTRIBUTE, "XML Attribute"))
                .addChild(new ContextNode(XML_XSL_TEXT, "XSL Text"));
        roots.add(xml);

        ContextNode html = new ContextNode(HTML, "HTML")
                .addChild(new ContextNode(HTML_TEXT, "HTML Text"))
                .addChild(new ContextNode(HTML_TAG, "HTML Tag"))
                .addChild(new ContextNode(HTML_ATTRIBUTE, "HTML Attribute"));
        roots.add(html);

        roots.add(new ContextNode(JAVASCRIPT, "JavaScript"));
        roots.add(new ContextNode(TYPESCRIPT, "TypeScript"));
        roots.add(new ContextNode(JSON, "JSON"));
        roots.add(new ContextNode(CSS, "CSS"));
        roots.add(new ContextNode(SQL, "SQL"));
        roots.add(new ContextNode(SHELL, "Shell Script"));
        roots.add(new ContextNode(YAML, "YAML"));

        return roots;
    }

    /**
     * Produces the exact IntelliJ IDEA descriptor label below the code editor:
     * e.g. "Applicable in XML: XSL Text." or "Applicable in Java: declaration inside a compact sou."
     */
    public static String formatApplicableText(Set<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "No applicable contexts.";
        }
        if (contexts.contains(EVERYWHERE)) {
            return "Applicable in Everywhere.";
        }
        if (contexts.contains(XML_XSL_TEXT)) {
            return "Applicable in XML: XSL Text.";
        }
        if (contexts.contains(JAVA_DECLARATION)) {
            return "Applicable in Java: declaration inside a compact sou.";
        }
        if (contexts.contains(JAVA_STATEMENT)) {
            return "Applicable in Java: statement.";
        }
        if (contexts.contains(JAVA_EXPRESSION)) {
            return "Applicable in Java: expression.";
        }
        if (contexts.contains(JAVA)) {
            return "Applicable in Java.";
        }
        if (contexts.contains(KOTLIN_STATEMENT) || contexts.contains(KOTLIN_TOP_LEVEL) || contexts.contains(KOTLIN)) {
            return "Applicable in Kotlin.";
        }
        if (contexts.contains(HTML_TEXT) || contexts.contains(HTML)) {
            return "Applicable in HTML: HTML Text.";
        }
        if (contexts.contains(XML_TEXT) || contexts.contains(XML)) {
            return "Applicable in XML.";
        }
        if (contexts.contains(JAVASCRIPT) || contexts.contains(TYPESCRIPT)) {
            return "Applicable in JavaScript / TypeScript.";
        }
        if (contexts.contains(JSON)) {
            return "Applicable in JSON.";
        }
        if (contexts.contains(CSS)) {
            return "Applicable in CSS.";
        }
        if (contexts.contains(SQL)) {
            return "Applicable in SQL.";
        }
        if (contexts.contains(SHELL)) {
            return "Applicable in Shell Script.";
        }
        if (contexts.contains(YAML)) {
            return "Applicable in YAML.";
        }

        String first = contexts.iterator().next();
        return "Applicable in " + first + ".";
    }

    private LiveTemplateContext() {}
}
