package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for JavaScript.
 * Strictly decoupled, dynamically configured without hardcoding, and brand-isolated.
 * Supports all 8 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790582906008.png)
 * 2. Spaces (media_1790584294419.png, media_1790584318346.png)
 * 3. Wrapping and Braces (media_1790584332985.png, media_1790584373383.png, media_1790584397014.png)
 * 4. Blank Lines
 * 5. Punctuation
 * 6. Code Generation
 * 7. Imports
 * 8. Arrangement
 */
public class JavaScriptCodeStyleSettings extends LanguageCodeStyleSettings {

    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    public static final List<String> BRACE_PLACEMENT_OPTIONS = List.of(
            "End of line",
            "Next line"
    );

    public static final List<String> FORCE_BRACE_OPTIONS = List.of(
            "Do not force",
            "When multiline",
            "Always"
    );

    // ==========================================
    // Property Keys - Tabs and Indents (media_1790582906008.png)
    // ==========================================
    public static final String INDENT_CHAINED_METHODS = "js_indent_chained_methods";
    public static final String INDENT_ALL_CHAINED_CALLS_IN_GROUP = "js_indent_all_chained_calls_in_group";

    // ==========================================
    // Property Keys - Spaces: Before parentheses (media_1790584294419.png)
    // ==========================================
    public static final String SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES = "js_space_before_function_declaration_parentheses";
    public static final String SPACE_BEFORE_FUNCTION_CALL_PARENTHESES = "js_space_before_function_call_parentheses";
    public static final String SPACE_BEFORE_IF_PARENTHESES = "js_space_before_if_parentheses";
    public static final String SPACE_BEFORE_FOR_PARENTHESES = "js_space_before_for_parentheses";
    public static final String SPACE_BEFORE_WHILE_PARENTHESES = "js_space_before_while_parentheses";
    public static final String SPACE_BEFORE_SWITCH_PARENTHESES = "js_space_before_switch_parentheses";
    public static final String SPACE_BEFORE_CATCH_PARENTHESES = "js_space_before_catch_parentheses";
    public static final String SPACE_BEFORE_FUNCTION_EXPRESSION_PARENTHESES = "js_space_before_function_expression_parentheses";
    public static final String SPACE_BEFORE_ASYNC_ARROW_PARENTHESES = "js_space_before_async_arrow_parentheses";

    // ==========================================
    // Property Keys - Spaces: Around operators
    // ==========================================
    public static final String SPACE_AROUND_ASSIGNMENT_OPERATORS = "js_space_around_assignment_operators";
    public static final String SPACE_AROUND_LOGICAL_OPERATORS = "js_space_around_logical_operators";
    public static final String SPACE_AROUND_EQUALITY_OPERATORS = "js_space_around_equality_operators";
    public static final String SPACE_AROUND_RELATIONAL_OPERATORS = "js_space_around_relational_operators";
    public static final String SPACE_AROUND_BITWISE_OPERATORS = "js_space_around_bitwise_operators";
    public static final String SPACE_AROUND_ADDITIVE_OPERATORS = "js_space_around_additive_operators";
    public static final String SPACE_AROUND_MULTIPLICATIVE_OPERATORS = "js_space_around_multiplicative_operators";
    public static final String SPACE_AROUND_SHIFT_OPERATORS = "js_space_around_shift_operators";
    public static final String SPACE_AROUND_UNARY_ADDITIVE_OPERATORS = "js_space_around_unary_additive_operators";
    public static final String SPACE_AROUND_ARROW_FUNCTION = "js_space_around_arrow_function";
    public static final String SPACE_BEFORE_UNARY_NOT = "js_space_before_unary_not";
    public static final String SPACE_AFTER_UNARY_NOT = "js_space_after_unary_not";

    // ==========================================
    // Property Keys - Spaces: Before left brace
    // ==========================================
    public static final String SPACE_BEFORE_FUNCTION_LEFT_BRACE = "js_space_before_function_left_brace";
    public static final String SPACE_BEFORE_IF_LEFT_BRACE = "js_space_before_if_left_brace";
    public static final String SPACE_BEFORE_ELSE_LEFT_BRACE = "js_space_before_else_left_brace";
    public static final String SPACE_BEFORE_FOR_LEFT_BRACE = "js_space_before_for_left_brace";
    public static final String SPACE_BEFORE_WHILE_LEFT_BRACE = "js_space_before_while_left_brace";
    public static final String SPACE_BEFORE_DO_LEFT_BRACE = "js_space_before_do_left_brace";
    public static final String SPACE_BEFORE_SWITCH_LEFT_BRACE = "js_space_before_switch_left_brace";
    public static final String SPACE_BEFORE_TRY_LEFT_BRACE = "js_space_before_try_left_brace";
    public static final String SPACE_BEFORE_CATCH_LEFT_BRACE = "js_space_before_catch_left_brace";
    public static final String SPACE_BEFORE_FINALLY_LEFT_BRACE = "js_space_before_finally_left_brace";
    public static final String SPACE_BEFORE_CLASS_LEFT_BRACE = "js_space_before_class_left_brace";

    // ==========================================
    // Property Keys - Spaces: Before keywords
    // ==========================================
    public static final String SPACE_BEFORE_ELSE_KEYWORD = "js_space_before_else_keyword";
    public static final String SPACE_BEFORE_WHILE_KEYWORD = "js_space_before_while_keyword";
    public static final String SPACE_BEFORE_CATCH_KEYWORD = "js_space_before_catch_keyword";
    public static final String SPACE_BEFORE_FINALLY_KEYWORD = "js_space_before_finally_keyword";

    // ==========================================
    // Property Keys - Spaces: Within (media_1790584294419.png)
    // ==========================================
    public static final String SPACES_WITHIN_INDEX_ACCESS_BRACKETS = "js_spaces_within_index_access_brackets";
    public static final String SPACES_WITHIN_GROUPING_PARENTHESES = "js_spaces_within_grouping_parentheses";
    public static final String SPACES_WITHIN_FUNCTION_DECLARATION_PARENTHESES = "js_spaces_within_function_declaration_parentheses";
    public static final String SPACES_WITHIN_FUNCTION_CALL_PARENTHESES = "js_spaces_within_function_call_parentheses";
    public static final String SPACES_WITHIN_IF_PARENTHESES = "js_spaces_within_if_parentheses";
    public static final String SPACES_WITHIN_FOR_PARENTHESES = "js_spaces_within_for_parentheses";
    public static final String SPACES_WITHIN_WHILE_PARENTHESES = "js_spaces_within_while_parentheses";
    public static final String SPACES_WITHIN_SWITCH_PARENTHESES = "js_spaces_within_switch_parentheses";
    public static final String SPACES_WITHIN_CATCH_PARENTHESES = "js_spaces_within_catch_parentheses";
    public static final String SPACES_WITHIN_OBJECT_LITERAL_BRACES = "js_spaces_within_object_literal_braces";
    public static final String SPACES_WITHIN_ES6_IMPORT_EXPORT_BRACES = "js_spaces_within_es6_import_export_braces";
    public static final String SPACES_WITHIN_ARRAY_BRACKETS = "js_spaces_within_array_brackets";
    public static final String SPACES_WITHIN_INTERPOLATION_EXPRESSIONS = "js_spaces_within_interpolation_expressions";

    // ==========================================
    // Property Keys - Spaces: In ternary operator (?:)
    // ==========================================
    public static final String SPACE_BEFORE_TERNARY_QUESTION = "js_space_before_ternary_question";
    public static final String SPACE_AFTER_TERNARY_QUESTION = "js_space_after_ternary_question";
    public static final String SPACE_BEFORE_TERNARY_COLON = "js_space_before_ternary_colon";
    public static final String SPACE_AFTER_TERNARY_COLON = "js_space_after_ternary_colon";

    // ==========================================
    // Property Keys - Spaces: Other (media_1790584318346.png)
    // ==========================================
    public static final String SPACE_BEFORE_COMMA = "js_space_before_comma";
    public static final String SPACE_AFTER_COMMA = "js_space_after_comma";
    public static final String SPACE_BEFORE_FOR_SEMICOLON = "js_space_before_for_semicolon";
    public static final String SPACE_BEFORE_PROPERTY_NAME_VALUE_SEPARATOR = "js_space_before_property_name_value_separator";
    public static final String SPACE_AFTER_PROPERTY_NAME_VALUE_SEPARATOR = "js_space_after_property_name_value_separator";
    public static final String SPACE_AFTER_REST_SPREAD = "js_space_after_rest_spread";
    public static final String SPACE_BEFORE_GENERATOR_STAR = "js_space_before_generator_star";
    public static final String SPACE_AFTER_GENERATOR_STAR = "js_space_after_generator_star";

    // ==========================================
    // Property Keys - Spaces: In Flow (media_1790584318346.png)
    // ==========================================
    public static final String FLOW_SPACE_BEFORE_TYPE_REFERENCE_COLON = "js_flow_space_before_type_reference_colon";
    public static final String FLOW_SPACE_AFTER_TYPE_REFERENCE_COLON = "js_flow_space_after_type_reference_colon";
    public static final String FLOW_OBJECT_LITERAL_TYPE_BRACES = "js_flow_object_literal_type_braces";
    public static final String FLOW_UNION_AND_INTERSECTION_TYPES = "js_flow_union_and_intersection_types";

