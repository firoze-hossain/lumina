package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dedicated code style settings and dynamic provider for Groovy.
 * Strictly decoupled and brand-isolated.
 * Supports the 7 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790578371136.png)
 * 2. Spaces (media_1790578385278.png, media_1790578412189.png, media_1790578441103.png)
 * 3. Wrapping and Braces
 * 4. Blank Lines
 * 5. GroovyDoc
 * 6. Imports
 * 7. Code Generation
 */
public class GroovyCodeStyleSettings extends LanguageCodeStyleSettings {

    // ==========================================
    // Property Keys - Spaces
    // ==========================================
    // 1. Before parentheses
    public static final String SPACE_BEFORE_METHOD_DECLARATION_PARENTHESES = "groovy_space_before_method_decl_paren";
    public static final String SPACE_BEFORE_METHOD_CALL_PARENTHESES = "groovy_space_before_method_call_paren";
    public static final String SPACE_BEFORE_EMPTY_METHOD_CALL_PARENTHESES = "groovy_space_before_empty_method_call_paren";
    public static final String SPACE_BEFORE_IF_PARENTHESES = "groovy_space_before_if_paren";
    public static final String SPACE_BEFORE_FOR_PARENTHESES = "groovy_space_before_for_paren";
    public static final String SPACE_BEFORE_WHILE_PARENTHESES = "groovy_space_before_while_paren";
    public static final String SPACE_BEFORE_SWITCH_PARENTHESES = "groovy_space_before_switch_paren";
    public static final String SPACE_BEFORE_TRY_PARENTHESES = "groovy_space_before_try_paren";
    public static final String SPACE_BEFORE_CATCH_PARENTHESES = "groovy_space_before_catch_paren";
    public static final String SPACE_BEFORE_SYNCHRONIZED_PARENTHESES = "groovy_space_before_sync_paren";
    public static final String SPACE_BEFORE_TYPE_CAST_PARENTHESES = "groovy_space_before_type_cast_paren";
    public static final String SPACE_BEFORE_ANNOTATION_PARENTHESES = "groovy_space_before_ann_paren";
    public static final String SPACE_BEFORE_LIST_AND_MAPS_LITERALS = "groovy_space_before_list_map_literals";
    public static final String SPACE_BEFORE_GSTRING_INJECTION_BRACES = "groovy_space_before_gstring_braces";
    public static final String SPACE_BEFORE_TUPLE_ASSIGNMENT = "groovy_space_before_tuple_assignment";
    public static final String SPACE_BEFORE_RECORD_PARAMETER_LIST = "groovy_space_before_record_param_list";

    // 2. Around operators
    public static final String SPACE_AROUND_ASSIGNMENT_OPERATORS = "groovy_space_around_assignment_ops";
    public static final String SPACE_AROUND_LOGICAL_OPERATORS = "groovy_space_around_logical_ops";
    public static final String SPACE_AROUND_EQUALITY_OPERATORS = "groovy_space_around_equality_ops";
    public static final String SPACE_AROUND_RELATIONAL_OPERATORS = "groovy_space_around_relational_ops";
    public static final String SPACE_AROUND_BITWISE_OPERATORS = "groovy_space_around_bitwise_ops";
    public static final String SPACE_AROUND_ADDITIVE_OPERATORS = "groovy_space_around_additive_ops";
    public static final String SPACE_AROUND_MULTIPLICATIVE_OPERATORS = "groovy_space_around_multiplicative_ops";
    public static final String SPACE_AROUND_SHIFT_OPERATORS = "groovy_space_around_shift_ops";
    public static final String SPACE_AROUND_LAMBDA_ARROW = "groovy_space_around_lambda_arrow";
    public static final String SPACE_AROUND_REGEXP_OPERATORS = "groovy_space_around_regexp_ops";

    // 3. Before left brace
    public static final String SPACE_BEFORE_CLASS_LBRACE = "groovy_space_before_class_lbrace";
    public static final String SPACE_BEFORE_METHOD_LBRACE = "groovy_space_before_method_lbrace";
    public static final String SPACE_BEFORE_IF_LBRACE = "groovy_space_before_if_lbrace";
    public static final String SPACE_BEFORE_ELSE_LBRACE = "groovy_space_before_else_lbrace";
    public static final String SPACE_BEFORE_FOR_LBRACE = "groovy_space_before_for_lbrace";
    public static final String SPACE_BEFORE_WHILE_LBRACE = "groovy_space_before_while_lbrace";
    public static final String SPACE_BEFORE_DO_LBRACE = "groovy_space_before_do_lbrace";
    public static final String SPACE_BEFORE_SWITCH_LBRACE = "groovy_space_before_switch_lbrace";
    public static final String SPACE_BEFORE_TRY_LBRACE = "groovy_space_before_try_lbrace";
    public static final String SPACE_BEFORE_CATCH_LBRACE = "groovy_space_before_catch_lbrace";
    public static final String SPACE_BEFORE_FINALLY_LBRACE = "groovy_space_before_finally_lbrace";
    public static final String SPACE_BEFORE_SYNCHRONIZED_LBRACE = "groovy_space_before_sync_lbrace";
    public static final String SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE = "groovy_space_before_array_init_lbrace";
    public static final String SPACE_BEFORE_CLOSURE_LBRACE_IN_CALLS = "groovy_space_before_closure_lbrace_calls";

    // 4. Before keywords
    public static final String SPACE_BEFORE_ELSE_KEYWORD = "groovy_space_before_else_kw";
    public static final String SPACE_BEFORE_WHILE_KEYWORD = "groovy_space_before_while_kw";
    public static final String SPACE_BEFORE_CATCH_KEYWORD = "groovy_space_before_catch_kw";
    public static final String SPACE_BEFORE_FINALLY_KEYWORD = "groovy_space_before_finally_kw";

    // 5. Within
    public static final String SPACE_WITHIN_CODE_BRACES = "groovy_space_within_code_braces";
    public static final String SPACE_WITHIN_BRACKETS = "groovy_space_within_brackets";
    public static final String SPACE_WITHIN_ARRAY_INITIALIZER_BRACES = "groovy_space_within_array_init_braces";
    public static final String SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES = "groovy_space_within_empty_array_init_braces";
    public static final String SPACE_WITHIN_GROUPING_PARENTHESES = "groovy_space_within_grouping_parens";
    public static final String SPACE_WITHIN_METHOD_DECLARATION_PARENTHESES = "groovy_space_within_method_decl_parens";
    public static final String SPACE_WITHIN_METHOD_CALL_PARENTHESES = "groovy_space_within_method_call_parens";
    public static final String SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES = "groovy_space_within_empty_method_call_parens";
    public static final String SPACE_WITHIN_IF_PARENTHESES = "groovy_space_within_if_parens";
    public static final String SPACE_WITHIN_FOR_PARENTHESES = "groovy_space_within_for_parens";
    public static final String SPACE_WITHIN_WHILE_PARENTHESES = "groovy_space_within_while_parens";
    public static final String SPACE_WITHIN_SWITCH_PARENTHESES = "groovy_space_within_switch_parens";
    public static final String SPACE_WITHIN_TRY_PARENTHESES = "groovy_space_within_try_parens";
    public static final String SPACE_WITHIN_CATCH_PARENTHESES = "groovy_space_within_catch_parens";
    public static final String SPACE_WITHIN_SYNCHRONIZED_PARENTHESES = "groovy_space_within_sync_parens";
    public static final String SPACE_WITHIN_TYPE_CAST_PARENTHESES = "groovy_space_within_type_cast_parens";
    public static final String SPACE_WITHIN_ANNOTATION_PARENTHESES = "groovy_space_within_ann_parens";

    // 6. In ternary operator (?:)
    public static final String SPACE_BEFORE_TERNARY_QUESTION = "groovy_space_before_ternary_question";
    public static final String SPACE_AFTER_TERNARY_QUESTION = "groovy_space_after_ternary_question";
    public static final String SPACE_BEFORE_TERNARY_COLON = "groovy_space_before_ternary_colon";
    public static final String SPACE_AFTER_TERNARY_COLON = "groovy_space_after_ternary_colon";

    // 7. Within type arguments
    public static final String SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = "groovy_space_after_comma_type_args";

