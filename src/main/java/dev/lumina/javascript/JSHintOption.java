package dev.lumina.javascript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Descriptor for a JSHint configurable option in Lumina IDE.
 */
public class JSHintOption {

    public enum Category {
        ENFORCING,
        RELAXING,
        ENVIRONMENT,
        TRAILING
    }

    private final String key;
    private final String label;
    private final Category category;
    private final boolean defaultBooleanValue;
    private final String description;
    private final List<String> codeBadges;
    private final boolean parametric;
    private final String paramDefaultValue;

    public JSHintOption(String key, String label, Category category, boolean defaultBooleanValue) {
        this(key, label, category, defaultBooleanValue, "", List.of(), false, "");
    }

    public JSHintOption(String key, String label, Category category, boolean defaultBooleanValue, String description) {
        this(key, label, category, defaultBooleanValue, description, List.of(), false, "");
    }

    public JSHintOption(String key, String label, Category category, boolean defaultBooleanValue, String description, List<String> codeBadges) {
        this(key, label, category, defaultBooleanValue, description, codeBadges, false, "");
    }

    public JSHintOption(String key, String label, Category category, String paramDefaultValue, String description) {
        this(key, label, category, false, description, List.of(), true, paramDefaultValue);
    }

    private JSHintOption(String key, String label, Category category, boolean defaultBooleanValue,
                         String description, List<String> codeBadges, boolean parametric, String paramDefaultValue) {
        this.key = key;
        this.label = label;
        this.category = category;
        this.defaultBooleanValue = defaultBooleanValue;
        this.description = description != null ? description : "";
        this.codeBadges = codeBadges != null ? codeBadges : List.of();
        this.parametric = parametric;
        this.paramDefaultValue = paramDefaultValue != null ? paramDefaultValue : "";
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isDefaultBooleanValue() {
        return defaultBooleanValue;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCodeBadges() {
        return codeBadges;
    }

    public boolean isParametric() {
        return parametric;
    }

    public String getParamDefaultValue() {
        return paramDefaultValue;
    }

    public static List<JSHintOption> getAllOptions() {
        List<JSHintOption> list = new ArrayList<>();

        // ==========================================
        // 1. Enforcing options
        // ==========================================
        list.add(new JSHintOption("bitwise", "Warn about using bitwise operators", Category.ENFORCING, true,
                "This option prohibits the use of bitwise operators such as ^ (XOR), | (OR) and others. Bitwise operators are very rare in JavaScript programs and quite often & is simply a mistyped &&."));
        list.add(new JSHintOption("camelcase", "Warn about variable naming", Category.ENFORCING, false,
                "This option allows you to force all variable names to use either camelCase style or UPPER_CASE with underscores."));
        list.add(new JSHintOption("curly", "Warn when blocks omit {}", Category.ENFORCING, true,
                "This option requires you to always put curly braces around blocks in loops and conditionals.", List.of("{}")));
        list.add(new JSHintOption("enforceall", "Warn when code doesn't follow the most strict configuration", Category.ENFORCING, false,
                "This option enables all enforcing options and disables all relaxing options, enforcing the most strict JSHint configuration possible."));
        list.add(new JSHintOption("eqeqeq", "Warn about unsafe comparisons", Category.ENFORCING, true,
                "This option prohibits the use of == and != in favor of === and !==."));
        list.add(new JSHintOption("es3", "Warn about incompatibilities with the ES3 specification", Category.ENFORCING, false,
                "This option tells JSHint that your code needs to adhere to ECMAScript 3 specification. Use this option if you need your program to be executable in older JavaScript environments."));
        list.add(new JSHintOption("es5", "Warn about incompatibilities with the ES5 specification", Category.ENFORCING, false,
                "This option tells JSHint that your code needs to adhere to ECMAScript 5 specification. It allows features like Getters and Setters and reserved keywords as property names."));
        list.add(new JSHintOption("forin", "Warn about unsafe for..in", Category.ENFORCING, true,
                "This option requires all for..in loops to filter object's items using hasOwnProperty().", List.of("for..in")));
        list.add(new JSHintOption("freeze", "Warn about overwriting prototypes of native objects", Category.ENFORCING, false,
                "This option prohibits overwriting prototypes of native objects such as Array, Date and so on."));
        list.add(new JSHintOption("immed", "Warn about the use of immediate function invocations without wrapping them in parentheses", Category.ENFORCING, false,
                "This option prohibits the use of immediate function invocations without wrapping them in parentheses."));
        list.add(new JSHintOption("newcap", "Warn about the use of a uncapitalized constructor", Category.ENFORCING, false,
                "This option requires you to capitalize names of constructor functions."));
        list.add(new JSHintOption("noarg", "Warn about arguments.caller and .callee", Category.ENFORCING, true,
                "This option prohibits the use of arguments.caller and arguments.callee, which are deprecated and forbidden in strict mode.", List.of("arguments.caller", ".callee")));
        list.add(new JSHintOption("nocomma", "Warn about the use of the comma operator", Category.ENFORCING, false,
                "This option prohibits the use of the comma operator."));
        list.add(new JSHintOption("noempty", "Warn about empty blocks", Category.ENFORCING, true,
                "This option warns when you have an empty block in your code."));
        list.add(new JSHintOption("nonbsp", "Warn about \"non-breaking whitespace\" characters", Category.ENFORCING, false,
                "This option warns about \"non-breaking whitespace\" characters."));
        list.add(new JSHintOption("nonew", "Warn about new usage for side effects", Category.ENFORCING, true,
                "This option prohibits the use of constructor functions for 'side-effects': without saving their result into any variable.", List.of("new")));
        list.add(new JSHintOption("undef", "Warn when variable is undefined", Category.ENFORCING, true,
                "This option prohibits the use of explicitly undeclared variables. This option is very useful for spotting leaking and misspelled variables."));
        list.add(new JSHintOption("varstmt", "Warn about the use of VariableStatements", Category.ENFORCING, false,
                "When set to true, this option disallows the use of var in favor of let and const."));

        // Enforcing parametric items
        list.add(new JSHintOption("esversion", "Warn about incompatibilities with the specified ECMAScript version:", Category.ENFORCING, "any",
                "This option tells JSHint that your code uses ECMAScript specified version (3, 5, 6, 7, 8, 9, 10, or 11)."));
        list.add(new JSHintOption("latedef", "Warn about the use of a variable before it was defined:", Category.ENFORCING, "false",
                "This option prohibits the use of a variable before it was defined."));
        list.add(new JSHintOption("unused", "Warn about unused variables:", Category.ENFORCING, "false",
                "This option warns when you define and never use your variables."));
        list.add(new JSHintOption("indent", "Indentation:", Category.ENFORCING, "any",
                "This option enforces specific tab width or number of spaces for indentation."));
        list.add(new JSHintOption("quotmark", "Quotation marks:", Category.ENFORCING, "false",
                "This option enforces the consistency of quotation marks used throughout your code."));
        list.add(new JSHintOption("maxparams", "Max number of formal parameter in a function:", Category.ENFORCING, "any",
                "This option lets you set the max number of formal parameters allowed per function."));
        list.add(new JSHintOption("maxdepth", "Max depth of your blocks:", Category.ENFORCING, "any",
                "This option lets you control how nested your blocks can be."));
        list.add(new JSHintOption("maxstatements", "Max number of statements in a function:", Category.ENFORCING, "any",
                "This option lets you set the max number of statements allowed per function."));
        list.add(new JSHintOption("maxcomplexity", "Max cyclomatic complexity throughout your code:", Category.ENFORCING, "any",
                "This option lets you control cyclomatic complexity throughout your code."));
        list.add(new JSHintOption("maxlen", "Max length of a line:", Category.ENFORCING, "any",
                "This option lets you set the maximum length of a code line."));

        // Additional Enforcing checkboxes
        list.add(new JSHintOption("funcscope", "Suppress warnings about variable usage outside of its declared block", Category.ENFORCING, false,
                "This option suppresses warnings about declaring variables inside of control structures while accessing them later from the outside."));
        list.add(new JSHintOption("futurehostile", "Warns about the use of identifiers which are defined in future versions of JavaScript", Category.ENFORCING, false,
                "This option enables warnings about the use of identifiers which are defined in future versions of JavaScript."));
        list.add(new JSHintOption("globalstrict", "Suppress warnings about the use of global strict mode", Category.ENFORCING, false,
                "This option suppresses warnings about the use of global strict mode."));
        list.add(new JSHintOption("iterator", "Suppress warnings about the __iterator__ property", Category.ENFORCING, false,
                "This option suppresses warnings about the __iterator__ property.", List.of("__iterator__")));
        list.add(new JSHintOption("notypeof", "Suppress warnings about invalid typeof operator values", Category.ENFORCING, false,
                "This option suppresses warnings about invalid typeof operator values.", List.of("typeof")));
        list.add(new JSHintOption("shadow", "Suppress warnings about variable shadowing", Category.ENFORCING, false,
                "This option suppresses warnings about variable shadowing."));
        list.add(new JSHintOption("singleGroups", "Prohibits the use of the grouping operator when it is not strictly required", Category.ENFORCING, false,
                "This option prohibits the use of the grouping operator when it is not strictly required."));

        // Enforcing trailing parametric items
        list.add(new JSHintOption("maxerr", "Maximum number of errors:", Category.ENFORCING, "50",
                "This option allows you to set the maximum amount of warnings JSHint will produce before giving up."));
        list.add(new JSHintOption("predef", "Predefined ( , separated)", Category.ENFORCING, "",
                "This option allows you to predefine global variables as a comma-separated list."));

        // ==========================================
        // 2. Relaxing options
        // ==========================================
        list.add(new JSHintOption("plusplus", "Warn about the use of unary increment and decrement operators", Category.RELAXING, false,
                "This option prohibits the use of unary increment and decrement operators."));
        list.add(new JSHintOption("strict", "Warn when code is not in strict mode", Category.RELAXING, true,
                "This option requires the code to run in ECMAScript 5 strict mode."));
        list.add(new JSHintOption("asi", "Suppress warnings about missing semicolons", Category.RELAXING, false,
                "This option suppresses warnings about missing semicolons."));
        list.add(new JSHintOption("boss", "Suppress warnings about assignments inside if/for/...", Category.RELAXING, false,
                "This option suppresses warnings about the use of assignments in cases where comparisons are expected.", List.of("if/for/...")));
        list.add(new JSHintOption("debug", "Suppress warnings about debugging code", Category.RELAXING, false,
                "This option suppresses warnings about the debugger statements in your code."));
        list.add(new JSHintOption("elision", "Suppress warnings about ES3 array elision elements", Category.RELAXING, false,
                "This option tells JSHint that your code uses ES3 array elision elements, or empty elements in array literals."));
        list.add(new JSHintOption("eqnull", "Suppress warnings about == null", Category.RELAXING, false,
                "This option suppresses warnings about == null comparisons.", List.of("== null")));
        list.add(new JSHintOption("esnext", "EcmaScript.next", Category.RELAXING, false,
                "This option tells JSHint that your code uses ECMAScript.next syntax."));
        list.add(new JSHintOption("evil", "Suppress warnings about eval", Category.RELAXING, false,
                "This option suppresses warnings about the use of eval.", List.of("eval")));
        list.add(new JSHintOption("expr", "Suppress warnings about the use of expressions as statements", Category.RELAXING, false,
                "This option suppresses warnings about the use of expressions where normally you would expect to see assignments or function calls."));
        list.add(new JSHintOption("lastsemic", "Suppress warnings about missing semicolons, but only when the semicolon is omitted for the last statement in a one-line block", Category.RELAXING, false,
                "This option suppresses warnings about missing semicolons, but only when the semicolon is omitted for the last statement in a one-line block."));
        list.add(new JSHintOption("laxbreak", "Suppress warnings about unsafe line breaks", Category.RELAXING, false,
                "This option suppresses most of the warnings about possibly unsafe line breaks in your code."));
        list.add(new JSHintOption("laxcomma", "Suppress warnings about comma-first coding style", Category.RELAXING, false,
                "This option suppresses warnings about comma-first coding style."));
        list.add(new JSHintOption("loopfunc", "Suppress warnings about functions inside loops", Category.RELAXING, false,
                "This option suppresses warnings about functions inside of loops."));
        list.add(new JSHintOption("moz", "Check if your code uses Mozilla JavaScript extensions", Category.RELAXING, false,
                "This option tells JSHint that your code uses Mozilla JavaScript extensions."));
        list.add(new JSHintOption("multistr", "Suppress warnings about multi-line strings", Category.RELAXING, false,
                "This option suppresses warnings about multi-line strings."));
        list.add(new JSHintOption("proto", "Suppress warnings about the __proto__ property", Category.RELAXING, false,
                "This option suppresses warnings about the __proto__ property.", List.of("__proto__")));
        list.add(new JSHintOption("scripturl", "Suppress warnings about the use of script-targeted URLs", Category.RELAXING, false,
                "This option suppresses warnings about the use of script-targeted URLs—such as javascript:..."));
        list.add(new JSHintOption("sub", "Suppress warnings about using [] notation when it can be expressed in dot notation", Category.RELAXING, false,
                "This option suppresses warnings about using [] notation when it can still be expressed in dot notation.", List.of("[]")));
        list.add(new JSHintOption("supernew", "Suppress warnings about \"weird\" constructions", Category.RELAXING, false,
                "This option suppresses warnings about \"weird\" constructions like new function () { ... } and new Object;"));
        list.add(new JSHintOption("validthis", "Suppress warnings about possible strict violations", Category.RELAXING, false,
                "This option suppresses warnings about possible strict violations when the code is running in strict mode and you use this in a non-constructor function."));
        list.add(new JSHintOption("withstmt", "Suppresses warnings about the use of the with statement", Category.RELAXING, false,
                "This option suppresses warnings about the use of the with statement."));
        list.add(new JSHintOption("noyield", "Suppress warnings about generator functions with no yield statement in them", Category.RELAXING, false,
                "This option suppresses warnings about generator functions with no yield statement in them."));

        // ==========================================
        // 3. Environments
        // ==========================================
        list.add(new JSHintOption("browser", "Browser", Category.ENVIRONMENT, true,
                "This option defines globals exposed by modern browsers: document, navigator, FileReader and so on."));
        list.add(new JSHintOption("browserify", "Browserify", Category.ENVIRONMENT, false,
                "This option defines globals available when using the Browserify tool to build a project."));
        list.add(new JSHintOption("couch", "CouchDB", Category.ENVIRONMENT, false,
                "This option defines globals exposed by CouchDB."));
        list.add(new JSHintOption("devel", "Development", Category.ENVIRONMENT, false,
                "This option defines globals that are usually used for logging poor-man's debugging: console, alert, etc."));
        list.add(new JSHintOption("dojo", "Dojo Toolkit", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the Dojo Toolkit."));
        list.add(new JSHintOption("jasmine", "Jasmine", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the Jasmine unit testing framework."));
        list.add(new JSHintOption("jquery", "jQuery", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the jQuery JavaScript library."));
        list.add(new JSHintOption("mocha", "Mocha", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the Mocha unit testing framework."));
        list.add(new JSHintOption("module", "ECMAScript 6 module", Category.ENVIRONMENT, false,
                "This option informs JSHint that the input code describes an ECMAScript 6 module."));
        list.add(new JSHintOption("mootools", "MooTools", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the MooTools JavaScript framework."));
        list.add(new JSHintOption("node", "Node.js", Category.ENVIRONMENT, false,
                "This option defines globals available when your code is running inside of the Node runtime environment."));
        list.add(new JSHintOption("nonstandard", "Escape and unescape", Category.ENVIRONMENT, false,
                "This option defines non-standard but widely adopted globals such as escape and unescape."));
        list.add(new JSHintOption("phantom", "PhantomJS", Category.ENVIRONMENT, false,
                "This option defines globals available when your code is running inside of the PhantomJS runtime environment."));
        list.add(new JSHintOption("prototypejs", "Prototype", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the Prototype JavaScript framework."));
        list.add(new JSHintOption("qunit", "QUnit", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the QUnit unit testing framework."));
        list.add(new JSHintOption("rhino", "Rhino", Category.ENVIRONMENT, false,
                "This option defines globals available when your code is running inside of the Rhino runtime environment."));
        list.add(new JSHintOption("shelljs", "ShellJS", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the ShellJS library."));
        list.add(new JSHintOption("typed", "Typed arrays", Category.ENVIRONMENT, false,
                "This option defines globals for typed array constructors."));
        list.add(new JSHintOption("worker", "Web Worker", Category.ENVIRONMENT, false,
                "This option defines globals available when code is running inside of a Web Worker."));
        list.add(new JSHintOption("wsh", "Windows Script Host", Category.ENVIRONMENT, false,
                "This option defines globals available when your code is running as a script for the Windows Script Host."));
        list.add(new JSHintOption("yui", "YUI", Category.ENVIRONMENT, false,
                "This option defines globals exposed by the YUI framework."));

        // ==========================================
        // 4. Trailing options
        // ==========================================
        list.add(new JSHintOption("trailing", "Warn about trailing whitespace", Category.TRAILING, false,
                "This option makes it an error to leave trailing whitespace in your code."));
        list.add(new JSHintOption("gcl", "Makes JSHint compatible with Google Closure Compiler", Category.TRAILING, false,
                "This option makes JSHint compatible with Google Closure Compiler."));
        list.add(new JSHintOption("smarttabs", "Suppress warnings about mixed tabs and spaces when the latter are used for alignment only", Category.TRAILING, false,
                "This option suppresses warnings about mixed tabs and spaces when the latter are used for alignment only."));
        list.add(new JSHintOption("nomen", "Disallow the use of _ in variables", Category.TRAILING, false,
                "This option disallows the use of dangling _ in variables.", List.of("_")));
        list.add(new JSHintOption("onevar", "One var statement per function", Category.TRAILING, false,
                "This option allows only one var statement per function."));
        list.add(new JSHintOption("passfail", "Stop on first error", Category.TRAILING, false,
                "This option makes JSHint stop on the first error or warning."));
        list.add(new JSHintOption("white", "Disallow messy white space", Category.TRAILING, false,
                "This option checks against messy whitespace and indentation rules."));

        return Collections.unmodifiableList(list);
    }
}