    // ==========================================
    // Property Keys - Wrapping and Braces (media_1790584332985.png, media_1790584373383.png, media_1790584397014.png)
    // ==========================================
    public static final String HARD_WRAP_AT = "js_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "js_wrap_on_typing";
    public static final String VISUAL_GUIDES = "js_visual_guides";

    // Keep when reformatting
    public static final String KEEP_LINE_BREAKS = "js_keep_line_breaks";
    public static final String KEEP_COMMENT_AT_FIRST_COLUMN = "js_keep_comment_at_first_column";
    public static final String KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = "js_keep_simple_blocks_in_one_line";
    public static final String KEEP_SIMPLE_METHODS_IN_ONE_LINE = "js_keep_simple_methods_in_one_line";

    // Comments
    public static final String WRAP_COMMENTS_AT_RIGHT_MARGIN = "js_wrap_comments_at_right_margin";
    public static final String ALIGN_MULTILINE_COMMENTS = "js_align_multiline_comments";

    // Braces placement
    public static final String BRACE_PLACEMENT_CLASS = "js_brace_placement_class";
    public static final String BRACE_PLACEMENT_FUNCTION = "js_brace_placement_function";
    public static final String BRACE_PLACEMENT_FUNCTION_EXPRESSION = "js_brace_placement_function_expression";
    public static final String BRACE_PLACEMENT_OTHER = "js_brace_placement_other";

    // Extends list & Extends keyword
    public static final String WRAP_EXTENDS_LIST = "js_wrap_extends_list";
    public static final String ALIGN_MULTILINE_EXTENDS_LIST = "js_align_multiline_extends_list";
    public static final String WRAP_EXTENDS_KEYWORD = "js_wrap_extends_keyword";

    // Function declaration parameters
    public static final String WRAP_FUNCTION_PARAMETERS = "js_wrap_function_parameters";
    public static final String ALIGN_MULTILINE_FUNCTION_PARAMETERS = "js_align_multiline_function_parameters";
    public static final String NEW_LINE_AFTER_FUNCTION_PARAMETERS_LPAREN = "js_new_line_after_function_parameters_lparen";
    public static final String PLACE_RPAREN_ON_NEW_LINE_FUNCTION_PARAMETERS = "js_place_rparen_on_new_line_function_parameters";

    // Function call arguments
    public static final String WRAP_FUNCTION_ARGUMENTS = "js_wrap_function_arguments";
    public static final String ALIGN_MULTILINE_FUNCTION_ARGUMENTS = "js_align_multiline_function_arguments";
    public static final String CALL_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN = "js_call_arguments_take_priority_over_call_chain";
    public static final String NEW_LINE_AFTER_FUNCTION_ARGUMENTS_LPAREN = "js_new_line_after_function_arguments_lparen";
    public static final String PLACE_RPAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS = "js_place_rparen_on_new_line_function_arguments";

    // Chained method calls
    public static final String WRAP_CHAINED_METHOD_CALLS = "js_wrap_chained_method_calls";
    public static final String ALIGN_MULTILINE_CHAINED_METHODS = "js_align_multiline_chained_methods";
    public static final String CHAINED_METHOD_DOT_ON_NEW_LINE = "js_chained_method_dot_on_new_line";

    // 'if()' statement
    public static final String IF_FORCE_BRACES = "js_if_force_braces";
    public static final String WRAP_IF_ELSE_ON_NEW_LINE = "js_wrap_if_else_on_new_line";
    public static final String SPECIAL_ELSE_IF_TREATMENT = "js_special_else_if_treatment";

    // 'for()' statement
    public static final String WRAP_FOR_STATEMENT = "js_wrap_for_statement";
    public static final String ALIGN_MULTILINE_FOR_STATEMENT = "js_align_multiline_for_statement";
    public static final String NEW_LINE_AFTER_FOR_LPAREN = "js_new_line_after_for_lparen";
    public static final String PLACE_RPAREN_ON_NEW_LINE_FOR = "js_place_rparen_on_new_line_for";
    public static final String FOR_FORCE_BRACES = "js_for_force_braces";

    // 'while()' statement
    public static final String WHILE_FORCE_BRACES = "js_while_force_braces";

    // 'do ... while()' statement
    public static final String DO_WHILE_FORCE_BRACES = "js_do_while_force_braces";
    public static final String WRAP_DO_WHILE_ON_NEW_LINE = "js_wrap_do_while_on_new_line";

    // 'switch' statement
    public static final String WRAP_SWITCH_INDENT_CASE_BRANCHES = "js_wrap_switch_indent_case_branches";

    // 'try' statement
    public static final String WRAP_TRY_CATCH_ON_NEW_LINE = "js_wrap_try_catch_on_new_line";
    public static final String WRAP_TRY_FINALLY_ON_NEW_LINE = "js_wrap_try_finally_on_new_line";

    // Binary expressions
    public static final String WRAP_BINARY_EXPRESSIONS = "js_wrap_binary_expressions";
    public static final String ALIGN_MULTILINE_BINARY_EXPRESSIONS = "js_align_multiline_binary_expressions";
    public static final String BINARY_OPERATION_SIGN_ON_NEXT_LINE = "js_binary_operation_sign_on_next_line";
    public static final String NEW_LINE_AFTER_BINARY_LPAREN = "js_new_line_after_binary_lparen";
    public static final String PLACE_RPAREN_ON_NEW_LINE_BINARY = "js_place_rparen_on_new_line_binary";

    // Assignment statement
    public static final String WRAP_ASSIGNMENT_STATEMENT = "js_wrap_assignment_statement";
    public static final String ASSIGNMENT_SIGN_ON_NEXT_LINE = "js_assignment_sign_on_next_line";

    // Ternary operation
    public static final String WRAP_TERNARY_OPERATION = "js_wrap_ternary_operation";
    public static final String ALIGN_MULTILINE_TERNARY_OPERATION = "js_align_multiline_ternary_operation";
    public static final String TERNARY_QUESTION_AND_COLON_ON_NEXT_LINE = "js_ternary_question_and_colon_on_next_line";

    // Arrays
    public static final String WRAP_ARRAYS = "js_wrap_arrays";
    public static final String ALIGN_MULTILINE_ARRAYS = "js_align_multiline_arrays";
    public static final String NEW_LINE_AFTER_ARRAY_LBRACKET = "js_new_line_after_array_lbracket";
    public static final String PLACE_RBRACKET_ON_NEW_LINE_ARRAY = "js_place_rbracket_on_new_line_array";

    // Objects
    public static final String WRAP_OBJECTS = "js_wrap_objects";
    public static final String OBJECTS_ALIGN = "js_objects_align";

    // Variable declarations
    public static final String WRAP_VARIABLE_DECLARATIONS = "js_wrap_variable_declarations";
    public static final String VARIABLE_DECLARATIONS_ALIGN = "js_variable_declarations_align";

    // ES6 import/export
    public static final String WRAP_ES6_IMPORT_EXPORT = "js_wrap_es6_import_export";
    public static final String ES6_ALIGN_FROM_CLAUSES = "js_es6_align_from_clauses";

    // Decorators
    public static final String WRAP_FUNCTION_PARAMETER_DECORATORS = "js_wrap_function_parameter_decorators";
    public static final String WRAP_CLASS_DECORATORS = "js_wrap_class_decorators";
    public static final String WRAP_CLASS_FIELD_DECORATORS = "js_wrap_class_field_decorators";
    public static final String WRAP_CLASS_METHOD_DECORATORS = "js_wrap_class_method_decorators";

    // ==========================================
    // Property Keys - Blank Lines (media_1790587078488.png)
    // ==========================================
    public static final String BLANK_LINES_KEEP_IN_CODE = "js_blank_lines_keep_in_code";
    public static final String BLANK_LINES_KEEP_IN_DECLARATIONS = "js_blank_lines_keep_in_declarations";
    public static final String BLANK_LINES_AFTER_IMPORTS = "js_blank_lines_after_imports";
    public static final String BLANK_LINES_AROUND_CLASS = "js_blank_lines_around_class";
    public static final String BLANK_LINES_AROUND_FIELD = "js_blank_lines_around_field";
    public static final String BLANK_LINES_AROUND_METHOD = "js_blank_lines_around_method";
    public static final String BLANK_LINES_AROUND_FUNCTION = "js_blank_lines_around_function";

    // ==========================================
    // Property Keys - Punctuation (media_1790587087019.png)
    // ==========================================
    public static final String USE_SEMICOLON = "js_use_semicolon";
    public static final String SEMICOLON_SCOPE = "js_semicolon_scope";
    public static final String QUOTE_STYLE = "js_quote_style";
    public static final String QUOTE_SCOPE = "js_quote_scope";
    public static final String TRAILING_COMMA = "js_trailing_comma";

    public static final List<String> USE_SEMICOLON_OPTIONS = List.of(
            "Use",
            "Don't use"
    );

    public static final List<String> SEMICOLON_SCOPE_OPTIONS = List.of(
            "in code generated by IDE",
            "always"
    );

    public static final List<String> QUOTE_STYLE_OPTIONS = List.of(
            "double",
            "single",
            "none"
    );