    // 8. Other
    public static final String SPACE_BEFORE_COMMA = "groovy_space_before_comma";
    public static final String SPACE_AFTER_COMMA = "groovy_space_after_comma";
    public static final String SPACE_BEFORE_FOR_SEMICOLON = "groovy_space_before_for_semicolon";
    public static final String SPACE_AFTER_FOR_SEMICOLON = "groovy_space_after_for_semicolon";
    public static final String SPACE_AFTER_TYPE_CAST = "groovy_space_after_type_cast";
    public static final String SPACE_IN_NAMED_ARGUMENT_BEFORE_COLON = "groovy_space_in_named_arg_before_colon";
    public static final String SPACE_IN_NAMED_ARGUMENT_AFTER_COLON = "groovy_space_in_named_arg_after_colon";
    public static final String SPACE_BEFORE_ASSERT_SEPARATOR = "groovy_space_before_assert_separator";
    public static final String SPACE_AFTER_ASSERT_SEPARATOR = "groovy_space_after_assert_separator";

    // Label Indent Style
    public static final String LABEL_INDENT_STYLE = "groovy_label_indent_style";

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String HARD_WRAP_AT = "groovy_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "groovy_wrap_on_typing";
    public static final String VISUAL_GUIDES = "groovy_visual_guides";

    // Keep when reformatting
    public static final String WRAP_KEEP_LINE_BREAKS = "groovy_wrap_keep_line_breaks";
    public static final String WRAP_KEEP_COMMENT_AT_FIRST_COLUMN = "groovy_wrap_keep_comment_first_col";
    public static final String WRAP_KEEP_CONTROL_STATEMENT_IN_ONE_LINE = "groovy_wrap_keep_control_stmt_one_line";
    public static final String WRAP_KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = "groovy_wrap_keep_multiple_expr_one_line";
    public static final String WRAP_KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = "groovy_wrap_keep_simple_blocks_one_line";
    public static final String WRAP_KEEP_SIMPLE_METHODS_IN_ONE_LINE = "groovy_wrap_keep_simple_methods_one_line";
    public static final String WRAP_KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE = "groovy_wrap_keep_simple_lambdas_one_line";
    public static final String WRAP_KEEP_SIMPLE_CLASSES_IN_ONE_LINE = "groovy_wrap_keep_simple_classes_one_line";

    public static final String WRAP_ENSURE_RIGHT_MARGIN_NOT_EXCEEDED = "groovy_wrap_ensure_right_margin";

    // Braces placement
    public static final String BRACE_PLACEMENT_CLASS = "groovy_brace_class";
    public static final String BRACE_PLACEMENT_METHOD = "groovy_brace_method";
    public static final String BRACE_PLACEMENT_LAMBDA = "groovy_brace_lambda";
    public static final String BRACE_PLACEMENT_OTHER = "groovy_brace_other";
    public static final String BRACE_USE_FLYING_GEESE = "groovy_brace_flying_geese";

    // Extends/implements/permits list
    public static final String WRAP_EXTENDS_LIST = "groovy_wrap_extends_list";
    public static final String WRAP_ALIGN_EXTENDS_LIST = "groovy_wrap_align_extends_list";
    public static final String WRAP_EXTENDS_KEYWORD = "groovy_wrap_extends_keyword";

    // Throws list
    public static final String WRAP_THROWS_LIST = "groovy_wrap_throws_list";
    public static final String WRAP_ALIGN_THROWS_LIST = "groovy_wrap_align_throws_list";
    public static final String WRAP_ALIGN_THROWS_TO_METHOD_START = "groovy_wrap_align_throws_method_start";
    public static final String WRAP_THROWS_KEYWORD = "groovy_wrap_throws_keyword";

    // Method declaration parameters
    public static final String WRAP_METHOD_PARAMETERS = "groovy_wrap_method_params";
    public static final String WRAP_ALIGN_METHOD_PARAMETERS = "groovy_wrap_align_method_params";
    public static final String WRAP_NEW_LINE_AFTER_LPAREN_METHOD_PARAMETERS = "groovy_wrap_newline_lparen_method_params";
    public static final String WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_PARAMETERS = "groovy_wrap_place_rparen_newline_method_params";

    // Method call arguments
    public static final String WRAP_METHOD_ARGUMENTS = "groovy_wrap_method_args";
    public static final String WRAP_ALIGN_METHOD_ARGUMENTS = "groovy_wrap_align_method_args";
    public static final String WRAP_METHOD_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN = "groovy_wrap_method_args_priority_call_chain";
    public static final String WRAP_NEW_LINE_AFTER_LPAREN_METHOD_ARGUMENTS = "groovy_wrap_newline_lparen_method_args";
    public static final String WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_ARGUMENTS = "groovy_wrap_place_rparen_newline_method_args";

    // Method parentheses
    public static final String WRAP_ALIGN_METHOD_PARENTHESES = "groovy_wrap_align_method_parens";

    // Chained method calls
    public static final String WRAP_CHAINED_CALLS = "groovy_wrap_chained_calls";
    public static final String WRAP_ALIGN_CHAINED_CALLS = "groovy_wrap_align_chained_calls";
    public static final String WRAP_CHAINED_CALLS_WRAP_AFTER_DOT = "groovy_wrap_chained_calls_after_dot";

    // 'if()' statement
    public static final String WRAP_IF_FORCE_BRACES = "groovy_wrap_if_force_braces";
    public static final String WRAP_IF_ELSE_ON_NEW_LINE = "groovy_wrap_if_else_newline";
    public static final String WRAP_IF_SPECIAL_ELSE_IF = "groovy_wrap_if_special_else_if";

    // 'for()' statement
    public static final String WRAP_FOR_STATEMENT = "groovy_wrap_for_stmt";
    public static final String WRAP_ALIGN_FOR_STATEMENT = "groovy_wrap_align_for_stmt";
    public static final String WRAP_NEW_LINE_AFTER_LPAREN_FOR = "groovy_wrap_newline_lparen_for";
    public static final String WRAP_PLACE_RPAREN_ON_NEW_LINE_FOR = "groovy_wrap_place_rparen_newline_for";
    public static final String WRAP_FOR_FORCE_BRACES = "groovy_wrap_for_force_braces";

    // 'while()' statement
    public static final String WRAP_WHILE_FORCE_BRACES = "groovy_wrap_while_force_braces";

    // 'do ... while()' statement
    public static final String WRAP_DO_WHILE_FORCE_BRACES = "groovy_wrap_do_while_force_braces";
    public static final String WRAP_DO_WHILE_ON_NEW_LINE = "groovy_wrap_do_while_newline";

    // 'switch' statement
    public static final String WRAP_SWITCH_INDENT_CASE_BRANCHES = "groovy_wrap_switch_indent_case";

    // 'try-with-resources'
    public static final String WRAP_TRY_WITH_RESOURCES = "groovy_wrap_try_resources";
    public static final String WRAP_ALIGN_TRY_WITH_RESOURCES = "groovy_wrap_align_try_resources";
    public static final String WRAP_NEW_LINE_AFTER_LPAREN_TRY_WITH_RESOURCES = "groovy_wrap_newline_lparen_try_resources";
    public static final String WRAP_PLACE_RPAREN_ON_NEW_LINE_TRY_WITH_RESOURCES = "groovy_wrap_place_rparen_newline_try_resources";

    // 'try' statement
    public static final String WRAP_TRY_CATCH_ON_NEW_LINE = "groovy_wrap_try_catch_newline";
    public static final String WRAP_TRY_FINALLY_ON_NEW_LINE = "groovy_wrap_try_finally_newline";

    // Binary expressions
    public static final String WRAP_BINARY_EXPRESSIONS = "groovy_wrap_binary_expressions";
    public static final String WRAP_ALIGN_BINARY_EXPRESSIONS = "groovy_wrap_align_binary_expressions";
    public static final String WRAP_NEW_LINE_AFTER_LPAREN_BINARY_EXPRESSIONS = "groovy_wrap_newline_lparen_binary_expr";
    public static final String WRAP_PLACE_RPAREN_ON_NEW_LINE_BINARY_EXPRESSIONS = "groovy_wrap_place_rparen_newline_binary_expr";

    // Assignment statement
    public static final String WRAP_ASSIGNMENT_STATEMENT = "groovy_wrap_assignment_stmt";
    public static final String WRAP_ALIGN_ASSIGNMENT_STATEMENT = "groovy_wrap_align_assignment_stmt";

    // Group declarations
    public static final String WRAP_ALIGN_FIELDS_IN_COLUMNS = "groovy_wrap_align_fields_columns";

    // Ternary operation
    public static final String WRAP_TERNARY_OPERATION = "groovy_wrap_ternary_operation";
    public static final String WRAP_ALIGN_TERNARY_OPERATION = "groovy_wrap_align_ternary_operation";

    // Array initializer
    public static final String WRAP_ARRAY_INITIALIZER = "groovy_wrap_array_initializer";
    public static final String WRAP_ALIGN_ARRAY_INITIALIZER = "groovy_wrap_align_array_initializer";
    public static final String WRAP_NEW_LINE_AFTER_LBRACE_ARRAY_INITIALIZER = "groovy_wrap_newline_lbrace_array_init";
    public static final String WRAP_PLACE_RBRACE_ON_NEW_LINE_ARRAY_INITIALIZER = "groovy_wrap_place_rbrace_newline_array_init";

