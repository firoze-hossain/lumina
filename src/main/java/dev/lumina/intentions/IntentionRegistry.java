package dev.lumina.intentions;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic registry managing all intention actions, hierarchical categories,
 * tri-state computations, and state persistence.
 */
public class IntentionRegistry {

    public enum TriState {
        CHECKED, UNCHECKED, INDETERMINATE
    }

    private static IntentionRegistry instance;

    // All registered intentions indexed by ID
    private final Map<String, IntentionAction> intentionsById = new LinkedHashMap<>();
    // Order of top-level categories
    private final List<String> topCategories = new ArrayList<>();
    // Category -> (Subcategory -> List<IntentionAction>)
    private final Map<String, Map<String, List<IntentionAction>>> hierarchy = new LinkedHashMap<>();

    // User overrides (id -> enabled)
    private final Map<String, Boolean> initialStates = new HashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private IntentionRegistry() {
        initCatalog();
        captureInitialStates();
    }

    public static synchronized IntentionRegistry getInstance() {
        if (instance == null) {
            instance = new IntentionRegistry();
        }
        return instance;
    }

    public synchronized void resetToDefaults() {
        for (IntentionAction action : intentionsById.values()) {
            action.setEnabled(action.isDefaultEnabled());
        }
        captureInitialStates();
        notifyListeners();
    }

    public synchronized void reset() {
        for (Map.Entry<String, Boolean> entry : initialStates.entrySet()) {
            IntentionAction action = intentionsById.get(entry.getKey());
            if (action != null) {
                action.setEnabled(entry.getValue());
            }
        }
        notifyListeners();
    }

    public synchronized void apply() {
        captureInitialStates();
        notifyListeners();
    }