    public static final List<String> QUOTE_SCOPE_OPTIONS = List.of(
            "in code generated by IDE",
            "always"
    );

    public static final List<String> TRAILING_COMMA_OPTIONS = List.of(
            "Keep",
            "Remove",
            "Add when multiline"
    );

    // ==========================================
    // Property Keys - Code Generation (media_1790587100814.png)
    // ==========================================
    public static final String FIELD_PREFIX = "js_field_prefix";
    public static final String PROPERTY_PREFIX = "js_property_prefix";
    public static final String FILENAME_CONVENTION = "js_filename_convention";
    public static final String CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN = "js_code_gen_line_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START = "js_code_gen_add_space_at_line_comment_start";
    public static final String CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN = "js_code_gen_block_comment_at_first_column";
    public static final String CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS = "js_code_gen_add_spaces_around_block_comments";

    public static final List<String> FILENAME_CONVENTION_OPTIONS = List.of(
            "Reuse case of current file",
            "kebab-case",
            "snake_case",
            "camelCase",
            "PascalCase"
    );

    // ==========================================
    // Property Keys - Imports (media_1790587112114.png)
    // ==========================================
    public static final String MERGE_IMPORTS_SAME_MODULE = "js_merge_imports_same_module";
    public static final String USE_RELATIVE_PATHS = "js_use_relative_paths";
    public static final String USE_DIRECTORY_IMPORT = "js_use_directory_import";
    public static final String USE_FILE_EXTENSION = "js_use_file_extension";
    public static final String USE_PATH_ALIASES = "js_use_path_aliases";
    public static final String DO_NOT_IMPORT_EXACTLY_FROM = "js_do_not_import_exactly_from";
    public static final String SORT_IMPORTED_MEMBERS = "js_sort_imported_members";
    public static final String SORT_IMPORTS_BY_MODULES = "js_sort_imports_by_modules";

    public static final List<String> USE_FILE_EXTENSION_OPTIONS = List.of(
            "Auto",
            "Always",
            "Never",
            "js"
    );

    public static final List<String> USE_PATH_ALIASES_OPTIONS = List.of(
            "Always",
            "Never",
            "Auto"
    );

    // ==========================================
    // Property Keys - Arrangement (media_1790587120119.png)
    // ==========================================
    public static final String GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER = "js_group_property_field_with_getter_setter";
    public static final String GROUP_FIELDS_WITH_ARROW_FUNCTIONS = "js_group_fields_with_arrow_functions";
    public static final String KEEP_OVERRIDDEN_METHODS_TOGETHER = "js_keep_overridden_methods_together";
    public static final String OVERRIDDEN_METHODS_ORDER = "js_overridden_methods_order";

    public static final List<String> OVERRIDDEN_METHODS_ORDER_OPTIONS = List.of(
            "keep order",
            "order by name"
    );

    // Arrangement Matching Rules
    private final List<String> matchingRules = new ArrayList<>();

    public JavaScriptCodeStyleSettings() {
        this("JavaScript");
    }