    // Modifier list
    public static final String WRAP_AFTER_MODIFIER_LIST = "groovy_wrap_after_modifier_list";

    // Assert / Enum / Annotations
    public static final String WRAP_ASSERT_STATEMENT = "groovy_wrap_assert_statement";
    public static final String WRAP_ENUM_CONSTANTS = "groovy_wrap_enum_constants";
    public static final String WRAP_CLASS_ANNOTATIONS = "groovy_wrap_class_annotations";
    public static final String WRAP_METHOD_ANNOTATIONS = "groovy_wrap_method_annotations";
    public static final String WRAP_FIELD_ANNOTATIONS = "groovy_wrap_field_annotations";
    public static final String WRAP_PARAMETER_ANNOTATIONS = "groovy_wrap_parameter_annotations";
    public static final String WRAP_LOCAL_VARIABLE_ANNOTATIONS = "groovy_wrap_local_var_annotations";
    public static final String WRAP_IMPORT_ANNOTATIONS = "groovy_wrap_import_annotations";

    // List and map literals
    public static final String WRAP_ALIGN_LIST_MAP_MULTIPLE = "groovy_wrap_align_list_map_multiple";
    public static final String WRAP_ALIGN_LIST_MAP_MULTILINE_NAMED_ARGS = "groovy_wrap_align_list_map_named_args";

    // GINQ clauses
    public static final String WRAP_GINQ_CLAUSES = "groovy_wrap_ginq_clauses";
    public static final String WRAP_GINQ_ON_CLAUSE = "groovy_wrap_ginq_on_clause";
    public static final String WRAP_GINQ_INDENT_ON_CLAUSE = "groovy_wrap_ginq_indent_on_clause";
    public static final String WRAP_GINQ_HAVING_CLAUSE = "groovy_wrap_ginq_having_clause";
    public static final String WRAP_GINQ_INDENT_HAVING_CLAUSE = "groovy_wrap_ginq_indent_having_clause";
    public static final String WRAP_GINQ_PUT_SPACE_AFTER_KEYWORDS = "groovy_wrap_ginq_space_after_kw";

    // ==========================================
    // Property Keys - Blank Lines
    // ==========================================
    public static final String BLANK_LINES_KEEP_IN_DECLARATIONS = "groovy_blank_lines_keep_in_declarations";
    public static final String BLANK_LINES_KEEP_IN_CODE = "groovy_blank_lines_keep_in_code";
    public static final String BLANK_LINES_KEEP_BEFORE_RBRACE = "groovy_blank_lines_keep_before_rbrace";

    public static final String BLANK_LINES_BEFORE_PACKAGE = "groovy_blank_lines_before_package";
    public static final String BLANK_LINES_AFTER_PACKAGE = "groovy_blank_lines_after_package";
    public static final String BLANK_LINES_BEFORE_IMPORTS = "groovy_blank_lines_before_imports";
    public static final String BLANK_LINES_AFTER_IMPORTS = "groovy_blank_lines_after_imports";
    public static final String BLANK_LINES_AROUND_CLASS = "groovy_blank_lines_around_class";
    public static final String BLANK_LINES_AFTER_CLASS_HEADER = "groovy_blank_lines_after_class_header";
    public static final String BLANK_LINES_AROUND_FIELD_IN_INTERFACE = "groovy_blank_lines_around_field_in_interface";
    public static final String BLANK_LINES_AROUND_FIELD = "groovy_blank_lines_around_field";
    public static final String BLANK_LINES_AROUND_METHOD_IN_INTERFACE = "groovy_blank_lines_around_method_in_interface";
    public static final String BLANK_LINES_AROUND_METHOD = "groovy_blank_lines_around_method";
    public static final String BLANK_LINES_BEFORE_METHOD_BODY = "groovy_blank_lines_before_method_body";

    // ==========================================
    // Property Keys - GroovyDoc (media_1790579404607.png)
    // ==========================================
    public static final String GROOVY_DOC_ENABLE_FORMATTING = "groovy_doc_enable_formatting";

    // ==========================================
    // Property Keys - Imports (media_1790579415188.png)
    // ==========================================
    public static final String IMPORTS_USE_SINGLE_CLASS_IMPORT = "groovy_imports_use_single_class_import";
    public static final String IMPORTS_USE_FQ_CLASS_NAMES = "groovy_imports_use_fq_class_names";
    public static final String IMPORTS_INSERT_FOR_INNER_CLASSES = "groovy_imports_insert_for_inner_classes";
    public static final String IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC = "groovy_imports_use_fq_class_names_in_javadoc";
    public static final String IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = "groovy_imports_class_count_star";
    public static final String IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND = "groovy_imports_static_count_star";
    public static final String IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY = "groovy_imports_layout_static_separately";

    // ==========================================
    // Property Keys - Code Generation (media_1790579425910.png)
    // ==========================================
    public static final String CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN = "groovy_code_gen_line_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START = "groovy_code_gen_add_space_line_comment";
    public static final String CODE_GEN_ENFORCE_ON_REFORMAT = "groovy_code_gen_enforce_on_reformat";
    public static final String CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN = "groovy_code_gen_block_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS = "groovy_code_gen_add_spaces_around_block_comments";

    // Choices
    public static final List<String> WRAP_OPTIONS = List.of("Do not wrap", "Wrap if long", "Chop down if long", "Wrap always");
    public static final List<String> BRACE_PLACEMENT_OPTIONS = List.of("End of line", "Next line", "Next line shifted", "Next line each");
    public static final List<String> FORCE_BRACES_OPTIONS = List.of("Do not force", "When multiline", "Always");
    public static final List<String> WRAP_ON_TYPING_OPTIONS = List.of("Default: No", "Yes", "No");
    public static final List<String> VISUAL_GUIDES_OPTIONS = List.of("Default: None", "None", "80", "120");

    /**
     * Entry in the Groovy Imports tables.
     */
    public static class GroovyImportEntry {
        private boolean isStatic;
        private String packageName;
        private boolean withSubpackages;

        public GroovyImportEntry(boolean isStatic, String packageName, boolean withSubpackages) {
            this.isStatic = isStatic;
            this.packageName = packageName != null ? packageName : "";
            this.withSubpackages = withSubpackages;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public void setStatic(boolean isStatic) {
            this.isStatic = isStatic;
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

        public boolean isSpecial() {
            return packageName.equals("<blank line>")
                    || packageName.equals("import all other imports")
                    || packageName.equals("import static all other imports");
        }

        public GroovyImportEntry copy() {
            return new GroovyImportEntry(isStatic, packageName, withSubpackages);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroovyImportEntry that = (GroovyImportEntry) o;
            return isStatic == that.isStatic &&
                    withSubpackages == that.withSubpackages &&
                    Objects.equals(packageName, that.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isStatic, packageName, withSubpackages);
        }
    }

    private final List<GroovyImportEntry> packagesToUseImportOnDemand = new ArrayList<>();
    private final List<GroovyImportEntry> importLayout = new ArrayList<>();
    private final List<String> orderOfMembers = new ArrayList<>();

    public List<GroovyImportEntry> getPackagesToUseImportOnDemand() {
        return packagesToUseImportOnDemand;
    }

    public List<GroovyImportEntry> getImportLayout() {
        return importLayout;
    }

    public List<String> getOrderOfMembers() {
        return orderOfMembers;
    }

    public GroovyCodeStyleSettings() {
        this("Groovy");
    }

    public GroovyCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tabs and Indents (media_1790578371136.png)
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);
        setLabelIndent(0);
        setString(LABEL_INDENT_STYLE, "Indent statements after label");

        // Spaces - Before parentheses
        setBoolean(SPACE_BEFORE_METHOD_DECLARATION_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_METHOD_CALL_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_EMPTY_METHOD_CALL_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_IF_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_FOR_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_WHILE_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_SWITCH_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_TRY_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_CATCH_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_SYNCHRONIZED_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_TYPE_CAST_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_ANNOTATION_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_LIST_AND_MAPS_LITERALS, false);
        setBoolean(SPACE_BEFORE_GSTRING_INJECTION_BRACES, false);
        setBoolean(SPACE_BEFORE_TUPLE_ASSIGNMENT, false);
        setBoolean(SPACE_BEFORE_RECORD_PARAMETER_LIST, false);