    public synchronized boolean isModified() {
        for (Map.Entry<String, Boolean> entry : initialStates.entrySet()) {
            IntentionAction action = intentionsById.get(entry.getKey());
            if (action != null && action.isEnabled() != entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    private void captureInitialStates() {
        initialStates.clear();
        for (IntentionAction a : intentionsById.values()) {
            initialStates.put(a.getId(), a.isEnabled());
        }
    }

    public void register(IntentionAction action) {
        intentionsById.put(action.getId(), action);
        String cat = action.getCategory();
        if (!topCategories.contains(cat)) {
            topCategories.add(cat);
        }
        String sub = action.getSubcategory() != null ? action.getSubcategory() : "";
        hierarchy.computeIfAbsent(cat, k -> new LinkedHashMap<>())
                 .computeIfAbsent(sub, k -> new ArrayList<>())
                 .add(action);
    }

    public List<String> getTopCategories() {
        return Collections.unmodifiableList(topCategories);
    }

    public Map<String, List<IntentionAction>> getSubcategories(String category) {
        return hierarchy.getOrDefault(category, Collections.emptyMap());
    }

    public List<IntentionAction> getActions(String category, String subcategory) {
        String sub = subcategory != null ? subcategory : "";
        Map<String, List<IntentionAction>> subs = hierarchy.get(category);
        if (subs != null) {
            return subs.getOrDefault(sub, Collections.emptyList());
        }
        return Collections.emptyList();
    }

    public List<IntentionAction> getAllActionsForCategory(String category) {
        List<IntentionAction> result = new ArrayList<>();
        Map<String, List<IntentionAction>> subs = hierarchy.get(category);
        if (subs != null) {
            for (List<IntentionAction> list : subs.values()) {
                result.addAll(list);
            }
        }
        return result;
    }

    public IntentionAction findIntention(String id) {
        return intentionsById.get(id);
    }

    public Collection<IntentionAction> getAllIntentions() {
        return Collections.unmodifiableCollection(intentionsById.values());
    }

    // --- Tri-State Computations ---

    public TriState getCategoryState(String category) {
        List<IntentionAction> all = getAllActionsForCategory(category);
        if (all.isEmpty()) return TriState.CHECKED;
        int enabledCount = 0;
        for (IntentionAction a : all) {
            if (a.isEnabled()) enabledCount++;
        }
        if (enabledCount == 0) return TriState.UNCHECKED;
        if (enabledCount == all.size()) return TriState.CHECKED;
        return TriState.INDETERMINATE;
    }

    public TriState getSubcategoryState(String category, String subcategory) {
        List<IntentionAction> actions = getActions(category, subcategory);
        if (actions.isEmpty()) return TriState.CHECKED;
        int enabledCount = 0;
        for (IntentionAction a : actions) {
            if (a.isEnabled()) enabledCount++;
        }
        if (enabledCount == 0) return TriState.UNCHECKED;
        if (enabledCount == actions.size()) return TriState.CHECKED;
        return TriState.INDETERMINATE;
    }

    public void setCategoryEnabled(String category, boolean enabled) {
        for (IntentionAction a : getAllActionsForCategory(category)) {
            a.setEnabled(enabled);
        }
        notifyListeners();
    }

    public void setSubcategoryEnabled(String category, String subcategory, boolean enabled) {
        for (IntentionAction a : getActions(category, subcategory)) {
            a.setEnabled(enabled);
        }
        notifyListeners();
    }

    public void setIntentionEnabled(String id, boolean enabled) {
        IntentionAction a = intentionsById.get(id);
        if (a != null) {
            a.setEnabled(enabled);
            notifyListeners();
        }
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }

    // =========================================================================
    // Catalog Initialization (All 58 categories matching Images 1 & 2)
    // =========================================================================

    private void initCatalog() {
        // 1. AI Assistant (Image 1, 3)
        register(new IntentionAction("ai.actions", "AI Actions...", "AI Assistant", null,
                "Invoke context-sensitive Lumina AI Assistant operations.",
                "fun process(data: String) {\n  [// TODO: optimize query]\n}",
                "fun process(data: String) {\n  // Optimized by Lumina AI\n}"));
        register(new IntentionAction("ai.fix", "Fix with AI", "AI Assistant", null,
                "Request AI-suggested fix for current error or warning at caret.",
                "val x = [list.get(-1)]",
                "val x = list.getOrNull(0) ?: defaultVal"));

        // 2. Angular (Image 3)
        register(new IntentionAction("angular.extract.template", "Extract Angular component template to a separate file", "Angular", null,
                "Extracts inline component template into a separate HTML file.",
                "@Component({\n  selector: 'app-root',\n  template: `[<h1>Hello</h1>]`\n})",
                "@Component({\n  selector: 'app-root',\n  templateUrl: './app.component.html'\n})"));
        register(new IntentionAction("angular.inline.template", "Inline Angular component template", "Angular", null));
        register(new IntentionAction("angular.introduce.local.variable", "Introduce local variable", "Angular", null));

        // 3. Choose template data language (Image 3)
        register(new IntentionAction("template.data.lang.settings", "Template language settings", "Choose template data language", null));

        // 4. Copilot (Image 3)
        register(new IntentionAction("copilot.code.review", "Copilot Code Review", "Copilot", null,
                "Initiates AI-assisted code review on selected lines.",
                "fun sum(a: Int, b: Int) = [a + b]",
                "fun sum(a: Int, b: Int) = Math.addExact(a, b)"));
        register(new IntentionAction("copilot.inline.chat", "Copilot Inline Chat", "Copilot", null));

        // 5. CSS (Image 3)
        register(new IntentionAction("css.width.height.bg", "Apply width and height of background image", "CSS", null));
        register(new IntentionAction("css.change.color", "Change color", "CSS", null));
        register(new IntentionAction("css.convert.hex", "Convert color to #-hex", "CSS", null));
        register(new IntentionAction("css.convert.gray", "Convert color to gray()", "CSS", null));
        register(new IntentionAction("css.convert.hsl", "Convert color to hsl()", "CSS", null));
        register(new IntentionAction("css.convert.hwb", "Convert color to hwb()", "CSS", null));
        register(new IntentionAction("css.convert.lch", "Convert color to lch()", "CSS", null));
        register(new IntentionAction("css.convert.oklch", "Convert color to oklch()", "CSS", null));
        register(new IntentionAction("css.convert.rgb", "Convert color to rgb()", "CSS", null));
        register(new IntentionAction("css.create.selector", "Create selector", "CSS", null));
        register(new IntentionAction("css.expand.shorthand", "Expand shorthand property", "CSS", null));
        register(new IntentionAction("css.extract.image", "Extract image", "CSS", null));
        register(new IntentionAction("css.extract.inline", "Extract inline CSS", "CSS", null));
        register(new IntentionAction("css.extract.ruleset", "Extract ruleset", "CSS", null));
        register(new IntentionAction("css.replace.quotes", "Replace quotes", "CSS", null));
        register(new IntentionAction("css.replace.var.fallback", "Replace var() with its fallback value", "CSS", null));
        register(new IntentionAction("css.replace.color.name", "Replace with color name", "CSS", null));

        // 6. Database
        register(new IntentionAction("db.inject.language", "Inject SQL dialect into string literal", "Database", null));
        register(new IntentionAction("db.explain.plan", "Explain SQL execution plan", "Database", null));

        // 7. Dev Containers
        register(new IntentionAction("devcontainers.rebuild", "Rebuild Dev Container", "Dev Containers", null));
        register(new IntentionAction("devcontainers.edit.json", "Edit devcontainer.json properties", "Dev Containers", null));

        // 8. Docker
        register(new IntentionAction("docker.pull.image", "Pull Docker image at caret", "Docker", null));
        register(new IntentionAction("docker.compose.up", "Run Docker Compose service", "Docker", null));

        // 9. EditorConfig
        register(new IntentionAction("editorconfig.add.property", "Add missing EditorConfig property", "EditorConfig", null));

        // 10. Flow JS
        register(new IntentionAction("flow.annotate.type", "Add Flow type annotation", "Flow JS", null));

        // 11. FreeMarker
        register(new IntentionAction("freemarker.include.macro", "Extract to FreeMarker macro", "FreeMarker", null));

        // 12. GitLab CI
        register(new IntentionAction("gitlab.validate.ci", "Validate .gitlab-ci.yml section", "GitLab CI", null));

        // 13. Go
        register(new IntentionAction("go.generate.tags", "Add struct field tags", "Go", null));
        register(new IntentionAction("go.implement.interface", "Implement missing interface methods", "Go", null));

        // 14. Go modules
        register(new IntentionAction("go.modules.tidy", "Run 'go mod tidy'", "Go modules", null));

        // 15. Groovy
        register(new IntentionAction("groovy.convert.to.java", "Convert Groovy method to Java", "Groovy", null));

        // 16. HTML
        register(new IntentionAction("html.close.tag", "Insert closing HTML tag", "HTML", null));
        register(new IntentionAction("html.extract.entity", "Replace character with HTML entity", "HTML", null));

        // 17. HTTP Client
        register(new IntentionAction("http.client.run.request", "Run HTTP request", "HTTP Client", null));
        register(new IntentionAction("http.client.convert.curl", "Convert cURL request to HTTP Client specification", "HTTP Client", null));

        // 18. Java (Image 4 - Annotations, Collections, Comments, etc.)
        // Subcategory: Annotations
        IntentionAction addAnno = new IntentionAction("java.anno.add", "Add annotation", "Java", "Annotations",
                "Adds an annotation, for example @Nullable/@NotNull, to the selected element. If the code belongs to a library, the annotation will be stored in a separate file.",
                "class A {\n  Object getObject() {\n    //do smth\n    return null;\n  }\n}",
                "import dev.lumina.annotations.Nullable;\n\nclass A {\n\n  [@Nullable]\n  Object getObject() {\n    //do smth\n    return null;\n  }\n}");
        register(addAnno);
        register(new IntentionAction("java.anno.external", "Annotate externally", "Java", "Annotations"));
        register(new IntentionAction("java.anno.overriding", "Annotate overriding methods and their parameters", "Java", "Annotations"));
        register(new IntentionAction("java.anno.deannotate", "Deannotate", "Java", "Annotations"));
        register(new IntentionAction("java.anno.contract", "Edit method contract", "Java", "Annotations"));
        register(new IntentionAction("java.anno.range", "Edit range", "Java", "Annotations"));
        register(new IntentionAction("java.anno.make.external.explicit", "Make external annotations explicit", "Java", "Annotations"));
        register(new IntentionAction("java.anno.make.inferred.explicit", "Make inferred annotations explicit", "Java", "Annotations"));

        // Subcategory: Collections
        register(new IntentionAction("java.col.arrays.aslist", "Replace with 'Arrays.asList()'", "Java", "Collections"));
        register(new IntentionAction("java.col.mutable", "Replace with mutable collection", "Java", "Collections"));
        register(new IntentionAction("java.col.unmodifiable", "Wrap with unmodifiable collection or map", "Java", "Collections"));

        // Subcategory: Comments
        register(new IntentionAction("java.comm.separate.line", "Move comment to separate line", "Java", "Comments"));
        register(new IntentionAction("java.comm.block", "Replace with block comment", "Java", "Comments"));
        register(new IntentionAction("java.comm.end.of.line", "Replace with end-of-line comment", "Java", "Comments"));

        // Subcategories from Image 4
        register(new IntentionAction("java.concurrency.atomic", "Convert variable to AtomicReference", "Java", "Concurrency"));
        register(new IntentionAction("java.conditions.invert", "Invert condition expression", "Java", "Conditions"));
        register(new IntentionAction("java.controlflow.iterate", "Replace for-each with while loop", "Java", "Control flow"));
        register(new IntentionAction("java.decl.var", "Replace explicit type with 'var'", "Java", "Declaration"));
        register(new IntentionAction("java.expr.simplify", "Simplify boolean expression", "Java", "Expressions"));
        register(new IntentionAction("java.imports.optimize", "Optimize unused imports", "Java", "Imports"));
        register(new IntentionAction("java.i18n.extract", "Extract string resource", "Java", "Internationalization"));
        register(new IntentionAction("java.junit.assert", "Convert assert statement to Assertions.assertEquals()", "Java", "JUnit"));
        register(new IntentionAction("java.lombok.getter", "Add @Getter annotation", "Java", "Lombok"));
        register(new IntentionAction("java.other.switch", "Replace if-else tree with switch expression", "Java", "Other"));
        register(new IntentionAction("java.refactoring.extract.method", "Extract code to new method", "Java", "Refactorings"));
        register(new IntentionAction("java.streams.collector", "Collect stream to unmodifiable list", "Java", "Streams"));

        // 19. Java EE Persistence
        register(new IntentionAction("jpa.generate.named.query", "Generate @NamedQuery annotation", "Java EE Persistence", null));

        // 20. JavaFX
        register(new IntentionAction("javafx.fxml.create.controller", "Create controller field in FXML", "JavaFX", null));

        // 21. JavaScript
        register(new IntentionAction("js.convert.arrow", "Convert function expression to arrow function", "JavaScript", null));
        register(new IntentionAction("js.destructure.param", "Destructure object parameter", "JavaScript", null));

        // 22. JRuby
        register(new IntentionAction("jruby.interop", "Add Java import in JRuby script", "JRuby", null));

        // 23. JSON
        register(new IntentionAction("json.copy.pointer", "Copy JSON pointer", "JSON", null));
        register(new IntentionAction("json.format", "Reformat JSON fragment", "JSON", null));

        // 24. JSP
        register(new IntentionAction("jsp.import.directive", "Insert JSP <%@ page import %> directive", "JSP", null));

        // 25. Kotlin
        register(new IntentionAction("kotlin.convert.when", "Replace if expression with when statement", "Kotlin", null));
        register(new IntentionAction("kotlin.introduce.backing", "Introduce explicit backing property", "Kotlin", null));

        // 26. Ktor (Image 5)
        IntentionAction ktorParam = new IntentionAction("ktor.param.delegate", "Convert Ktor parameter to delegate", "Ktor", null,
                "Converts Ktor request parameter to delegate.",
                "get(\"/{path}\") {\n  [val path: Int by call.parameters]\n}",
                "get(\"/{path}\") {\n  val path = call.parameters[\"path\"]?.toIntOrNull() ?: return@get\n}");
        ktorParam.setPluginName("Ktor plugin");
        register(ktorParam);

        IntentionAction ktorTestMod = new IntentionAction("ktor.test.module", "Create test for Ktor module", "Ktor", null);
        ktorTestMod.setPluginName("Ktor plugin");
        register(ktorTestMod);

        IntentionAction ktorTestRoute = new IntentionAction("ktor.test.route", "Create test for Ktor route", "Ktor", null);
        ktorTestRoute.setPluginName("Ktor plugin");
        register(ktorTestRoute);

        IntentionAction ktorTestRoutes = new IntentionAction("ktor.test.routes", "Create test for Ktor routes", "Ktor", null);
        ktorTestRoutes.setPluginName("Ktor plugin");
        register(ktorTestRoutes);

        IntentionAction ktorExtractRoute = new IntentionAction("ktor.extract.route", "Extract Route to a Separate Method", "Ktor", null);
        ktorExtractRoute.setPluginName("Ktor plugin");
        register(ktorExtractRoute);

        IntentionAction ktorOpenApi = new IntentionAction("ktor.openapi.docs", "Generate OpenAPI documentation for current module", "Ktor", null);
        ktorOpenApi.setPluginName("Ktor plugin");
        register(ktorOpenApi);

        IntentionAction ktorInlineExt = new IntentionAction("ktor.inline.extension", "Inline Route Extension", "Ktor", null);
        ktorInlineExt.setPluginName("Ktor plugin");
        register(ktorInlineExt);

        // 27. Kubernetes
        register(new IntentionAction("k8s.add.container.port", "Add container port declaration", "Kubernetes", null));
        register(new IntentionAction("k8s.apply.resource", "Apply Kubernetes manifest to cluster", "Kubernetes", null));

        // 28. Language injection
        register(new IntentionAction("lang.inject.regex", "Inject RegExp language into string", "Language injection", null));
        register(new IntentionAction("lang.inject.json", "Inject JSON language into string", "Language injection", null));

        // 29. LightEdit mode
        register(new IntentionAction("lightedit.open.project", "Open file in full project workspace", "LightEdit mode", null));

        // 30. LSP
        register(new IntentionAction("lsp.restart.server", "Restart active Language Server Protocol daemon", "LSP", null));

        // 31. Markdown
        register(new IntentionAction("md.format.table", "Align Markdown table columns", "Markdown", null));
        register(new IntentionAction("md.insert.image.link", "Insert relative image reference", "Markdown", null));

        // 32. Micronaut
        register(new IntentionAction("micronaut.inject.service", "Add @Inject constructor parameter", "Micronaut", null));

        // 33. Microservices
        register(new IntentionAction("microservices.endpoints.url", "Copy REST endpoint URL path", "Microservices", null));

        // 34. MongoDB JSON
        register(new IntentionAction("mongo.json.run.query", "Execute MongoDB query in DataGrip console", "MongoDB JSON", null));

        // 35. Natural languages
        register(new IntentionAction("natlang.rename.typo", "Rename misspelled identifier", "Natural languages", null));
        register(new IntentionAction("natlang.add.to.dict", "Save word to user spellcheck dictionary", "Natural languages", null));

        // 36. OpenAPI specifications
        register(new IntentionAction("openapi.preview.swagger", "Open Swagger UI documentation preview", "OpenAPI specifications", null));

        // 37. Other
        register(new IntentionAction("other.wrap.parentheses", "Wrap expression in parentheses", "Other", null));

        // 38. PHP
        register(new IntentionAction("php.add.docblock", "Generate PHPDoc comment block", "PHP", null));
        register(new IntentionAction("php.convert.match", "Convert switch statement to match expression", "PHP", null));

        // 39. Properties
        register(new IntentionAction("props.sort.keys", "Sort properties alphabetically", "Properties", null));

        // 40. Python
        register(new IntentionAction("py.add.type.hint", "Add PEP 484 type hint annotation", "Python", null));
        register(new IntentionAction("py.convert.fstring", "Convert % formatting to f-string", "Python", null));

        // 41. Qodana configuration
        register(new IntentionAction("qodana.add.profile", "Add linter quality profile section", "Qodana configuration", null));

        // 42. Quarkus
        register(new IntentionAction("quarkus.generate.panache", "Generate PanacheEntityActiveRecord boilerplate", "Quarkus", null));

        // 43. React
        register(new IntentionAction("react.convert.functional", "Convert class component to functional component", "React", null));
        register(new IntentionAction("react.add.usecallback", "Wrap event handler with useCallback hook", "React", null));

        // 44. RegExp
        register(new IntentionAction("regexp.check.pattern", "Check regular expression with test inputs", "RegExp", null));

        // 45. Ruby
        register(new IntentionAction("ruby.symbolize.keys", "Convert hash string keys to symbols", "Ruby", null));

        // 46. Rust
        register(new IntentionAction("rust.add.derive", "Add #[derive(...)] attribute to struct", "Rust", null));
        register(new IntentionAction("rust.match.arms", "Fill missing match arms exhaustively", "Rust", null));

        // 47. Scala
        register(new IntentionAction("scala.add.type.ascription", "Add explicit type ascription", "Scala", null));

        // 48. Shell script
        register(new IntentionAction("sh.add.shebang", "Add #!/usr/bin/env bash shebang header", "Shell script", null));

        // 49. Spring
        register(new IntentionAction("spring.autowire.bean", "Autowire Spring component bean", "Spring", null));

        // 50. Spring Boot
        register(new IntentionAction("spring.boot.create.repo", "Create Spring Data repository interface", "Spring Boot", null));

        // 51. SQL
        register(new IntentionAction("sql.qualify.column", "Qualify column reference with table alias", "SQL", null));
        register(new IntentionAction("sql.expand.wildcard", "Expand SELECT * wildcard into explicit column list", "SQL", null));

        // 52. TOML
        register(new IntentionAction("toml.inline.table", "Convert section table to inline TOML table", "TOML", null));

        // 53. TypeScript
        register(new IntentionAction("ts.infer.type", "Infer explicit return type", "TypeScript", null));
        register(new IntentionAction("ts.convert.interface", "Convert type alias to interface declaration", "TypeScript", null));

        // 54. Velocity
        register(new IntentionAction("velocity.format.directive", "Indent Velocity directive block", "Velocity", null));

        // 55. Vue.JS
        register(new IntentionAction("vue.setup.script", "Convert export default component to <script setup>", "Vue.JS", null));

        // 56. XML
        register(new IntentionAction("xml.collapse.empty", "Collapse empty tag to self-closing tag", "XML", null));

        // 57. XSLT
        register(new IntentionAction("xslt.create.template", "Create <xsl:template> match definition", "XSLT", null));

        // 58. YAML
        register(new IntentionAction("yaml.quote.scalar", "Quote multiline scalar string", "YAML", null));
        register(new IntentionAction("yaml.sort.keys", "Sort dictionary keys alphabetically", "YAML", null));
    }
}