    public JavaScriptCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tab 1: Tabs and Indents (media_1790582906008.png)
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(4);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);
        setBoolean(INDENT_CHAINED_METHODS, true);
        setBoolean(INDENT_ALL_CHAINED_CALLS_IN_GROUP, false);

        // Tab 2: Spaces (media_1790584294419.png, media_1790584318346.png)
        setBoolean(SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_FUNCTION_CALL_PARENTHESES, false);
        setBoolean(SPACE_BEFORE_IF_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_FOR_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_WHILE_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_SWITCH_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_CATCH_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_FUNCTION_EXPRESSION_PARENTHESES, true);
        setBoolean(SPACE_BEFORE_ASYNC_ARROW_PARENTHESES, true);

        setBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, true);
        setBoolean(SPACE_AROUND_LOGICAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_EQUALITY_OPERATORS, true);
        setBoolean(SPACE_AROUND_RELATIONAL_OPERATORS, true);
        setBoolean(SPACE_AROUND_BITWISE_OPERATORS, true);
        setBoolean(SPACE_AROUND_ADDITIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, true);
        setBoolean(SPACE_AROUND_SHIFT_OPERATORS, true);
        setBoolean(SPACE_AROUND_UNARY_ADDITIVE_OPERATORS, false);
        setBoolean(SPACE_AROUND_ARROW_FUNCTION, true);
        setBoolean(SPACE_BEFORE_UNARY_NOT, false);
        setBoolean(SPACE_AFTER_UNARY_NOT, false);

        setBoolean(SPACE_BEFORE_FUNCTION_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_IF_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_ELSE_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_FOR_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_WHILE_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_DO_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_SWITCH_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_TRY_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_CATCH_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_FINALLY_LEFT_BRACE, true);
        setBoolean(SPACE_BEFORE_CLASS_LEFT_BRACE, true);

        setBoolean(SPACE_BEFORE_ELSE_KEYWORD, true);
        setBoolean(SPACE_BEFORE_WHILE_KEYWORD, true);
        setBoolean(SPACE_BEFORE_CATCH_KEYWORD, true);
        setBoolean(SPACE_BEFORE_FINALLY_KEYWORD, true);

        setBoolean(SPACES_WITHIN_INDEX_ACCESS_BRACKETS, false);
        setBoolean(SPACES_WITHIN_GROUPING_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_FUNCTION_DECLARATION_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_FUNCTION_CALL_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_IF_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_FOR_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_WHILE_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_SWITCH_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_CATCH_PARENTHESES, false);
        setBoolean(SPACES_WITHIN_OBJECT_LITERAL_BRACES, false);
        setBoolean(SPACES_WITHIN_ES6_IMPORT_EXPORT_BRACES, false);
        setBoolean(SPACES_WITHIN_ARRAY_BRACKETS, false);
        setBoolean(SPACES_WITHIN_INTERPOLATION_EXPRESSIONS, false);

        setBoolean(SPACE_BEFORE_TERNARY_QUESTION, true);
        setBoolean(SPACE_AFTER_TERNARY_QUESTION, true);
        setBoolean(SPACE_BEFORE_TERNARY_COLON, true);
        setBoolean(SPACE_AFTER_TERNARY_COLON, true);

        setBoolean(SPACE_BEFORE_COMMA, false);
        setBoolean(SPACE_AFTER_COMMA, true);
        setBoolean(SPACE_BEFORE_FOR_SEMICOLON, false);
        setBoolean(SPACE_BEFORE_PROPERTY_NAME_VALUE_SEPARATOR, false);
        setBoolean(SPACE_AFTER_PROPERTY_NAME_VALUE_SEPARATOR, true);
        setBoolean(SPACE_AFTER_REST_SPREAD, false);
        setBoolean(SPACE_BEFORE_GENERATOR_STAR, false);
        setBoolean(SPACE_AFTER_GENERATOR_STAR, true);

        setBoolean(FLOW_SPACE_BEFORE_TYPE_REFERENCE_COLON, false);
        setBoolean(FLOW_SPACE_AFTER_TYPE_REFERENCE_COLON, true);
        setBoolean(FLOW_OBJECT_LITERAL_TYPE_BRACES, false);
        setBoolean(FLOW_UNION_AND_INTERSECTION_TYPES, true);

        // Tab 3: Wrapping and Braces (media_1790584332985.png, media_1790584373383.png, media_1790584397014.png)
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "Default: None");

        setBoolean(KEEP_LINE_BREAKS, true);
        setBoolean(KEEP_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, false);
        setBoolean(KEEP_SIMPLE_METHODS_IN_ONE_LINE, false);

        setBoolean(WRAP_COMMENTS_AT_RIGHT_MARGIN, false);
        setBoolean(ALIGN_MULTILINE_COMMENTS, false);

        setString(BRACE_PLACEMENT_CLASS, "End of line");
        setString(BRACE_PLACEMENT_FUNCTION, "End of line");
        setString(BRACE_PLACEMENT_FUNCTION_EXPRESSION, "End of line");
        setString(BRACE_PLACEMENT_OTHER, "End of line");

        setString(WRAP_EXTENDS_LIST, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_EXTENDS_LIST, false);
        setString(WRAP_EXTENDS_KEYWORD, "Do not wrap");

        setString(WRAP_FUNCTION_PARAMETERS, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_FUNCTION_PARAMETERS, true);
        setBoolean(NEW_LINE_AFTER_FUNCTION_PARAMETERS_LPAREN, false);
        setBoolean(PLACE_RPAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, false);

        setString(WRAP_FUNCTION_ARGUMENTS, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_FUNCTION_ARGUMENTS, false);
        setBoolean(CALL_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, false);
        setBoolean(NEW_LINE_AFTER_FUNCTION_ARGUMENTS_LPAREN, false);
        setBoolean(PLACE_RPAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, false);

        setString(WRAP_CHAINED_METHOD_CALLS, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_CHAINED_METHODS, false);
        setBoolean(CHAINED_METHOD_DOT_ON_NEW_LINE, true);

        setString(IF_FORCE_BRACES, "Do not force");
        setBoolean(WRAP_IF_ELSE_ON_NEW_LINE, false);
        setBoolean(SPECIAL_ELSE_IF_TREATMENT, true);

        setString(WRAP_FOR_STATEMENT, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_FOR_STATEMENT, true);
        setBoolean(NEW_LINE_AFTER_FOR_LPAREN, false);
        setBoolean(PLACE_RPAREN_ON_NEW_LINE_FOR, false);
        setString(FOR_FORCE_BRACES, "Do not force");

        setString(WHILE_FORCE_BRACES, "Do not force");

        setString(DO_WHILE_FORCE_BRACES, "Do not force");
        setBoolean(WRAP_DO_WHILE_ON_NEW_LINE, false);

        setBoolean(WRAP_SWITCH_INDENT_CASE_BRANCHES, true);

        setBoolean(WRAP_TRY_CATCH_ON_NEW_LINE, false);
        setBoolean(WRAP_TRY_FINALLY_ON_NEW_LINE, false);

        setString(WRAP_BINARY_EXPRESSIONS, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_BINARY_EXPRESSIONS, false);
        setBoolean(BINARY_OPERATION_SIGN_ON_NEXT_LINE, false);
        setBoolean(NEW_LINE_AFTER_BINARY_LPAREN, false);
        setBoolean(PLACE_RPAREN_ON_NEW_LINE_BINARY, false);

        setString(WRAP_ASSIGNMENT_STATEMENT, "Do not wrap");
        setBoolean(ASSIGNMENT_SIGN_ON_NEXT_LINE, false);

        setString(WRAP_TERNARY_OPERATION, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_TERNARY_OPERATION, false);
        setBoolean(TERNARY_QUESTION_AND_COLON_ON_NEXT_LINE, false);

        setString(WRAP_ARRAYS, "Do not wrap");
        setBoolean(ALIGN_MULTILINE_ARRAYS, false);
        setBoolean(NEW_LINE_AFTER_ARRAY_LBRACKET, false);
        setBoolean(PLACE_RBRACKET_ON_NEW_LINE_ARRAY, false);

        setString(WRAP_OBJECTS, "Chop down if long");
        setString(OBJECTS_ALIGN, "Do not align");

        setString(WRAP_VARIABLE_DECLARATIONS, "Wrap if long");
        setString(VARIABLE_DECLARATIONS_ALIGN, "Do not align");

        setString(WRAP_ES6_IMPORT_EXPORT, "Chop down if long");
        setBoolean(ES6_ALIGN_FROM_CLAUSES, false);

        setString(WRAP_FUNCTION_PARAMETER_DECORATORS, "Do not wrap");
        setString(WRAP_CLASS_DECORATORS, "Wrap always");
        setString(WRAP_CLASS_FIELD_DECORATORS, "Do not wrap");
        setString(WRAP_CLASS_METHOD_DECORATORS, "Do not wrap");

        // Tab 4: Blank Lines (media_1790587078488.png)
        setInt(BLANK_LINES_KEEP_IN_CODE, 2);
        setInt(BLANK_LINES_KEEP_IN_DECLARATIONS, 2);
        setInt(BLANK_LINES_AFTER_IMPORTS, 1);
        setInt(BLANK_LINES_AROUND_CLASS, 1);
        setInt(BLANK_LINES_AROUND_FIELD, 0);
        setInt(BLANK_LINES_AROUND_METHOD, 1);
        setInt(BLANK_LINES_AROUND_FUNCTION, 1);

        // Tab 5: Punctuation (media_1790587087019.png)
        setString(USE_SEMICOLON, "Use");
        setString(SEMICOLON_SCOPE, "in code generated by IDE");
        setString(QUOTE_STYLE, "double");
        setString(QUOTE_SCOPE, "in code generated by IDE");
        setString(TRAILING_COMMA, "Keep");

        // Tab 6: Code Generation (media_1790587100814.png)
        setString(FIELD_PREFIX, "_");
        setString(PROPERTY_PREFIX, "");
        setString(FILENAME_CONVENTION, "Reuse case of current file");
        setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, false);
        setBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, true);
        setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true);
        setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false);

        // Tab 7: Imports (media_1790587112114.png)
        setBoolean(MERGE_IMPORTS_SAME_MODULE, true);
        setBoolean(USE_RELATIVE_PATHS, false);
        setBoolean(USE_DIRECTORY_IMPORT, true);
        setString(USE_FILE_EXTENSION, "Auto");
        setString(USE_PATH_ALIASES, "Always");
        setString(DO_NOT_IMPORT_EXACTLY_FROM, "rxjs,@angular/material/typings/**");
        setBoolean(SORT_IMPORTED_MEMBERS, true);
        setBoolean(SORT_IMPORTS_BY_MODULES, false);

        // Tab 8: Arrangement (media_1790587120119.png)
        setBoolean(GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER, true);
        setBoolean(GROUP_FIELDS_WITH_ARROW_FUNCTIONS, true);
        setBoolean(KEEP_OVERRIDDEN_METHODS_TOGETHER, false);
        setString(OVERRIDDEN_METHODS_ORDER, "keep order");

        matchingRules.clear();
        matchingRules.add("field, static");
        matchingRules.add("field");
        matchingRules.add("constructor");
        matchingRules.add("property, static");
        matchingRules.add("property");
        matchingRules.add("method, static");
        matchingRules.add("method");
    }

    @Override
    public JavaScriptCodeStyleSettings copy() {
        JavaScriptCodeStyleSettings copy = new JavaScriptCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setProperties(getAllProperties());

        copy.matchingRules.clear();
        copy.matchingRules.addAll(this.matchingRules);

        return copy;
    }

    public List<String> getMatchingRules() {
        return matchingRules;
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public boolean isIndentChainedMethods() {
        return getBoolean(INDENT_CHAINED_METHODS, true);
    }

    public void setIndentChainedMethods(boolean v) {
        setBoolean(INDENT_CHAINED_METHODS, v);
    }

    public boolean isIndentAllChainedCallsInGroup() {
        return getBoolean(INDENT_ALL_CHAINED_CALLS_IN_GROUP, false);
    }

    public void setIndentAllChainedCallsInGroup(boolean v) {
        setBoolean(INDENT_ALL_CHAINED_CALLS_IN_GROUP, v);
    }

    public boolean isSpaceBeforeIfParentheses() {
        return getBoolean(SPACE_BEFORE_IF_PARENTHESES, true);
    }

    public void setSpaceBeforeIfParentheses(boolean v) {
        setBoolean(SPACE_BEFORE_IF_PARENTHESES, v);
    }

    public boolean isSpaceBeforeFunctionDeclarationParentheses() {
        return getBoolean(SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES, false);
    }

    public void setSpaceBeforeFunctionDeclarationParentheses(boolean v) {
        setBoolean(SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES, v);
    }

    public boolean isSpaceAroundAssignmentOperators() {
        return getBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, true);
    }

    public void setSpaceAroundAssignmentOperators(boolean v) {
        setBoolean(SPACE_AROUND_ASSIGNMENT_OPERATORS, v);
    }

    public boolean isSpaceBeforeFunctionLeftBrace() {
        return getBoolean(SPACE_BEFORE_FUNCTION_LEFT_BRACE, true);
    }

    public void setSpaceBeforeFunctionLeftBrace(boolean v) {
        setBoolean(SPACE_BEFORE_FUNCTION_LEFT_BRACE, v);
    }

    public boolean isSpacesWithinCodeBraces() {
        return getBoolean(SPACES_WITHIN_OBJECT_LITERAL_BRACES, false);
    }

    public void setSpacesWithinCodeBraces(boolean v) {
        setBoolean(SPACES_WITHIN_OBJECT_LITERAL_BRACES, v);
    }

    public boolean isSpaceBeforeTernaryQuestion() {
        return getBoolean(SPACE_BEFORE_TERNARY_QUESTION, true);
    }

    public void setSpaceBeforeTernaryQuestion(boolean v) {
        setBoolean(SPACE_BEFORE_TERNARY_QUESTION, v);
    }

    public boolean isSpaceAfterComma() {
        return getBoolean(SPACE_AFTER_COMMA, true);
    }

    public void setSpaceAfterComma(boolean v) {
        setBoolean(SPACE_AFTER_COMMA, v);
    }

    public boolean isSpaceBeforeComma() {
        return getBoolean(SPACE_BEFORE_COMMA, false);
    }

    public void setSpaceBeforeComma(boolean v) {
        setBoolean(SPACE_BEFORE_COMMA, v);
    }

    public String getUseSemicolon() {
        return getString(USE_SEMICOLON, "Use");
    }

    public void setUseSemicolon(String v) {
        setString(USE_SEMICOLON, v);
    }

    public String getSemicolonScope() {
        return getString(SEMICOLON_SCOPE, "in code generated by IDE");
    }

    public void setSemicolonScope(String v) {
        setString(SEMICOLON_SCOPE, v);
    }

    public String getQuoteStyle() {
        return getString(QUOTE_STYLE, "double");
    }

    public void setQuoteStyle(String v) {
        setString(QUOTE_STYLE, v);
    }

    public String getQuoteScope() {
        return getString(QUOTE_SCOPE, "in code generated by IDE");
    }

    public void setQuoteScope(String v) {
        setString(QUOTE_SCOPE, v);
    }

    public String getTrailingComma() {
        return getString(TRAILING_COMMA, "Keep");
    }

    public void setTrailingComma(String v) {
        setString(TRAILING_COMMA, v);
    }

    public String getBracePlacementFunction() {
        return getString(BRACE_PLACEMENT_FUNCTION, "End of line");
    }

    public void setBracePlacementFunction(String v) {
        setString(BRACE_PLACEMENT_FUNCTION, v);
    }

    public int getBlankLinesKeepInCode() {
        return getInt(BLANK_LINES_KEEP_IN_CODE, 2);
    }

    public void setBlankLinesKeepInCode(int v) {
        setInt(BLANK_LINES_KEEP_IN_CODE, v);
    }

    public int getBlankLinesKeepInDeclarations() {
        return getInt(BLANK_LINES_KEEP_IN_DECLARATIONS, 2);
    }

    public void setBlankLinesKeepInDeclarations(int v) {
        setInt(BLANK_LINES_KEEP_IN_DECLARATIONS, v);
    }

    public int getBlankLinesAfterImports() {
        return getInt(BLANK_LINES_AFTER_IMPORTS, 1);
    }

    public void setBlankLinesAfterImports(int v) {
        setInt(BLANK_LINES_AFTER_IMPORTS, v);
    }

    public int getBlankLinesAroundClass() {
        return getInt(BLANK_LINES_AROUND_CLASS, 1);
    }

    public void setBlankLinesAroundClass(int v) {
        setInt(BLANK_LINES_AROUND_CLASS, v);
    }

    public int getBlankLinesAroundField() {
        return getInt(BLANK_LINES_AROUND_FIELD, 0);
    }

    public void setBlankLinesAroundField(int v) {
        setInt(BLANK_LINES_AROUND_FIELD, v);
    }

    public int getBlankLinesAroundMethod() {
        return getInt(BLANK_LINES_AROUND_METHOD, 1);
    }

    public void setBlankLinesAroundMethod(int v) {
        setInt(BLANK_LINES_AROUND_METHOD, v);
    }

    public int getBlankLinesAroundFunction() {
        return getInt(BLANK_LINES_AROUND_FUNCTION, 1);
    }

    public void setBlankLinesAroundFunction(int v) {
        setInt(BLANK_LINES_AROUND_FUNCTION, v);
    }

    public String getFieldPrefix() {
        return getString(FIELD_PREFIX, "_");
    }

    public void setFieldPrefix(String v) {
        setString(FIELD_PREFIX, v);
    }

    public String getPropertyPrefix() {
        return getString(PROPERTY_PREFIX, "");
    }

    public void setPropertyPrefix(String v) {
        setString(PROPERTY_PREFIX, v);
    }

    public String getFilenameConvention() {
        return getString(FILENAME_CONVENTION, "Reuse case of current file");
    }

    public void setFilenameConvention(String v) {
        setString(FILENAME_CONVENTION, v);
    }

    public boolean isCodeGenLineCommentAtFirstColumn() {
        return getBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, false);
    }

    public void setCodeGenLineCommentAtFirstColumn(boolean v) {
        setBoolean(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, v);
    }

    public boolean isCodeGenAddSpaceAtLineCommentStart() {
        return getBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, true);
    }

    public void setCodeGenAddSpaceAtLineCommentStart(boolean v) {
        setBoolean(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, v);
    }

    public boolean isCodeGenBlockCommentAtFirstColumn() {
        return getBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, true);
    }

    public void setCodeGenBlockCommentAtFirstColumn(boolean v) {
        setBoolean(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, v);
    }

    public boolean isCodeGenAddSpacesAroundBlockComments() {
        return getBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, false);
    }

    public void setCodeGenAddSpacesAroundBlockComments(boolean v) {
        setBoolean(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, v);
    }

    public boolean isMergeImportsSameModule() {
        return getBoolean(MERGE_IMPORTS_SAME_MODULE, true);
    }

    public void setMergeImportsSameModule(boolean v) {
        setBoolean(MERGE_IMPORTS_SAME_MODULE, v);
    }

    public boolean isUseRelativePaths() {
        return getBoolean(USE_RELATIVE_PATHS, false);
    }

    public void setUseRelativePaths(boolean v) {
        setBoolean(USE_RELATIVE_PATHS, v);
    }

    public boolean isUseDirectoryImport() {
        return getBoolean(USE_DIRECTORY_IMPORT, true);
    }

    public void setUseDirectoryImport(boolean v) {
        setBoolean(USE_DIRECTORY_IMPORT, v);
    }

    public String getUseFileExtension() {
        return getString(USE_FILE_EXTENSION, "Auto");
    }

    public void setUseFileExtension(String v) {
        setString(USE_FILE_EXTENSION, v);
    }

    public String getUsePathAliases() {
        return getString(USE_PATH_ALIASES, "Always");
    }

    public void setUsePathAliases(String v) {
        setString(USE_PATH_ALIASES, v);
    }

    public String getDoNotImportExactlyFrom() {
        return getString(DO_NOT_IMPORT_EXACTLY_FROM, "rxjs,@angular/material/typings/**");
    }

    public void setDoNotImportExactlyFrom(String v) {
        setString(DO_NOT_IMPORT_EXACTLY_FROM, v);
    }

    public boolean isSortImportedMembers() {
        return getBoolean(SORT_IMPORTED_MEMBERS, true);
    }

    public void setSortImportedMembers(boolean v) {
        setBoolean(SORT_IMPORTED_MEMBERS, v);
    }

    public boolean isSortImportsByModules() {
        return getBoolean(SORT_IMPORTS_BY_MODULES, false);
    }

    public void setSortImportsByModules(boolean v) {
        setBoolean(SORT_IMPORTS_BY_MODULES, v);
    }

    public boolean isGroupPropertyFieldWithGetterSetter() {
        return getBoolean(GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER, true);
    }

    public void setGroupPropertyFieldWithGetterSetter(boolean v) {
        setBoolean(GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER, v);
    }

    public boolean isGroupFieldsWithArrowFunctions() {
        return getBoolean(GROUP_FIELDS_WITH_ARROW_FUNCTIONS, true);
    }

    public void setGroupFieldsWithArrowFunctions(boolean v) {
        setBoolean(GROUP_FIELDS_WITH_ARROW_FUNCTIONS, v);
    }

    public boolean isKeepOverriddenMethodsTogether() {
        return getBoolean(KEEP_OVERRIDDEN_METHODS_TOGETHER, false);
    }

    public void setKeepOverriddenMethodsTogether(boolean v) {
        setBoolean(KEEP_OVERRIDDEN_METHODS_TOGETHER, v);
    }

    public String getOverriddenMethodsOrder() {
        return getString(OVERRIDDEN_METHODS_ORDER, "keep order");
    }

    public void setOverriddenMethodsOrder(String v) {
        setString(OVERRIDDEN_METHODS_ORDER, v);
    }

    // ==========================================
    // Sample Codes matching media_1790582906008.png, media_1790584294419.png, media_1790584332985.png
    // Strictly brand-isolated.
    // ==========================================
    public static final String SAMPLE_TABS_AND_INDENTS = """
foo(
    "demo",
    {
        title: "Demo",
        width: 100
    },
    function () {
        object.firstCall({
            a: 'a',
            b: 'b'
        })
            .secondCall();
    }
);
""";

    public static final String SAMPLE_SPACES = """
function* fibonacci(current = 1, next = 1) {
    yield current;
    yield* fibonacci(next, current + next);
}

let [first, second, ...rest] = take(fibonacci(), 10)

function foo(x, y, z) {
    var i = 0;
    var x = {0: "zero", 1: "one"};
    var a = [0, 1, 2];
    var foo = function () {
    }
    var asyncFoo = async (x, y, z) => {
    }
    var v = x.map(s => s.length);
    if (!i > 10) {
        for (var j = 0; j < 10; j++) {
            switch (j) {
                case 0:
                    value = "zero";
                    break;
                case 1:
                    value = "one";
                    break;
            }
        }
        var c = j > 5 ? "GT 5" : "LE 5";
    } else {
        var j = 0;
        try {
            while (j < 10) {
                if (i == j || j > 5) {
                    a[j] = 1 + j * 12;
                }
                i = (j << 2) & 4;
                j++;
            }
            do {
                j--;
            } while (j > 0)
        } catch (e) {
            alert("Failure: " + e.message);
        } finally {
            reset(a, 1);
        }
    }
}
""";

    public static final String SAMPLE_WRAPPING = """
import {Component} from 'react'
import {Rx} from 'rxjs/Observable'
import {
    property1,
    property2,
    property3
} from './myModule.js'

@ClassDecorator(param1, param2)
@ClassDecoratorExt
class Foo extends BarComponent implements BazService, QuuxProvider {

    @MethodDecorator(param1) @MethodDecoratorExt
    foo(@ParamDecoratorExt @Deprecated prop1, @ParamDecoratorExt(property1) prop2) {
    }

    @FieldDecorator
    @FieldDecoratorExt
    field1 = 1;
}

function buzz() {
    return 0;
}

var x = 1, y = 2,
    foregroundColor = 'transparent',
    highlightColor = 'lime',
    font = 'Arial';

/*
Multiline
 C-style
  Comment
*/
var myLink = {img: "btn.gif"},
    local = true,
    initial = -1;
width = 400
height = 300

var foo = {
    numbers: ['one', 'two', 'three', 'four', 'five', 'six'],
    data: {
        a: {
            id: 123,
            type: "String",
            isAvailable: true
        },
        b: {id: 456, type: "Int"}
    },
// fBar : function (x,y);
    fOne: function (a, b, c, d, e, f, g, h) {
        var x = a + b + c + d + e + f + g + h;
        fTwo(a, b, c, fThree(d, e, f, g, h));
        var z = a == 'Some string' ? 'yes' : 'no';
        z = a == 10 ? 'yes' : 'no';
        var colors = ['red', 'green', 'blue', 'black', 'white', 'gray'];
        for (j = 0; j < 2; j++) i = a;
        for (var i = 0; i < colors.length; i++)
            var colorString = this.numbers[i];
    },

    chainedCallSample: function (a, b, c, d, e, f) {
        chainRoot.firstCall(a, b, c, d, e, f, g).secondCall(a, b, c, d).thirdCall(a, b, c, d).fourCall(a, b);
        chainRoot.x().y()
    },
    do {
        number = number + 1;
    } while (number < 10);
    return d;
},

fThree: function ({
                      strA,
                      strB,
                      strC,
                      strD
                  }, strE) {
    var number = prompt("Enter a number:", 0);
    switch (number) {
        case 0:
            alert("Zero");
            break;
        case 1:
            alert("One");
            break;
    }
    try {
        a[2] = 10;
    } catch (e) {
        alert("Failure: " + e.message);
    }
    return strA + strB + strC + strD + strE;
}
};
""";

    public static final String SAMPLE_BLANK_LINES = """
/**
 * This is a sample file
 */
import {Component} from 'React'
import {add, subtract} from 'utils';

class Foo {
    field1 = 1;
    field2 = 2;

    foo() {
        console.log('foo')
    }

    static bar() {
        function hello(n) {
            console.log('hello ' + n)
        }

        var x = 1;

        while (x < 10) {
            hello(x)
        }
    }
}
""";

    public static final String SAMPLE_PUNCTUATION = """
const myLink = {
    img: "btn.gif",
    text: text,
    width: 128
}

const cssClasses = ["bold", "red",]
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "JavaScript";
            }

            @Override
            public String getDisplayName() {
                return "JavaScript";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of(
                        "Tabs and Indents",
                        "Spaces",
                        "Wrapping and Braces",
                        "Blank Lines",
                        "Punctuation",
                        "Code Generation",
                        "Imports",
                        "Arrangement"
                );
            }

            @Override
            public boolean hasPreview(String tabName) {
                return !"Code Generation".equals(tabName) && !"Imports".equals(tabName) && !"Arrangement".equals(tabName);
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new JavaScriptCodeStyleSettings("JavaScript");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public String getSampleCode(String tabName) {
                if ("Spaces".equals(tabName)) {
                    return SAMPLE_SPACES;
                }
                if ("Wrapping and Braces".equals(tabName)) {
                    return SAMPLE_WRAPPING;
                }
                if ("Blank Lines".equals(tabName)) {
                    return SAMPLE_BLANK_LINES;
                }
                if ("Punctuation".equals(tabName)) {
                    return SAMPLE_PUNCTUATION;
                }
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup indents = CodeStyleGroup.flat("Tabs and Indents");
                    indents.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    indents.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    indents.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    indents.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    indents.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 4));
                    indents.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    indents.addOption(CodeStyleOption.checkbox(INDENT_CHAINED_METHODS, "Indent chained methods", true));
                    indents.addOption(CodeStyleOption.checkbox(INDENT_ALL_CHAINED_CALLS_IN_GROUP, "Indent all chained calls in a group", false));
                    customizer.addGroup(indents);

                } else if ("Spaces".equals(tabName)) {
                    // 1. Before parentheses (media_1790584294419.png)
                    CodeStyleGroup beforeParen = CodeStyleGroup.collapsible("Before parentheses");
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FUNCTION_DECLARATION_PARENTHESES, "Function declaration parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FUNCTION_CALL_PARENTHESES, "Function call parentheses", false));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_IF_PARENTHESES, "'if' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_PARENTHESES, "'for' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_PARENTHESES, "'while' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SWITCH_PARENTHESES, "'switch' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_PARENTHESES, "'catch' parentheses", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FUNCTION_EXPRESSION_PARENTHESES, "In function expression", true));
                    beforeParen.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ASYNC_ARROW_PARENTHESES, "In async arrow function", true));
                    customizer.addGroup(beforeParen);

                    // 2. Around operators
                    CodeStyleGroup aroundOps = CodeStyleGroup.collapsible("Around operators");
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ASSIGNMENT_OPERATORS, "Assignment operators (=, +=, ...)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_LOGICAL_OPERATORS, "Logical operators (&&, ||)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_EQUALITY_OPERATORS, "Equality operators (==, !=)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_RELATIONAL_OPERATORS, "Relational operators (<, >, <=, >=)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_BITWISE_OPERATORS, "Bitwise operators (&, |, ^)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ADDITIVE_OPERATORS, "Additive operators (+, -)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_MULTIPLICATIVE_OPERATORS, "Multiplicative operators (*, /, %)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_SHIFT_OPERATORS, "Shift operators (<<, >>, >>>)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_UNARY_ADDITIVE_OPERATORS, "Unary additive operators (+,-,++,--)", false));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AROUND_ARROW_FUNCTION, "Arrow function (=>)", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_UNARY_NOT, "Before unary 'not' (!) and '!!'", false));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACE_AFTER_UNARY_NOT, "After unary 'not' (!) and '!!'", false));
                    customizer.addGroup(aroundOps);

                    // 3. Before left brace
                    CodeStyleGroup beforeBrace = CodeStyleGroup.collapsible("Before left brace");
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FUNCTION_LEFT_BRACE, "Function left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_IF_LEFT_BRACE, "'if' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ELSE_LEFT_BRACE, "'else' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_LEFT_BRACE, "'for' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_LEFT_BRACE, "'while' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_DO_LEFT_BRACE, "'do' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_SWITCH_LEFT_BRACE, "'switch' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TRY_LEFT_BRACE, "'try' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_LEFT_BRACE, "'catch' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FINALLY_LEFT_BRACE, "'finally' left brace", true));
                    beforeBrace.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CLASS_LEFT_BRACE, "Class left brace", true));
                    customizer.addGroup(beforeBrace);

                    // 4. Before keywords
                    CodeStyleGroup beforeKeywords = CodeStyleGroup.collapsible("Before keywords");
                    beforeKeywords.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_ELSE_KEYWORD, "'else' keyword", true));
                    beforeKeywords.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_WHILE_KEYWORD, "'while' keyword", true));
                    beforeKeywords.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_CATCH_KEYWORD, "'catch' keyword", true));
                    beforeKeywords.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FINALLY_KEYWORD, "'finally' keyword", true));
                    customizer.addGroup(beforeKeywords);

                    // 5. Within (media_1790584294419.png)
                    CodeStyleGroup within = CodeStyleGroup.collapsible("Within");
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_INDEX_ACCESS_BRACKETS, "Index access brackets", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_GROUPING_PARENTHESES, "Grouping parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_FUNCTION_DECLARATION_PARENTHESES, "Function declaration parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_FUNCTION_CALL_PARENTHESES, "Function call parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_IF_PARENTHESES, "'if' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_FOR_PARENTHESES, "'for' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_WHILE_PARENTHESES, "'while' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_SWITCH_PARENTHESES, "'switch' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_CATCH_PARENTHESES, "'catch' parentheses", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_OBJECT_LITERAL_BRACES, "Object literal braces", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_ES6_IMPORT_EXPORT_BRACES, "ES6 import/export braces", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_ARRAY_BRACKETS, "Array brackets", false));
                    within.addOption(CodeStyleOption.checkbox(SPACES_WITHIN_INTERPOLATION_EXPRESSIONS, "Interpolation expressions", false));
                    customizer.addGroup(within);

                    // 6. In ternary operator (?:)
                    CodeStyleGroup ternary = CodeStyleGroup.collapsible("In ternary operator (?:)");
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TERNARY_QUESTION, "Before '?'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_AFTER_TERNARY_QUESTION, "After '?'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_TERNARY_COLON, "Before ':'", true));
                    ternary.addOption(CodeStyleOption.checkbox(SPACE_AFTER_TERNARY_COLON, "After ':'", true));
                    customizer.addGroup(ternary);

                    // 7. Other (media_1790584318346.png)
                    CodeStyleGroup other = CodeStyleGroup.collapsible("Other");
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COMMA, "Before comma", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COMMA, "After comma", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_FOR_SEMICOLON, "Before 'for' semicolon", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_PROPERTY_NAME_VALUE_SEPARATOR, "Before property name-value separator ':'", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_PROPERTY_NAME_VALUE_SEPARATOR, "After property name-value separator ':'", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_REST_SPREAD, "After '...' in rest/spread", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_GENERATOR_STAR, "Before '*' in generator", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_GENERATOR_STAR, "After '*' in generator", true));
                    customizer.addGroup(other);

                    // 8. In Flow (media_1790584318346.png)
                    CodeStyleGroup flow = CodeStyleGroup.collapsible("In Flow");
                    flow.addOption(CodeStyleOption.checkbox(FLOW_SPACE_BEFORE_TYPE_REFERENCE_COLON, "Before type reference colon ':'", false));
                    flow.addOption(CodeStyleOption.checkbox(FLOW_SPACE_AFTER_TYPE_REFERENCE_COLON, "After type reference colon ':'", true));
                    flow.addOption(CodeStyleOption.checkbox(FLOW_OBJECT_LITERAL_TYPE_BRACES, "Object literal type braces", false));
                    flow.addOption(CodeStyleOption.checkbox(FLOW_UNION_AND_INTERSECTION_TYPES, "Union and intersection types", true));
                    customizer.addGroup(flow);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    // General / Top level
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.number(HARD_WRAP_AT, "Hard wrap at:", 120));
                    general.addOption(CodeStyleOption.combo(
                            WRAP_ON_TYPING,
                            "Wrap on typing",
                            List.of("Default: No", "Yes", "No"),
                            "Default: No"
                    ));
                    general.addOption(CodeStyleOption.combo(
                            VISUAL_GUIDES,
                            "Visual guides",
                            List.of("Default: None", "None", "80", "120"),
                            "Default: None"
                    ));
                    customizer.addGroup(general);

                    // Keep when reformatting (media_1790584332985.png)
                    CodeStyleGroup keep = CodeStyleGroup.collapsible("Keep when reformatting");
                    keep.addOption(CodeStyleOption.checkbox(KEEP_LINE_BREAKS, "Line breaks", true));
                    keep.addOption(CodeStyleOption.checkbox(KEEP_COMMENT_AT_FIRST_COLUMN, "Comment at first column", true));
                    keep.addOption(CodeStyleOption.checkbox(KEEP_SIMPLE_BLOCKS_IN_ONE_LINE, "Simple blocks in one line", false));
                    keep.addOption(CodeStyleOption.checkbox(KEEP_SIMPLE_METHODS_IN_ONE_LINE, "Simple methods in one line", false));
                    customizer.addGroup(keep);

                    // Comments
                    CodeStyleGroup comments = CodeStyleGroup.collapsible("Comments");
                    comments.addOption(CodeStyleOption.checkbox(WRAP_COMMENTS_AT_RIGHT_MARGIN, "Wrap at right margin", false));
                    comments.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_COMMENTS, "Align multiline", false));
                    customizer.addGroup(comments);

                    // Braces placement
                    CodeStyleGroup placement = CodeStyleGroup.collapsible("Braces placement");
                    placement.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_CLASS, "In class declaration", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    placement.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_FUNCTION, "In function declaration", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    placement.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_FUNCTION_EXPRESSION, "In function expression", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    placement.addOption(CodeStyleOption.combo(BRACE_PLACEMENT_OTHER, "Other", BRACE_PLACEMENT_OPTIONS, "End of line"));
                    customizer.addGroup(placement);

                    // Extends list
                    CodeStyleGroup extendsList = CodeStyleGroup.collapsibleWithCombo("Extends list", WRAP_EXTENDS_LIST, WRAP_OPTIONS, "Do not wrap");
                    extendsList.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_EXTENDS_LIST, "Align when multiline", false));
                    customizer.addGroup(extendsList);

                    // Extends keyword
                    CodeStyleGroup extendsKw = CodeStyleGroup.flat("Extends keyword");
                    extendsKw.addOption(CodeStyleOption.combo(WRAP_EXTENDS_KEYWORD, "Extends keyword", List.of("Do not wrap", "Wrap if long", "Wrap always"), "Do not wrap"));
                    customizer.addGroup(extendsKw);

                    // Function declaration parameters
                    CodeStyleGroup fnParams = CodeStyleGroup.collapsibleWithCombo("Function declaration parameters", WRAP_FUNCTION_PARAMETERS, WRAP_OPTIONS, "Do not wrap");
                    fnParams.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_FUNCTION_PARAMETERS, "Align when multiline", true));
                    fnParams.addOption(CodeStyleOption.checkbox(NEW_LINE_AFTER_FUNCTION_PARAMETERS_LPAREN, "New line after '('", false));
                    fnParams.addOption(CodeStyleOption.checkbox(PLACE_RPAREN_ON_NEW_LINE_FUNCTION_PARAMETERS, "Place ')' on new line", false));
                    customizer.addGroup(fnParams);

                    // Function call arguments
                    CodeStyleGroup fnArgs = CodeStyleGroup.collapsibleWithCombo("Function call arguments", WRAP_FUNCTION_ARGUMENTS, WRAP_OPTIONS, "Do not wrap");
                    fnArgs.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_FUNCTION_ARGUMENTS, "Align when multiline", false));
                    fnArgs.addOption(CodeStyleOption.checkbox(CALL_ARGUMENTS_TAKE_PRIORITY_OVER_CALL_CHAIN, "Take priority over call chain wrapping", false));
                    fnArgs.addOption(CodeStyleOption.checkbox(NEW_LINE_AFTER_FUNCTION_ARGUMENTS_LPAREN, "New line after '('", false));
                    fnArgs.addOption(CodeStyleOption.checkbox(PLACE_RPAREN_ON_NEW_LINE_FUNCTION_ARGUMENTS, "Place ')' on new line", false));
                    customizer.addGroup(fnArgs);

                    // Chained method calls
                    CodeStyleGroup chained = CodeStyleGroup.collapsibleWithCombo("Chained method calls", WRAP_CHAINED_METHOD_CALLS, WRAP_OPTIONS, "Do not wrap");
                    chained.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_CHAINED_METHODS, "Align when multiline", false));
                    chained.addOption(CodeStyleOption.checkbox(CHAINED_METHOD_DOT_ON_NEW_LINE, "'.' on new line", true));
                    customizer.addGroup(chained);

                    // 'if()' statement (media_1790584373383.png)
                    CodeStyleGroup ifStmt = CodeStyleGroup.collapsible("'if()' statement");
                    ifStmt.addOption(CodeStyleOption.combo(IF_FORCE_BRACES, "Force braces", FORCE_BRACE_OPTIONS, "Do not force"));
                    ifStmt.addOption(CodeStyleOption.checkbox(WRAP_IF_ELSE_ON_NEW_LINE, "'else' on new line", false));
                    ifStmt.addOption(CodeStyleOption.checkbox(SPECIAL_ELSE_IF_TREATMENT, "Special 'else if' treatment", true));
                    customizer.addGroup(ifStmt);

                    // 'for()' statement
                    CodeStyleGroup forStmt = CodeStyleGroup.collapsibleWithCombo("'for()' statement", WRAP_FOR_STATEMENT, WRAP_OPTIONS, "Do not wrap");
                    forStmt.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_FOR_STATEMENT, "Align when multiline", true));
                    forStmt.addOption(CodeStyleOption.checkbox(NEW_LINE_AFTER_FOR_LPAREN, "New line after '('", false));
                    forStmt.addOption(CodeStyleOption.checkbox(PLACE_RPAREN_ON_NEW_LINE_FOR, "Place ')' on new line", false));
                    forStmt.addOption(CodeStyleOption.combo(FOR_FORCE_BRACES, "Force braces", FORCE_BRACE_OPTIONS, "Do not force"));
                    customizer.addGroup(forStmt);

                    // 'while()' statement
                    CodeStyleGroup whileStmt = CodeStyleGroup.collapsible("'while()' statement");
                    whileStmt.addOption(CodeStyleOption.combo(WHILE_FORCE_BRACES, "Force braces", FORCE_BRACE_OPTIONS, "Do not force"));
                    customizer.addGroup(whileStmt);

                    // 'do ... while()' statement
                    CodeStyleGroup doWhileStmt = CodeStyleGroup.collapsible("'do ... while()' statement");
                    doWhileStmt.addOption(CodeStyleOption.combo(DO_WHILE_FORCE_BRACES, "Force braces", FORCE_BRACE_OPTIONS, "Do not force"));
                    doWhileStmt.addOption(CodeStyleOption.checkbox(WRAP_DO_WHILE_ON_NEW_LINE, "'while' on new line", false));
                    customizer.addGroup(doWhileStmt);

                    // 'switch' statement
                    CodeStyleGroup switchStmt = CodeStyleGroup.collapsible("'switch' statement");
                    switchStmt.addOption(CodeStyleOption.checkbox(WRAP_SWITCH_INDENT_CASE_BRANCHES, "Indent 'case' branches", true));
                    customizer.addGroup(switchStmt);

                    // 'try' statement
                    CodeStyleGroup tryStmt = CodeStyleGroup.collapsible("'try' statement");
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_TRY_CATCH_ON_NEW_LINE, "'catch' on new line", false));
                    tryStmt.addOption(CodeStyleOption.checkbox(WRAP_TRY_FINALLY_ON_NEW_LINE, "'finally' on new line", false));
                    customizer.addGroup(tryStmt);

                    // Binary expressions
                    CodeStyleGroup binary = CodeStyleGroup.collapsibleWithCombo("Binary expressions", WRAP_BINARY_EXPRESSIONS, WRAP_OPTIONS, "Do not wrap");
                    binary.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_BINARY_EXPRESSIONS, "Align when multiline", false));
                    binary.addOption(CodeStyleOption.checkbox(BINARY_OPERATION_SIGN_ON_NEXT_LINE, "Operation sign on next line", false));
                    binary.addOption(CodeStyleOption.checkbox(NEW_LINE_AFTER_BINARY_LPAREN, "New line after '('", false));
                    binary.addOption(CodeStyleOption.checkbox(PLACE_RPAREN_ON_NEW_LINE_BINARY, "Place ')' on new line", false));
                    customizer.addGroup(binary);

                    // Assignment statement
                    CodeStyleGroup assign = CodeStyleGroup.collapsibleWithCombo("Assignment statement", WRAP_ASSIGNMENT_STATEMENT, WRAP_OPTIONS, "Do not wrap");
                    assign.addOption(CodeStyleOption.checkbox(ASSIGNMENT_SIGN_ON_NEXT_LINE, "Assignment sign on next line", false));
                    customizer.addGroup(assign);

                    // Ternary operation
                    CodeStyleGroup ternaryWrap = CodeStyleGroup.collapsibleWithCombo("Ternary operation", WRAP_TERNARY_OPERATION, WRAP_OPTIONS, "Do not wrap");
                    ternaryWrap.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_TERNARY_OPERATION, "Align when multiline", false));
                    ternaryWrap.addOption(CodeStyleOption.checkbox(TERNARY_QUESTION_AND_COLON_ON_NEXT_LINE, "'?' and ':' signs on next line", false));
                    customizer.addGroup(ternaryWrap);

                    // Arrays (media_1790584397014.png)
                    CodeStyleGroup arrays = CodeStyleGroup.collapsibleWithCombo("Arrays", WRAP_ARRAYS, WRAP_OPTIONS, "Do not wrap");
                    arrays.addOption(CodeStyleOption.checkbox(ALIGN_MULTILINE_ARRAYS, "Align when multiline", false));
                    arrays.addOption(CodeStyleOption.checkbox(NEW_LINE_AFTER_ARRAY_LBRACKET, "New line after '['", false));
                    arrays.addOption(CodeStyleOption.checkbox(PLACE_RBRACKET_ON_NEW_LINE_ARRAY, "Place ']' on new line", false));
                    customizer.addGroup(arrays);

                    // Objects
                    CodeStyleGroup objects = CodeStyleGroup.collapsibleWithCombo("Objects", WRAP_OBJECTS, WRAP_OPTIONS, "Chop down if long");
                    objects.addOption(CodeStyleOption.combo(OBJECTS_ALIGN, "Align", List.of("Do not align", "On colon", "On value"), "Do not align"));
                    customizer.addGroup(objects);

                    // Variable declarations
                    CodeStyleGroup varDecl = CodeStyleGroup.collapsibleWithCombo("Variable declarations", WRAP_VARIABLE_DECLARATIONS, WRAP_OPTIONS, "Wrap if long");
                    varDecl.addOption(CodeStyleOption.combo(VARIABLE_DECLARATIONS_ALIGN, "Align", List.of("Do not align", "When multiline"), "Do not align"));
                    customizer.addGroup(varDecl);

                    // ES6 import/export
                    CodeStyleGroup es6 = CodeStyleGroup.collapsibleWithCombo("ES6 import/export", WRAP_ES6_IMPORT_EXPORT, WRAP_OPTIONS, "Chop down if long");
                    es6.addOption(CodeStyleOption.checkbox(ES6_ALIGN_FROM_CLAUSES, "Align 'from' clauses", false));
                    customizer.addGroup(es6);

                    // Decorators (media_1790584397014.png)
                    CodeStyleGroup decorators = CodeStyleGroup.flat("Decorators");
                    decorators.addOption(CodeStyleOption.combo(WRAP_FUNCTION_PARAMETER_DECORATORS, "Function parameter decorators", WRAP_OPTIONS, "Do not wrap"));
                    decorators.addOption(CodeStyleOption.combo(WRAP_CLASS_DECORATORS, "Class decorators", WRAP_OPTIONS, "Wrap always"));
                    decorators.addOption(CodeStyleOption.combo(WRAP_CLASS_FIELD_DECORATORS, "Class field decorators", WRAP_OPTIONS, "Do not wrap"));
                    decorators.addOption(CodeStyleOption.combo(WRAP_CLASS_METHOD_DECORATORS, "Class method decorators", WRAP_OPTIONS, "Do not wrap"));
                    customizer.addGroup(decorators);

                } else if ("Blank Lines".equals(tabName)) {
                    CodeStyleGroup keep = CodeStyleGroup.divider("Keep maximum blank lines");
                    keep.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_CODE, "In code:", 2));
                    customizer.addGroup(keep);

                    CodeStyleGroup min = CodeStyleGroup.divider("Minimum blank lines");
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AFTER_IMPORTS, "After imports:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_CLASS, "Around class:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_FIELD, "Around field:", 0));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_METHOD, "Around method:", 1));
                    min.addOption(CodeStyleOption.number(BLANK_LINES_AROUND_FUNCTION, "Around function:", 1));
                    customizer.addGroup(min);

                } else if ("Punctuation".equals(tabName)) {
                    CodeStyleGroup punct = CodeStyleGroup.divider("Punctuation");
                    punct.addOption(CodeStyleOption.combo(USE_SEMICOLON, "Semicolon", USE_SEMICOLON_OPTIONS, "Use"));
                    punct.addOption(CodeStyleOption.combo(SEMICOLON_SCOPE, "Semicolon scope", SEMICOLON_SCOPE_OPTIONS, "in code generated by IDE"));
                    punct.addOption(CodeStyleOption.combo(QUOTE_STYLE, "Quotes", QUOTE_STYLE_OPTIONS, "double"));
                    punct.addOption(CodeStyleOption.combo(QUOTE_SCOPE, "Quotes scope", QUOTE_SCOPE_OPTIONS, "in code generated by IDE"));
                    punct.addOption(CodeStyleOption.combo(TRAILING_COMMA, "Trailing comma", TRAILING_COMMA_OPTIONS, "Keep"));
                    customizer.addGroup(punct);

                } else if ("Code Generation".equals(tabName)) {
                    CodeStyleGroup naming = CodeStyleGroup.divider("Naming Conventions");
                    naming.addOption(CodeStyleOption.text(FIELD_PREFIX, "Field prefix:", "_"));
                    naming.addOption(CodeStyleOption.text(PROPERTY_PREFIX, "Property prefix:", ""));
                    naming.addOption(CodeStyleOption.combo(FILENAME_CONVENTION, "Filename convention:", FILENAME_CONVENTION_OPTIONS, "Reuse case of current file"));
                    customizer.addGroup(naming);

                    CodeStyleGroup comments = CodeStyleGroup.divider("Comments");
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_LINE_COMMENT_AT_FIRST_COLUMN, "Line comment at first column", false));
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_ADD_SPACE_AT_LINE_COMMENT_START, "Add a space at line comment start", true));
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_BLOCK_COMMENT_AT_FIRST_COLUMN, "Block comment at first column", true));
                    comments.addOption(CodeStyleOption.checkbox(CODE_GEN_ADD_SPACES_AROUND_BLOCK_COMMENTS, "Add spaces around block comments", false));
                    customizer.addGroup(comments);

                } else if ("Imports".equals(tabName)) {
                    CodeStyleGroup imports = CodeStyleGroup.flat("Imports");
                    imports.addOption(CodeStyleOption.checkbox(MERGE_IMPORTS_SAME_MODULE, "Merge imports for members from the same module", true));
                    imports.addOption(CodeStyleOption.checkbox(USE_RELATIVE_PATHS, "Use paths relative to the project, resource or sources roots", false));
                    imports.addOption(CodeStyleOption.checkbox(USE_DIRECTORY_IMPORT, "Use directory import when index.js is available (node-style module resolution)", true));
                    imports.addOption(CodeStyleOption.combo(USE_FILE_EXTENSION, "Use file extension:", USE_FILE_EXTENSION_OPTIONS, "Auto"));
                    imports.addOption(CodeStyleOption.combo(USE_PATH_ALIASES, "Use path aliases:", USE_PATH_ALIASES_OPTIONS, "Always"));
                    imports.addOption(CodeStyleOption.text(DO_NOT_IMPORT_EXACTLY_FROM, "Do not import exactly from:", "rxjs,@angular/material/typings/**"));
                    imports.addOption(CodeStyleOption.checkbox(SORT_IMPORTED_MEMBERS, "Sort imported members", true));
                    imports.addOption(CodeStyleOption.checkbox(SORT_IMPORTS_BY_MODULES, "Sort imports by modules", false));
                    customizer.addGroup(imports);

                } else if ("Arrangement".equals(tabName)) {
                    CodeStyleGroup grouping = CodeStyleGroup.divider("Grouping rules:");
                    grouping.addOption(CodeStyleOption.checkbox(GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER, "Group property field with corresponding getter/setter", true));
                    grouping.addOption(CodeStyleOption.checkbox(GROUP_FIELDS_WITH_ARROW_FUNCTIONS, "Group fields initialized with arrow functions with methods", true));
                    grouping.addOption(CodeStyleOption.checkbox(KEEP_OVERRIDDEN_METHODS_TOGETHER, "Keep overridden methods together", false));
                    grouping.addOption(CodeStyleOption.combo(OVERRIDDEN_METHODS_ORDER, "Order of overridden methods", OVERRIDDEN_METHODS_ORDER_OPTIONS, "keep order"));
                    customizer.addGroup(grouping);
                }
            }
        };
    }
}
