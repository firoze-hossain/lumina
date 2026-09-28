package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dedicated Kotlin code style settings and dynamic provider definition.
 * Strictly decoupled and brand-isolated.
 * Supports all 8 configuration tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790572860199.png)
 * 2. Spaces (media_1790572880868.png)
 * 3. Wrapping and Braces (media_1790572899375.png & media_1790572915955.png)
 * 4. Blank Lines (media_1790572928571.png)
 * 5. Imports (media_1790573944336.png)
 * 6. Other (media_1790573953178.png)
 * 7. Code Generation (media_1790573962261.png)
 * 8. Load/Save (media_1790573974710.png)
 */
public class KotlinCodeStyleSettings extends LanguageCodeStyleSettings {

    // Common wrapping choices
    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    // ==========================================
    // Property Keys - Spaces
    // ==========================================
    public static final String SPACE_BEFORE_IF_PARENTHESES = "space_before_if_parentheses";
    public static final String SPACE_BEFORE_FOR_PARENTHESES = "space_before_for_parentheses";
    public static final String SPACE_BEFORE_WHILE_PARENTHESES = "space_before_while_parentheses";
    public static final String SPACE_BEFORE_CATCH_PARENTHESES = "space_before_catch_parentheses";
    public static final String SPACE_BEFORE_WHEN_PARENTHESES = "space_before_when_parentheses";

    public static final String SPACE_AROUND_ASSIGNMENT_OPERATORS = "space_around_assignment_operators";
    public static final String SPACE_AROUND_LOGICAL_OPERATORS = "space_around_logical_operators";
    public static final String SPACE_AROUND_EQUALITY_OPERATORS = "space_around_equality_operators";
    public static final String SPACE_AROUND_RELATIONAL_OPERATORS = "space_around_relational_operators";
    public static final String SPACE_AROUND_ADDITIVE_OPERATORS = "space_around_additive_operators";
    public static final String SPACE_AROUND_MULTIPLICATIVE_OPERATORS = "space_around_multiplicative_operators";
    public static final String SPACE_AROUND_UNARY_OPERATORS = "space_around_unary_operators";
    public static final String SPACE_AROUND_RANGE_OPERATORS = "space_around_range_operators";
    public static final String SPACE_AROUND_ELVIS_OPERATOR = "space_around_elvis_operator";

    public static final String SPACE_BEFORE_COMMA = "space_before_comma";
    public static final String SPACE_AFTER_COMMA = "space_after_comma";
    public static final String SPACE_BEFORE_COLON_AFTER_DECLARATION_NAME = "space_before_colon_after_declaration_name";
    public static final String SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE = "space_after_colon_before_declaration_type";
    public static final String SPACE_BEFORE_COLON_IN_NEW_TYPE_DEFINITION = "space_before_colon_in_new_type_definition";
    public static final String SPACE_AFTER_COLON_IN_NEW_TYPE_DEFINITION = "space_after_colon_in_new_type_definition";
    public static final String SPACE_IN_SIMPLE_ONE_LINE_METHODS = "space_in_simple_one_line_methods";
    public static final String SPACE_AROUND_ARROW_IN_FUNCTION_TYPES = "space_around_arrow_in_function_types";
    public static final String SPACE_AROUND_ARROW_IN_WHEN_CLAUSE = "space_around_arrow_in_when_clause";
    public static final String SPACE_BEFORE_LAMBDA_ARROW = "space_before_lambda_arrow";

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String HARD_WRAP_AT = "hard_wrap_at";
    public static final String WRAP_ON_TYPING = "wrap_on_typing";
    public static final String VISUAL_GUIDES = "visual_guides";

    public static final String WRAP_KEEP_LINE_BREAKS = "wrap_keep_line_breaks";
    public static final String WRAP_KEEP_COMMENT_AT_FIRST_COLUMN = "wrap_keep_comment_at_first_column";

    public static final String WRAP_EXTENDS_LIST = "wrap_extends_list";
    public static final String WRAP_ALIGN_MULTILINE_EXTENDS_LIST = "wrap_align_multiline_extends_list";
    public static final String WRAP_CONTINUATION_INDENT_EXTENDS_LIST = "wrap_continuation_indent_extends_list";

    public static final String WRAP_FUNCTION_PARAMETERS = "wrap_function_parameters";
    public static final String WRAP_ALIGN_MULTILINE_FUNCTION_PARAMETERS = "wrap_align_multiline_function_parameters";
    public static final String WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_PARAMETERS = "wrap_new_line_after_open_paren_function_parameters";
    public static final String WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_PARAMETERS = "wrap_place_close_paren_on_new_line_function_parameters";
    public static final String WRAP_CONTINUATION_INDENT_FUNCTION_PARAMETERS = "wrap_continuation_indent_function_parameters";

    public static final String WRAP_FUNCTION_ARGUMENTS = "wrap_function_arguments";
    public static final String WRAP_ALIGN_MULTILINE_FUNCTION_ARGUMENTS = "wrap_align_multiline_function_arguments";
    public static final String WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_ARGUMENTS = "wrap_new_line_after_open_paren_function_arguments";
    public static final String WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS = "wrap_place_close_paren_on_new_line_function_arguments";
    public static final String WRAP_CONTINUATION_INDENT_FUNCTION_ARGUMENTS = "wrap_continuation_indent_function_arguments";

    public static final String WRAP_ALIGN_MULTILINE_FUNCTION_PARENTHESES = "wrap_align_multiline_function_parentheses";

    public static final String WRAP_CHAINED_CALLS = "wrap_chained_calls";
    public static final String WRAP_FIRST_CALL_CHAINED_CALLS = "wrap_first_call_chained_calls";
    public static final String WRAP_CONTINUATION_INDENT_CHAINED_CALLS = "wrap_continuation_indent_chained_calls";

    public static final String WRAP_ELSE_ON_NEW_LINE = "wrap_else_on_new_line";
    public static final String WRAP_IF_CLOSE_PAREN_ON_NEW_LINE = "wrap_if_close_paren_on_new_line";
    public static final String WRAP_USE_CONTINUATION_INDENT_IN_CONDITIONS = "wrap_use_continuation_indent_in_conditions";

    public static final String WRAP_WHILE_ON_NEW_LINE = "wrap_while_on_new_line";
    public static final String WRAP_CATCH_ON_NEW_LINE = "wrap_catch_on_new_line";
    public static final String WRAP_FINALLY_ON_NEW_LINE = "wrap_finally_on_new_line";

    public static final String WRAP_ALIGN_MULTILINE_BINARY_EXPRESSIONS = "wrap_align_multiline_binary_expressions";

    public static final String WRAP_ASSIGNMENT_STATEMENT = "wrap_assignment_statement";
    public static final String WRAP_ENUM_CONSTANTS = "wrap_enum_constants";
    public static final String WRAP_CLASS_ANNOTATIONS = "wrap_class_annotations";
    public static final String WRAP_FUNCTION_ANNOTATIONS = "wrap_function_annotations";
    public static final String WRAP_PROPERTY_ANNOTATIONS = "wrap_property_annotations";
    public static final String WRAP_PARAMETER_ANNOTATIONS = "wrap_parameter_annotations";
    public static final String WRAP_LOCAL_VARIABLE_ANNOTATIONS = "wrap_local_variable_annotations";
    public static final String WRAP_PROPERTY_CONTEXT_PARAMETERS = "wrap_property_context_parameters";
    public static final String WRAP_FUNCTION_CONTEXT_PARAMETERS = "wrap_function_context_parameters";

    public static final String WRAP_ALIGN_WHEN_BRANCHES_IN_COLUMNS = "wrap_align_when_branches_in_columns";
    public static final String WRAP_NEW_LINE_AFTER_MULTILINE_WHEN_ENTRY = "wrap_new_line_after_multiline_when_entry";
    public static final String WRAP_INDENT_BEFORE_ARROW_ON_NEW_LINE = "wrap_indent_before_arrow_on_new_line";

    public static final String WRAP_PUT_LEFT_BRACE_ON_NEW_LINE = "wrap_put_left_brace_on_new_line";