        // Spaces - Around operators
        setBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, true);
        setBoolean(SPACE_AROUND_LOGICAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_EQUALITY_OPERATORS, true);
        setBoolean(SPACE_AROUND_RELATIONAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_BITWISE_OPERATORS, true);
        setBoolean(SPACE_AROUND_ADDITIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_SHIFT_OPERATORS, true);
        setBoolean(SPACE_AROUND_LAMBDA_ARROW, true);
        setBoolean(SPACE_AROUND_REGEXP_OPERATORS, true);

        // Spaces - Before left brace
        setBoolean(SPACE_BEFORE_CLASS_LBRACE, true);
        setBoolean(SPACE_BEFORE_METHOD_LBRACE, true);
        setBoolean(SPACE_BEFORE_IF_LBRACE, true);
        setBoolean(SPACE_BEFORE_ELSE_LBRACE, true);
        setBoolean(SPACE_BEFORE_FOR_LBRACE, true);
        setBoolean(SPACE_BEFORE_WHILE_LBRACE, true);
        setBoolean(SPACE_BEFORE_DO_LBRACE, true);
        setBoolean(SPACE_BEFORE_SWITCH_LBRACE, true);
        setBoolean(SPACE_BEFORE_TRY_LBRACE, true);
        setBoolean(SPACE_BEFORE_CATCH_LBRACE, true);
        setBoolean(SPACE_BEFORE_FINALLY_LBRACE, true);
        setBoolean(SPACE_BEFORE_SYNCHRONIZED_LBRACE, true);
        setBoolean(SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE, false);
        setBoolean(SPACE_BEFORE_CLOSURE_LBRACE_IN_CALLS, false);

        // Spaces - Before keywords
        setBoolean(SPACE_BEFORE_ELSE_KEYWORD, true);
        setBoolean(SPACE_BEFORE_WHILE_KEYWORD, true);
        setBoolean(SPACE_BEFORE_CATCH_KEYWORD, true);
        setBoolean(SPACE_BEFORE_FINALLY_KEYWORD, true);

        // Spaces - Within
        setBoolean(SPACE_WITHIN_CODE_BRACES, true);
        setBoolean(SPACE_WITHIN_BRACKETS, false);
        setBoolean(SPACE_WITHIN_ARRAY_INITIALIZER_BRACES, false);
        setBoolean(SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES, false);
        setBoolean(SPACE_WITHIN_GROUPING_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_METHOD_DECLARATION_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_METHOD_CALL_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_IF_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_FOR_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_WHILE_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_SWITCH_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_TRY_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_CATCH_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_SYNCHRONIZED_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_TYPE_CAST_PARENTHESES, false);
        setBoolean(SPACE_WITHIN_ANNOTATION_PARENTHESES, false);

        // Spaces - In ternary operator
        setBoolean(SPACE_BEFORE_TERNARY_QUESTION, true);
        setBoolean(SPACE_AFTER_TERNARY_QUESTION, true);
        setBoolean(SPACE_BEFORE_TERNARY_COLON, true);
        setBoolean(SPACE_AFTER_TERNARY_COLON, true);

        // Spaces - Within type arguments
        setBoolean(SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS, true);

        // Spaces - Other
        setBoolean(SPACE_BEFORE_COMMA, false);
        setBoolean(SPACE_AFTER_COMMA, true);
        setBoolean(SPACE_BEFORE_FOR_SEMICOLON, false);
        setBoolean(SPACE_AFTER_FOR_SEMICOLON, true);
        setBoolean(SPACE_AFTER_TYPE_CAST, true);
        setBoolean(SPACE_IN_NAMED_ARGUMENT_BEFORE_COLON, false);
        setBoolean(SPACE_IN_NAMED_ARGUMENT_AFTER_COLON, true);
        setBoolean(SPACE_BEFORE_ASSERT_SEPARATOR, false);
        setBoolean(SPACE_AFTER_ASSERT_SEPARATOR, true);

