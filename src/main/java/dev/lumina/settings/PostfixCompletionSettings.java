package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > General > Postfix Completion settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic templates, custom templates,
 * dirty tracking, and live editor expansion.
 */
public final class PostfixCompletionSettings {

    public static final List<String> ALL_LANGUAGES = List.of(
            "TypeScript",
            "Java",
            "Rust",
            "Scala",
            "JavaScript",
            "Go",
            "Kotlin",
            "PHP",
            "JVM languages",
            "Groovy",
            "Ruby",
            "Python",
            "SQL"
    );

    public static class TemplateItem {
        private final String id;
        private final String language;
        private final String key;
        private final String example;
        private final String beforeSample;
        private final String afterSample;
        private final String description;
        private final String expansion;
        private final boolean custom;
        private boolean enabled;

        public TemplateItem(String id, String language, String key, String example,
                            String beforeSample, String afterSample, String description,
                            boolean custom, boolean enabled) {
            this(id, language, key, example, beforeSample, afterSample, description, afterSample, custom, enabled);
        }

        public TemplateItem(String id, String language, String key, String example,
                            String beforeSample, String afterSample, String description,
                            String expansion, boolean custom, boolean enabled) {
            this.id = id;
            this.language = language;
            this.key = key;
            this.example = example;
            this.beforeSample = beforeSample;
            this.afterSample = afterSample;
            this.description = description;
            this.expansion = expansion != null ? expansion : afterSample;
            this.custom = custom;
            this.enabled = enabled;
        }