    public static final String WRAP_EXPRESSION_BODY_FUNCTIONS = "wrap_expression_body_functions";
    public static final String WRAP_CONTINUATION_INDENT_EXPRESSION_BODY = "wrap_continuation_indent_expression_body";

    public static final String WRAP_ELVIS_EXPRESSIONS = "wrap_elvis_expressions";
    public static final String WRAP_CONTINUATION_INDENT_ELVIS_EXPRESSIONS = "wrap_continuation_indent_elvis_expressions";

    // ==========================================
    // Property Keys - Blank Lines
    // ==========================================
    public static final String BLANK_LINES_KEEP_IN_DECLARATIONS = "blank_lines_keep_in_declarations";
    public static final String BLANK_LINES_KEEP_IN_CODE = "blank_lines_keep_in_code";
    public static final String BLANK_LINES_KEEP_BEFORE_CLOSING_BRACE = "blank_lines_keep_before_closing_brace";

    public static final String BLANK_LINES_MIN_AFTER_CLASS_HEADER = "blank_lines_min_after_class_header";
    public static final String BLANK_LINES_MIN_AROUND_WHEN_BRANCHES_WITH_BRACES = "blank_lines_min_around_when_branches_with_braces";
    public static final String BLANK_LINES_MIN_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION = "blank_lines_min_before_declaration_with_comment_or_annotation";

    // ==========================================
    // Property Keys - Imports (media_1790573944336.png)
    // ==========================================
    public static final String TOP_LEVEL_IMPORT_MODE = "top_level_import_mode";
    public static final String TOP_LEVEL_IMPORT_THRESHOLD = "top_level_import_threshold";
    public static final String JAVA_STATICS_IMPORT_MODE = "java_statics_import_mode";
    public static final String JAVA_STATICS_IMPORT_THRESHOLD = "java_statics_import_threshold";
    public static final String INSERT_IMPORTS_FOR_NESTED_CLASSES = "insert_imports_for_nested_classes";
    public static final String IMPORT_ALIASES_SEPARATELY = "import_aliases_separately";

    // ==========================================
    // Property Keys - Other / Trailing Comma (media_1790573953178.png)
    // ==========================================
    public static final String TRAILING_COMMA_ENABLED = "trailing_comma_enabled";
    public static final String TRAILING_COMMA_TYPE_PARAMETER_LIST = "trailing_comma_type_parameter_list";
    public static final String TRAILING_COMMA_DESTRUCTURING_DECLARATION = "trailing_comma_destructuring_declaration";
    public static final String TRAILING_COMMA_WHEN_ENTRY = "trailing_comma_when_entry";
    public static final String TRAILING_COMMA_FUNCTION_LITERAL = "trailing_comma_function_literal";
    public static final String TRAILING_COMMA_VALUE_PARAMETER_LIST = "trailing_comma_value_parameter_list";
    public static final String TRAILING_COMMA_CONTEXT_RECEIVER_LIST = "trailing_comma_context_receiver_list";
    public static final String TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION = "trailing_comma_collection_literal_expression";
    public static final String TRAILING_COMMA_TYPE_ARGUMENT_LIST = "trailing_comma_type_argument_list";
    public static final String TRAILING_COMMA_INDICES = "trailing_comma_indices";
    public static final String TRAILING_COMMA_VALUE_ARGUMENT_LIST = "trailing_comma_value_argument_list";

    // ==========================================
    // Property Keys - Code Generation (media_1790573962261.png)
    // ==========================================
    public static final String CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN = "code_gen_line_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START = "code_gen_add_space_at_line_comment_start";
    public static final String CODE_GEN_ENFORCE_ON_REFORMAT = "code_gen_enforce_on_reformat";
    public static final String CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN = "code_gen_block_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS = "code_gen_add_spaces_around_block_comments";

    // ==========================================
    // Property Keys - Load/Save (media_1790573974710.png)
    // ==========================================
    public static final String LOAD_SAVE_USE_DEFAULTS_FROM = "load_save_use_defaults_from";

    /**
     * Import Table Entry representation.
     */
    public static class ImportEntry {
        private String packageName;
        private boolean withSubpackages;

        public ImportEntry(String packageName, boolean withSubpackages) {
            this.packageName = packageName != null ? packageName : "";
            this.withSubpackages = withSubpackages;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName != null ? packageName : "";
        }

        public boolean isWithSubpackages() {
            return withSubpackages;
        }

        public void setWithSubpackages(boolean withSubpackages) {
            this.withSubpackages = withSubpackages;
        }

        public ImportEntry copy() {
            return new ImportEntry(packageName, withSubpackages);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImportEntry that = (ImportEntry) o;
            return withSubpackages == that.withSubpackages && Objects.equals(packageName, that.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, withSubpackages);
        }
    }

    private final List<ImportEntry> packagesToUseImportOnDemand = new ArrayList<>();
    private final List<ImportEntry> importLayout = new ArrayList<>();

    public KotlinCodeStyleSettings() {
        this("Kotlin");
    }