        // Wrapping and Braces defaults (media_1790578808143.png, media_1790578849775.png, media_1790578902174.png, media_1790578944643.png)
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "Default: None");

        setBoolean(WRAP_KEEP_LINE_BREAKS, true);
        setBoolean(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(WRAP_KEEP_CONTROL_STATEMENT_IN_ONE_LINE, true);
        setBoolean(WRAP_KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE, false);
        setBoolean(WRAP_KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, false);
        setBoolean(WRAP_KEEP_SIMPLE_METHODS_IN_ONE_LINE, true);
        setBoolean(WRAP_KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE, true);
        setBoolean(WRAP_KEEP_SIMPLE_CLASSES_IN_ONE_LINE, true);

        setBoolean(WRAP_ENSURE_RIGHT_MARGIN_NOT_EXCEEDED, false);

        setString(BRACE_PLACEMENT_CLASS, "End of line");
        setString(BRACE_PLACEMENT_METHOD, "End of line");
        setString(BRACE_PLACEMENT_LAMBDA, "End of line");
        setString(BRACE_PLACEMENT_OTHER, "End of line");
        setBoolean(BRACE_USE_FLYING_GEESE, false);

        setString(WRAP_EXTENDS_LIST, "Do not wrap");
        setBoolean(WRAP_ALIGN_EXTENDS_LIST, false);
        setString(WRAP_EXTENDS_KEYWORD, "Do not wrap");

        setString(WRAP_THROWS_LIST, "Do not wrap");
        setBoolean(WRAP_ALIGN_THROWS_LIST, false);
        setBoolean(WRAP_ALIGN_THROWS_TO_METHOD_START, false);
        setString(WRAP_THROWS_KEYWORD, "Do not wrap");

        setString(WRAP_METHOD_PARAMETERS, "Do not wrap");
        setBoolean(WRAP_ALIGN_METHOD_PARAMETERS, true);
        setBoolean(WRAP_NEW_LINE_AFTER_LPAREN_METHOD_PARAMETERS, false);
        setBoolean(WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_PARAMETERS, false);

        setString(WRAP_METHOD_ARGUMENTS, "Do not wrap");
        setBoolean(WRAP_ALIGN_METHOD_ARGUMENTS, false);
        setBoolean(WRAP_METHOD_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, false);
        setBoolean(WRAP_NEW_LINE_AFTER_LPAREN_METHOD_ARGUMENTS, false);
        setBoolean(WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_ARGUMENTS, false);

        setBoolean(WRAP_ALIGN_METHOD_PARENTHESES, false);

        setString(WRAP_CHAINED_CALLS, "Do not wrap");
        setBoolean(WRAP_ALIGN_CHAINED_CALLS, false);
        setBoolean(WRAP_CHAINED_CALLS_WRAP_AFTER_DOT, false);

        setString(WRAP_IF_FORCE_BRACES, "Do not force");
        setBoolean(WRAP_IF_ELSE_ON_NEW_LINE, false);
        setBoolean(WRAP_IF_SPECIAL_ELSE_IF, true);

        setString(WRAP_FOR_STATEMENT, "Do not wrap");
        setBoolean(WRAP_ALIGN_FOR_STATEMENT, true);
        setBoolean(WRAP_NEW_LINE_AFTER_LPAREN_FOR, false);
        setBoolean(WRAP_PLACE_RPAREN_ON_NEW_LINE_FOR, false);
        setString(WRAP_FOR_FORCE_BRACES, "Do not force");

        setString(WRAP_WHILE_FORCE_BRACES, "Do not force");

        setString(WRAP_DO_WHILE_FORCE_BRACES, "Do not force");
        setBoolean(WRAP_DO_WHILE_ON_NEW_LINE, false);

        setBoolean(WRAP_SWITCH_INDENT_CASE_BRANCHES, true);

        setString(WRAP_TRY_WITH_RESOURCES, "Do not wrap");
        setBoolean(WRAP_ALIGN_TRY_WITH_RESOURCES, false);
        setBoolean(WRAP_NEW_LINE_AFTER_LPAREN_TRY_WITH_RESOURCES, false);
        setBoolean(WRAP_PLACE_RPAREN_ON_NEW_LINE_TRY_WITH_RESOURCES, false);

        setBoolean(WRAP_TRY_CATCH_ON_NEW_LINE, false);
        setBoolean(WRAP_TRY_FINALLY_ON_NEW_LINE, false);

        setString(WRAP_BINARY_EXPRESSIONS, "Do not wrap");
        setBoolean(WRAP_ALIGN_BINARY_EXPRESSIONS, false);
        setBoolean(WRAP_NEW_LINE_AFTER_LPAREN_BINARY_EXPRESSIONS, false);
        setBoolean(WRAP_PLACE_RPAREN_ON_NEW_LINE_BINARY_EXPRESSIONS, false);

        setString(WRAP_ASSIGNMENT_STATEMENT, "Do not wrap");
        setBoolean(WRAP_ALIGN_ASSIGNMENT_STATEMENT, false);

        setBoolean(WRAP_ALIGN_FIELDS_IN_COLUMNS, false);

        setString(WRAP_TERNARY_OPERATION, "Do not wrap");
        setBoolean(WRAP_ALIGN_TERNARY_OPERATION, false);

        setString(WRAP_ARRAY_INITIALIZER, "Do not wrap");
        setBoolean(WRAP_ALIGN_ARRAY_INITIALIZER, false);
        setBoolean(WRAP_NEW_LINE_AFTER_LBRACE_ARRAY_INITIALIZER, false);
        setBoolean(WRAP_PLACE_RBRACE_ON_NEW_LINE_ARRAY_INITIALIZER, false);

        setBoolean(WRAP_AFTER_MODIFIER_LIST, false);

        setString(WRAP_ASSERT_STATEMENT, "Do not wrap");
        setString(WRAP_ENUM_CONSTANTS, "Do not wrap");
        setString(WRAP_CLASS_ANNOTATIONS, "Wrap always");
        setString(WRAP_METHOD_ANNOTATIONS, "Wrap always");
        setString(WRAP_FIELD_ANNOTATIONS, "Wrap always");
        setString(WRAP_PARAMETER_ANNOTATIONS, "Do not wrap");
        setString(WRAP_LOCAL_VARIABLE_ANNOTATIONS, "Do not wrap");
        setString(WRAP_IMPORT_ANNOTATIONS, "Wrap always");

        setBoolean(WRAP_ALIGN_LIST_MAP_MULTIPLE, true);
        setBoolean(WRAP_ALIGN_LIST_MAP_MULTILINE_NAMED_ARGS, true);

        setString(WRAP_GINQ_CLAUSES, "Wrap always");
        setString(WRAP_GINQ_ON_CLAUSE, "Wrap if long");
        setBoolean(WRAP_GINQ_INDENT_ON_CLAUSE, true);
        setString(WRAP_GINQ_HAVING_CLAUSE, "Wrap if long");
        setBoolean(WRAP_GINQ_INDENT_HAVING_CLAUSE, true);
        setBoolean(WRAP_GINQ_PUT_SPACE_AFTER_KEYWORDS, true);

        // Blank Lines defaults (media_1790578964702.png)
        setInt(BLANK_LINES_KEEP_IN_DECLARATIONS, 2);
        setInt(BLANK_LINES_KEEP_IN_CODE, 2);
        setInt(BLANK_LINES_KEEP_BEFORE_RBRACE, 2);

        setInt(BLANK_LINES_BEFORE_PACKAGE, 0);
        setInt(BLANK_LINES_AFTER_PACKAGE, 1);
        setInt(BLANK_LINES_BEFORE_IMPORTS, 1);
        setInt(BLANK_LINES_AFTER_IMPORTS, 1);
        setInt(BLANK_LINES_AROUND_CLASS, 1);
        setInt(BLANK_LINES_AFTER_CLASS_HEADER, 0);
        setInt(BLANK_LINES_AROUND_FIELD_IN_INTERFACE, 0);
        setInt(BLANK_LINES_AROUND_FIELD, 0);
        setInt(BLANK_LINES_AROUND_METHOD_IN_INTERFACE, 1);
        setInt(BLANK_LINES_AROUND_METHOD, 1);
        setInt(BLANK_LINES_BEFORE_METHOD_BODY, 0);

        // GroovyDoc defaults (media_1790579404607.png)
        setBoolean(GROOVY_DOC_ENABLE_FORMATTING, true);

        // Imports defaults (media_1790579415188.png)
        setBoolean(IMPORTS_USE_SINGLE_CLASS_IMPORT, true);
        setBoolean(IMPORTS_USE_FQ_CLASS_NAMES, false);
        setBoolean(IMPORTS_INSERT_FOR_INNER_CLASSES, false);
        setBoolean(IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, true);
        setInt(IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, 5);
        setInt(IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, 3);
        setBoolean(IMPORTS_LAYOUT_STATIC_IMPORTS_SEPARATELY, true);

        packagesToUseImportOnDemand.clear();
        packagesToUseImportOnDemand.add(new GroovyImportEntry(false, "import java.awt.*", false));
        packagesToUseImportOnDemand.add(new GroovyImportEntry(false, "import javax.swing.*", false));

        importLayout.clear();
        importLayout.add(new GroovyImportEntry(false, "import all other imports", false));
        importLayout.add(new GroovyImportEntry(false, "<blank line>", false));
        importLayout.add(new GroovyImportEntry(false, "import javax.*", true));
        importLayout.add(new GroovyImportEntry(false, "import java.*", true));
        importLayout.add(new GroovyImportEntry(false, "<blank line>", false));
        importLayout.add(new GroovyImportEntry(true, "import static all other imports", false));

        // Code Generation defaults (media_1790579425910.png)
        orderOfMembers.clear();
        orderOfMembers.addAll(List.of(
                "Static fields",
                "Instance fields",
                "Constructors",
                "Static methods",
                "Instance methods",
                "Static inner classes",
                "Inner classes"
        ));

        setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, false);
        setBoolean(CODE_GEN_ENFORCE_ON_REFORMAT, false);
        setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false);
    }

    @Override
    public GroovyCodeStyleSettings copy() {
        GroovyCodeStyleSettings copy = new GroovyCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setLabelIndent(getLabelIndent());
        copy.setProperties(getAllProperties());

        copy.packagesToUseImportOnDemand.clear();
        for (GroovyImportEntry e : this.packagesToUseImportOnDemand) {
            copy.packagesToUseImportOnDemand.add(e.copy());
        }

        copy.importLayout.clear();
        for (GroovyImportEntry e : this.importLayout) {
            copy.importLayout.add(e.copy());
        }

        copy.orderOfMembers.clear();
        copy.orderOfMembers.addAll(this.orderOfMembers);

        return copy;
    }

    // ==========================================
    // Sample Codes matching reference screenshots verbatim
    // ==========================================
    public static final String SAMPLE_TABS_AND_INDENTS = """
def foo(int arg) {
    label1:
        for (i in 1..10) {
            label2:
                foo(1)
        }
        return Math.max(arg,
                0)
}

class HelloSpock extends spock.lang.Specification {
    def "length of Spock's and his friends' names"() {
        expect:
        name.size() == length

        where:
        name     | length | foo
        "Spock"  | 5
        "Kirk"   | 4      | xxx | yyy
        "Scotty" | 6      | dddddddddd | fff

        //aaa
        a        | b      | c
    }
}
""";

    public static final String SAMPLE_SPACES = """
class Foo {
    @Annotation(param = "foo")
    @Ann([1, 2])
    public static <T1, T2> void foo(int x, int y) {
        int[] array = new int[]{1, 2, 3, 4, 5, 6, 7}
        Foo[] emptyArray = new Foo[]{}

        for (int i = 0; i < x; i++) {
            y += (y ^ 0x123) << 2
        }

        10.times {
            print it
        }
        int j = 0
        while (j < 10) {
            try (def resource = obtainResource()) {
                if (0 < x && x < 10) {
                    while (x != y) {
                        x = f(x * 3 + 5)
                    }
                } else {
                    synchronized (this) {
                        switch (e.getCode()) {
                            //...
                        }
                    }
                }
            } catch (MyException e) {
                logError(method: "foo", exception: e)
            } finally {
                int[] arr = (int[]) g(y)
                x = y >= 0 ? arr[y] : -1
                y = [1, 2, 3] ?: 4
            }
        }
        do {
            operation()
        } while (condition)
        def cl = { Math.sin(it) }
        def lambda = a -> b
        print ckl(2)
        assert condition: message
    }

    def inject(x) { "cos($x) = ${Math.cos(x)}" }
}

record R(int x) {}
""";

    public static final String SAMPLE_WRAPPING_AND_BRACES = """
/*
 * This is a sample file.
 */

public class ThisIsASampleClass extends C1 implements I1, I2, I3, I4, I5 {
    private int f1 = 1
    private String field2 = ""

    void m(int i1, long i2, short i3, double i4, byte i5, boolean i6, float i7) {
    }

    public static void longerMethod() throws Exception1, Exception2, Exception3 {
// todo something
        int
            i = 0
        int var1 = 1;
        int var2 = 2
        new int[]{-1000, -100, -10, -1, 0, 1, 10, 100, 1000, 10000, 100000}
        foo1(0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057)
        foo2 "a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg", "abcdefgh"
        int x = (3 + 4 + 5 + 6) * (7 + 8 + 9 + 10) * (11 + 12 + 13 + 14 + 0xFFFFFFFF)
        String s1, s2, s3
        s1 = s2 = s3 = "012345678901456"
        assert i + j + k + l + n + m <= 2: "assert description"
        int y = 2 > 3 ? 7 + 8 + 9 : 11 + 12 + 13
        super.getFoo().foo().getBar().bar()
        def oneLineLambda = (a, b, c) -> {
            print b }
        def multilineLambda = (a, b, c) -> {
            def d = a
            print d
        }

        label:
        if (2 < 3) return else if (2 > 3) return else return
        for (int i = 0; i < 0xFFFFFF; i += 2) System.out.println(i)
        print([
                l1: expr1,
                label2: expr2
        ])
        while (x < 50000) x++
        do x++ while (x < 50000)
        do x++
        while (x < 50000)
        switch (a) {
            case 0:
                doCase0()
                break
            default:
                doDefault()
        }
    }

    public static void test()
            throws Exception {
        foo.foo().bar("arg1",
                "arg2")
        new Object() {}
    }

    class TestInnerClass {}

    interface TestInnerInterface {
    }
}

try (def a = r(); def b = r2; def c; def d = r4()) {
} catch (Exception e) {
    processException(e)
} finally {
    processFinally()
}

enum Breed {
    Dalmatian(), Labrador(), Dachshund()
}

@Annotation1
@Annotation2
@Annotation3(param1 = "value1", param2 = "value2")
@Annotation4
class Foo {
    @Annotation1
    @Annotation3(param1 = "value1", param2 = "value2")
    public static void foo() {
    }
    @Annotation1
    @Annotation3(param1 = "value1", param2 = "value2")
    public static int myFoo

    public void method(@Annotation1 @Annotation3(param1 = "value1", param2 = "value2") final int param1,
                       @Annotation1 @Annotation3(param1 = "value1", param2 = "value2") final int localVariable) {
    }
}
""";

    public static final String SAMPLE_BLANK_LINES = """
/*
 * This is a sample file.
 */
package dev.lumina.samples

import dev.lumina.Main

import javax.swing.*
import java.util.Vector

public class Foo {
    private int field1
    private int field2

    public void foo1() {
        new Runnable() {
            public void run() {
            }
        }
    }

    public class InnerClass {
    }
}

class AnotherClass {
}

interface TestInterface {
    int MAX = 10
    int MIN = 1

    def method1()

    void method2()
}
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "Groovy";
            }

            @Override
            public String getDisplayName() {
                return "Groovy";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines", "GroovyDoc", "Imports", "Code Generation");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return "Tabs and Indents".equals(tabName)
                        || "Spaces".equals(tabName)
                        || "Wrapping and Braces".equals(tabName)
                        || "Blank Lines".equals(tabName);
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new GroovyCodeStyleSettings("Groovy");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public String getSampleCode(String tabName) {
                if ("Spaces".equals(tabName)) return SAMPLE_SPACES;
                if ("Wrapping and Braces".equals(tabName)) return SAMPLE_WRAPPING_AND_BRACES;
                if ("Blank Lines".equals(tabName)) return SAMPLE_BLANK_LINES;
                return SAMPLE_TABS_AND_INDENTS;
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
                    g.addOption(CodeStyleOption.number("label_indent", "Label indent:", 0));
                    g.addOption(CodeStyleOption.combo(
                            LABEL_INDENT_STYLE,
                            "Label indent style:",
                            List.of("Indent statements after label", "Do not indent statements after label"),
                            "Indent statements after label"
                    ));
                    customizer.addGroup(g);

                } else if ("Spaces".equals(tabName)) {
                    // 1. Before parentheses
                    CodeStyleGroup beforeParen = CodeStyleGroup.collapsible("Before parentheses");
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_METHOD_DECLARATION_PARENTHESES, "Method declaration parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_METHOD_CALL_PARENTHESES, "Method call parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_EMPTY_METHOD_CALL_PARENTHESES, "Empty method call parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_IF_PARENTHESES, "'if' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_PARENTHESES, "'for' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_PARENTHESES, "'while' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SWITCH_PARENTHESES, "'switch' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TRY_PARENTHESES, "'try' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_PARENTHESES, "'catch' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SYNCHRONIZED_PARENTHESES, "'synchronized' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TYPE_CAST_PARENTHESES, "Type cast parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ANNOTATION_PARENTHESES, "Annotation parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_LIST_AND_MAPS_LITERALS, "List and maps literals", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_GSTRING_INJECTION_BRACES, "GString injection braces", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TUPLE_ASSIGNMENT, "Tuple assignment expression", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_RECORD_PARAMETER_LIST, "Before record parameter list", false));
                    customizer.addGroup(beforeParen);

                    // 2. Around operators
                    CodeStyleGroup aroundOps = CodeStyleGroup.collapsible("Around operators");
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ASSIGNMENT_OPERATORS, "Assignment operators (=, +=, ...)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_LOGICAL_OPERATORS, "Logical operators (&&, ||, ==>)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_EQUALITY_OPERATORS, "Equality operators (==, !=)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_RELATIONAL_OPERATORS, "Relational operators (<, >, <=, >=, <=>)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_BITWISE_OPERATORS, "Bitwise operators (&, |, ^)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ADDITIVE_OPERATORS, "Additive operators (+, -)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, "Multiplicative operators (*, /, %)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_SHIFT_OPERATORS, "Shift operators (<<, >>, >>>)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_LAMBDA_ARROW, "Lambda arrow", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_REGEXP_OPERATORS, "Regexp expression (=~, ==~)", true));
                    customizer.addGroup(aroundOps);

                    // 3. Before left brace
                    CodeStyleGroup beforeLBrace = CodeStyleGroup.collapsible("Before left brace");
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CLASS_LBRACE, "Class left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_METHOD_LBRACE, "Method left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_IF_LBRACE, "'if' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ELSE_LBRACE, "'else' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_LBRACE, "'for' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_LBRACE, "'while' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_DO_LBRACE, "'do' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SWITCH_LBRACE, "'switch' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TRY_LBRACE, "'try' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_LBRACE, "'catch' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FINALLY_LBRACE, "'finally' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SYNCHRONIZED_LBRACE, "'synchronized' left brace", true));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE, "Array initializer left brace", false));
                    beforeLBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CLOSURE_LBRACE_IN_CALLS, "Closure left brace in method calls", false));
                    customizer.addGroup(beforeLBrace);

                    // 4. Before keywords
                    CodeStyleGroup beforeKw = CodeStyleGroup.collapsible("Before keywords");
                    beforeKw.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ELSE_KEYWORD, "'else' keyword", true));
                    beforeKw.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_KEYWORD, "'while' keyword", true));
                    beforeKw.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_KEYWORD, "'catch' keyword", true));
                    beforeKw.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FINALLY_KEYWORD, "'finally' keyword", true));
                    customizer.addGroup(beforeKw);

                    // 5. Within
                    CodeStyleGroup within = CodeStyleGroup.collapsible("Within");
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_CODE_BRACES, "Code braces", true));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_BRACKETS, "Brackets", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_ARRAY_INITIALIZER_BRACES, "Array initializer braces", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES, "Empty array initializer braces", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_GROUPING_PARENTHESES, "Grouping parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_METHOD_DECLARATION_PARENTHESES, "Method declaration parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_METHOD_CALL_PARENTHESES, "Method call parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES, "Empty method call parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_IF_PARENTHESES, "'if' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_FOR_PARENTHESES, "'for' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_WHILE_PARENTHESES, "'while' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_SWITCH_PARENTHESES, "'switch' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_TRY_PARENTHESES, "'try' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_CATCH_PARENTHESES, "'catch' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_SYNCHRONIZED_PARENTHESES, "'synchronized' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_TYPE_CAST_PARENTHESES, "Type cast parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_ANNOTATION_PARENTHESES, "Annotation parentheses", false));
                    customizer.addGroup(within);

                    // 6. In ternary operator (?:)
                    CodeStyleGroup ternary = CodeStyleGroup.collapsible("In ternary operator (?:)");
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TERNARY_QUESTION, "Before '?'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_AFTER_TERNARY_QUESTION, "After '?'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TERNARY_COLON, "Before ':'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_AFTER_TERNARY_COLON, "After ':'", true));
                    customizer.addGroup(ternary);

                    // 7. Within type arguments
                    CodeStyleGroup withinTypeArgs = CodeStyleGroup.collapsible("Within type arguments");
                    withinTypeArgs.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS, "After comma", true));
                    customizer.addGroup(withinTypeArgs);

                    // 8. Other
                    CodeStyleGroup other = CodeStyleGroup.collapsible("Other");
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COMMA, "Before comma", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COMMA, "After comma", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_SEMICOLON, "Before 'for' semicolon", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_FOR_SEMICOLON, "After 'for' semicolon", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_TYPE_CAST, "After type cast", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_IN_NAMED_ARGUMENT_BEFORE_COLON, "In named argument before ':'", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_IN_NAMED_ARGUMENT_AFTER_COLON, "In named argument after ':'", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ASSERT_SEPARATOR, "Before 'assert' separator", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_ASSERT_SEPARATOR, "After 'assert' separator", true));
                    customizer.addGroup(other);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.number(HARD_WRAP_AT, "Hard wrap at:", 120));
                    general.addOption(CodeStyleOption.combo(WRAP_ON_TYPING, "Wrap on typing", WRAP_ON_TYPING_OPTIONS, "Default: No"));
                    general.addOption(CodeStyleOption.combo(VISUAL_GUIDES, "Visual guides", VISUAL_GUIDES_OPTIONS, "Default: None"));
                    customizer.addGroup(general);

                    CodeStyleGroup keep = CodeStyleGroup.collapsible("Keep when reformatting");
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_LINE_BREAKS, "Line breaks", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_COMMENT_AT_FIRST_COLUMN, "Comment at first column", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_CONTROL_STATEMENT_IN_ONE_LINE, "Control statement in one line", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE, "Multiple expressions in one line", false));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, "Simple blocks in one line", false));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_SIMPLE_METHODS_IN_ONE_LINE, "Simple methods in one line", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE, "Simple lambdas/closures in one line", true));
                    keep.addOption(CodeStyleOption.checkbox(WRAP_KEEP_SIMPLE_CLASSES_IN_ONE_LINE, "Simple classes in one line", true));
                    customizer.addGroup(keep);

                    CodeStyleGroup margin = CodeStyleGroup.flat("Margin");
                    margin.addOption(CodeStyleOption.checkbox(WRAP_ENSURE_RIGHT_MARGIN_NOT_EXCEEDED, "Ensure right margin is not exceeded", false));
                    customizer.addGroup(margin);

                    CodeStyleGroup braces = CodeStyleGroup.collapsible("Braces placement");
                    braces.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_CLASS, "In class declaration", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    braces.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_METHOD, "In method declaration", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    braces.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_LAMBDA, "In lambda declaration", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    braces.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_OTHER, "Other", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    braces.addOption(CodeStyleOption.checkbox(BRACE_USE_FLYING_GEESE, "Use flying geese braces", false));
                    customizer.addGroup(braces);

                    CodeStyleGroup extendsList = CodeStyleGroup.collapsibleWithCombo(
                            "Extends/implements/permits list", WRAP_EXTENDS_LIST, WRAP_OPTIONS, "Do not wrap");
                    extendsList.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_EXTENDS_LIST, "Align when multiline", false));
                    customizer.addGroup(extendsList);

                    CodeStyleGroup extendsKw = CodeStyleGroup.flat("Extends keyword");
                    extendsKw.addOption(CodeStyleOption.combo(WRAP_EXTENDS_KEYWORD, "Extends/implements/permits keyword", WRAP_OPTIONS, "Do not wrap"));
                    customizer.addGroup(extendsKw);

                    CodeStyleGroup throwsList = CodeStyleGroup.collapsibleWithCombo(
                            "Throws list", WRAP_THROWS_LIST, WRAP_OPTIONS, "Do not wrap");
                    throwsList.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_THROWS_LIST, "Align when multiline", false));
                    throwsList.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_THROWS_TO_METHOD_START, "Align 'throws' to method start", false));
                    customizer.addGroup(throwsList);

                    CodeStyleGroup throwsKw = CodeStyleGroup.flat("Throws keyword");
                    throwsKw.addOption(CodeStyleOption.combo(WRAP_THROWS_KEYWORD, "Throws keyword", WRAP_OPTIONS, "Do not wrap"));
                    customizer.addGroup(throwsKw);

                    CodeStyleGroup methodParams = CodeStyleGroup.collapsibleWithCombo(
                            "Method declaration parameters", WRAP_METHOD_PARAMETERS, WRAP_OPTIONS, "Do not wrap");
                    methodParams.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_METHOD_PARAMETERS, "Align when multiline", true));
                    methodParams.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LPAREN_METHOD_PARAMETERS, "New line after '('", false));
                    methodParams.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_PARAMETERS, "Place ')' on new line", false));
                    customizer.addGroup(methodParams);

                    CodeStyleGroup methodArgs = CodeStyleGroup.collapsibleWithCombo(
                            "Method call arguments", WRAP_METHOD_ARGUMENTS, WRAP_OPTIONS, "Do not wrap");
                    methodArgs.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_METHOD_ARGUMENTS, "Align when multiline", false));
                    methodArgs.addOption(CodeStyleOption.indentedCheckbox(WRAP_METHOD_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, "Take priority over call chain wrapping", false));
                    methodArgs.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LPAREN_METHOD_ARGUMENTS, "New line after '('", false));
                    methodArgs.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RPAREN_ON_NEW_LINE_METHOD_ARGUMENTS, "Place ')' on new line", false));
                    customizer.addGroup(methodArgs);

                    CodeStyleGroup methodParens = CodeStyleGroup.collapsible("Method parentheses");
                    methodParens.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_METHOD_PARENTHESES, "Align when multiline", false));
                    customizer.addGroup(methodParens);

                    CodeStyleGroup chainedCalls = CodeStyleGroup.collapsibleWithCombo(
                            "Chained method calls", WRAP_CHAINED_CALLS, WRAP_OPTIONS, "Do not wrap");
                    chainedCalls.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_CHAINED_CALLS, "Align when multiline", false));
                    chainedCalls.addOption(CodeStyleOption.indentedCheckbox(WRAP_CHAINED_CALLS_WRAP_AFTER_DOT, "Wrap after dot", false));
                    customizer.addGroup(chainedCalls);

                    CodeStyleGroup ifStmt = CodeStyleGroup.collapsible("'if()' statement");
                    ifStmt.addOption(CodeStyleOption.combo(WRAP_IF_FORCE_BRACES, "Force braces", FORCE_BRACES_OPTIONS, "Do not force"));
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_IF_ELSE_ON_NEW_LINE, "'else' on new line", false));
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_IF_SPECIAL_ELSE_IF, "Special 'else if' treatment", true));
                    customizer.addGroup(ifStmt);

                    CodeStyleGroup forStmt = CodeStyleGroup.collapsibleWithCombo(
                            "'for()' statement", WRAP_FOR_STATEMENT, WRAP_OPTIONS, "Do not wrap");
                    forStmt.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_FOR_STATEMENT, "Align when multiline", true));
                    forStmt.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LPAREN_FOR, "New line after '('", false));
                    forStmt.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RPAREN_ON_NEW_LINE_FOR, "Place ')' on new line", false));
                    forStmt.addOption(CodeStyleOption.combo(WRAP_FOR_FORCE_BRACES, "Force braces", FORCE_BRACES_OPTIONS, "Do not force"));
                    customizer.addGroup(forStmt);

                    CodeStyleGroup whileStmt = CodeStyleGroup.collapsible("'while()' statement");
                    whileStmt.addOption(CodeStyleOption.combo(WRAP_WHILE_FORCE_BRACES, "Force braces", FORCE_BRACES_OPTIONS, "Do not force"));
                    customizer.addGroup(whileStmt);

                    CodeStyleGroup doWhileStmt = CodeStyleGroup.collapsible("'do ... while()' statement");
                    doWhileStmt.addOption(CodeStyleOption.combo(WRAP_DO_WHILE_FORCE_BRACES, "Force braces", FORCE_BRACES_OPTIONS, "Do not force"));
                    doWhileStmt.addOption(CodeStyleOption.checkbox(WRAP_DO_WHILE_ON_NEW_LINE, "'while' on new line", false));
                    customizer.addGroup(doWhileStmt);

                    CodeStyleGroup switchStmt = CodeStyleGroup.collapsible("'switch' statement");
                    switchStmt.addOption(CodeStyleOption.checkbox(WRAP_SWITCH_INDENT_CASE_BRANCHES, "Indent 'case' branches", true));
                    customizer.addGroup(switchStmt);

                    CodeStyleGroup tryResources = CodeStyleGroup.collapsibleWithCombo(
                            "'try-with-resources'", WRAP_TRY_WITH_RESOURCES, WRAP_OPTIONS, "Do not wrap");
                    tryResources.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_TRY_WITH_RESOURCES, "Align when multiline", false));
                    tryResources.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LPAREN_TRY_WITH_RESOURCES, "New line after '('", false));
                    tryResources.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RPAREN_ON_NEW_LINE_TRY_WITH_RESOURCES, "Place ')' on new line", false));
                    customizer.addGroup(tryResources);

                    CodeStyleGroup tryStmt = CodeStyleGroup.collapsible("'try' statement");
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_TRY_CATCH_ON_NEW_LINE, "'catch' on new line", false));
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_TRY_FINALLY_ON_NEW_LINE, "'finally' on new line", false));
                    customizer.addGroup(tryStmt);

                    CodeStyleGroup binaryExpr = CodeStyleGroup.collapsibleWithCombo(
                            "Binary expressions", WRAP_BINARY_EXPRESSIONS, WRAP_OPTIONS, "Do not wrap");
                    binaryExpr.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_BINARY_EXPRESSIONS, "Align when multiline", false));
                    binaryExpr.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LPAREN_BINARY_EXPRESSIONS, "New line after '('", false));
                    binaryExpr.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RPAREN_ON_NEW_LINE_BINARY_EXPRESSIONS, "Place ')' on new line", false));
                    customizer.addGroup(binaryExpr);

                    CodeStyleGroup assignment = CodeStyleGroup.collapsibleWithCombo(
                            "Assignment statement", WRAP_ASSIGNMENT_STATEMENT, WRAP_OPTIONS, "Do not wrap");
                    assignment.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_ASSIGNMENT_STATEMENT, "Align when multiline", false));
                    customizer.addGroup(assignment);

                    CodeStyleGroup groupDecl = CodeStyleGroup.collapsible("Group declarations");
                    groupDecl.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_FIELDS_IN_COLUMNS, "Align fields in columns", false));
                    customizer.addGroup(groupDecl);

                    CodeStyleGroup ternary = CodeStyleGroup.collapsibleWithCombo(
                            "Ternary operation", WRAP_TERNARY_OPERATION, WRAP_OPTIONS, "Do not wrap");
                    ternary.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_TERNARY_OPERATION, "Align when multiline", false));
                    customizer.addGroup(ternary);

                    CodeStyleGroup arrayInit = CodeStyleGroup.collapsibleWithCombo(
                            "Array initializer", WRAP_ARRAY_INITIALIZER, WRAP_OPTIONS, "Do not wrap");
                    arrayInit.addOption(CodeStyleOption.indentedCheckbox(WRAP_ALIGN_ARRAY_INITIALIZER, "Align when multiline", false));
                    arrayInit.addOption(CodeStyleOption.indentedCheckbox(WRAP_NEW_LINE_AFTER_LBRACE_ARRAY_INITIALIZER, "New line after '{'", false));
                    arrayInit.addOption(CodeStyleOption.indentedCheckbox(WRAP_PLACE_RBRACE_ON_NEW_LINE_ARRAY_INITIALIZER, "Place '}' on new line", false));
                    customizer.addGroup(arrayInit);

                    CodeStyleGroup modifierList = CodeStyleGroup.collapsible("Modifier list");
                    modifierList.addOption(CodeStyleOption.checkbox(WRAP_AFTER_MODIFIER_LIST, "Wrap after modifier list", false));
                    customizer.addGroup(modifierList);

                    CodeStyleGroup annotationsAndStatements = CodeStyleGroup.flat("Annotations and Statements");
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_ASSERT_STATEMENT, "Assert statement", WRAP_OPTIONS, "Do not wrap"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_ENUM_CONSTANTS, "Enum constants", WRAP_OPTIONS, "Do not wrap"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_CLASS_ANNOTATIONS, "Class annotations", WRAP_OPTIONS, "Wrap always"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_METHOD_ANNOTATIONS, "Method annotations", WRAP_OPTIONS, "Wrap always"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_FIELD_ANNOTATIONS, "Field annotations", WRAP_OPTIONS, "Wrap always"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_PARAMETER_ANNOTATIONS, "Parameter annotations", WRAP_OPTIONS, "Do not wrap"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_LOCAL_VARIABLE_ANNOTATIONS, "Local variable annotations", WRAP_OPTIONS, "Do not wrap"));
                    annotationsAndStatements.addOption(CodeStyleOption.combo(WRAP_IMPORT_ANNOTATIONS, "Import annotations", WRAP_OPTIONS, "Wrap always"));
                    customizer.addGroup(annotationsAndStatements);

                    CodeStyleGroup listAndMap = CodeStyleGroup.collapsible("List and map literals");
                    listAndMap.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_LIST_MAP_MULTIPLE, "Align when multiple", true));
                    listAndMap.addOption(CodeStyleOption.checkbox(WRAP_ALIGN_LIST_MAP_MULTILINE_NAMED_ARGS, "Align multiline named arguments", true));
                    customizer.addGroup(listAndMap);

                    CodeStyleGroup ginq = CodeStyleGroup.collapsibleWithCombo(
                            "GINQ clauses", WRAP_GINQ_CLAUSES, WRAP_OPTIONS, "Wrap always");
                    ginq.addOption(CodeStyleOption.combo(WRAP_GINQ_ON_CLAUSE, "Wrap 'on' clause", WRAP_OPTIONS, "Wrap if long"));
                    ginq.addOption(CodeStyleOption.indentedCheckbox(WRAP_GINQ_INDENT_ON_CLAUSE, "Indent 'on' clause", true));
                    ginq.addOption(CodeStyleOption.combo(WRAP_GINQ_HAVING_CLAUSE, "Wrap 'having' clause", WRAP_OPTIONS, "Wrap if long"));
                    ginq.addOption(CodeStyleOption.indentedCheckbox(WRAP_GINQ_INDENT_HAVING_CLAUSE, "Indent 'having' clause", true));
                    ginq.addOption(CodeStyleOption.indentedCheckbox(WRAP_GINQ_PUT_SPACE_AFTER_KEYWORDS, "Put space after keywords", true));
                    customizer.addGroup(ginq);

                } else if ("Blank Lines".equals(tabName)) {
                    CodeStyleGroup keep = CodeStyleGroup.divider("Keep maximum blank lines");
                    keep.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_DECLARATIONS, "In declarations:", 2));
                    keep.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_CODE, "In code:", 2));
                    keep.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_BEFORE_RBRACE, "Before '}':", 2));
                    customizer.addGroup(keep);

                    CodeStyleGroup min = CodeStyleGroup.divider("Minimum blank lines");
                    min.addOption(CodeStyleOption.number(BLANK_LINES_BEFORE_PACKAGE, "Before package statement:", 0));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AFTER_PACKAGE, "After package statement:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_BEFORE_IMPORTS, "Before imports:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AFTER_IMPORTS, "After imports:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_CLASS, "Around class:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AFTER_CLASS_HEADER, "After class header:", 0));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_FIELD_IN_INTERFACE, "Around field in interface:", 0));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_FIELD, "Around field:", 0));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_METHOD_IN_INTERFACE, "Around method in interface:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_METHOD, "Around method:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_BEFORE_METHOD_BODY, "Before method body:", 0));
                    customizer.addGroup(min);

                } else if ("GroovyDoc".equals(tabName)) {
                    CodeStyleGroup g = CodeStyleGroup.flat("GroovyDoc");
                    g.addOption(CodeStyleOption.checkbox(GROOVY_DOC_ENABLE_FORMATTING, "Enable GroovyDoc formatting", true));
                    customizer.addGroup(g);

                } else if ("Imports".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.divider("General");
                    general.addOption(CodeStyleOption.checkbox(IMPORTS_USE_SINGLE_CLASS_IMPORT, "Use single class import", true));
                    general.addOption(CodeStyleOption.checkbox(IMPORTS_USE_FQ_CLASS_NAMES, "Use fully qualified class names", false));
                    general.addOption(CodeStyleOption.checkbox(IMPORTS_INSERT_FOR_INNER_CLASSES, "Insert imports for inner classes", false));
                    general.addOption(CodeStyleOption.checkbox(IMPORTS_USE_FQ_CLASS_NAMES_IN_JAVADOC, "Use fully qualified class names in javadoc", true));
                    general.addOption(CodeStyleOption.number(IMPORTS_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND, "Class count to use import with '*':", 5));
                    general.addOption(CodeStyleOption.number(IMPORTS_NAMES_COUNT_TO_USE_STATIC_IMPORT_ON_DEMAND, "Names count to use static import with '*':", 3));
                    customizer.addGroup(general);

                } else if ("Code Generation".equals(tabName)) {
                    CodeStyleGroup commentCode = CodeStyleGroup.divider("Comment Code");
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, "Line comment at first column", true));
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, "Add a space at line comment start", false));
                    commentCode.addOption(CodeStyleOption.indentedCheckbox(CODE_GEN_ENFORCE_ON_REFORMAT, "Enforce on reformat", false));
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, "Block comment at first column", true));
                    commentCode.addOption(CodeStyleOption.checkbox(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, "Add spaces around block comments", false));
                    customizer.addGroup(commentCode);
                }
            }
        };
    }
}