        public String getId() { return id; }
        public String getLanguage() { return language; }
        public String getKey() { return key; }
        public String getExample() { return example; }
        public String getBeforeSample() { return beforeSample; }
        public String getAfterSample() { return afterSample; }
        public String getDescription() { return description; }
        public String getExpansion() { return expansion; }
        public boolean isCustom() { return custom; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public TemplateItem copy() {
            return new TemplateItem(id, language, key, example, beforeSample, afterSample, description, expansion, custom, enabled);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TemplateItem that)) return false;
            return custom == that.custom &&
                    enabled == that.enabled &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(language, that.language) &&
                    Objects.equals(key, that.key) &&
                    Objects.equals(example, that.example) &&
                    Objects.equals(beforeSample, that.beforeSample) &&
                    Objects.equals(afterSample, that.afterSample) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(expansion, that.expansion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, language, key, example, beforeSample, afterSample, description, expansion, custom, enabled);
        }
    }

    private static final PostfixCompletionSettings INSTANCE = new PostfixCompletionSettings();

    public static PostfixCompletionSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(PostfixCompletionSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private boolean enablePostfixCompletion = true;
    private boolean showAsCommandCompletions = false;
    private String expandShortcut = "Tab";

    private final Set<String> disabledLanguages = new LinkedHashSet<>();
    private final Map<String, TemplateItem> templates = new LinkedHashMap<>();

    public PostfixCompletionSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        enablePostfixCompletion = true;
        showAsCommandCompletions = false;
        expandShortcut = "Tab";

        disabledLanguages.clear();
        templates.clear();

        registerBuiltinTemplates();
    }

    private void registerBuiltinTemplates() {
        // --- Java (Matching exact templates and previews) ---
        addBuiltin("Java", "!", "!expr",
                "public class Foo {\n    void m(boolean b) {\n        m([b!]);\n    }\n}",
                "public class Foo {\n    void m(boolean b) {\n        m(!b);\n    }\n}",
                "Negates a boolean expression.",
                "!expr");
        addBuiltin("Java", "arg", "functionCall(expr)",
                "public class Foo {\n    void m(String s) {\n        [s].arg\n    }\n}",
                "public class Foo {\n    void m(String s) {\n        functionCall(s);\n    }\n}",
                "Passes expression as argument to a method call",
                "functionCall(expr)");
        addBuiltin("Java", "assert", "assert expr",
                "public class Foo {\n    void m(boolean b) {\n        [b].assert\n    }\n}",
                "public class Foo {\n    void m(boolean b) {\n        assert b;\n    }\n}",
                "Asserts expression",
                "assert expr;");
        addBuiltin("Java", "cast", "((SomeType) expr)",
                "public class Foo {\n    void m(Object o) {\n        [o].cast\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        ((SomeType) o)\n    }\n}",
                "Casts expression to type",
                "((SomeType) expr)");
        addBuiltin("Java", "castvar", "T name = (T)expr",
                "public class Foo {\n    void m(Object o) {\n        [o].castvar\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        SomeType name = (SomeType) o;\n    }\n}",
                "Casts and assigns expression to new variable",
                "SomeType name = (SomeType) expr;");
        addBuiltin("Java", "else", "if (!expr)",
                "public class Foo {\n    void m(boolean b) {\n        [b].else\n    }\n}",
                "public class Foo {\n    void m(boolean b) {\n        if (!b) {\n            \n        }\n    }\n}",
                "Checks negated boolean expression with if statement",
                "if (!expr) {\n    \n}");
        addBuiltin("Java", "field", "myField = expr",
                "public class Foo {\n    void m(int x) {\n        [x].field\n    }\n}",
                "public class Foo {\n    private int myField;\n    void m(int x) {\n        this.myField = x;\n    }\n}",
                "Introduces field for given expression",
                "this.myField = expr;");
        addBuiltin("Java", "for", "for (T item : expr)",
                "public class Foo {\n    void m(List<String> list) {\n        [list].for\n    }\n}",
                "public class Foo {\n    void m(List<String> list) {\n        for (String item : list) {\n            \n        }\n    }\n}",
                "Iterates over iterable or array with enhanced for-loop",
                "for (T item : expr) {\n    \n}");
        addBuiltin("Java", "fori", "for (int i = 0; i < expr.length; i++)",
                "public class Foo {\n    void m(int[] arr) {\n        [arr].fori\n    }\n}",
                "public class Foo {\n    void m(int[] arr) {\n        for (int i = 0; i < arr.length; i++) {\n            \n        }\n    }\n}",
                "Iterates with index over elements",
                "for (int i = 0; i < expr.length; i++) {\n    \n}");
        addBuiltin("Java", "format", "String.format(expr)",
                "public class Foo {\n    void m(String s) {\n        [s].format\n    }\n}",
                "public class Foo {\n    void m(String s) {\n        String.format(s)\n    }\n}",
                "Wraps expression in String.format(...)",
                "String.format(expr)");
        addBuiltin("Java", "forr", "for (int i = expr.length - 1; i >= 0; i--)",
                "public class Foo {\n    void m(int[] arr) {\n        [arr].forr\n    }\n}",
                "public class Foo {\n    void m(int[] arr) {\n        for (int i = expr.length - 1; i >= 0; i--) {\n            \n        }\n    }\n}",
                "Iterates backwards over elements",
                "for (int i = expr.length - 1; i >= 0; i--) {\n    \n}");
        addBuiltin("Java", "if", "if (expr)",
                "public class Foo {\n    void m(boolean b) {\n        [b].if\n    }\n}",
                "public class Foo {\n    void m(boolean b) {\n        if (b) {\n            \n        }\n    }\n}",
                "Checks boolean expression with if statement",
                "if (expr) {\n    \n}");
        addBuiltin("Java", "inst", "expr instanceof Type ? ((Type) expr) : null",
                "public class Foo {\n    void m(Object o) {\n        [o].inst\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        o instanceof Type ? ((Type) o) : null\n    }\n}",
                "Type check ternary expression",
                "expr instanceof Type ? ((Type) expr) : null");
        addBuiltin("Java", "instanceof", "expr instanceof Type ? ((Type) expr) : null",
                "public class Foo {\n    void m(Object o) {\n        [o].instanceof\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        o instanceof Type ? ((Type) o) : null\n    }\n}",
                "Type check ternary expression",
                "expr instanceof Type ? ((Type) expr) : null");
        addBuiltin("Java", "iter", "for (T item : expr)",
                "public class Foo {\n    void m(Iterable<String> it) {\n        [it].iter\n    }\n}",
                "public class Foo {\n    void m(Iterable<String> it) {\n        for (String item : it) {\n            \n        }\n    }\n}",
                "Iterates over collection",
                "for (T item : expr) {\n    \n}");
        addBuiltin("Java", "lambda", "() -> expr",
                "public class Foo {\n    void m(int x) {\n        [x].lambda\n    }\n}",
                "public class Foo {\n    void m(int x) {\n        () -> x\n    }\n}",
                "Converts expression to lambda",
                "() -> expr");
        addBuiltin("Java", "new", "new T()",
                "public class Foo {\n    void m() {\n        [Foo].new\n    }\n}",
                "public class Foo {\n    void m() {\n        new Foo()\n    }\n}",
                "Instantiates class",
                "new expr()");
        addBuiltin("Java", "nn", "if (expr != null)",
                "public class Foo {\n    void m(Object o) {\n        [o].nn\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        if (o != null) {\n            \n        }\n    }\n}",
                "Checks expression to be not null",
                "if (expr != null) {\n    \n}");
        addBuiltin("Java", "not", "!expr",
                "public class Foo {\n    void m(boolean b) {\n        [b].not\n    }\n}",
                "public class Foo {\n    void m(boolean b) {\n        !b\n    }\n}",
                "Negates boolean expression",
                "!expr");
        addBuiltin("Java", "notnull", "if (expr != null)",
                "public class Foo {\n    void m(Object o) {\n        [o].notnull\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        if (o != null) {\n            \n        }\n    }\n}",
                "Checks expression to be not null",
                "if (expr != null) {\n    \n}");
        addBuiltin("Java", "null", "if (expr == null)",
                "public class Foo {\n    void m(Object o) {\n        [o].null\n    }\n}",
                "public class Foo {\n    void m(Object o) {\n        if (o == null) {\n            \n        }\n    }\n}",
                "Checks expression to be null",
                "if (expr == null) {\n    \n}");
        addBuiltin("Java", "opt", "Optional.ofNullable(expr)",
                "public class Foo {\n    void m(String s) {\n        [s].opt\n    }\n}",
                "public class Foo {\n    void m(String s) {\n        Optional.ofNullable(s)\n    }\n}",
                "Wraps expression in Optional",
                "Optional.ofNullable(expr)");
        addBuiltin("Java", "par", "(expr)",
                "public class Foo {\n    void m(int x) {\n        [x + 1].par\n    }\n}",
                "public class Foo {\n    void m(int x) {\n        (x + 1)\n    }\n}",
                "Wraps expression in parentheses",
                "(expr)");
        addBuiltin("Java", "reqnonnull", "Objects.requireNonNull(expr)",
                "public class Foo {\n    void m(String s) {\n        [s].reqnonnull\n    }\n}",
                "public class Foo {\n    void m(String s) {\n        Objects.requireNonNull(s)\n    }\n}",
                "Guards against null using Objects.requireNonNull",
                "Objects.requireNonNull(expr)");
        addBuiltin("Java", "return", "return expr",
                "public class Foo {\n    int m() {\n        [42].return\n    }\n}",
                "public class Foo {\n    int m() {\n        return 42;\n    }\n}",
                "Returns expression from surrounding method",
                "return expr;");
        addBuiltin("Java", "serr", "System.err.println(expr)",
                "public class Foo {\n    void m(String msg) {\n        [msg].serr\n    }\n}",
                "public class Foo {\n    void m(String msg) {\n        System.err.println(msg);\n    }\n}",
                "Prints given expression to System.err",
                "System.err.println(expr);");
        addBuiltin("Java", "souf", "System.out.printf(\"\", expr)",
                "public class Foo {\n    void m(String s) {\n        [s].souf\n    }\n}",
                "public class Foo {\n    void m(String s) {\n        System.out.printf(\"%s\\n\", s);\n    }\n}",
                "Prints formatted expression to System.out",
                "System.out.printf(\"\", expr);");
        addBuiltin("Java", "sout", "System.out.println(expr)",
                "public class Foo {\n    void m(String msg) {\n        [msg].sout\n    }\n}",
                "public class Foo {\n    void m(String msg) {\n        System.out.println(msg);\n    }\n}",
                "Prints given expression to System.out",
                "System.out.println(expr);");
        addBuiltin("Java", "soutv", "System.out.println(expr)",
                "public class Foo {\n    void m(int x) {\n        [x].soutv\n    }\n}",
                "public class Foo {\n    void m(int x) {\n        System.out.println(\"x = \" + x);\n    }\n}",
                "Prints given expression value to System.out",
                "System.out.println(\"expr = \" + expr);");
        addBuiltin("Java", "stream", "Arrays.stream(expr)",
                "public class Foo {\n    void m(int[] arr) {\n        [arr].stream\n    }\n}",
                "public class Foo {\n    void m(int[] arr) {\n        Arrays.stream(arr)\n    }\n}",
                "Wraps array with Arrays.stream(expr)",
                "Arrays.stream(expr)");
        addBuiltin("Java", "switch", "switch(expr)",
                "public class Foo {\n    void m(int val) {\n        [val].switch\n    }\n}",
                "public class Foo {\n    void m(int val) {\n        switch (val) {\n            \n        }\n    }\n}",
                "Produces switch statement over expression",
                "switch (expr) {\n    \n}");
        addBuiltin("Java", "var", "T name = expr",
                "public class Foo {\n    void m(int x) {\n        [x].var\n    }\n}",
                "public class Foo {\n    void m(int x) {\n        var name = x;\n    }\n}",
                "Introduces variable for given expression",
                "var name = expr;");
        addBuiltin("Java", "throw", "throw expr",
                "public class Foo {\n    void m() {\n        [new RuntimeException()].throw\n    }\n}",
                "public class Foo {\n    void m() {\n        throw new RuntimeException();\n    }\n}",
                "Throws given Throwable",
                "throw expr;");
        addBuiltin("Java", "try", "try { expr } catch (Exception e) {}",
                "public class Foo {\n    void m() {\n        [doWork()].try\n    }\n}",
                "public class Foo {\n    void m() {\n        try {\n            doWork();\n        } catch (Exception e) {\n            e.printStackTrace();\n        }\n    }\n}",
                "Surrounds expression with try-catch",
                "try {\n    expr;\n} catch (Exception e) {\n    e.printStackTrace();\n}");

        // --- TypeScript ---
        addBuiltin("TypeScript", "cast", "(<any>value)",
                "function m(arg: TypeValue) {\n    [arg].cast\n}",
                "function m(arg: TypeValue) {\n    (<any>arg)\n}",
                "Inserts a TypeScript type assertion and wraps it in parentheses if needed.\nAvailable only in TypeScript.\n\nA type assertion is inserted in the form of a prefix, except for TSX files, where the as expression is used.",
                "(<any>expr)");
        addBuiltin("TypeScript", "log", "console.log(expr)", "console.log([expr]);", "console.log(expr);", "Logs expression to console");
        addBuiltin("TypeScript", "const", "const name = expr", "const name = [expr];", "const name = expr;", "Assigns to const variable");
        addBuiltin("TypeScript", "let", "let name = expr", "let name = [expr];", "let name = expr;", "Assigns to let variable");
        addBuiltin("TypeScript", "var", "var name = expr", "var name = [expr];", "var name = expr;", "Assigns to var variable");
        addBuiltin("TypeScript", "if", "if (expr)", "if ([expr]) {}", "if (expr) {\n    \n}", "Checks with if statement");
        addBuiltin("TypeScript", "else", "if (!expr)", "if (![expr]) {}", "if (!expr) {\n    \n}", "Checks negated if statement");
        addBuiltin("TypeScript", "forof", "for (let item of expr)", "for (let item of [expr]) {}", "for (let item of expr) {\n    \n}", "Iterates using for-of loop");
        addBuiltin("TypeScript", "forin", "for (let key in expr)", "for (let key in [expr]) {}", "for (let key in expr) {\n    \n}", "Iterates using for-in loop");
        addBuiltin("TypeScript", "return", "return expr", "return [expr];", "return expr;", "Returns expression");
        addBuiltin("TypeScript", "throw", "throw expr", "throw [expr];", "throw expr;", "Throws expression");
        addBuiltin("TypeScript", "await", "await expr", "await [expr];", "await expr;", "Awaits Promise expression");
        addBuiltin("TypeScript", "typeof", "typeof expr", "typeof [expr]", "typeof expr", "Computes typeof expression");
        addBuiltin("TypeScript", "not", "!expr", "![expr]", "!expr", "Negates expression");
        addBuiltin("TypeScript", "null", "if (expr === null)", "if ([expr] === null) {}", "if (expr === null) {\n    \n}", "Checks null");
        addBuiltin("TypeScript", "notnull", "if (expr !== null)", "if ([expr] !== null) {}", "if (expr !== null) {\n    \n}", "Checks not null");

        // --- Rust ---
        addBuiltin("Rust", "println!", "println!(\"{:?}\", expr);", "println!(\"{:?}\", [expr]);", "println!(\"{:?}\", expr);", "Prints with println! macro");
        addBuiltin("Rust", "dbg", "dbg!(expr);", "dbg!([expr]);", "dbg!(expr);", "Debugs expression with dbg! macro");
        addBuiltin("Rust", "let", "let name = expr;", "let name = [expr];", "let name = expr;", "Assigns expression to let");
        addBuiltin("Rust", "match", "match expr {}", "match [expr] {}", "match expr {\n    \n}", "Matches expression patterns");
        addBuiltin("Rust", "if", "if expr {}", "if [expr] {}", "if expr {\n    \n}", "Checks boolean with if");
        addBuiltin("Rust", "ok", "Ok(expr)", "Ok([expr])", "Ok(expr)", "Wraps in Result::Ok");
        addBuiltin("Rust", "err", "Err(expr)", "Err([expr])", "Err(expr)", "Wraps in Result::Err");
        addBuiltin("Rust", "some", "Some(expr)", "Some([expr])", "Some(expr)", "Wraps in Option::Some");
        addBuiltin("Rust", "assert!", "assert!(expr);", "assert!([expr]);", "assert!(expr);", "Asserts boolean with assert!");
        addBuiltin("Rust", "ref", "&expr", "&[expr]", "&expr", "Borrows expression reference");
        addBuiltin("Rust", "deref", "*expr", "*[expr]", "*expr", "Dereferences expression");

        // --- Kotlin ---
        addBuiltin("Kotlin", "sout", "println(expr)", "println([expr])", "println(expr)", "Prints expression with println");
        addBuiltin("Kotlin", "val", "val name = expr", "val name = [expr]", "val name = expr", "Assigns to read-only val");
        addBuiltin("Kotlin", "var", "var name = expr", "var name = [expr]", "var name = expr", "Assigns to mutable var");
        addBuiltin("Kotlin", "if", "if (expr) {}", "if ([expr]) {}", "if (expr) {\n    \n}", "Checks condition with if");
        addBuiltin("Kotlin", "when", "when (expr) {}", "when ([expr]) {}", "when (expr) {\n    \n}", "Inspects branches with when");
        addBuiltin("Kotlin", "nn", "if (expr != null) {}", "if ([expr] != null) {}", "if (expr != null) {\n    \n}", "Null safety check");
        addBuiltin("Kotlin", "null", "if (expr == null) {}", "if ([expr] == null) {}", "if (expr == null) {\n    \n}", "Null check");
        addBuiltin("Kotlin", "for", "for (item in expr) {}", "for (item in [expr]) {}", "for (item in expr) {\n    \n}", "Iterates in collection");
        addBuiltin("Kotlin", "return", "return expr", "return [expr]", "return expr", "Returns expression");

        // --- JavaScript ---
        addBuiltin("JavaScript", "log", "console.log(expr)", "console.log([expr]);", "console.log(expr);", "Logs expression to console");
        addBuiltin("JavaScript", "const", "const name = expr", "const name = [expr];", "const name = expr;", "Assigns to const variable");
        addBuiltin("JavaScript", "let", "let name = expr", "let name = [expr];", "let name = expr;", "Assigns to let variable");
        addBuiltin("JavaScript", "if", "if (expr)", "if ([expr]) {}", "if (expr) {\n    \n}", "Checks condition with if");
        addBuiltin("JavaScript", "return", "return expr", "return [expr];", "return expr;", "Returns expression");

        // --- Python ---
        addBuiltin("Python", "print", "print(expr)", "print([expr])", "print(expr)", "Prints expression");
        addBuiltin("Python", "if", "if expr:", "if [expr]:", "if expr:\n    pass", "Checks condition with if");
        addBuiltin("Python", "for", "for item in expr:", "for item in [expr]:", "for item in expr:\n    pass", "Iterates in for loop");
        addBuiltin("Python", "len", "len(expr)", "len([expr])", "len(expr)", "Length of sequence");
        addBuiltin("Python", "return", "return expr", "return [expr]", "return expr", "Returns expression");

        // --- Go ---
        addBuiltin("Go", "print", "fmt.Println(expr)", "fmt.Println([expr])", "fmt.Println(expr)", "Prints expression");
        addBuiltin("Go", "var", "name := expr", "name := [expr]", "name := expr", "Short variable declaration");
        addBuiltin("Go", "return", "return expr", "return [expr]", "return expr", "Returns expression");
        addBuiltin("Go", "for", "for _, item := range expr {}", "for _, item := range [expr] {}", "for _, item := range expr {\n    \n}", "Range loop");
        addBuiltin("Go", "if", "if expr {}", "if [expr] {}", "if expr {\n    \n}", "If condition");

        // --- SQL ---
        addBuiltin("SQL", "from", "select * from authors", "select * from [authors]", "SELECT * FROM authors;", "Select all from table");
        addBuiltin("SQL", "cfrom", "select [all columns] from authors", "select [all columns] from [authors]", "SELECT col1, col2 FROM authors;", "Select specific columns");
        addBuiltin("SQL", "join", "select * from authors join on |", "select * from [authors] join", "SELECT * FROM authors JOIN other ON authors.id = other.id;", "Join table");

        // --- Groovy & JVM languages ---
        addBuiltin("JVM languages", "autowire", "@Autowired T t", "@Autowired\nprivate [T] t;", "@Autowired\nprivate T t;", "Spring autowire annotation");
        addBuiltin("JVM languages", "inject", "@Inject T t", "@Inject\nprivate [T] t;", "@Inject\nprivate T t;", "Inject annotation");
        addBuiltin("JVM languages", "sout", "print(expr)", "print([expr])", "print(expr)", "Prints expression");

        addBuiltin("Groovy", "sout", "println(expr)", "println [expr]", "println expr", "Prints expression");
        addBuiltin("Groovy", "def", "def name = expr", "def name = [expr]", "def name = expr", "Defines dynamic variable");

        // --- Ruby ---
        addBuiltin("Ruby", "puts", "puts expr", "puts [expr]", "puts expr", "Prints expression to stdout");
        addBuiltin("Ruby", "each", "expr.each { |item| }", "[expr].each { |item| }", "expr.each do |item|\n    \nend", "Iterates each item");

        // --- Scala ---
        addBuiltin("Scala", "sout", "println(expr)", "println([expr])", "println(expr)", "Prints expression");
        addBuiltin("Scala", "val", "val name = expr", "val name = [expr]", "val name = expr", "Defines val");

        // --- PHP ---
        addBuiltin("PHP", "echo", "echo expr;", "echo [expr];", "echo expr;", "Echoes expression");
        addBuiltin("PHP", "var", "$name = expr;", "$name = [expr];", "$name = expr;", "Assigns PHP variable");
    }

    private void addBuiltin(String language, String key, String example,
                            String beforeSample, String afterSample, String description) {
        addBuiltin(language, key, example, beforeSample, afterSample, description, afterSample);
    }

    private void addBuiltin(String language, String key, String example,
                            String beforeSample, String afterSample, String description,
                            String expansion) {
        String id = language.toLowerCase().replace(" ", "_") + "." + key;
        templates.put(id, new TemplateItem(id, language, key, example, beforeSample, afterSample, description, expansion, false, true));
    }

    public void addCustomTemplate(String language, String key, String example,
                                  String beforeSample, String afterSample, String description) {
        String id = "custom." + language.toLowerCase().replace(" ", "_") + "." + key + "_" + System.currentTimeMillis();
        templates.put(id, new TemplateItem(id, language, key, example, beforeSample, afterSample, description, true, true));
        notifyListeners();
    }

    public void removeTemplate(String id) {
        TemplateItem item = templates.get(id);
        if (item != null && item.isCustom()) {
            templates.remove(id);
            notifyListeners();
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        enablePostfixCompletion = getBool("enabled", enablePostfixCompletion);
        showAsCommandCompletions = getBool("show_as_command", showAsCommandCompletions);
        expandShortcut = getStr("expand_shortcut", expandShortcut);

        String disLangs = Settings.get("editor.postfix.completion.disabled_languages");
        if (disLangs != null) {
            disabledLanguages.clear();
            for (String part : disLangs.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    disabledLanguages.add(trimmed);
                }
            }
        }

        String disTemps = Settings.get("editor.postfix.completion.disabled_templates");
        Set<String> disSet = new HashSet<>();
        if (disTemps != null) {
            for (String part : disTemps.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) disSet.add(trimmed);
            }
        }

        for (TemplateItem item : templates.values()) {
            if (!item.isCustom()) {
                item.setEnabled(!disSet.contains(item.getId()));
            }
        }

        // Custom templates: id|||lang|||key|||example|||before|||after|||desc|||enabled;;;...
        String customStr = Settings.get("editor.postfix.completion.custom_templates");
        if (customStr != null && !customStr.isBlank()) {
            for (String entry : customStr.split(";;;")) {
                String[] parts = entry.split("\\|\\|\\|", -1);
                if (parts.length >= 8) {
                    String id = parts[0];
                    String lang = parts[1];
                    String key = parts[2];
                    String example = parts[3];
                    String before = parts[4];
                    String after = parts[5];
                    String desc = parts[6];
                    boolean enabled = Boolean.parseBoolean(parts[7]);
                    templates.put(id, new TemplateItem(id, lang, key, example, before, after, desc, true, enabled));
                }
            }
        }
    }

    public void save() {
        putBool("enabled", enablePostfixCompletion);
        putBool("show_as_command", showAsCommandCompletions);
        putStr("expand_shortcut", expandShortcut);

        Settings.put("editor.postfix.completion.disabled_languages", String.join(",", disabledLanguages));

        List<String> disabledIds = new ArrayList<>();
        List<String> customList = new ArrayList<>();

        for (TemplateItem item : templates.values()) {
            if (item.isCustom()) {
                customList.add(String.join("|||",
                        item.getId(),
                        item.getLanguage(),
                        item.getKey(),
                        item.getExample(),
                        item.getBeforeSample(),
                        item.getAfterSample(),
                        item.getDescription(),
                        String.valueOf(item.isEnabled())
                ));
            } else if (!item.isEnabled()) {
                disabledIds.add(item.getId());
            }
        }

        Settings.put("editor.postfix.completion.disabled_templates", String.join(",", disabledIds));
        Settings.put("editor.postfix.completion.custom_templates", String.join(";;;", customList));

        notifyListeners();
    }

    public void copyFrom(PostfixCompletionSettings o) {
        this.enablePostfixCompletion = o.enablePostfixCompletion;
        this.showAsCommandCompletions = o.showAsCommandCompletions;
        this.expandShortcut = o.expandShortcut;

        this.disabledLanguages.clear();
        this.disabledLanguages.addAll(o.disabledLanguages);

        this.templates.clear();
        for (Map.Entry<String, TemplateItem> entry : o.templates.entrySet()) {
            this.templates.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public PostfixCompletionSettings copy() {
        PostfixCompletionSettings c = new PostfixCompletionSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(PostfixCompletionSettings o) {
        if (o == null) return true;
        if (this.enablePostfixCompletion != o.enablePostfixCompletion
                || this.showAsCommandCompletions != o.showAsCommandCompletions
                || !Objects.equals(this.expandShortcut, o.expandShortcut)
                || !this.disabledLanguages.equals(o.disabledLanguages)
                || this.templates.size() != o.templates.size()) {
            return true;
        }

        for (Map.Entry<String, TemplateItem> entry : this.templates.entrySet()) {
            TemplateItem otherItem = o.templates.get(entry.getKey());
            if (otherItem == null || !entry.getValue().equals(otherItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Live editor expansion helper: given a language and expression + postfix key, returns replacement code.
     */
    public Optional<String> expand(String language, String key, String expr) {
        if (!enablePostfixCompletion) return Optional.empty();
        if (isLanguageDisabled(language)) return Optional.empty();

        for (TemplateItem item : templates.values()) {
            if (item.isEnabled() && item.getLanguage().equalsIgnoreCase(language) && item.getKey().equalsIgnoreCase(key)) {
                String template = item.getExpansion() != null ? item.getExpansion() : item.getAfterSample();
                if (template != null && !template.isBlank()) {
                    return Optional.of(template.replace("expr", expr));
                }
            }
        }
        return Optional.empty();
    }

    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.postfix.completion." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.postfix.completion." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.postfix.completion." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.postfix.completion." + key, val != null ? val : "");
    }

    // Getters and Setters
    public boolean isEnablePostfixCompletion() {
        return enablePostfixCompletion;
    }

    public void setEnablePostfixCompletion(boolean enablePostfixCompletion) {
        this.enablePostfixCompletion = enablePostfixCompletion;
    }

    public boolean isShowAsCommandCompletions() {
        return showAsCommandCompletions;
    }

    public void setShowAsCommandCompletions(boolean showAsCommandCompletions) {
        this.showAsCommandCompletions = showAsCommandCompletions;
    }

    public String getExpandShortcut() {
        return expandShortcut;
    }

    public void setExpandShortcut(String expandShortcut) {
        this.expandShortcut = expandShortcut != null ? expandShortcut : "Tab";
    }

    public boolean isLanguageDisabled(String language) {
        return disabledLanguages.contains(language);
    }

    public void setLanguageEnabled(String language, boolean enabled) {
        if (enabled) {
            disabledLanguages.remove(language);
        } else {
            disabledLanguages.add(language);
        }
        // Also enable or disable all templates in this language
        for (TemplateItem t : templates.values()) {
            if (t.getLanguage().equalsIgnoreCase(language)) {
                t.setEnabled(enabled);
            }
        }
    }

    public List<TemplateItem> getTemplatesForLanguage(String language) {
        List<TemplateItem> list = new ArrayList<>();
        for (TemplateItem t : templates.values()) {
            if (t.getLanguage().equalsIgnoreCase(language)) {
                list.add(t);
            }
        }
        list.sort((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()));
        return list;
    }

    public Map<String, TemplateItem> getAllTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public TemplateItem getTemplate(String id) {
        return templates.get(id);
    }
}