    public KotlinCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tabs & Indents
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);

        // Spaces - Before parentheses
        setBoolean(SPACE_BEFORE_IF_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_FOR_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_WHILE_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_CATCH_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_WHEN_PARENTHESES, true);

        // Spaces - Around operators
        setBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, true);
        setBoolean(SPACE_AROUND_LOGICAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_EQUALITY_OPERATORS, true);
        setBoolean(SPACE_AROUND_RELATIONAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_ADDITIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_UNARY_OPERATORS, false);
        setBoolean(SPACE_AROUND_RANGE_OPERATORS, false);
        setBoolean(SPACE_AROUND_ELVIS_OPERATOR, true);

        // Spaces - Other
        setBoolean(SPACE_BEFORE_COMMA, false);
        setBoolean(SPACE_AFTER_COMMA, true);
        setBoolean(SPACE_BEFORE_COLON_AFTER_DECLARATION_NAME, false);
        setBoolean(SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE, true);
        setBoolean(SPACE_BEFORE_COLON_IN_NEW_TYPE_DEFINITION, true);
        setBoolean(SPACE_AFTER_COLON_IN_NEW_TYPE_DEFINITION, true);
        setBoolean(SPACE_IN_SIMPLE_ONE_LINE_METHODS, true);
        setBoolean(SPACE_AROUND_ARROW_IN_FUNCTION_TYPES, true);
        setBoolean(SPACE_AROUND_ARROW_IN_WHEN_CLAUSE, true);
        setBoolean(SPACE_BEFORE_LAMBDA_ARROW, true);

        // Wrapping and Braces
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "Default: None");

        setBoolean(WRAP_KEEP_LINE_BREAKS, true);
        setBoolean(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, false);

        setString(WRAP_EXTENDS_LIST, "Wrap if long");
        setBoolean(WRAP_ALIGN_MULTILINE_EXTENDS_LIST, false);
        setBoolean(WRAP_CONTINUATION_INDENT_EXTENDS_LIST, false);

        setString(WRAP_FUNCTION_PARAMETERS, "Chop down if long");
        setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARAMETERS, true);
        setBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_PARAMETERS, true);
        setBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, true);
        setBoolean(WRAP_CONTINUATION_INDENT_FUNCTION_PARAMETERS, false);

        setString(WRAP_FUNCTION_ARGUMENTS, "Chop down if long");
        setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_ARGUMENTS, false);
        setBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_ARGUMENTS, true);
        setBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, true);
        setBoolean(WRAP_CONTINUATION_INDENT_FUNCTION_ARGUMENTS, false);

        setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARENTHESES, false);

        setString(WRAP_CHAINED_CALLS, "Wrap if long");
        setBoolean(WRAP_FIRST_CALL_CHAINED_CALLS, false);
        setBoolean(WRAP_CONTINUATION_INDENT_CHAINED_CALLS, false);

        setBoolean(WRAP_ELSE_ON_NEW_LINE, false);
        setBoolean(WRAP_IF_CLOSE_PAREN_ON_NEW_LINE, true);
        setBoolean(WRAP_USE_CONTINUATION_INDENT_IN_CONDITIONS, false);

        setBoolean(WRAP_WHILE_ON_NEW_LINE, false);
        setBoolean(WRAP_CATCH_ON_NEW_LINE, false);
        setBoolean(WRAP_FINALLY_ON_NEW_LINE, false);

        setBoolean(WRAP_ALIGN_MULTILINE_BINARY_EXPRESSIONS, false);

        setString(WRAP_ASSIGNMENT_STATEMENT, "Wrap if long");
        setString(WRAP_ENUM_CONSTANTS, "Do not wrap");
        setString(WRAP_CLASS_ANNOTATIONS, "Wrap always");
        setString(WRAP_FUNCTION_ANNOTATIONS, "Wrap always");
        setString(WRAP_PROPERTY_ANNOTATIONS, "Wrap always");
        setString(WRAP_PARAMETER_ANNOTATIONS, "Do not wrap");
        setString(WRAP_LOCAL_VARIABLE_ANNOTATIONS, "Do not wrap");
        setString(WRAP_PROPERTY_CONTEXT_PARAMETERS, "Wrap always");
        setString(WRAP_FUNCTION_CONTEXT_PARAMETERS, "Wrap always");

        setBoolean(WRAP_ALIGN_WHEN_BRANCHES_IN_COLUMNS, false);
        setBoolean(WRAP_NEW_LINE_AFTER_MULTILINE_WHEN_ENTRY, true);
        setBoolean(WRAP_INDENT_BEFORE_ARROW_ON_NEW_LINE, true);

        setBoolean(WRAP_PUT_LEFT_BRACE_ON_NEW_LINE, false);

        setString(WRAP_EXPRESSION_BODY_FUNCTIONS, "Wrap if long");
        setBoolean(WRAP_CONTINUATION_INDENT_EXPRESSION_BODY, false);

        setString(WRAP_ELVIS_EXPRESSIONS, "Wrap if long");
        setBoolean(WRAP_CONTINUATION_INDENT_ELVIS_EXPRESSIONS, false);

        // Blank lines
        setInt(BLANK_LINES_KEEP_IN_DECLARATIONS, 2);
        setInt(BLANK_LINES_KEEP_IN_CODE, 2);
        setInt(BLANK_LINES_KEEP_BEFORE_CLOSING_BRACE, 2);

        setInt(BLANK_LINES_MIN_AFTER_CLASS_HEADER, 0);
        setInt(BLANK_LINES_MIN_AROUND_WHEN_BRANCHES_WITH_BRACES, 0);
        setInt(BLANK_LINES_MIN_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION, 1);

        // Imports (media_1790573944336.png)
        setString(TOP_LEVEL_IMPORT_MODE, "WHEN_AT_LEAST");
        setInt(TOP_LEVEL_IMPORT_THRESHOLD, 5);
        setString(JAVA_STATICS_IMPORT_MODE, "WHEN_AT_LEAST");
        setInt(JAVA_STATICS_IMPORT_THRESHOLD, 3);
        setBoolean(INSERT_IMPORTS_FOR_NESTED_CLASSES, false);
        setBoolean(IMPORT_ALIASES_SEPARATELY, true);

        packagesToUseImportOnDemand.clear();
        packagesToUseImportOnDemand.add(new ImportEntry("import java.util.*", false));
        packagesToUseImportOnDemand.add(new ImportEntry("import kotlinx.android.synthetic.*", true));
        packagesToUseImportOnDemand.add(new ImportEntry("import io.ktor.*", true));

        importLayout.clear();
        importLayout.add(new ImportEntry("import javax.*", true));
        importLayout.add(new ImportEntry("import kotlin.*", true));
        importLayout.add(new ImportEntry("import all alias imports", false));

        // Other / Trailing Comma (media_1790573953178.png)
        setBoolean(TRAILING_COMMA_ENABLED, false);
        setBoolean(TRAILING_COMMA_TYPE_PARAMETER_LIST, true);
        setBoolean(TRAILING_COMMA_DESTRUCTURING_DECLARATION, false);
        setBoolean(TRAILING_COMMA_WHEN_ENTRY, true);
        setBoolean(TRAILING_COMMA_FUNCTION_LITERAL, true);
        setBoolean(TRAILING_COMMA_VALUE_PARAMETER_LIST, true);
        setBoolean(TRAILING_COMMA_CONTEXT_RECEIVER_LIST, true);
        setBoolean(TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION, false);
        setBoolean(TRAILING_COMMA_TYPE_ARGUMENT_LIST, false);
        setBoolean(TRAILING_COMMA_INDICES, false);
        setBoolean(TRAILING_COMMA_VALUE_ARGUMENT_LIST, false);

        // Code Generation (media_1790573962261.png)
        setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, false);
        setBoolean(CODE_GEN_ENFORCE_ON_REFORMAT, false);
        setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false);

        // Load/Save (media_1790573974710.png)
        setString(LOAD_SAVE_USE_DEFAULTS_FROM, "<ide defaults>");
    }

    @Override
    public KotlinCodeStyleSettings copy() {
        KotlinCodeStyleSettings copy = new KotlinCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setLabelIndent(getLabelIndent());
        copy.setAbsoluteLabelIndent(isAbsoluteLabelIndent());
        copy.setDoNotIndentTopLevelMembers(isDoNotIndentTopLevelMembers());
        copy.setUseIndentsRelativeToExpressionStart(isUseIndentsRelativeToExpressionStart());
        copy.setProperties(getAllProperties());

        copy.packagesToUseImportOnDemand.clear();
        for (ImportEntry e : this.packagesToUseImportOnDemand) {
            copy.packagesToUseImportOnDemand.add(e.copy());
        }

        copy.importLayout.clear();
        for (ImportEntry e : this.importLayout) {
            copy.importLayout.add(e.copy());
        }

        return copy;
    }

    // ==========================================
    // Strongly-Typed Getters & Setters - Spaces
    // ==========================================
    public boolean isSpaceBeforeIfParentheses() { return getBoolean(SPACE_BEFORE_IF_PARENTHESES, true); }
    public void setSpaceBeforeIfParentheses(boolean v) { setBoolean(SPACE_BEFORE_IF_PARENTHESES, v); }

    public boolean isSpaceBeforeForParentheses() { return getBoolean(SPACE_BEFORE_FOR_PARENTHESES, true); }
    public void setSpaceBeforeForParentheses(boolean v) { setBoolean(SPACE_BEFORE_FOR_PARENTHESES, v); }

    public boolean isSpaceBeforeWhileParentheses() { return getBoolean(SPACE_BEFORE_WHILE_PARENTHESES, true); }
    public void setSpaceBeforeWhileParentheses(boolean v) { setBoolean(SPACE_BEFORE_WHILE_PARENTHESES, v); }

    public boolean isSpaceBeforeCatchParentheses() { return getBoolean(SPACE_BEFORE_CATCH_PARENTHESES, true); }
    public void setSpaceBeforeCatchParentheses(boolean v) { setBoolean(SPACE_BEFORE_CATCH_PARENTHESES, v); }

    public boolean isSpaceBeforeWhenParentheses() { return getBoolean(SPACE_BEFORE_WHEN_PARENTHESES, true); }
    public void setSpaceBeforeWhenParentheses(boolean v) { setBoolean(SPACE_BEFORE_WHEN_PARENTHESES, v); }

    public boolean isSpaceAroundAssignmentOperators() { return getBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, true); }
    public void setSpaceAroundAssignmentOperators(boolean v) { setBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, v); }

    public boolean isSpaceAroundLogicalOperators() { return getBoolean(SPACE_AROUND_LOGICAL_OPERATORS, true); }
    public void setSpaceAroundLogicalOperators(boolean v) { setBoolean(SPACE_AROUND_LOGICAL_OPERATORS, v); }

    public boolean isSpaceAroundEqualityOperators() { return getBoolean(SPACE_AROUND_EQUALITY_OPERATORS, true); }
    public void setSpaceAroundEqualityOperators(boolean v) { setBoolean(SPACE_AROUND_EQUALITY_OPERATORS, v); }

    public boolean isSpaceAroundRelationalOperators() { return getBoolean(SPACE_AROUND_RELATIONAL_OPERATORS, true); }
    public void setSpaceAroundRelationalOperators(boolean v) { setBoolean(SPACE_AROUND_RELATIONAL_OPERATORS, v); }

    public boolean isSpaceAroundAdditiveOperators() { return getBoolean(SPACE_AROUND_ADDITIVE_OPERATORS, true); }
    public void setSpaceAroundAdditiveOperators(boolean v) { setBoolean(SPACE_AROUND_ADDITIVE_OPERATORS, v); }

    public boolean isSpaceAroundMultiplicativeOperators() { return getBoolean(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true); }
    public void setSpaceAroundMultiplicativeOperators(boolean v) { setBoolean(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, v); }

    public boolean isSpaceAroundUnaryOperators() { return getBoolean(SPACE_AROUND_UNARY_OPERATORS, false); }
    public void setSpaceAroundUnaryOperators(boolean v) { setBoolean(SPACE_AROUND_UNARY_OPERATORS, v); }

    public boolean isSpaceAroundRangeOperators() { return getBoolean(SPACE_AROUND_RANGE_OPERATORS, false); }
    public void setSpaceAroundRangeOperators(boolean v) { setBoolean(SPACE_AROUND_RANGE_OPERATORS, v); }

    public boolean isSpaceAroundElvisOperator() { return getBoolean(SPACE_AROUND_ELVIS_OPERATOR, true); }
    public void setSpaceAroundElvisOperator(boolean v) { setBoolean(SPACE_AROUND_ELVIS_OPERATOR, v); }

    public boolean isSpaceBeforeComma() { return getBoolean(SPACE_BEFORE_COMMA, false); }
    public void setSpaceBeforeComma(boolean v) { setBoolean(SPACE_BEFORE_COMMA, v); }

    public boolean isSpaceAfterComma() { return getBoolean(SPACE_AFTER_COMMA, true); }
    public void setSpaceAfterComma(boolean v) { setBoolean(SPACE_AFTER_COMMA, v); }

    public boolean isSpaceBeforeColonAfterDeclarationName() { return getBoolean(SPACE_BEFORE_COLON_AFTER_DECLARATION_NAME, false); }
    public void setSpaceBeforeColonAfterDeclarationName(boolean v) { setBoolean(SPACE_BEFORE_COLON_AFTER_DECLARATION_NAME, v); }

    public boolean isSpaceAfterColonBeforeDeclarationType() { return getBoolean(SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE, true); }
    public void setSpaceAfterColonBeforeDeclarationType(boolean v) { setBoolean(SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE, v); }

    public boolean isSpaceBeforeColonInNewTypeDefinition() { return getBoolean(SPACE_BEFORE_COLON_IN_NEW_TYPE_DEFINITION, true); }
    public void setSpaceBeforeColonInNewTypeDefinition(boolean v) { setBoolean(SPACE_BEFORE_COLON_IN_NEW_TYPE_DEFINITION, v); }

    public boolean isSpaceAfterColonInNewTypeDefinition() { return getBoolean(SPACE_AFTER_COLON_IN_NEW_TYPE_DEFINITION, true); }
    public void setSpaceAfterColonInNewTypeDefinition(boolean v) { setBoolean(SPACE_AFTER_COLON_IN_NEW_TYPE_DEFINITION, v); }

    public boolean isSpaceInSimpleOneLineMethods() { return getBoolean(SPACE_IN_SIMPLE_ONE_LINE_METHODS, true); }
    public void setSpaceInSimpleOneLineMethods(boolean v) { setBoolean(SPACE_IN_SIMPLE_ONE_LINE_METHODS, v); }

    public boolean isSpaceAroundArrowInFunctionTypes() { return getBoolean(SPACE_AROUND_ARROW_IN_FUNCTION_TYPES, true); }
    public void setSpaceAroundArrowInFunctionTypes(boolean v) { setBoolean(SPACE_AROUND_ARROW_IN_FUNCTION_TYPES, v); }

    public boolean isSpaceAroundArrowInWhenClause() { return getBoolean(SPACE_AROUND_ARROW_IN_WHEN_CLAUSE, true); }
    public void setSpaceAroundArrowInWhenClause(boolean v) { setBoolean(SPACE_AROUND_ARROW_IN_WHEN_CLAUSE, v); }

    public boolean isSpaceBeforeLambdaArrow() { return getBoolean(SPACE_BEFORE_LAMBDA_ARROW, true); }
    public void setSpaceBeforeLambdaArrow(boolean v) { setBoolean(SPACE_BEFORE_LAMBDA_ARROW, v); }

    // ==========================================
    // Strongly-Typed Getters & Setters - Wrapping
    // ==========================================
    public boolean isLineBreaksKeepWhenReformatting() { return getBoolean(WRAP_KEEP_LINE_BREAKS, true); }
    public void setLineBreaksKeepWhenReformatting(boolean v) { setBoolean(WRAP_KEEP_LINE_BREAKS, v); }

    public boolean isCommentAtFirstColumnKeepWhenReformatting() { return getBoolean(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, false); }
    public void setCommentAtFirstColumnKeepWhenReformatting(boolean v) { setBoolean(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, v); }

    public String getExtendsListWrap() { return getString(WRAP_EXTENDS_LIST, "Wrap if long"); }
    public void setExtendsListWrap(String v) { setString(WRAP_EXTENDS_LIST, v); }

    public boolean isAlignMultilineExtendsList() { return getBoolean(WRAP_ALIGN_MULTILINE_EXTENDS_LIST, false); }
    public void setAlignMultilineExtendsList(boolean v) { setBoolean(WRAP_ALIGN_MULTILINE_EXTENDS_LIST, v); }

    public String getFunctionParametersWrap() { return getString(WRAP_FUNCTION_PARAMETERS, "Chop down if long"); }
    public void setFunctionParametersWrap(String v) { setString(WRAP_FUNCTION_PARAMETERS, v); }

    public boolean isAlignMultilineFunctionParameters() { return getBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARAMETERS, true); }
    public void setAlignMultilineFunctionParameters(boolean v) { setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARAMETERS, v); }

    public boolean isNewLineAfterOpenParenFunctionParameters() { return getBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_PARAMETERS, true); }
    public void setNewLineAfterOpenParenFunctionParameters(boolean v) { setBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_PARAMETERS, v); }

    public boolean isPlaceCloseParenOnNewLineFunctionParameters() { return getBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, true); }
    public void setPlaceCloseParenOnNewLineFunctionParameters(boolean v) { setBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, v); }

    public String getFunctionArgumentsWrap() { return getString(WRAP_FUNCTION_ARGUMENTS, "Chop down if long"); }
    public void setFunctionArgumentsWrap(String v) { setString(WRAP_FUNCTION_ARGUMENTS, v); }

    public boolean isAlignMultilineFunctionArguments() { return getBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_ARGUMENTS, false); }
    public void setAlignMultilineFunctionArguments(boolean v) { setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_ARGUMENTS, v); }

    public boolean isNewLineAfterOpenParenFunctionArguments() { return getBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_ARGUMENTS, true); }
    public void setNewLineAfterOpenParenFunctionArguments(boolean v) { setBoolean(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_ARGUMENTS, v); }

    public boolean isPlaceCloseParenOnNewLineFunctionArguments() { return getBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, true); }
    public void setPlaceCloseParenOnNewLineFunctionArguments(boolean v) { setBoolean(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, v); }

    public boolean isAlignMultilineFunctionParentheses() { return getBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARENTHESES, false); }
    public void setAlignMultilineFunctionParentheses(boolean v) { setBoolean(WRAP_ALIGN_MULTILINE_FUNCTION_PARENTHESES, v); }

    public String getChainedCallsWrap() { return getString(WRAP_CHAINED_CALLS, "Wrap if long"); }
    public void setChainedCallsWrap(String v) { setString(WRAP_CHAINED_CALLS, v); }

    public boolean isWrapFirstCallChainedCalls() { return getBoolean(WRAP_FIRST_CALL_CHAINED_CALLS, false); }
    public void setWrapFirstCallChainedCalls(boolean v) { setBoolean(WRAP_FIRST_CALL_CHAINED_CALLS, v); }

    public boolean isElseOnNewLine() { return getBoolean(WRAP_ELSE_ON_NEW_LINE, false); }
    public void setElseOnNewLine(boolean v) { setBoolean(WRAP_ELSE_ON_NEW_LINE, v); }

    public boolean isIfCloseParenOnNewLine() { return getBoolean(WRAP_IF_CLOSE_PAREN_ON_NEW_LINE, true); }
    public void setIfCloseParenOnNewLine(boolean v) { setBoolean(WRAP_IF_CLOSE_PAREN_ON_NEW_LINE, v); }

    public boolean isWhileOnNewLine() { return getBoolean(WRAP_WHILE_ON_NEW_LINE, false); }
    public void setWhileOnNewLine(boolean v) { setBoolean(WRAP_WHILE_ON_NEW_LINE, v); }

    public boolean isCatchOnNewLine() { return getBoolean(WRAP_CATCH_ON_NEW_LINE, false); }
    public void setCatchOnNewLine(boolean v) { setBoolean(WRAP_CATCH_ON_NEW_LINE, v); }

    public boolean isFinallyOnNewLine() { return getBoolean(WRAP_FINALLY_ON_NEW_LINE, false); }
    public void setFinallyOnNewLine(boolean v) { setBoolean(WRAP_FINALLY_ON_NEW_LINE, v); }

    public boolean isAlignMultilineBinaryExpressions() { return getBoolean(WRAP_ALIGN_MULTILINE_BINARY_EXPRESSIONS, false); }
    public void setAlignMultilineBinaryExpressions(boolean v) { setBoolean(WRAP_ALIGN_MULTILINE_BINARY_EXPRESSIONS, v); }

    public String getAssignmentWrap() { return getString(WRAP_ASSIGNMENT_STATEMENT, "Wrap if long"); }
    public void setAssignmentWrap(String v) { setString(WRAP_ASSIGNMENT_STATEMENT, v); }

    public String getEnumConstantsWrap() { return getString(WRAP_ENUM_CONSTANTS, "Do not wrap"); }
    public void setEnumConstantsWrap(String v) { setString(WRAP_ENUM_CONSTANTS, v); }

    public String getClassAnnotationsWrap() { return getString(WRAP_CLASS_ANNOTATIONS, "Wrap always"); }
    public void setClassAnnotationsWrap(String v) { setString(WRAP_CLASS_ANNOTATIONS, v); }

    public String getFunctionAnnotationsWrap() { return getString(WRAP_FUNCTION_ANNOTATIONS, "Wrap always"); }
    public void setFunctionAnnotationsWrap(String v) { setString(WRAP_FUNCTION_ANNOTATIONS, v); }

    public String getPropertyAnnotationsWrap() { return getString(WRAP_PROPERTY_ANNOTATIONS, "Wrap always"); }
    public void setPropertyAnnotationsWrap(String v) { setString(WRAP_PROPERTY_ANNOTATIONS, v); }

    public String getParameterAnnotationsWrap() { return getString(WRAP_PARAMETER_ANNOTATIONS, "Do not wrap"); }
    public void setParameterAnnotationsWrap(String v) { setString(WRAP_PARAMETER_ANNOTATIONS, v); }

    public String getLocalVariableAnnotationsWrap() { return getString(WRAP_LOCAL_VARIABLE_ANNOTATIONS, "Do not wrap"); }
    public void setLocalVariableAnnotationsWrap(String v) { setString(WRAP_LOCAL_VARIABLE_ANNOTATIONS, v); }

    public String getPropertyContextParametersWrap() { return getString(WRAP_PROPERTY_CONTEXT_PARAMETERS, "Wrap always"); }
    public void setPropertyContextParametersWrap(String v) { setString(WRAP_PROPERTY_CONTEXT_PARAMETERS, v); }

    public String getFunctionContextParametersWrap() { return getString(WRAP_FUNCTION_CONTEXT_PARAMETERS, "Wrap always"); }
    public void setFunctionContextParametersWrap(String v) { setString(WRAP_FUNCTION_CONTEXT_PARAMETERS, v); }

    public boolean isAlignWhenBranchesInColumns() { return getBoolean(WRAP_ALIGN_WHEN_BRANCHES_IN_COLUMNS, false); }
    public void setAlignWhenBranchesInColumns(boolean v) { setBoolean(WRAP_ALIGN_WHEN_BRANCHES_IN_COLUMNS, v); }

    public boolean isNewLineAfterMultilineWhenEntry() { return getBoolean(WRAP_NEW_LINE_AFTER_MULTILINE_WHEN_ENTRY, true); }
    public void setNewLineAfterMultilineWhenEntry(boolean v) { setBoolean(WRAP_NEW_LINE_AFTER_MULTILINE_WHEN_ENTRY, v); }

    public boolean isIndentBeforeArrowOnNewLine() { return getBoolean(WRAP_INDENT_BEFORE_ARROW_ON_NEW_LINE, true); }
    public void setIndentBeforeArrowOnNewLine(boolean v) { setBoolean(WRAP_INDENT_BEFORE_ARROW_ON_NEW_LINE, v); }

    public boolean isPutLeftBraceOnNewLine() { return getBoolean(WRAP_PUT_LEFT_BRACE_ON_NEW_LINE, false); }
    public void setPutLeftBraceOnNewLine(boolean v) { setBoolean(WRAP_PUT_LEFT_BRACE_ON_NEW_LINE, v); }

    public String getExpressionBodyFunctionsWrap() { return getString(WRAP_EXPRESSION_BODY_FUNCTIONS, "Wrap if long"); }
    public void setExpressionBodyFunctionsWrap(String v) { setString(WRAP_EXPRESSION_BODY_FUNCTIONS, v); }

    public String getElvisExpressionsWrap() { return getString(WRAP_ELVIS_EXPRESSIONS, "Wrap if long"); }
    public void setElvisExpressionsWrap(String v) { setString(WRAP_ELVIS_EXPRESSIONS, v); }

    // ==========================================
    // Strongly-Typed Getters & Setters - Blank Lines
    // ==========================================
    public int getKeepBlankLinesInDeclarations() { return getInt(BLANK_LINES_KEEP_IN_DECLARATIONS, 2); }
    public void setKeepBlankLinesInDeclarations(int v) { setInt(BLANK_LINES_KEEP_IN_DECLARATIONS, v); }

    public int getKeepBlankLinesInCode() { return getInt(BLANK_LINES_KEEP_IN_CODE, 2); }
    public void setKeepBlankLinesInCode(int v) { setInt(BLANK_LINES_KEEP_IN_CODE, v); }

    public int getKeepBlankLinesBeforeClosingBrace() { return getInt(BLANK_LINES_KEEP_BEFORE_CLOSING_BRACE, 2); }
    public void setKeepBlankLinesBeforeClosingBrace(int v) { setInt(BLANK_LINES_KEEP_BEFORE_CLOSING_BRACE, v); }

    public int getMinBlankLinesAfterClassHeader() { return getInt(BLANK_LINES_MIN_AFTER_CLASS_HEADER, 0); }
    public void setMinBlankLinesAfterClassHeader(int v) { setInt(BLANK_LINES_MIN_AFTER_CLASS_HEADER, v); }

    public int getMinBlankLinesAroundWhenBranchesWithBraces() { return getInt(BLANK_LINES_MIN_AROUND_WHEN_BRANCHES_WITH_BRACES, 0); }
    public void setMinBlankLinesAroundWhenBranchesWithBraces(int v) { setInt(BLANK_LINES_MIN_AROUND_WHEN_BRANCHES_WITH_BRACES, v); }

    public int getMinBlankLinesBeforeDeclarationWithCommentOrAnnotation() { return getInt(BLANK_LINES_MIN_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION, 1); }
    public void setMinBlankLinesBeforeDeclarationWithCommentOrAnnotation(int v) { setInt(BLANK_LINES_MIN_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION, v); }

    // ==========================================
    // Strongly-Typed Getters & Setters - Imports
    // ==========================================
    public String getTopLevelImportMode() { return getString(TOP_LEVEL_IMPORT_MODE, "WHEN_AT_LEAST"); }
    public void setTopLevelImportMode(String v) { setString(TOP_LEVEL_IMPORT_MODE, v); }

    public int getTopLevelImportThreshold() { return getInt(TOP_LEVEL_IMPORT_THRESHOLD, 5); }
    public void setTopLevelImportThreshold(int v) { setInt(TOP_LEVEL_IMPORT_THRESHOLD, v); }

    public String getJavaStaticsImportMode() { return getString(JAVA_STATICS_IMPORT_MODE, "WHEN_AT_LEAST"); }
    public void setJavaStaticsImportMode(String v) { setString(JAVA_STATICS_IMPORT_MODE, v); }

    public int getJavaStaticsImportThreshold() { return getInt(JAVA_STATICS_IMPORT_THRESHOLD, 3); }
    public void setJavaStaticsImportThreshold(int v) { setInt(JAVA_STATICS_IMPORT_THRESHOLD, v); }

    public boolean isInsertImportsForNestedClasses() { return getBoolean(INSERT_IMPORTS_FOR_NESTED_CLASSES, false); }
    public void setInsertImportsForNestedClasses(boolean v) { setBoolean(INSERT_IMPORTS_FOR_NESTED_CLASSES, v); }

    public boolean isImportAliasesSeparately() { return getBoolean(IMPORT_ALIASES_SEPARATELY, true); }
    public void setImportAliasesSeparately(boolean v) { setBoolean(IMPORT_ALIASES_SEPARATELY, v); }

    public List<ImportEntry> getPackagesToUseImportOnDemand() { return packagesToUseImportOnDemand; }
    public List<ImportEntry> getImportLayout() { return importLayout; }

    // ==========================================
    // Strongly-Typed Getters & Setters - Other (Trailing Comma)
    // ==========================================
    public boolean isTrailingCommaEnabled() { return getBoolean(TRAILING_COMMA_ENABLED, false); }
    public void setTrailingCommaEnabled(boolean v) { setBoolean(TRAILING_COMMA_ENABLED, v); }

    public boolean isTrailingCommaTypeParameterList() { return getBoolean(TRAILING_COMMA_TYPE_PARAMETER_LIST, true); }
    public void setTrailingCommaTypeParameterList(boolean v) { setBoolean(TRAILING_COMMA_TYPE_PARAMETER_LIST, v); }

    public boolean isTrailingCommaDestructuringDeclaration() { return getBoolean(TRAILING_COMMA_DESTRUCTURING_DECLARATION, false); }
    public void setTrailingCommaDestructuringDeclaration(boolean v) { setBoolean(TRAILING_COMMA_DESTRUCTURING_DECLARATION, v); }

    public boolean isTrailingCommaWhenEntry() { return getBoolean(TRAILING_COMMA_WHEN_ENTRY, true); }
    public void setTrailingCommaWhenEntry(boolean v) { setBoolean(TRAILING_COMMA_WHEN_ENTRY, v); }

    public boolean isTrailingCommaFunctionLiteral() { return getBoolean(TRAILING_COMMA_FUNCTION_LITERAL, true); }
    public void setTrailingCommaFunctionLiteral(boolean v) { setBoolean(TRAILING_COMMA_FUNCTION_LITERAL, v); }

    public boolean isTrailingCommaValueParameterList() { return getBoolean(TRAILING_COMMA_VALUE_PARAMETER_LIST, true); }
    public void setTrailingCommaValueParameterList(boolean v) { setBoolean(TRAILING_COMMA_VALUE_PARAMETER_LIST, v); }

    public boolean isTrailingCommaContextReceiverList() { return getBoolean(TRAILING_COMMA_CONTEXT_RECEIVER_LIST, true); }
    public void setTrailingCommaContextReceiverList(boolean v) { setBoolean(TRAILING_COMMA_CONTEXT_RECEIVER_LIST, v); }

    public boolean isTrailingCommaCollectionLiteralExpression() { return getBoolean(TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION, false); }
    public void setTrailingCommaCollectionLiteralExpression(boolean v) { setBoolean(TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION, v); }

    public boolean isTrailingCommaTypeArgumentList() { return getBoolean(TRAILING_COMMA_TYPE_ARGUMENT_LIST, false); }
    public void setTrailingCommaTypeArgumentList(boolean v) { setBoolean(TRAILING_COMMA_TYPE_ARGUMENT_LIST, v); }

    public boolean isTrailingCommaIndices() { return getBoolean(TRAILING_COMMA_INDICES, false); }
    public void setTrailingCommaIndices(boolean v) { setBoolean(TRAILING_COMMA_INDICES, v); }

    public boolean isTrailingCommaValueArgumentList() { return getBoolean(TRAILING_COMMA_VALUE_ARGUMENT_LIST, false); }
    public void setTrailingCommaValueArgumentList(boolean v) { setBoolean(TRAILING_COMMA_VALUE_ARGUMENT_LIST, v); }

    // ==========================================
    // Strongly-Typed Getters & Setters - Code Generation
    // ==========================================
    public boolean isLineCommentAtFirstColumn() { return getBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, true); }
    public void setLineCommentAtFirstColumn(boolean v) { setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, v); }

    public boolean isAddSpaceAtLineCommentStart() { return getBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, false); }
    public void setAddSpaceAtLineCommentStart(boolean v) { setBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, v); }

    public boolean isEnforceOnReformat() { return getBoolean(CODE_GEN_ENFORCE_ON_REFORMAT, false); }
    public void setEnforceOnReformat(boolean v) { setBoolean(CODE_GEN_ENFORCE_ON_REFORMAT, v); }

    public boolean isBlockCommentAtFirstColumn() { return getBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true); }
    public void setBlockCommentAtFirstColumn(boolean v) { setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, v); }

    public boolean isAddSpacesAroundBlockComments() { return getBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false); }
    public void setAddSpacesAroundBlockComments(boolean v) { setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, v); }

    // ==========================================
    // Conventions Presets
    // ==========================================
    public void applyKotlinCodingConventions() {
        setContinuationIndent(4);
        setString(LOAD_SAVE_USE_DEFAULTS_FROM, "Kotlin Coding Conventions");
    }

    public void applyIdeDefaults() {
        setContinuationIndent(8);
        setString(LOAD_SAVE_USE_DEFAULTS_FROM, "<ide defaults>");
    }

    // ==========================================
    // Sample Codes matching Screenshots 1-5 verbatim
    // ==========================================
    public static final String SAMPLE_TABS_AND_SPACES = """
open class Some {
    private val f: (Int) -> Int = { a: Int -> a * 2 }
    fun foo(): Int {
        val bar: Int? = 5
        val test: Int = bar ?: 12
        for (i in 10..<42) {
            println(
                when {
                    i < test -> -1
                    i > test -> 1
                    else -> 0
                }
            )
        }
        if (true) {
        }
        while (true) {
            break
        }
        try {
            when (test) {
                12 -> println("foo")
                in 10..42 -> println("baz")
                else -> println("bar")
            }
        } catch (e: Exception) {
        } finally {
        }
        return test
    }

    private fun <T> foo2(): Int where T : List<T> {
        return 0
    }

    fun multilineMethod(
        foo: String,
        bar: String?,
        x: Int?
    ) {
    }
}
""";

    public static final String SAMPLE_WRAPPING_AND_BRACES = """
@Deprecated("Foo")
public class ThisIsASampleClass :
    Comparable<*>,
    Appendable {
    val test =
        12

    @Deprecated("Foo")
    context(ctx: String)
    fun foo1(
        i1: Int,
        i2: Int,
        i3: Int,
        a: Any
    ): Int {
        when (i1) {
            is Number -> 0
            else -> 1
        }
        when (a) {
            is Int,
            is String
            -> 0

            else -> 1
        }
        if (i2 > 0 &&
            i3 < 0
        ) {
            return 2
        }
        return 0
    }

    private fun foo2(): Int {
// todo: something
        return 1
    }

    fun foo3(
        @Named("param1") param1: Int,
        param2: String
    ) {
        @Deprecated val foo =
            1
    }

    fun multilineMethod(
        foo: String,
        bar: String?,
        x: Int?
    ) {
        foo.toUpperCase()
            .trim()
            .length
        val barLen =
            bar?.length() ?: x
            ?: -1
        if (foo.length > 0 &&
            barLen > 0
        ) {
            println("> 0")
        }
    }

@Deprecated
val bar = 1

enum class Enumeration {
    A, B
}

fun veryLongExpressionBodyMethod() =
    "abc"
}
""";

    public static final String SAMPLE_BLANK_LINES = """
class Foo {
    private var field1: Int = 1
    private val field2: String? = null


    init {
        field1 = 2;
    }

    fun foo1() {
        run {


            field1
        }


        when (field1) {
            1 -> println("1")
            2 -> println("2")
            3 ->
                println(
                    "3" +
                        "4"
                )
        }


        when (field2) {
            1 -> {
                println("1")
            }

            2 -> {
                println("2")
            }
        }
    }
}
""";

    public static final String SAMPLE_IMPORTS = """
package dev.lumina.demo

import java.util.List
import java.util.ArrayList
import java.util.Map

class ImportDemo {
    val items: List<String> = ArrayList()
}
""";

    // ==========================================
    // Provider Factory
    // ==========================================
    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "Kotlin";
            }

            @Override
            public String getDisplayName() {
                return "Kotlin";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of(
                        "Tabs and Indents",
                        "Spaces",
                        "Wrapping and Braces",
                        "Blank Lines",
                        "Imports",
                        "Other",
                        "Code Generation",
                        "Load/Save"
                );
            }

            @Override
            public boolean hasPreview(String tabName) {
                // Tabs 1-4 have live preview; Tabs 5-8 are full-width option panes matching reference screenshots
                return "Tabs and Indents".equals(tabName)
                        || "Spaces".equals(tabName)
                        || "Wrapping and Braces".equals(tabName)
                        || "Blank Lines".equals(tabName);
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new KotlinCodeStyleSettings("Kotlin");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_SPACES;
            }

            @Override
            public String getSampleCode(String tabName) {
                if (tabName == null) return SAMPLE_TABS_AND_SPACES;
                return switch (tabName) {
                    case "Wrapping and Braces" -> SAMPLE_WRAPPING_AND_BRACES;
                    case "Blank Lines" -> SAMPLE_BLANK_LINES;
                    case "Imports" -> SAMPLE_IMPORTS;
                    default -> SAMPLE_TABS_AND_SPACES;
                };
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup g = CodeStyleGroup.flat("Tabs and Indents");
                    g.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    g.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    g.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    g.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    g.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
                    g.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(g);
                } else if ("Spaces".equals(tabName)) {
                    CodeStyleGroup beforeParen = CodeStyleGroup.collapsible("Before parentheses");
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_IF_PARENTHESES, "'if' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_PARENTHESES, "'for' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_PARENTHESES, "'while' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_PARENTHESES, "'catch' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHEN_PARENTHESES, "'when' parentheses", true));
                    customizer.addGroup(beforeParen);

                    CodeStyleGroup aroundOps = CodeStyleGroup.collapsible("Around operators");
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ASSIGNMENT_OPERATORS, "Assignment operators (=, +=, ...)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_LOGICAL_OPERATORS, "Logical operators (&&, ||)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_EQUALITY_OPERATORS, "Equality operators (==, !=)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_RELATIONAL_OPERATORS, "Relational operators (<, >, <=, >=)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ADDITIVE_OPERATORS, "Additive operators (+, -)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, "Multiplicative operators (*, /, %)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_UNARY_OPERATORS, "Unary operators (!, -, +, ++, --)", false));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_RANGE_OPERATORS, "Range operators (..., ..<)", false));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ELVIS_OPERATOR, "Elvis operator (?:)", true));
                    customizer.addGroup(aroundOps);

                    CodeStyleGroup other = CodeStyleGroup.collapsible("Other");
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COMMA, "Before comma", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COMMA, "After comma", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COLON_AFTER_DECLARATION_NAME, "Before colon, after declaration name", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COLON_BEFORE_DECLARATION_TYPE, "After colon, before declaration type", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COLON_IN_NEW_TYPE_DEFINITION, "Before colon in new type definition", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COLON_IN_NEW_TYPE_DEFINITION, "After colon in new type definition", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_IN_SIMPLE_ONE_LINE_METHODS, "In simple one line methods", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ARROW_IN_FUNCTION_TYPES, "Around arrow in function types", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ARROW_IN_WHEN_CLAUSE, "Around arrow in \"when\" clause", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_LAMBDA_ARROW, "Before lambda arrow", true));
                    customizer.addGroup(other);
                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.number(HARD_WRAP_AT, "Hard wrap at:", 120));
                    general.addOption(CodeStyleOption.combo(WRAP_ON_TYPING, "Wrap on typing", List.of("Default: No", "Yes", "No"), "Default: No"));
                    general.addOption(CodeStyleOption.combo(VISUAL_GUIDES, "Visual guides", List.of("Default: None", "None", "80", "120"), "Default: None"));
                    customizer.addGroup(general);

                    CodeStyleGroup keep = CodeStyleGroup.collapsible("Keep when reformatting");
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_LINE_BREAKS, "Line breaks", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, "Comment at first column", false));
                    customizer.addGroup(keep);

                    CodeStyleGroup extendsList = CodeStyleGroup.collapsibleWithCombo(
                            "Extends/implements/permits list", WRAP_EXTENDS_LIST, WRAP_OPTIONS, "Wrap if long");
                    extendsList.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_MULTILINE_EXTENDS_LIST, "Align when multiline", false));
                    extendsList.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_EXTENDS_LIST, "Use continuation indent", false));
                    customizer.addGroup(extendsList);

                    CodeStyleGroup fnParams = CodeStyleGroup.collapsibleWithCombo(
                            "Function declaration parameters", WRAP_FUNCTION_PARAMETERS, WRAP_OPTIONS, "Chop down if long");
                    fnParams.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_MULTILINE_FUNCTION_PARAMETERS, "Align when multiline", true));
                    fnParams.addOption(CodeStyleOption.checkbox(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_PARAMETERS, "New line after '('", true));
                    fnParams.addOption(CodeStyleOption.checkbox(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, "Place ')' on new line", true));
                    fnParams.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_FUNCTION_PARAMETERS, "Use continuation indent", false));
                    customizer.addGroup(fnParams);

                    CodeStyleGroup fnArgs = CodeStyleGroup.collapsibleWithCombo(
                            "Function call arguments", WRAP_FUNCTION_ARGUMENTS, WRAP_OPTIONS, "Chop down if long");
                    fnArgs.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_MULTILINE_FUNCTION_ARGUMENTS, "Align when multiline", false));
                    fnArgs.addOption(CodeStyleOption.checkbox(WRAP_NEW_LINE_AFTER_OPEN_PAREN_FUNCTION_ARGUMENTS, "New line after '('", true));
                    fnArgs.addOption(CodeStyleOption.checkbox(WRAP_PLACE_CLOSE_PAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, "Place ')' on new line", true));
                    fnArgs.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_FUNCTION_ARGUMENTS, "Use continuation indent", false));
                    customizer.addGroup(fnArgs);

                    CodeStyleGroup fnParens = CodeStyleGroup.collapsible("Function parentheses");
                    fnParens.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_MULTILINE_FUNCTION_PARENTHESES, "Align when multiline", false));
                    customizer.addGroup(fnParens);

                    CodeStyleGroup chainedCalls = CodeStyleGroup.collapsibleWithCombo(
                            "Chained function calls", WRAP_CHAINED_CALLS, WRAP_OPTIONS, "Wrap if long");
                    chainedCalls.addOption(CodeStyleOption.checkbox(WRAP_FIRST_CALL_CHAINED_CALLS, "Wrap first call", false));
                    chainedCalls.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_CHAINED_CALLS, "Use continuation indent", false));
                    customizer.addGroup(chainedCalls);

                    CodeStyleGroup ifStmt = CodeStyleGroup.collapsible("'if()' statement");
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_ELSE_ON_NEW_LINE, "'else' on new line", false));
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_IF_CLOSE_PAREN_ON_NEW_LINE, "Place ')' on new line", true));
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_USE_CONTINUATION_INDENT_IN_CONDITIONS, "Use continuation indent in conditions", false));
                    customizer.addGroup(ifStmt);

                    CodeStyleGroup doWhileStmt = CodeStyleGroup.collapsible("'do ... while()' statement");
                    doWhileStmt.addOption(CodeStyleOption.checkbox(WRAP_WHILE_ON_NEW_LINE, "'while' on new line", false));
                    customizer.addGroup(doWhileStmt);

                    CodeStyleGroup tryStmt = CodeStyleGroup.collapsible("'try' statement");
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_CATCH_ON_NEW_LINE, "'catch' on new line", false));
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_FINALLY_ON_NEW_LINE, "'finally' on new line", false));
                    customizer.addGroup(tryStmt);

                    CodeStyleGroup binExpr = CodeStyleGroup.collapsible("Binary expressions");
                    binExpr.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_MULTILINE_BINARY_EXPRESSIONS, "Align when multiline", false));
                    customizer.addGroup(binExpr);

                    CodeStyleGroup standaloneCombos = CodeStyleGroup.flat("Expressions and Annotations");
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_ASSIGNMENT_STATEMENT, "Assignment statement", WRAP_OPTIONS, "Wrap if long"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_ENUM_CONSTANTS, "Enum constants", WRAP_OPTIONS, "Do not wrap"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_CLASS_ANNOTATIONS, "Class annotations", WRAP_OPTIONS, "Wrap always"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_FUNCTION_ANNOTATIONS, "Function annotations", WRAP_OPTIONS, "Wrap always"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_PROPERTY_ANNOTATIONS, "Property annotations", WRAP_OPTIONS, "Wrap always"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_PARAMETER_ANNOTATIONS, "Parameter annotations", WRAP_OPTIONS, "Do not wrap"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_LOCAL_VARIABLE_ANNOTATIONS, "Local variable annotations", WRAP_OPTIONS, "Do not wrap"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_PROPERTY_CONTEXT_PARAMETERS, "Property context parameters", WRAP_OPTIONS, "Wrap always"));
                    standaloneCombos.addOption(CodeStyleOption.combo(WRAP_FUNCTION_CONTEXT_PARAMETERS, "Function context parameters", WRAP_OPTIONS, "Wrap always"));
                    customizer.addGroup(standaloneCombos);

                    CodeStyleGroup whenStmt = CodeStyleGroup.collapsible("'when' statements");
                    whenStmt.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_WHEN_BRANCHES_IN_COLUMNS, "Align 'when' branches in columns", false));
                    whenStmt.addOption(CodeStyleOption.checkbox(WRAP_NEW_LINE_AFTER_MULTILINE_WHEN_ENTRY, "New line after multiline entry", true));
                    whenStmt.addOption(CodeStyleOption.checkbox(WRAP_INDENT_BEFORE_ARROW_ON_NEW_LINE, "Indent before '->' on new line", true));
                    customizer.addGroup(whenStmt);

                    CodeStyleGroup braces = CodeStyleGroup.collapsible("Braces placement");
                    braces.addOption(CodeStyleOption.checkbox(WRAP_PUT_LEFT_BRACE_ON_NEW_LINE, "Put left brace on new line", false));
                    customizer.addGroup(braces);

                    CodeStyleGroup exprBody = CodeStyleGroup.collapsibleWithCombo(
                            "Expression body functions", WRAP_EXPRESSION_BODY_FUNCTIONS, WRAP_OPTIONS, "Wrap if long");
                    exprBody.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_EXPRESSION_BODY, "Use continuation indent", false));
                    customizer.addGroup(exprBody);

                    CodeStyleGroup elvisExpr = CodeStyleGroup.collapsibleWithCombo(
                            "Elvis expressions", WRAP_ELVIS_EXPRESSIONS, WRAP_OPTIONS, "Wrap if long");
                    elvisExpr.addOption(CodeStyleOption.checkbox(WRAP_CONTINUATION_INDENT_ELVIS_EXPRESSIONS, "Use continuation indent", false));
                    customizer.addGroup(elvisExpr);

                } else if ("Blank Lines".equals(tabName)) {
                    CodeStyleGroup maxGroup = CodeStyleGroup.divider("Keep maximum blank lines");
                    maxGroup.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_DECLARATIONS, "In declarations:", 2));
                    maxGroup.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_CODE, "In code:", 2));
                    maxGroup.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_BEFORE_CLOSING_BRACE, "Before '}':", 2));
                    customizer.addGroup(maxGroup);

                    CodeStyleGroup minGroup = CodeStyleGroup.divider("Minimum blank lines");
                    minGroup.addOption(CodeStyleOption.number(BLANK_LINES_MIN_AFTER_CLASS_HEADER, "After class header:", 0));
                    minGroup.addOption(CodeStyleOption.number(BLANK_LINES_MIN_AROUND_WHEN_BRANCHES_WITH_BRACES, "Around 'when' branches with {}:", 0));
                    minGroup.addOption(CodeStyleOption.number(BLANK_LINES_MIN_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION, "Before declaration with comment or annotation:", 1));
                    customizer.addGroup(minGroup);

                } else if ("Other".equals(tabName)) {
                    CodeStyleGroup trailingComma = CodeStyleGroup.divider("Trailing Comma");
                    trailingComma.addOption(CodeStyleOption.checkbox(TRAILING_COMMA_ENABLED, "Use trailing comma", false));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_TYPE_PARAMETER_LIST, "Type parameter list", true));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_DESTRUCTURING_DECLARATION, "Destructuring declaration", false));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_WHEN_ENTRY, "When entry", true));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_FUNCTION_LITERAL, "Function literal", true));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_VALUE_PARAMETER_LIST, "Value parameter list", true));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_CONTEXT_RECEIVER_LIST, "Context receiver list", true));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION, "Collection literal expression", false));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_TYPE_ARGUMENT_LIST, "Type argument list", false));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_INDICES, "Indices", false));
                    trailingComma.addOption(CodeStyleOption.indentedCheckbox(TRAILING_COMMA_VALUE_ARGUMENT_LIST, "Value argument list", false));
                    customizer.addGroup(trailingComma);

                } else if ("Code Generation".equals(tabName)) {
                    CodeStyleGroup commentCode = CodeStyleGroup.divider("Comment Code");
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, "Line comment at first column", true));
                    commentCode.addOption(CodeStyleOption.indentedCheckbox(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, "Add a space at line comment start", false));
                    commentCode.addOption(CodeStyleOption.doubleIndentedCheckbox(CODE_GEN_ENFORCE_ON_REFORMAT, "Enforce on reformat", false));
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, "Block comment at first column", true));
                    commentCode.addOption(CodeStyleOption.indentedCheckbox(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, "Add spaces around block comments", false));
                    customizer.addGroup(commentCode);

                } else if ("Load/Save".equals(tabName)) {
                    CodeStyleGroup loadSave = CodeStyleGroup.flat("Load/Save Defaults");
                    loadSave.addOption(CodeStyleOption.combo(
                            LOAD_SAVE_USE_DEFAULTS_FROM,
                            "Use defaults from:",
                            List.of("<ide defaults>", "Kotlin Coding Conventions", "Kotlin obsolete codestyle"),
                            "<ide defaults>"
                    ));
                    customizer.addGroup(loadSave);
                }
            }
        };
    }
}
