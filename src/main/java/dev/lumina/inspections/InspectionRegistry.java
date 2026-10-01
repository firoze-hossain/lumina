package dev.lumina.inspections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic registry for code inspections.
 * Allows language plugins and extensions to register inspection tools and providers
 * at runtime without modifying or hardcoding the UI layer.
 */
public final class InspectionRegistry {

    private static final InspectionRegistry INSTANCE = new InspectionRegistry();

    public static InspectionRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<String, InspectionTool> toolsById = new ConcurrentHashMap<>();
    private final List<InspectionProvider> providers = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private InspectionRegistry() {
        initDefaultProviders();
    }

    public synchronized void register(InspectionTool tool) {
        if (tool == null) return;
        toolsById.put(tool.getId(), tool);
        notifyChanged();
    }

    public synchronized void unregister(String toolId) {
        if (toolId == null) return;
        if (toolsById.remove(toolId) != null) {
            notifyChanged();
        }
    }

    public synchronized void registerProvider(InspectionProvider provider) {
        if (provider == null || providers.contains(provider)) return;
        providers.add(provider);
        for (InspectionTool tool : provider.getInspections()) {
            toolsById.put(tool.getId(), tool);
        }
        notifyChanged();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }

    public InspectionTool getTool(String id) {
        if (id == null) return null;
        return toolsById.get(id);
    }

    public List<InspectionTool> getAllTools() {
        List<InspectionTool> list = new ArrayList<>(toolsById.values());
        list.sort(Comparator.comparing(InspectionTool::getDisplayName));
        return Collections.unmodifiableList(list);
    }

    public static String getTopLevelCategory(String groupPath) {
        if (groupPath == null) return "General";
        if (groupPath.equalsIgnoreCase("GitLab CI/CD") || groupPath.toLowerCase(Locale.ROOT).startsWith("gitlab ci/cd/")) {
            return "GitLab CI/CD";
        }
        if (groupPath.equalsIgnoreCase("Sass/SCSS") || groupPath.toLowerCase(Locale.ROOT).startsWith("sass/scss/")) {
            return "Sass/SCSS";
        }
        int slash = groupPath.indexOf('/');
        if (slash > 0) {
            return groupPath.substring(0, slash);
        }
        return groupPath;
    }

    public List<String> getAllCategories() {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (InspectionTool tool : toolsById.values()) {
            set.add(getTopLevelCategory(tool.getGroupPath()));
        }
        return new ArrayList<>(set);
    }

    public List<String> getAllSubCategories(String parentCategory) {
        if (parentCategory == null) return Collections.emptyList();
        String prefix = parentCategory.toLowerCase(Locale.ROOT) + "/";
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (InspectionTool tool : toolsById.values()) {
            String gp = tool.getGroupPath();
            if (gp.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                int nextSlash = gp.indexOf('/', prefix.length());
                if (nextSlash > 0) {
                    set.add(gp.substring(0, nextSlash));
                } else {
                    set.add(gp);
                }
            }
        }
        return new ArrayList<>(set);
    }

    public List<InspectionTool> getToolsForCategory(String category) {
        if (category == null) return Collections.emptyList();
        List<InspectionTool> result = new ArrayList<>();
        String prefix = category.toLowerCase(Locale.ROOT) + "/";
        for (InspectionTool tool : toolsById.values()) {
            String gp = tool.getGroupPath();
            if (gp.equalsIgnoreCase(category) || gp.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(tool);
            }
        }
        result.sort(Comparator.comparing(InspectionTool::getDisplayName));
        return result;
    }

    public List<InspectionTool> getToolsDirectlyInCategory(String category) {
        if (category == null) return Collections.emptyList();
        List<InspectionTool> result = new ArrayList<>();
        for (InspectionTool tool : toolsById.values()) {
            if (category.equalsIgnoreCase(tool.getGroupPath())) {
                result.add(tool);
            }
        }
        result.sort(Comparator.comparing(InspectionTool::getDisplayName));
        return result;
    }

    public List<InspectionTool> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllTools();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<InspectionTool> matches = new ArrayList<>();
        for (InspectionTool tool : toolsById.values()) {
            if (tool.getDisplayName().toLowerCase(Locale.ROOT).contains(q) ||
                tool.getGroupPath().toLowerCase(Locale.ROOT).contains(q) ||
                tool.getDescription().toLowerCase(Locale.ROOT).contains(q) ||
                tool.getId().toLowerCase(Locale.ROOT).contains(q)) {
                matches.add(tool);
            }
        }
        matches.sort(Comparator.comparing(InspectionTool::getDisplayName));
        return matches;
    }

    /**
     * Initializes default inspection tools across all categories matching the reference IDE.
     */
    private void initDefaultProviders() {
        // 1. User defined (indeterminate [-])
        addTool("UserDefined.RegExpSearch", "RegExp search inspection", "User defined",
                "Matches user-defined regular expression search patterns in code.",
                HighlightSeverity.WARNING, true, "General");
        addTool("UserDefined.RegExpReplace", "RegExp replace inspection", "User defined",
                "Matches user-defined regular expression replace patterns in code.",
                HighlightSeverity.WEAK_WARNING, false, "General");
        addTool("UserDefined.StructuralSearch", "Structural search inspection", "User defined",
                "Reports AST patterns matching custom structural search templates.",
                HighlightSeverity.WARNING, true, "General");
        addTool("UserDefined.StructuralReplace", "Structural replace inspection", "User defined",
                "Reports AST patterns matching custom structural replace templates.",
                HighlightSeverity.WARNING, false, "General");

        // 2. Angular ([✓]) - All 32 inspections from screenshot 4
        addTool("Angular.AccessingLengthOfUncalledSignal", "Accessing length property of an uncalled signal", "Angular",
                "Reports accessing .length property of an uncalled Angular signal function.",
                HighlightSeverity.WARNING, true, "TypeScript");
        addTool("Angular.AmbiguousComponentTag", "Ambiguous component tag", "Angular",
                "Reports component tags that match multiple Angular component selectors.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.CliAddDependency", "Angular CLI add dependency", "Angular",
                "Suggests installing missing Angular CLI package dependencies.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("Angular.ContentInsideNgContent", "Content inside <ng-content> tag", "Angular",
                "Reports content placed inside an <ng-content> element which will be ignored by Angular.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.IllegalForLoopAccess", "Illegal @for loop access", "Angular",
                "Reports invalid variable references inside Angular @for control flow blocks.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.InaccessibleComponentMember", "Inaccessible component member or directive input", "Angular",
                "Reports private or protected component members referenced in Angular templates.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.IncorrectComponentTemplateDefinition", "Incorrect component template definition", "Angular",
                "Reports invalid templateUrl or inline template syntax in component decorators.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.IncorrectUsageOfLetDeclaration", "Incorrect usage of @let declaration", "Angular",
                "Reports invalid scope or duplicate identifier in @let template declarations.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.IncorrectUsageOfAngularBlock", "Incorrect usage of Angular block", "Angular",
                "Reports malformed @if, @else, @switch, or @case block syntax.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.InsecureBindingToEvent", "Insecure binding to event", "Angular",
                "Reports event bindings susceptible to script injection or unsanitized expressions.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.InvalidAnimationTriggerAssignment", "Invalid animation trigger assignment", "Angular",
                "Validates animation trigger assignment values in component templates.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.InvalidBindingType", "Invalid binding type", "Angular",
                "Reports property binding types that are incompatible with input property types.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.InvalidEntryComponent", "Invalid entry component", "Angular",
                "Reports entry components that cannot be loaded dynamically.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.InvalidI18nAttribute", "Invalid i18n attribute", "Angular",
                "Reports malformed i18n translation comments and metadata attributes.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.InvalidImportedOrDeclaredSymbol", "Invalid imported or declared symbol", "Angular",
                "Reports symbols imported or declared in NgModule that are not components, directives, or pipes.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.InvalidUsageOfImportsInNonStandalone", "Invalid usage of imports in non-standalone components", "Angular",
                "Reports 'imports' array specified on non-standalone Angular components.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.IssuesWithNgSrcUsage", "Issues with ngSrc usage in img tags", "Angular",
                "Reports missing width, height, or priority attributes when using NgOptimizedImage.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("Angular.MissingEventHandler", "Missing event handler", "Angular",
                "Reports event bindings without a target handler expression.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.MissingOrInvalidDeclarationInModule", "Missing or invalid component, directive or pipe declaration in a module", "Angular",
                "Reports components, directives, or pipes missing in declarations array.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.MissingOrInvalidSelector", "Missing or invalid selector", "Angular",
                "Reports directive or component definitions missing a valid CSS selector string.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.MissingRequiredDirectiveInput", "Missing required directive input", "Angular",
                "Reports usages of directives or components without bound required inputs.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.MultipleStructuralDirectives", "Multiple structural directives on one element", "Angular",
                "Reports more than one structural directive (*ngIf, *ngFor) applied to a single HTML tag.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.NonIterableTypeInForBlock", "Non-iterable type in @for block", "Angular",
                "Reports @for expressions over non-iterable collections.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.ProblemsWithDeferOnTriggers", "Problems with @defer 'on' triggers", "Angular",
                "Reports invalid trigger arguments or incompatible options on @defer blocks.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.RecursiveImportOrExport", "Recursive import or export of an Angular module or a standalone component", "Angular",
                "Reports cyclic dependency cycles between imported Angular modules or components.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.UnboundOrAmbiguousTemplateRef", "Unbound or ambiguous template reference variable", "Angular",
                "Reports template reference variables (#ref) that cannot be resolved or have ambiguous targets.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.UndefinedBinding", "Undefined binding", "Angular",
                "Reports bound properties that do not exist on the target DOM element or directive.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.UndefinedExportFromModule", "Undefined export from Angular module", "Angular",
                "Reports symbols declared in 'exports' array that are not declared or imported in the module.",
                HighlightSeverity.ERROR, true, "TypeScript");
        addTool("Angular.UndefinedTag", "Undefined tag", "Angular",
                "Reports unknown component tags that are not declared in any imported module or standalone component.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.UnresolvedPipe", "Unresolved pipe", "Angular",
                "Reports pipe names in template expressions that cannot be resolved.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.UnsupportedExpressionSyntax", "Unsupported Angular expression syntax", "Angular",
                "Reports JavaScript expressions not supported by Angular template expression parser.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Angular.UnusedImportInComponentDeclaration", "Unused import in an Angular component declaration", "Angular",
                "Reports unused components, directives, or pipes in standalone component 'imports'.",
                HighlightSeverity.ERROR, true, "TypeScript");

        // 3. AOP ([✓]) - All 6 inspections from screenshot 5
        addTool("AOP.AdviceParametersConsistency", "Advice parameters (argNames, returning, throwing) consistency check", "AOP",
                "Validates consistency between advice method parameter names and pointcut bound arguments.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("AOP.AroundAdviceStyle", "Around advice style inspection", "AOP",
                "Reports around advice methods lacking ProceedingJoinPoint argument.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("AOP.IntroductionsErrors", "Introductions (declare parents) errors", "AOP",
                "Reports errors in @DeclareParents / introduction type definitions.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("AOP.PointcutExpressionErrors", "Pointcut expression errors", "AOP",
                "Reports syntax errors, unresolved method references, and type errors in AspectJ pointcuts.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("AOP.PointcutMethodStyle", "Pointcut method style", "AOP",
                "Reports non-standard pointcut method signatures or non-empty pointcut method bodies.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("AOP.WarningArgNamesNotDefined", "Warning: argNames not defined", "AOP",
                "Reports advice annotations missing explicit argNames when debugging symbols are omitted.",
                HighlightSeverity.WARNING, true, "Java");

        // 4. Application servers ([✓]) - Matching screenshot 5
        addTool("AppServer.JBossWildFly", "JBoss/WildFly", "Application servers",
                "Reports JBoss and WildFly specific server deployment and module configuration errors.",
                HighlightSeverity.ERROR, true, "General");
        addTool("AppServer.DeploymentDescriptor", "Deployment descriptor validation", "Application servers",
                "Validates application server deployment descriptor XML configurations against standard schemas.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("AppServer.ServerConfig", "Server configuration problem", "Application servers",
                "Reports missing or misconfigured application server runtime properties and ports.",
                HighlightSeverity.WARNING, true, "General");

        // 5. Bean Validation ([✓]) - Matching screenshot 5
        addTool("BeanValidation.IncorrectMinMax", "Incorrect 'min' and 'max' values in Bean Validation annotations", "Bean Validation",
                "Reports 'min' value greater than 'max' in @Size, @DecimalMin/@DecimalMax, or @Min/@Max.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("BeanValidation.ConstraintMappingsXml", "Incorrect elements in Bean Validation <constraint-mappings> files", "Bean Validation",
                "Reports invalid element structures in validation constraint-mappings XML descriptor files.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("BeanValidation.ValidationConfigXml", "Incorrect elements in Bean Validation <validation-config> files", "Bean Validation",
                "Reports schema violations in validation.xml configuration files.",
                HighlightSeverity.ERROR, true, "XML");

        // 6. CDI (Contexts and Dependency Injection) ([✓]) - Matching screenshot 5
        addTool("CDI.ScopeCollisionInStereotypes", "Bean has collision of scope in stereotypes", "CDI (Contexts and Dependency Injection)",
                "Reports beans declared with multiple stereotypes specifying conflicting scopes.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.NonDefaultScopePublicFields", "Bean with non-default scope declares public fields", "CDI (Contexts and Dependency Injection)",
                "Reports normal-scoped CDI managed beans declaring non-static public fields.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("CDI.DisposerWithoutProducers", "Disposer method parameter without producers", "CDI (Contexts and Dependency Injection)",
                "Reports disposer methods (@Disposes) having no matching producer method or field.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("CDI.IncorrectDecoratorClass", "Incorrect @Decorator class", "CDI (Contexts and Dependency Injection)",
                "Reports decorator beans that do not implement decorated types or miss @Delegate injection.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectSpecializesUsage", "Incorrect @Specializes usage", "CDI (Contexts and Dependency Injection)",
                "Reports @Specializes on beans that do not directly extend another bean class.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectStereotypeClass", "Incorrect @Stereotype annotation class", "CDI (Contexts and Dependency Injection)",
                "Reports stereotype annotations missing required meta-annotations.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectTypedUsage", "Incorrect @Typed annotation usage", "CDI (Contexts and Dependency Injection)",
                "Reports @Typed restricting bean types to classes that are not implemented.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectBeansXmlDefinitions", "Incorrect bean definitions in beans.xml", "CDI (Contexts and Dependency Injection)",
                "Reports XML syntax and schema errors in CDI beans.xml deployment descriptors.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("CDI.IncorrectBeanScope", "Incorrect bean scope", "CDI (Contexts and Dependency Injection)",
                "Reports invalid scope annotations applied to special CDI artifacts.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("CDI.IncorrectDependencyInjectionPlace", "Incorrect dependency injection place", "CDI (Contexts and Dependency Injection)",
                "Reports @Inject placed on static fields, final fields, or abstract methods.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectDisposerMethod", "Incorrect disposer method", "CDI (Contexts and Dependency Injection)",
                "Reports disposer methods declaring multiple @Disposes parameters.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectManagedBeanDefinition", "Incorrect managed bean definition", "CDI (Contexts and Dependency Injection)",
                "Reports managed bean classes lacking appropriate default constructors.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.IncorrectObserverMethod", "Incorrect observer method", "CDI (Contexts and Dependency Injection)",
                "Reports observer methods declaring multiple @Observes parameters or inside decorators.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.UnproxyableBeanType", "Incorrect usage of bean type that cannot be proxied", "CDI (Contexts and Dependency Injection)",
                "Reports normal-scoped beans that cannot be proxied due to final methods or classes.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.AmbiguousDependencies", "Injection point with ambiguous dependencies", "CDI (Contexts and Dependency Injection)",
                "Reports injection points that match multiple eligible beans with identical qualifiers.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.InterceptorWithoutBindings", "@Interceptor class without binding types", "CDI (Contexts and Dependency Injection)",
                "Reports interceptor classes declared without interceptor binding annotations.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("CDI.VetoedAlternativeBean", "Vetoed @Alternative bean", "CDI (Contexts and Dependency Injection)",
                "Reports @Alternative beans that have been vetoed by extensions.",
                HighlightSeverity.ERROR, true, "Java");

        // 7. Code Coverage ([✓])
        addTool("CodeCoverage.Go", "Check GO source code coverage (available for Code | Inspect Code)", "Code Coverage",
                "Calculates and highlights test coverage for Go source files.",
                HighlightSeverity.WARNING, true, "Go", true, false);
        addTool("CodeCoverage.JavaScript", "Check JavaScript and TypeScript source code coverage (available for Code | Inspect Code)", "Code Coverage",
                "Calculates and highlights test coverage for JavaScript and TypeScript source files.",
                HighlightSeverity.WARNING, true, "JavaScript", true, false);
        addTool("CodeCoverage.Java", "Check Kotlin and Java source code coverage (available for Code | Inspect Code)", "Code Coverage",
                "Calculates and highlights test coverage for Kotlin and Java source files.",
                HighlightSeverity.WARNING, true, "Java", true, false);
        addTool("CodeCoverage.Scala", "Check Scala source code coverage (available for Code | Inspect Code)", "Code Coverage",
                "Calculates and highlights test coverage for Scala source files.",
                HighlightSeverity.WARNING, true, "Scala", true, false);
        addTool("CodeCoverage.PHP", "Check the PHP source code coverage (available for Code | Inspect Code)", "Code Coverage",
                "Calculates and highlights test coverage for PHP source files.",
                HighlightSeverity.WARNING, true, "PHP", true, false);

        // 8. Code metrics ([ ])
        addTool("CodeMetrics.Calculate", "Calculate Kotlin and Java code metrics", "Code metrics",
                "Calculates cyclomatic complexity, coupling, and size metrics for Kotlin and Java files.",
                HighlightSeverity.WARNING, false, "Java");

        // 9. Compose Multiplatform Preview ([✓])
        addTool("Compose.MultiplePreviewParameter", "Multiple @PreviewParameter are not allowed", "Compose Multiplatform Preview",
                "Reports multiple parameters annotated with @PreviewParameter in a composable function.",
                HighlightSeverity.ERROR, true, "Kotlin");
        addTool("Compose.PreviewWithParameters", "Preview used on a Composable function with parameters", "Compose Multiplatform Preview",
                "Reports @Preview annotation on a composable function that requires non-default parameters.",
                HighlightSeverity.ERROR, true, "Kotlin");

        // 10. Cron ([✓])
        addTool("Cron.ValidateExpression", "Validate Cron expression", "Cron",
                "Validates cron expression syntax and schedule fields.",
                HighlightSeverity.ERROR, true, "General");

        // 11. CSS ([-] with subcategories)
        // Subcategory: Code quality tools
        addTool("CSS.Stylelint", "Stylelint", "CSS/Code quality tools",
                "Integrates Stylelint linter to detect code errors and enforce stylistic conventions.",
                HighlightSeverity.WARNING, false, "CSS");

        // Subcategory: Code style issues
        addTool("CSS.MissingSemicolon", "Missing semicolon", "CSS/Code style issues",
                "Reports missing semicolons at the end of CSS declarations.",
                HighlightSeverity.WARNING, false, "CSS");
        addTool("CSS.RedundantMeasureUnit", "Redundant measure unit", "CSS/Code style issues",
                "Reports measure units on zero values where they are redundant (e.g., 0px instead of 0).",
                HighlightSeverity.WARNING, true, "CSS");

        // Subcategory: Invalid elements (17 tools)
        addTool("CSS.InvalidPropertyDeclaration", "Invalid @property declaration", "CSS/Invalid elements",
                "Reports syntax errors in @property rule declarations.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidPropertyName", "Invalid @property name", "CSS/Invalid elements",
                "Reports invalid custom property names in @property rules.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidFunction", "Invalid function", "CSS/Invalid elements",
                "Reports calls to unknown or invalid CSS functions.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidMediaFeature", "Invalid media feature", "CSS/Invalid elements",
                "Reports invalid media features in @media queries.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidNestedSelector", "Invalid nested selector", "CSS/Invalid elements",
                "Reports invalid CSS nesting selectors.",
                HighlightSeverity.WARNING, false, "CSS");
        addTool("CSS.InvalidPropertyValue", "Invalid property value", "CSS/Invalid elements",
                "Reports values that are incompatible with the property definition.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidPseudoSelector", "Invalid pseudo-selector", "CSS/Invalid elements",
                "Reports unknown pseudo-classes or pseudo-elements.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.InvalidTypeSelector", "Invalid type selector", "CSS/Invalid elements",
                "Reports invalid type (HTML tag) selectors in stylesheets.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.MisplacedImport", "Misplaced @import", "CSS/Invalid elements",
                "Reports @import rules placed after other CSS rules.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.MisplacedCharset", "Misplaced or incorrect @charset", "CSS/Invalid elements",
                "Reports @charset rules not placed at the very beginning of the CSS file.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.NegativePropertyValue", "Negative property value", "CSS/Invalid elements",
                "Reports negative values for properties that only accept non-negative values.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.UnknownAtRule", "Unknown at-rule", "CSS/Invalid elements",
                "Reports unknown @ at-rules.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.UnknownProperty", "Unknown property", "CSS/Invalid elements",
                "Reports unknown CSS property names.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.UnknownUnit", "Unknown unit", "CSS/Invalid elements",
                "Reports unknown CSS measure units.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.UnresolvedClassInComposes", "Unresolved class in 'composes' rule", "CSS/Invalid elements",
                "Reports unresolved CSS classes in CSS Module composes rules.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.UnresolvedCustomProperty", "Unresolved custom property", "CSS/Invalid elements",
                "Reports unresolved CSS custom property variables (var(--...)).",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.UnresolvedFileReference", "Unresolved file reference", "CSS/Invalid elements",
                "Reports unresolved URLs and file paths in url(...) statements.",
                HighlightSeverity.ERROR, true, "CSS");

        // Subcategory: Probable bugs (11 tools)
        addTool("CSS.MissingCommaInSelectorList", "Missing comma in selector list", "CSS/Probable bugs",
                "Reports missing commas between selectors in a selector list.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.MissingGenericFontFamilyName", "Missing generic font family name", "CSS/Probable bugs",
                "Reports font-family definitions lacking a fallback generic family (e.g., serif, sans-serif).",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.NonIntegerLengthInPixels", "Non-integer length in pixels", "CSS/Probable bugs",
                "Reports fractional pixel lengths that may cause rendering artifacts.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.ColorHexReplacement", "Color could be replaced with #-hex", "CSS/Probable bugs",
                "Suggests replacing rgb(...) or named colors with hexadecimal notation.",
                HighlightSeverity.WEAK_WARNING, false, "CSS");
        addTool("CSS.ColorRgbReplacement", "Color could be replaced with rgb()", "CSS/Probable bugs",
                "Suggests replacing named or hex colors with rgb() function.",
                HighlightSeverity.WEAK_WARNING, false, "CSS");
        addTool("CSS.DeprecatedValue", "Deprecated value", "CSS/Probable bugs",
                "Reports deprecated CSS property values.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("CSS.OverwrittenProperty", "Overwritten property", "CSS/Probable bugs",
                "Reports CSS properties duplicated within the same declaration block.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.PropertiesMayBeSafelyReplaced", "Properties may be safely replaced with a shorthand", "CSS/Probable bugs",
                "Reports individual longhand properties that can safely be collapsed into a shorthand property.",
                HighlightSeverity.WARNING, true, "CSS");
        addTool("CSS.PropertiesMayProbablyBeReplaced", "Properties may probably be replaced with a shorthand", "CSS/Probable bugs",
                "Reports longhand properties that might be replaceable with shorthand properties.",
                HighlightSeverity.WEAK_WARNING, true, "CSS");
        addTool("CSS.PropertyIncompatibleWithBrowsers", "Property is incompatible with selected browsers", "CSS/Probable bugs",
                "Reports CSS properties not supported by targeted browser versions.",
                HighlightSeverity.WARNING, false, "CSS");
        addTool("CSS.UnusedSelector", "Unused selector", "CSS/Probable bugs",
                "Reports CSS selectors that do not match any HTML element in the project.",
                HighlightSeverity.WARNING, false, "CSS");

        // 12. Dev Container ([✓])
        addTool("DevContainer.FolderStructureProblems", "Dev Container folder structure problems", "Dev Container",
                "Reports misconfigured devcontainer directories or missing configuration files.",
                HighlightSeverity.ERROR, true, "JSON");
        addTool("DevContainer.ValidateIdeSettings", "Validate IDE settings", "Dev Container",
                "Validates customizations and settings sections inside devcontainer.json.",
                HighlightSeverity.ERROR, true, "JSON");

        // 13. Docker-compose ([✓])
        addTool("DockerCompose.ErroneousRelation", "Erroneous relation in docker-compose YAML", "Docker-compose",
                "Reports invalid service dependencies and network/volume links in docker-compose files.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("DockerCompose.MissingKeys", "Missing docker-compose YAML keys", "Docker-compose",
                "Reports required service and configuration keys missing from docker-compose YAML.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("DockerCompose.UnknownKeys", "Unknown docker-compose YAML keys", "Docker-compose",
                "Reports unparsed or invalid keys in docker-compose configuration files.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("DockerCompose.UnknownValues", "Unknown docker-compose YAML values", "Docker-compose",
                "Reports unrecognized values assigned to docker-compose configuration directives.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("DockerCompose.UnquotedPortMappings", "Unquoted port mappings", "Docker-compose",
                "Reports unquoted port mapping values that may be parsed as base-60 numbers by YAML parsers.",
                HighlightSeverity.WARNING, true, "YAML");

        // 14. Dockerfile ([✓])
        addTool("Dockerfile.SingleQuotedStringJsonArray", "A single quoted string in JSON array format", "Dockerfile",
                "Reports JSON array forms in ENTRYPOINT or CMD using single quotes instead of valid double quotes.",
                HighlightSeverity.WARNING, true, "Dockerfile");
        addTool("Dockerfile.DuplicatedStageName", "Duplicated stage name", "Dockerfile",
                "Reports duplicated build stage names in multi-stage Dockerfiles.",
                HighlightSeverity.ERROR, true, "Dockerfile");
        addTool("Dockerfile.HeredocLastArgument", "Heredoc as a last argument (destination) to ADD/COPY", "Dockerfile",
                "Reports heredocs improperly positioned as destination arguments to ADD or COPY instructions.",
                HighlightSeverity.ERROR, true, "Dockerfile");
        addTool("Dockerfile.HeredocMismatchedDelimiters", "Heredoc mismatched delimiters", "Dockerfile",
                "Reports heredoc blocks with opening and closing delimiters that do not match.",
                HighlightSeverity.ERROR, true, "Dockerfile");
        addTool("Dockerfile.InvalidDestinationAddCopy", "Invalid destination for \"ADD\"/\"COPY\" commands", "Dockerfile",
                "Reports invalid destination directory or file paths in ADD and COPY instructions.",
                HighlightSeverity.WARNING, true, "Dockerfile");
        addTool("Dockerfile.InvalidSpacesKeyValue", "Invalid spaces in \"key=value\" pair", "Dockerfile",
                "Reports unexpected spaces surrounding '=' in ENV or LABEL instructions.",
                HighlightSeverity.WARNING, true, "Dockerfile");
        addTool("Dockerfile.MissingExecInEntrypoint", "Missing 'exec' in entrypoint shell form", "Dockerfile",
                "Reports entrypoint shell scripts that do not execute the container command with exec.",
                HighlightSeverity.WARNING, true, "Dockerfile");
        addTool("Dockerfile.MissingContinuationCharacterRun", "Missing continuation character for \"RUN\" command", "Dockerfile",
                "Reports multi-line RUN instructions missing trailing backslash continuation characters.",
                HighlightSeverity.WARNING, true, "Dockerfile");
        addTool("Dockerfile.WrongNumberOfArguments", "Wrong number of arguments", "Dockerfile",
                "Reports Dockerfile instructions invoked with too few or too many arguments.",
                HighlightSeverity.ERROR, true, "Dockerfile");

        // 15. EditorConfig ([✓])
        addTool("EditorConfig.DeprecatedProperty", "Deprecated property", "EditorConfig",
                "Reports deprecated .editorconfig property keys.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.DuplicateCharacterClassLetter", "Duplicate character class letter", "EditorConfig",
                "Reports duplicate character entries within square bracket glob character classes.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.DuplicateRedundantPattern", "Duplicate or redundant pattern", "EditorConfig",
                "Reports redundant or duplicated pattern segments in section headers.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.SectionNotUnique", "EditorConfig section is not unique", "EditorConfig",
                "Reports duplicate section headers defined in the same .editorconfig file.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.EmptyHeader", "Empty header", "EditorConfig",
                "Reports empty section headers '[]' in .editorconfig files.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.EmptySection", "Empty section", "EditorConfig",
                "Reports section blocks containing no property declarations.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.ExtraTopLevelDeclaration", "Extra top-level declaration", "EditorConfig",
                "Reports declarations at file top-level other than root = true.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.CharsetMismatch", "File encoding doesn't match EditorConfig charset", "EditorConfig",
                "Reports files whose actual byte encoding differs from the charset configured in .editorconfig.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.InvalidFile", "Invalid .editorconfig file", "EditorConfig",
                "Reports fundamental syntax and parsing errors in .editorconfig files.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.InvalidPropertyValue", "Invalid property value", "EditorConfig",
                "Reports invalid values assigned to standard EditorConfig properties.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.InvalidReference", "Invalid reference", "EditorConfig",
                "Reports invalid references in section glob patterns.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.NoMatchingFiles", "No matching files", "EditorConfig",
                "Reports section patterns that match no files in the project.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.NonUniqueListValue", "Non-unique list value", "EditorConfig",
                "Reports duplicate entries in comma-separated property lists.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.OverlappingSections", "Overlapping sections", "EditorConfig",
                "Reports section glob patterns that match identical file sets with conflicting rules.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.OverriddenProperty", "Overridden property", "EditorConfig",
                "Reports properties overridden by subsequent definitions in the same section.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.OverridingProperty", "Overriding property", "EditorConfig",
                "Reports properties that override previous definitions in the same section.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.RedundantProperty", "Redundant property", "EditorConfig",
                "Reports properties setting identical values to those inherited from parent sections.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.RedundantWildcard", "Redundant wildcard", "EditorConfig",
                "Reports redundant wildcard characters (e.g., '**/**' or '***') in glob patterns.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.RequiredDeclarationsMissing", "Required declarations are missing", "EditorConfig",
                "Reports required configuration declarations missing from section blocks.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.SpaceInFilePattern", "Space in file pattern", "EditorConfig",
                "Reports unescaped spaces in section glob patterns that may cause unintended matching.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.TooManyWildcards", "Too many wildcards", "EditorConfig",
                "Reports patterns with excessive wildcard nesting that degrade path matching performance.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.UnexpectedComma", "Unexpected comma", "EditorConfig",
                "Reports unexpected leading, trailing, or double commas in brace expansion patterns.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.UnexpectedKeyValuePair", "Unexpected key-value pair", "EditorConfig",
                "Reports key-value pairs positioned outside of any section header.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.UnexpectedTopLevelDeclaration", "Unexpected top-level declaration", "EditorConfig",
                "Reports unexpected declarations placed before the first section.",
                HighlightSeverity.ERROR, true, "EditorConfig");
        addTool("EditorConfig.UnexpectedValueList", "Unexpected value list", "EditorConfig",
                "Reports comma-separated value lists where a scalar value is expected.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.UnknownProperty", "Unknown property", "EditorConfig",
                "Reports unknown or misspelled property keys in .editorconfig files.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.UnnecessaryBraces", "Unnecessary braces", "EditorConfig",
                "Reports empty or single-item brace expansions '{item}' that can be simplified.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.UnnecessaryCharacterClass", "Unnecessary character class", "EditorConfig",
                "Reports single-character character classes '[a]' that can be written directly as literal characters.",
                HighlightSeverity.WARNING, true, "EditorConfig");
        addTool("EditorConfig.UnusedDeclaration", "Unused declaration", "EditorConfig",
                "Reports declarations whose values are unused or completely superseded.",
                HighlightSeverity.WARNING, true, "EditorConfig");

        // 16. EL ([✓])
        addTool("EL.Validation", "EL validation", "EL",
                "Validates Jakarta/Java Expression Language syntax, identifiers, operators, and property access.",
                HighlightSeverity.WARNING, true, "EL");

        // 17. FreeMarker ([✓])
        addTool("FreeMarker.BuiltInErrors", "Built-in errors", "FreeMarker",
                "Reports invalid arguments or usage errors for FreeMarker built-in filters (?filter).",
                HighlightSeverity.ERROR, true, "FreeMarker");
        addTool("FreeMarker.BuiltInDeprecated", "Built-in is deprecated", "FreeMarker",
                "Reports usage of deprecated FreeMarker built-ins.",
                HighlightSeverity.WARNING, true, "FreeMarker");
        addTool("FreeMarker.DirectiveMalformed", "Directive is malformed", "FreeMarker",
                "Reports malformed syntax in FreeMarker tags and directives.",
                HighlightSeverity.ERROR, true, "FreeMarker");
        addTool("FreeMarker.IncorrectExpressionType", "Incorrect expression type", "FreeMarker",
                "Reports expressions whose evaluated type does not match directive requirements.",
                HighlightSeverity.WARNING, true, "FreeMarker");
        addTool("FreeMarker.InvalidCallDirective", "Invalid call directive", "FreeMarker",
                "Reports invalid invocations of FreeMarker macros or transform directives.",
                HighlightSeverity.WARNING, true, "FreeMarker");
        addTool("FreeMarker.UnresolvedExternalCall", "Unresolved external call", "FreeMarker",
                "Reports calls to undefined macros or external functions in included templates.",
                HighlightSeverity.ERROR, true, "FreeMarker");
        addTool("FreeMarker.UnresolvedFileReference", "Unresolved file reference", "FreeMarker",
                "Reports unresolved template file paths in <#include> and <#import> directives.",
                HighlightSeverity.ERROR, true, "FreeMarker");
        addTool("FreeMarker.UnresolvedReference", "Unresolved reference", "FreeMarker",
                "Reports unresolved variable and model property references in FreeMarker templates.",
                HighlightSeverity.WARNING, true, "FreeMarker");

        // 18. General ([-])
        addTool("General.Annotator", "Annotator (available for Code | Inspect Code)", "General",
                "Runs language-specific annotators and syntax highlighters during batch code analysis.",
                HighlightSeverity.ERROR, true, "General", true, false);
        addTool("General.DuplicatedCodeFragment", "Duplicated code fragment", "General",
                "Reports code fragments duplicated across multiple locations in the project.",
                HighlightSeverity.WARNING, false, "General");
        addTool("General.EmptyDirectory", "Empty directory (available for Code | Inspect Code)", "General",
                "Reports empty project folders that do not contain source or resource files.",
                HighlightSeverity.WARNING, false, "General", true, false);
        addTool("General.InconsistentLineSeparators", "Inconsistent line separators", "General",
                "Reports files containing mixed CRLF and LF line ending separators.",
                HighlightSeverity.WARNING, false, "General");
        addTool("General.IncorrectFormatting", "Incorrect formatting", "General",
                "Reports source code formatting that deviates from configured code style guidelines.",
                HighlightSeverity.WARNING, false, "General");
        addTool("General.InjectedReferences", "Injected references", "General",
                "Validates references inside injected language fragments.",
                HighlightSeverity.ERROR, true, "General");
        addTool("General.JavaAnnotator", "Java annotator", "General",
                "Runs deep semantic annotations on Java source files.",
                HighlightSeverity.ERROR, false, "Java");
        addTool("General.JavaSanity", "Java sanity", "General",
                "Performs sanity and integrity checks on Java AST elements and bytecode targets.",
                HighlightSeverity.ERROR, false, "Java");
        addTool("General.KotlinSanity", "Kotlin sanity", "General",
                "Performs sanity and integrity checks on Kotlin PSI tree elements.",
                HighlightSeverity.ERROR, false, "Kotlin");
        addTool("General.LineIsLongerThanAllowed", "Line is longer than allowed by code style", "General",
                "Reports lines of code that exceed the maximum configured right margin column limit.",
                HighlightSeverity.WARNING, false, "General");
        addTool("General.ProblematicWhitespace", "Problematic whitespace", "General",
                "Reports trailing whitespace and irregular spaces in source files.",
                HighlightSeverity.WARNING, false, "General");
        addTool("General.ReassignedToPlainText", "Reassigned to plain text", "General",
                "Reports known source files that have been explicitly reassigned to plain text file type.",
                HighlightSeverity.WARNING, true, "General");
        addTool("General.RedundantSuppression", "Redundant suppression", "General",
                "Reports @SuppressWarnings annotations or comments that suppress warnings that do not occur.",
                HighlightSeverity.WARNING, true, "General");
        addTool("General.TodoComment", "TODO comment", "General",
                "Tracks TODO, FIXME, and review comments in source code.",
                HighlightSeverity.INFO, false, "General");

        // 19. GitHub actions ([✓])
        addTool("GitHubActions.CyclicJobDependency", "Cyclic job dependency", "GitHub actions",
                "Reports circular dependency loops in job 'needs' clauses.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("GitHubActions.InvalidParameters", "Invalid parameters", "GitHub actions",
                "Reports invalid parameters passed to action workflows or steps.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("GitHubActions.StandardLibraryFunctionsValidation", "Standard library functions validation", "GitHub actions",
                "Validates expressions calling GitHub Actions built-in functions.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("GitHubActions.UndefinedActionFileReference", "Undefined action/file reference", "GitHub actions",
                "Reports action or repository references in 'uses' that cannot be resolved.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("GitHubActions.UndefinedJobDependency", "Undefined job dependency", "GitHub actions",
                "Reports unknown job IDs referenced in 'needs' dependency declarations.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("GitHubActions.UndefinedParameters", "Undefined parameters", "GitHub actions",
                "Reports undefined parameters referenced in step inputs.",
                HighlightSeverity.ERROR, true, "YAML");

        // 20. GitLab CI/CD ([✓])
        addTool("GitLabCI.DuplicatedJobUsage", "Duplicated job usage", "GitLab CI/CD",
                "Reports job names defined more than once in .gitlab-ci.yml.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("GitLabCI.UndefinedJob", "Undefined job", "GitLab CI/CD",
                "Reports unknown job references in 'extends' or dependency declarations.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("GitLabCI.UndefinedStage", "Undefined stage", "GitLab CI/CD",
                "Reports job stage names not declared in the top-level 'stages' configuration.",
                HighlightSeverity.ERROR, true, "YAML");

        // 21. Go ([-] with subcategories)
        // Subcategory: Code style issues (15 tools)
        addTool("Go.CommentHasNoLeadingSpace", "Comment has no leading space", "Go/Code style issues",
                "Reports line comments without a leading space after '//'.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.CommentExportedIncorrectName", "Comment of exported element starts with the incorrect name", "Go/Code style issues",
                "Reports doc comments for exported symbols that do not start with the symbol name.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ConvertStringLiterals", "Convert string literals", "Go/Code style issues",
                "Reports string literals that can be converted between interpreted and raw string literals.",
                HighlightSeverity.WEAK_WARNING, true, "Go");
        addTool("Go.ErrorStringCapitalizedOrPunctuation", "Error string should not be capitalized or end with punctuation", "Go/Code style issues",
                "Reports error message strings starting with capital letters or ending with punctuation.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ExportedElementShouldHaveComment", "Exported element should have a comment", "Go/Code style issues",
                "Reports exported functions, types, and constants lacking doc comments.",
                HighlightSeverity.WARNING, false, "Go");
        addTool("Go.ExportedElementOwnDeclaration", "Exported element should have its own declaration", "Go/Code style issues",
                "Reports exported symbols defined in multi-symbol declarations instead of individual declarations.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.NameStartsWithPackageName", "Name starts with a package name", "Go/Code style issues",
                "Reports symbol names that stutter by repeating the enclosing package name.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ReceiverHasGenericName", "Receiver has a generic name", "Go/Code style issues",
                "Reports method receivers using generic names like 'this' or 'self' instead of idiomatic short identifiers.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantElseInIf", "Redundant 'else' in 'if'", "Go/Code style issues",
                "Reports redundant 'else' blocks after 'if' statements that terminate with return, break, or continue.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantTrueInForCondition", "Redundant 'true' in for loop condition", "Go/Code style issues",
                "Reports 'for true {...}' loops that can be written idiomatically as 'for {...}'.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.StructInitializationWithoutFieldNames", "Struct initialization without field names", "Go/Code style issues",
                "Reports struct literal instantiations that use positional values instead of explicit field names.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.TypeParameterDeclaredInLowercase", "Type parameter is declared in lowercase", "Go/Code style issues",
                "Reports generic type parameters declared in lowercase instead of uppercase letters.",
                HighlightSeverity.WARNING, false, "Go");
        addTool("Go.UnitSpecificSuffixTimeDuration", "Unit-specific suffix for 'time.Duration'", "Go/Code style issues",
                "Reports time.Duration variables or constants named with unit suffixes (e.g. 'timeoutSec').",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnsortedImports", "Unsorted imports", "Go/Code style issues",
                "Reports import declaration blocks that do not adhere to alphabetical grouping conventions.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UsageOfSnakeCase", "Usage of Snake_Case", "Go/Code style issues",
                "Reports Go identifiers using snake_case instead of idiomatic MixedCaps / CamelCase.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: Control flow issues (Image 1)
        addTool("Go.AssignmentToReceiver", "Assignment to a receiver", "Go/Control flow issues",
                "Reports assignments to value method receivers which do not mutate the caller state.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DeferInTheLoop", "'defer' in the loop", "Go/Control flow issues",
                "Reports 'defer' statements used inside loops, which may cause resource exhaustion.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.InfiniteForLoop", "Infinite 'for' loop", "Go/Control flow issues",
                "Reports 'for' loops that have no exit condition, break, or return statement.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnreachableCode", "Unreachable code", "Go/Control flow issues",
                "Reports statements positioned after unconditional return, panic, or infinite loops.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: Data flow analysis (Image 1)
        addTool("Go.ConstantCondition", "Constant condition", "Go/Data flow analysis",
                "Reports boolean expressions that always evaluate to true or false.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ErrorMayBeNotNil", "Error may be not nil", "Go/Data flow analysis",
                "Reports ignored or unchecked error return values that may lead to unexpected behavior.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.InterproceduralPotentialNilDereference", "Interprocedural potential nil dereference", "Go/Data flow analysis",
                "Reports potential nil pointer dereferences across function boundaries.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.PotentialNilDereference", "Potential nil dereference", "Go/Data flow analysis",
                "Reports values that can be nil when dereferenced.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.PotentialResourceLeak", "Potential resource leak", "Go/Data flow analysis",
                "Reports io.Closer resources that are not properly closed on all execution paths.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: Declaration redundancy (Image 1)
        addTool("Go.BoolCondition", "Bool condition", "Go/Declaration redundancy",
                "Reports redundant boolean comparisons, such as comparing a boolean expression to true or false.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.EmptyDeclaration", "Empty declaration", "Go/Declaration redundancy",
                "Reports empty 'var', 'const', or 'type' declaration blocks.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.EmptySliceDeclaredUsingLiteral", "Empty slice declared using a literal", "Go/Declaration redundancy",
                "Reports empty slices declared using composite literals []T{} instead of 'var s []T'.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantBlankArgumentInRange", "Redundant blank argument in range", "Go/Declaration redundancy",
                "Reports redundant blank identifiers '_' in range clauses.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantComma", "Redundant comma", "Go/Declaration redundancy",
                "Reports redundant trailing commas in single-line literal expressions.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantImportAlias", "Redundant import alias", "Go/Declaration redundancy",
                "Reports import aliases that match the package's default identifier name.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantSecondIndexInSlices", "Redundant second index in slices", "Go/Declaration redundancy",
                "Reports slice expressions where the high bound matches the capacity or length.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantSemicolon", "Redundant semicolon", "Go/Declaration redundancy",
                "Reports redundant semicolons at the end of statements.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantTypeConversion", "Redundant type conversion", "Go/Declaration redundancy",
                "Reports type conversions converting an expression to its already existing type.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantTypesInCompositeLiterals", "Redundant types in composite literals", "Go/Declaration redundancy",
                "Reports redundant type specifications in elements of array, slice, or map composite literals.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.SelfAssignment", "Self assignment", "Go/Declaration redundancy",
                "Reports assignments where the target and source are identical variables.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.TypeCanBeOmitted", "Type can be omitted", "Go/Declaration redundancy",
                "Reports variable declarations where the explicit type is redundant due to initialization value.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedConstant", "Unused constant", "Go/Declaration redundancy",
                "Reports constants that are declared but never referenced in code.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedExportedFunction", "Unused exported function", "Go/Declaration redundancy",
                "Reports exported functions that are never referenced within the project.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedExportedType", "Unused exported type", "Go/Declaration redundancy",
                "Reports exported types that are never used in declarations or instantiations.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedFunction", "Unused function", "Go/Declaration redundancy",
                "Reports unexported functions that are never called in the package.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedGlobalVariable", "Unused global variable", "Go/Declaration redundancy",
                "Reports package-level global variables that are never read.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedParameter", "Unused parameter", "Go/Declaration redundancy",
                "Reports function or method parameters that are never referenced in the body.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedType", "Unused type", "Go/Declaration redundancy",
                "Reports unexported types that are never instantiated or used.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedTypeParameter", "Unused type parameter", "Go/Declaration redundancy",
                "Reports generic type parameters that are not used in function signatures or struct fields.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: General (Image 2)
        addTool("Go.CyclicImports", "Cyclic imports", "Go/General",
                "Reports circular import cycles between Go packages.",
                HighlightSeverity.ERROR, true, "Go");
        addTool("Go.DeprecatedElement", "Deprecated element", "Go/General",
                "Reports usage of deprecated symbols in Go packages.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DetectUsagesOfRuntimeSetFinalizer", "Detect usages of runtime.SetFinalizer", "Go/General",
                "Reports usages of runtime.SetFinalizer which can cause memory leaks and unpredictable behavior.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DisabledGopathAnalysis", "Disabled GOPATH analysis", "Go/General",
                "Reports packages located outside configured GOPATH or Go module boundaries.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.FuzzingSupportedStartingWithGo118", "Fuzzing is supported starting with Go 1.18", "Go/General",
                "Reports fuzz tests used with Go language levels earlier than 1.18.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.MalformedTestFunctionName", "Malformed test function name", "Go/General",
                "Reports test, benchmark, or example function names that do not match Go testing conventions.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.RedundantParentheses", "Redundant parentheses", "Go/General",
                "Reports parentheses that are not required by operator precedence.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnexportedReturnTypeOfExportedFunction", "Unexported return type of the exported function", "Go/General",
                "Reports exported functions returning unexported types that make caller usage cumbersome.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnnecessarilyExportedIdentifier", "Unnecessarily exported identifier", "Go/General",
                "Reports exported package symbols that are only used within their own package.",
                HighlightSeverity.WARNING, false, "Go");
        addTool("Go.UsageOfInterfaceAsType", "Usage of 'interface{}' as a type", "Go/General",
                "Suggests replacing 'interface{}' with 'any' in Go 1.18+ code.",
                HighlightSeverity.WARNING, false, "Go");
        addTool("Go.UsageOfContextTodo", "Usage of context.TODO()", "Go/General",
                "Reports placeholders using context.TODO() where an explicit context should be passed.",
                HighlightSeverity.WARNING, false, "Go");

        // Subcategory: Probable bugs (Images 2 & 3)
        addTool("Go.ContextCancelFuncNotCalled", "'context.CancelFunc' is not called", "Go/Probable bugs",
                "Reports context cancel functions returned by WithCancel, WithTimeout, etc. that are not deferred or called.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DeferGoCallsRecoverOrPanicDirectly", "Defer/go statement calls 'recover' or 'panic' directly", "Go/Probable bugs",
                "Reports defer or go statements calling recover() or panic() directly instead of wrapping in a function.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DirectComparisonOfErrors", "Direct comparison of errors", "Go/Probable bugs",
                "Suggests using errors.Is() instead of direct equality comparison on wrapped error values.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.DivisionByZero", "Division by zero", "Go/Probable bugs",
                "Reports division or remainder operations where the divisor evaluates to zero.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ExceededShiftExpression", "Exceeded shift expression", "Go/Probable bugs",
                "Reports bitwise shift counts that exceed the width of the target integer type.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.FailNowInNonTestGoroutine", "'FailNow' in a non-test goroutine", "Go/Probable bugs",
                "Reports calls to t.FailNow, t.Fatal, etc. inside goroutines spawned during test execution.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ImportedPackageNameAsNameIdentifier", "Imported package name as a name identifier", "Go/Probable bugs",
                "Reports local variables that shadow imported package names.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ImpossibleInterfaceTypeAssertion", "Impossible interface type assertion", "Go/Probable bugs",
                "Reports type assertions to interfaces that cannot possibly be implemented by the source type.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IncorrectStringsReplaceCountArgument", "Incorrect 'strings.Replace' count argument", "Go/Probable bugs",
                "Reports strings.Replace calls where count argument is likely mistaken.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IncorrectUsageOfFmtFunctions", "Incorrect usage of 'fmt.Printf' and 'fmt.Println' functions", "Go/Probable bugs",
                "Reports format string and argument mismatches in fmt package functions.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IncorrectUsageOfErrorsAs", "Incorrect usage of the 'errors.As' function", "Go/Probable bugs",
                "Reports errors.As calls where target argument is not a non-nil pointer to an error type.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IncorrectUsageOfSyncAtomic", "Incorrect usage of the 'sync/atomic' package", "Go/Probable bugs",
                "Reports direct 64-bit atomic operations on non-aligned memory structures.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IntegerToStringTypeConversion", "Integer to string type conversion", "Go/Probable bugs",
                "Reports conversion string(int) which converts code point to rune string rather than decimal representation.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.InvalidConversionsUintptrToUnsafePointer", "Invalid conversions of 'uintptr' to 'unsafe.Pointer'", "Go/Probable bugs",
                "Reports unsafe conversions that violate GC pointer tracking rules.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.IrregularUsageOfIota", "Irregular usage of 'iota'", "Go/Probable bugs",
                "Reports non-standard iota expressions in const blocks that may lead to unexpected ordinal values.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.LeadingWhitespaceInDirectiveComment", "Leading whitespace in directive comment", "Go/Probable bugs",
                "Reports compiler directive comments (e.g. //go:build) that contain invalid leading spaces.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.LocksMistakenlyPassedByValue", "Locks mistakenly passed by value", "Go/Probable bugs",
                "Reports sync.Mutex, sync.RWMutex, or types embedding them being copied by value.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.LoopVariablesCapturedByFuncLiteral", "Loop variables captured by the func literal", "Go/Probable bugs",
                "Reports loop variables captured in closure goroutines in Go versions prior to 1.22.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.MalformedBuildTag", "Malformed build tag", "Go/Probable bugs",
                "Reports invalid //go:build or // +build constraint syntax.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.MalformedStructTag", "Malformed struct tag", "Go/Probable bugs",
                "Reports struct tag strings that do not conform to reflect.StructTag key:\"value\" syntax.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.MissingCaseStatementsForIotaConstsInSwitch", "Missing 'case' statements for 'iota' consts in 'switch'", "Go/Probable bugs",
                "Reports switch statements over iota enum types that lack cases for all defined constants.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.MixedValueAndPointerReceivers", "Mixed value and pointer receivers", "Go/Probable bugs",
                "Reports structures with methods that use a mixture of types: value and pointer receivers.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.NonStandardSignatureForWellKnownFunctionNames", "Non-standard signature for well-known function names", "Go/Probable bugs",
                "Reports functions named 'init', 'main', or 'Error' with incorrect parameters or return types.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ReservedWordUsedAsName", "Reserved word used as name", "Go/Probable bugs",
                "Reports identifiers using Go predefined type or constant names like 'true', 'len', 'int'.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.ShadowingVariable", "Shadowing variable", "Go/Probable bugs",
                "Reports variables that shadow earlier declarations in an outer block.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.GoDebugDirectiveIgnoredEarlierVersions", "The 'go:debug' directive is ignored in Go versions earlier than 1.21", "Go/Probable bugs",
                "Reports //go:debug directives used in modules targeting Go < 1.21.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.GoDebugDirectiveMustBePlacedBeforePackageClause", "The 'go:debug' directive must be placed before the package clause", "Go/Probable bugs",
                "Reports //go:debug directives placed after the package declaration clause.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.TypeAssertionOnErrorsFailsOnWrappedErrors", "Type assertion on errors fails on wrapped errors", "Go/Probable bugs",
                "Suggests using errors.As() instead of direct type assertions on potentially wrapped errors.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnhandledError", "Unhandled error", "Go/Probable bugs",
                "Reports function calls returning errors whose error value is never handled.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnmarshalCalledWithIncorrectArgument", "'Unmarshal' is called with the incorrect argument", "Go/Probable bugs",
                "Reports json.Unmarshal or xml.Unmarshal calls passing non-pointer arguments.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("Go.UnusedFunctionOrMethodCallResult", "Unused function or method call result", "Go/Probable bugs",
                "Reports calls to pure functions whose return values are discarded.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: Security (Image 3)
        addTool("Go.VulnerableApiUsageGeneric", "Vulnerable API usage", "Go/Security",
                "Reports usages of known vulnerable Go APIs and security risk patterns.",
                HighlightSeverity.WARNING, false, "Go");
        addTool("Go.VulnerableApiUsageCve", "Vulnerable API usage", "Go/Security",
                "Reports calls to APIs vulnerable to known CVE vulnerabilities.",
                HighlightSeverity.WARNING, true, "Go");

        // 22. Go modules ([✓] with subcategories from Image 3)
        // Subcategory: Declaration redundancy
        addTool("GoModules.UnusedDependency", "Unused dependency", "Go modules/Declaration redundancy",
                "Reports direct module dependencies in go.mod that are not imported by any package in the module.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: Dependency issues (go list -m -u)
        addTool("GoModules.DependencyUpdateAvailable", "Dependency update available", "Go modules/Dependency issues (go list -m -u)",
                "Reports module dependencies that have newer releases available.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("GoModules.DeprecatedDependency", "Deprecated dependency", "Go modules/Dependency issues (go list -m -u)",
                "Reports module dependencies that have been marked as deprecated by module authors.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("GoModules.MultipleRequireDirectives", "Multiple 'require' directives can be merged in groups by dependency t", "Go modules/Dependency issues (go list -m -u)",
                "Reports multiple separate 'require' blocks that can be merged into grouped require declarations.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("GoModules.RetractedDependencyVersion", "Retracted dependency version", "Go modules/Dependency issues (go list -m -u)",
                "Reports dependencies resolved to versions that have been retracted by authors.",
                HighlightSeverity.WARNING, true, "Go");

        // Subcategory: General
        addTool("GoModules.MigrationToGoWorkspacePossible", "Migration to Go workspace is possible", "Go modules/General",
                "Suggests creating a go.work workspace when multiple Go modules are detected in the project.",
                HighlightSeverity.WARNING, true, "Go");
        addTool("GoModules.UnresolvedPathInIgnoreDirective", "Unresolved path in 'ignore' directive", "Go modules/General",
                "Reports invalid or non-existent filesystem paths specified in go.work 'ignore' directives.",
                HighlightSeverity.WARNING, true, "Go");

        // 23. Gradle ([✓] with subcategories from Image 4)
        // Subcategory: Best practises
        addTool("Gradle.ConfigurationAvoidance", "Configuration avoidance", "Gradle/Best practises",
                "Detects usage of API that interacts with tasks eagerly instead of lazy task configuration.",
                HighlightSeverity.WARNING, true, "Gradle");

        // Subcategory: Probable bugs
        addTool("Gradle.BintrayPublishingPluginDeprecated", "Bintray publishing plugin may stop working on May 1st, 2021", "Gradle/Probable bugs",
                "Reports usage of the obsolete Bintray publishing plugin.",
                HighlightSeverity.WARNING, true, "Gradle");
        addTool("Gradle.JCenterResolutionDeprecated", "Builds will no longer be able to resolve artifacts from JCenter after Feb", "Gradle/Probable bugs",
                "Reports usage of jcenter() repository which is sunset and should be migrated to mavenCentral().",
                HighlightSeverity.WARNING, true, "Gradle");
        addTool("Gradle.MultipleRepositoryUrls", "Multiple repository urls", "Gradle/Probable bugs",
                "Reports repository declarations configured with duplicate URLs.",
                HighlightSeverity.WARNING, true, "Gradle");
        addTool("Gradle.PossiblyMisplacedCallToGradleMethod", "Possibly misplaced call to Gradle method", "Gradle/Probable bugs",
                "Reports Gradle API methods called outside their appropriate script closure context.",
                HighlightSeverity.WARNING, true, "Gradle");
        addTool("Gradle.UnrecognizedDependencyNotation", "Unrecognized dependency notation", "Gradle/Probable bugs",
                "Reports dependency string coordinates that do not follow standard group:name:version format.",
                HighlightSeverity.WARNING, true, "Gradle");

        // Subcategory: Style
        addTool("Gradle.UnusedVersionCatalogEntry", "Unused version catalog entry", "Gradle/Style",
                "Reports libraries, plugins, and versions in libs.versions.toml that are never referenced.",
                HighlightSeverity.WARNING, true, "Gradle");

        // Subcategory: Validity issues
        addTool("Gradle.DeprecatedConfigurations", "Deprecated configurations", "Gradle/Validity issues",
                "Reports usage of deprecated dependency configurations such as 'compile' instead of 'implementation'.",
                HighlightSeverity.WARNING, true, "Gradle");
        addTool("Gradle.PluginDslStructure", "Plugin DSL structure", "Gradle/Validity issues",
                "Reports invalid plugins {} block syntax or instructions violating plugin DSL constraints.",
                HighlightSeverity.ERROR, true, "Gradle");

        // 24. Gradle Declarative ([✓] from Image 4)
        addTool("GradleDeclarative.UnresolvedReference", "Unresolved reference", "Gradle Declarative",
                "Reports unresolved schema elements, plugins, or properties in declarative Gradle build scripts.",
                HighlightSeverity.WARNING, true, "Gradle");

        // 25. Groovy ([-] with subcategories from Images 4 & 5)
        // Subcategory: Annotations (Image 5)
        addTool("Groovy.DelegatesTo", "@DelegatesTo", "Groovy/Annotations",
                "Validates @DelegatesTo closure parameters and target delegation strategy.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NamedVariantLabels", "@NamedVariant/@NamedParam/@NamedDelegate unresolved label", "Groovy/Annotations",
                "Reports unresolved argument labels in calls of methods annotated by @NamedVariant / @NamedParam / @NamedDelegate.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.PojoWithoutCompileStatic", "@POJO without @CompileStatic", "Groovy/Annotations",
                "Reports classes annotated with @POJO that do not have @CompileStatic enabled.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.SingletonConstructors", "@Singleton constructors", "Groovy/Annotations",
                "Reports non-private constructors in classes annotated with @Singleton.",
                HighlightSeverity.ERROR, true, "Groovy");
        addTool("Groovy.TupleConstructorAndMapConstructor", "@TupleConstructor and @MapConstructor", "Groovy/Annotations",
                "Reports incompatible AST transformation configurations on constructor generators.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Assignment issues (Image 5)
        addTool("Groovy.AssignmentReplaceWithOperator", "Assignment can be replaced with operator assignment", "Groovy/Assignment issues",
                "Suggests replacing x = x + y with compound operator assignment x += y.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.AssignmentToForLoopParameter", "Assignment to 'for' loop parameter", "Groovy/Assignment issues",
                "Reports reassignments to iteration variables inside 'for' loop bodies.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.AssignmentToMethodParameter", "Assignment to method parameter", "Groovy/Assignment issues",
                "Reports reassignments to method parameters.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.IncompatibleTypeAssignments", "Incompatible type assignments", "Groovy/Assignment issues",
                "Reports assignments where the value type is incompatible with the declared variable type.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NestedAssignment", "Nested assignment", "Groovy/Assignment issues",
                "Reports assignment expressions embedded inside other expressions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ResultOfAssignmentUsed", "Result of assignment used", "Groovy/Assignment issues",
                "Reports expressions that use the evaluated result of an assignment statement.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.SillyAssignment", "Silly assignment", "Groovy/Assignment issues",
                "Reports useless self-assignments such as x = x.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UncheckedAssignmentFromRawType", "Unchecked assignment from members of raw type", "Groovy/Assignment issues",
                "Reports assignments from raw generic types to parameterized generic targets.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Control flow issues (Image 5)
        addTool("Groovy.BreakStatement", "'break' statement", "Groovy/Control flow issues",
                "Reports 'break' statements outside switch or loop constructs.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ConstantConditionalExpression", "Constant conditional expression", "Groovy/Control flow issues",
                "Reports conditional expressions with constant boolean conditions.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ConstantIfStatement", "Constant if statement", "Groovy/Control flow issues",
                "Reports 'if' statements with constant true or false conditions.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ContinueStatement", "'continue' statement", "Groovy/Control flow issues",
                "Reports 'continue' statements that are redundant or misplaced.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.FallthroughInSwitchStatement", "Fallthrough in 'switch' statement", "Groovy/Control flow issues",
                "Reports switch cases that fall through to the next case without a break statement.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.FinalVariableAccess", "Final variable access", "Groovy/Control flow issues",
                "Reports reads of final fields before they have been initialized.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.IfStatementWithIdenticalBranches", "If statement with identical branches", "Groovy/Control flow issues",
                "Reports 'if-else' statements where 'then' and 'else' branches execute identical code.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.IfStatementWithTooManyBranches", "If statement with too many branches", "Groovy/Control flow issues",
                "Reports 'if' statements with excessive numbers of 'else if' branches.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.LoopStatementThatDoesntLoop", "Loop statement that doesn't loop", "Groovy/Control flow issues",
                "Reports loop statements whose body unconditionally returns or breaks on the first iteration.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.MultipleMainMethods", "Multiple main methods", "Groovy/Control flow issues",
                "Reports multiple main method entry points declared in a single script or class.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NestedClassWithInstanceMainMethod", "Nested class with instance main method", "Groovy/Control flow issues",
                "Reports non-static main methods inside inner classes.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.RedundantIfStatement", "Redundant 'if' statement", "Groovy/Control flow issues",
                "Reports 'if' statements that can be simplified to a single boolean expression.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.RedundantConditionalExpression", "Redundant conditional expression", "Groovy/Control flow issues",
                "Reports ternary expressions evaluating condition ? true : false.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ReturnStatementCanBeImplicit", "'return' statement can be implicit", "Groovy/Control flow issues",
                "Suggests removing explicit 'return' on trailing expressions in Groovy methods.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.SwitchStatementWithNoDefaultCase", "Switch statement with no default case", "Groovy/Control flow issues",
                "Reports switch statements that do not include a 'default' branch.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.TernaryReplaceWithElvis", "Ternary expression can be replaced with elvis expression", "Groovy/Control flow issues",
                "Suggests replacing x ? x : y with the Elvis operator x ?: y.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.TernaryReplaceWithSafeCall", "Ternary expression can be replaced with safe call", "Groovy/Control flow issues",
                "Suggests replacing x != null ? x.foo() : null with safe navigation x?.foo().",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.TernaryWithIdenticalBranches", "Ternary expression with identical branches", "Groovy/Control flow issues",
                "Reports conditional expressions returning the same value on both branches.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryContinueStatement", "Unnecessary 'continue' statement", "Groovy/Control flow issues",
                "Reports 'continue' statements positioned at the very end of loop bodies.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryReturnStatement", "Unnecessary 'return' statement", "Groovy/Control flow issues",
                "Reports 'return' statements returning void at the very end of void methods.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Additional Groovy subcategories (Image 4)
        // Subcategory: Data flow (Image 1)
        addTool("Groovy.MissingReturnStatement", "Missing return statement", "Groovy/Data flow",
                "Reports methods that have non-void return types but can exit without returning a value.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnusedAssignment", "Unused assignment", "Groovy/Data flow",
                "Reports variable assignments whose assigned value is never read.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnusedIncrementingOrDecrementing", "Unused incrementing or decrementing", "Groovy/Data flow",
                "Reports ++ or -- operations whose modified value is never read afterwards.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.VariableCanBeFinal", "Variable can be final", "Groovy/Data flow",
                "Reports variables that are never mutated after initialization and can be marked final.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.VariableNotAssigned", "Variable not assigned", "Groovy/Data flow",
                "Reports variables that are declared but never initialized or assigned a value.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Declaration redundancy (Image 1)
        addTool("Groovy.UnusedDeclaration", "Unused declaration", "Groovy/Declaration redundancy",
                "Reports classes, methods, or fields that are declared but never referenced.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Error handling (Image 1)
        addTool("Groovy.ContinueOrBreakFromFinallyBlock", "'continue' or 'break' from 'finally' block", "Groovy/Error handling",
                "Reports 'continue' or 'break' statements inside 'finally' blocks which discard exceptions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.EmptyCatchBlock", "Empty 'catch' block", "Groovy/Error handling",
                "Reports 'catch' blocks that contain no statements or comments.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.EmptyFinallyBlock", "Empty 'finally' block", "Groovy/Error handling",
                "Reports 'finally' blocks that contain no statements.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.EmptyTryBlock", "Empty 'try' block", "Groovy/Error handling",
                "Reports 'try' blocks that contain no executable statements.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ReturnInsideFinallyBlock", "'return' inside 'finally' block", "Groovy/Error handling",
                "Reports 'return' statements inside 'finally' blocks which can suppress unhandled exceptions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ThrowInsideFinallyBlock", "'throw' inside 'finally' block", "Groovy/Error handling",
                "Reports 'throw' statements inside 'finally' blocks which can obscure earlier exceptions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.UnusedCatchParameter", "Unused 'catch' parameter", "Groovy/Error handling",
                "Reports exception parameters in 'catch' clauses that are never referenced.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: GPath (Image 1)
        addTool("Groovy.CallToListGetCanBeKeyed", "Call to List.get can be keyed access", "Groovy/GPath",
                "Suggests replacing list.get(index) with subscript index notation list[index].",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.CallToListSetCanBeKeyed", "Call to List.set can be keyed access", "Groovy/GPath",
                "Suggests replacing list.set(index, value) with subscript assignment list[index] = value.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.CallToMapGetCanBeKeyed", "Call to Map.get can be keyed access", "Groovy/GPath",
                "Suggests replacing map.get(key) with property or subscript notation map[key].",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.CallToMapPutCanBeKeyed", "Call to Map.put can be keyed access", "Groovy/GPath",
                "Suggests replacing map.put(key, value) with subscript assignment map[key] = value.",
                HighlightSeverity.WARNING, false, "Groovy");

        // Subcategory: Method metrics (Image 1)
        addTool("Groovy.MethodWithMoreThanThreeNegations", "Method with more than three negations", "Groovy/Method metrics",
                "Reports methods containing more than three boolean negation '!' operations.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.MethodWithMultipleReturnPoints", "Method with multiple return points", "Groovy/Method metrics",
                "Reports methods that have more than one exit 'return' point.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.MethodWithTooManyParameters", "Method with too many parameters", "Groovy/Method metrics",
                "Reports methods with parameter counts exceeding configured thresholds.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.OverlyComplexMethod", "Overly complex method", "Groovy/Method metrics",
                "Reports methods exceeding configured cyclomatic complexity thresholds.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.OverlyLongMethod", "Overly long method", "Groovy/Method metrics",
                "Reports methods that exceed configured statement or line count limits.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.OverlyNestedMethod", "Overly nested method", "Groovy/Method metrics",
                "Reports methods exceeding maximum configured control statement nesting depth.",
                HighlightSeverity.WARNING, false, "Groovy");

        // Subcategory: Naming conventions (Image 2)
        addTool("Groovy.ClassNamingConvention", "Class naming convention", "Groovy/Naming conventions",
                "Reports class names that do not follow configured naming convention regular expressions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ConstantNamingConvention", "Constant naming convention", "Groovy/Naming conventions",
                "Reports constant names that do not adhere to uppercase naming conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.InstanceMethodNamingConvention", "Instance method naming convention", "Groovy/Naming conventions",
                "Reports instance methods whose names are too short, too long, or do not follow regex patterns.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.InstanceVariableNamingConvention", "Instance variable naming convention", "Groovy/Naming conventions",
                "Reports instance field names that do not match configured naming conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.LocalVariableNamingConvention", "Local variable naming convention", "Groovy/Naming conventions",
                "Reports local variable names that violate naming pattern conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.MethodParameterNamingConvention", "Method parameter naming convention", "Groovy/Naming conventions",
                "Reports method parameter names that violate naming pattern conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.StaticMethodNamingConvention", "Static method naming convention", "Groovy/Naming conventions",
                "Reports static method names that do not conform to configured naming conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.StaticVariableNamingConvention", "Static variable naming convention", "Groovy/Naming conventions",
                "Reports static variable names that do not conform to configured naming conventions.",
                HighlightSeverity.WARNING, false, "Groovy");

        // Subcategory: Other (Image 2)
        addTool("Groovy.MethodCanBeMadeStatic", "Method can be made 'static'", "Groovy/Other",
                "Reports instance methods that do not reference any instance state and can safely be static.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.TypeCustomizerInspection", "Type customizer inspection", "Groovy/Other",
                "Reports AST compilation customizers and script configuration mismatches.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Potentially confusing code constructs (Image 2)
        addTool("Groovy.ClashingGetters", "Clashing getters", "Groovy/Potentially confusing code constructs",
                "Reports multiple getter methods that resolve to the same property name.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ClashingTraitMethods", "Clashing trait methods", "Groovy/Potentially confusing code constructs",
                "Reports conflicting default method implementations inherited from multiple traits.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.DeprecatedApiUsage", "Deprecated API usage", "Groovy/Potentially confusing code constructs",
                "Reports usage of deprecated classes, methods, or properties.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.DoubleNegation", "Double negation", "Groovy/Potentially confusing code constructs",
                "Reports double negation expressions '!!' that can be simplified.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.GStringMapKey", "GString map key", "Groovy/Potentially confusing code constructs",
                "Reports GString instances used as map keys where string comparison by hash code may fail.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ImplicitNullArgument", "Implicit null argument", "Groovy/Potentially confusing code constructs",
                "Reports method invocations with omitted trailing arguments implicitly passed as null.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.LocalVariableReassignedInClosure", "Local variable is reassigned in closure or anonymous class", "Groovy/Potentially confusing code constructs",
                "Reports local variables that are modified from within enclosing closures or anonymous classes.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NegatedConditionalExpression", "Negated conditional expression", "Groovy/Potentially confusing code constructs",
                "Reports conditional expressions whose condition is explicitly negated.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NegatedIfConditionExpression", "Negated if condition expression", "Groovy/Potentially confusing code constructs",
                "Reports 'if' conditions that are negated with an 'else' block available to swap.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NestedConditionalExpression", "Nested conditional expression", "Groovy/Potentially confusing code constructs",
                "Reports nested ternary conditional expressions which decrease code readability.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NestedSwitchStatement", "Nested switch statement", "Groovy/Potentially confusing code constructs",
                "Reports switch statements nested inside other switch statements.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NewInstanceOfSingletonClass", "New instance of class annotated with @groovy.lang.Singleton", "Groovy/Potentially confusing code constructs",
                "Reports direct constructor calls on classes that declare @groovy.lang.Singleton.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.OctalInteger", "Octal integer", "Groovy/Potentially confusing code constructs",
                "Reports integer literals with leading zeroes that are interpreted as base-8 octal values.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.OverlyComplexArithmeticExpression", "Overly complex arithmetic expression", "Groovy/Potentially confusing code constructs",
                "Reports arithmetic expressions containing too many operators.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.OverlyComplexBooleanExpression", "Overly complex boolean expression", "Groovy/Potentially confusing code constructs",
                "Reports boolean expressions containing too many chained logical operators.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.PackageMismatch", "Package mismatch", "Groovy/Potentially confusing code constructs",
                "Reports package declarations that do not match directory path on the filesystem.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.PointlessArithmeticExpression", "Pointless arithmetic expression", "Groovy/Potentially confusing code constructs",
                "Reports arithmetic operations with identity values (e.g. + 0, * 1).",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.PointlessBooleanExpression", "Pointless boolean expression", "Groovy/Potentially confusing code constructs",
                "Reports boolean expressions that contain redundant boolean literal operands.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ResultOfIncrementOrDecrementUsed", "Result of increment or decrement used", "Groovy/Potentially confusing code constructs",
                "Reports expressions that rely on the evaluated result of prefix or postfix ++/-- operators.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.StatementWithEmptyBody", "Statement with empty body", "Groovy/Potentially confusing code constructs",
                "Reports control statements (if, while, for) that have empty body blocks.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryQualifiedReference", "Unnecessary qualified reference", "Groovy/Potentially confusing code constructs",
                "Reports fully qualified class references where an imported simple name could be used.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Probable bugs (Image 3)
        addTool("Groovy.DivisionByZero", "Division by zero", "Groovy/Probable bugs",
                "Reports division or modulus expressions where the denominator evaluates to zero.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.EqualsBetweenInconvertibleTypes", "'equals()' between objects of inconvertible types", "Groovy/Probable bugs",
                "Reports equals() calls between object types that share no common hierarchy.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.ExhaustivenessCheckForSwitch", "Exhaustiveness check for switch expressions", "Groovy/Probable bugs",
                "Reports switch statements on enums or sealed hierarchies that do not cover all cases.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.InaccessibleElement", "Inaccessible element", "Groovy/Probable bugs",
                "Reports references to private or package-private members from unauthorized contexts.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.IncompatibleInArgumentTypes", "Incompatible 'in' argument types", "Groovy/Probable bugs",
                "Reports 'in' expressions where target type cannot contain elements of the test type.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.IncorrectRangeArguments", "Incorrect range arguments", "Groovy/Probable bugs",
                "Reports range expressions with descending bounds or incompatible endpoint types.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.InfiniteLoopStatement", "Infinite loop statement", "Groovy/Probable bugs",
                "Reports loops that have no terminating condition or break statement.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.InfiniteRecursion", "Infinite recursion", "Groovy/Probable bugs",
                "Reports methods that unconditionally call themselves with identical arguments.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.LabeledStatementInspection", "Labeled statement inspection", "Groovy/Probable bugs",
                "Reports labeled statements that can be mistaken for map keys or goto labels.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NamedArgumentsOfConstructorCall", "Named arguments of constructor call", "Groovy/Probable bugs",
                "Reports constructor invocations with unrecognized named parameter property names.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.NonExtendingPermittedSubclasses", "Non-extending permitted subclasses", "Groovy/Probable bugs",
                "Reports classes listed in 'permits' clauses that do not actually extend the sealed type.",
                HighlightSeverity.ERROR, true, "Groovy");
        addTool("Groovy.NonShortCircuitBoolean", "Non short-circuit boolean", "Groovy/Probable bugs",
                "Reports bitwise & and | operators used where short-circuit && or || was intended.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.ResultOfObjectAllocationIgnored", "Result of object allocation ignored", "Groovy/Probable bugs",
                "Reports object instantiation expressions whose created instance is immediately discarded.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.SecondUnsafeCall", "Second unsafe call", "Groovy/Probable bugs",
                "Reports unsafe navigation calls chained on the result of a safe navigation ?. operator.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnresolvedGroovyDocReference", "Unresolved GroovyDoc reference", "Groovy/Probable bugs",
                "Reports @see or {@link} tags in GroovyDoc comments that cannot be resolved.",
                HighlightSeverity.ERROR, true, "Groovy");
        addTool("Groovy.UnresolvedReferenceExpression", "Unresolved reference expression", "Groovy/Probable bugs",
                "Reports unresolved variable, property, or method references.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UntypedReferenceExpression", "Untyped reference expression", "Groovy/Probable bugs",
                "Reports dynamic references whose type cannot be statically inferred by the compiler.",
                HighlightSeverity.WARNING, false, "Groovy");

        // Subcategory: Style (Image 3)
        addTool("Groovy.JavaStylePropertyAccess", "Java-style property access", "Groovy/Style",
                "Suggests using idiomatic Groovy property syntax instead of explicit get/set calls.",
                HighlightSeverity.WEAK_WARNING, true, "Groovy");
        addTool("Groovy.MethodCallReplacedWithOperator", "Method call can be replaced with operator invocation", "Groovy/Style",
                "Suggests replacing plus(), minus(), multiply() with standard +, -, * operators.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.OperatorInvocationReplacedWithMethodCall", "Operator invocation can be replaced with method call", "Groovy/Style",
                "Suggests replacing operator expressions with explicit method calls.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.StringStyleViolation", "String style violation", "Groovy/Style",
                "Reports string literals that violate configured single vs double quote conventions.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.TernaryExpression", "Ternary expression", "Groovy/Style",
                "Reports ternary expressions that can be rewritten with clearer if-else blocks.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.UnnecessaryDef", "Unnecessary 'def'", "Groovy/Style",
                "Reports redundant 'def' modifiers where a concrete type or other modifier is already specified.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryFinal", "Unnecessary 'final'", "Groovy/Style",
                "Reports redundant 'final' modifiers in contexts where immutability is implicit.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryNonSealedModifier", "Unnecessary 'non-sealed' modifier", "Groovy/Style",
                "Reports redundant non-sealed modifiers on classes.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryPublic", "Unnecessary 'public'", "Groovy/Style",
                "Reports explicit 'public' modifiers which are redundant since members are public by default.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessarySealedModifier", "Unnecessary 'sealed' modifier", "Groovy/Style",
                "Reports sealed modifiers on classes that have no permitting constraints.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessaryImportAlias", "Unnecessary import alias", "Groovy/Style",
                "Reports import aliases 'as Name' that are identical to the imported class name.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnnecessarySemicolon", "Unnecessary semicolon", "Groovy/Style",
                "Reports trailing semicolons that are optional in Groovy syntax.",
                HighlightSeverity.WARNING, true, "Groovy");

        // Subcategory: Threading issues (Image 4)
        addTool("Groovy.AccessToStaticFieldLockedOnInstance", "Access to static field locked on instance data", "Groovy/Threading issues",
                "Reports synchronization on instance objects while mutating shared static fields.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.BusyWait", "Busy wait", "Groovy/Threading issues",
                "Reports spinning wait loops that don't sleep or yield processor time.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.CallToSystemRunFinalizersOnExit", "Call to System.runFinalizersOnExit()", "Groovy/Threading issues",
                "Reports calls to dangerous and deprecated System.runFinalizersOnExit().",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.CallToThreadStopSuspendResume", "Call to Thread.stop(), Thread.suspend(), or Thread.resume()", "Groovy/Threading issues",
                "Reports calls to inherently deadlock-prone Thread control APIs.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.DoubleCheckedLocking", "Double-checked locking", "Groovy/Threading issues",
                "Reports double-checked locking patterns on fields that are not declared volatile.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.EmptySynchronizedBlock", "Empty 'synchronized' block", "Groovy/Threading issues",
                "Reports synchronized blocks that contain no statements.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NestedSynchronizedStatement", "Nested 'synchronized' statement", "Groovy/Threading issues",
                "Reports synchronized blocks nested inside other synchronized blocks.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NonPrivateFieldAccessedInSyncContext", "Non-private field accessed in synchronized context", "Groovy/Threading issues",
                "Reports non-private fields accessed inside synchronized methods without lock encapsulation.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.NotifyOrNotifyAllWhileNotSynced", "'notify()' or 'notifyAll()' while not synced", "Groovy/Threading issues",
                "Reports calls to notify() or notifyAll() on objects outside synchronized blocks.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.SynchronizationOnThis", "Synchronization on 'this'", "Groovy/Threading issues",
                "Reports synchronization on 'this' references which can lead to lock contention.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.SynchronizationOnNonFinalField", "Synchronization on non-final field", "Groovy/Threading issues",
                "Reports synchronization on fields that can be reassigned during concurrent execution.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.SynchronizationOnVariableInitLiteral", "Synchronization on variable initialized with literal", "Groovy/Threading issues",
                "Reports synchronization on String or numeric literal objects that may be interned.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.SynchronizedMethod", "Synchronized method", "Groovy/Threading issues",
                "Reports methods declared with 'synchronized' keyword instead of explicit mutex objects.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.UnconditionalWaitCall", "Unconditional 'wait' call", "Groovy/Threading issues",
                "Reports calls to Object.wait() that are not guarded by condition loops.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.UnsynchronizedMethodOverridesSynchronized", "Unsynchronized method overrides synchronized method", "Groovy/Threading issues",
                "Reports methods overriding synchronized superclass methods without synchronized modifier.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.WaitNotInLoop", "'wait()' not in loop", "Groovy/Threading issues",
                "Reports wait() invocations outside condition while loops, susceptible to spurious wakeups.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.WaitWhileNotSynced", "'wait()' while not synced", "Groovy/Threading issues",
                "Reports wait() called on an object without holding the corresponding monitor lock.",
                HighlightSeverity.WARNING, false, "Groovy");
        addTool("Groovy.WhileLoopSpinsOnField", "While loop spins on field", "Groovy/Threading issues",
                "Reports while loops spinning on non-volatile shared fields.",
                HighlightSeverity.WARNING, false, "Groovy");

        // Subcategory: Validity issues (Image 4)
        addTool("Groovy.DuplicateSwitchCase", "Duplicate switch case", "Groovy/Validity issues",
                "Reports duplicate case labels within the same switch statement.",
                HighlightSeverity.WARNING, true, "Groovy");
        addTool("Groovy.UnreachableStatement", "Unreachable statement", "Groovy/Validity issues",
                "Reports statements that cannot be reached due to prior unconditional transfer of control.",
                HighlightSeverity.WARNING, true, "Groovy");

        // 26. Hibernate ([-] from Image 5)
        addTool("Hibernate.ConfigurationXmlNotAddedToFacet", "Hibernate configuration XML is not added to facet", "Hibernate",
                "Reports Hibernate configuration XML files that are not registered in the project facet.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Hibernate.CriteriaApiUnrecognizedProperty", "Hibernate Criteria API unrecognized property", "Hibernate",
                "Reports unrecognized entity property names referenced in Criteria API queries.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Hibernate.IncorrectUseOfFindAnnotation", "Incorrect use of @Find annotation", "Hibernate",
                "Reports methods annotated with @Find whose parameter names or return types do not match entity fields.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Hibernate.InvalidDbRelatedXmlMappings", "Invalid Hibernate DB-related XML mappings", "Hibernate",
                "Reports database-related mapping errors in Hibernate XML files.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Hibernate.InvalidXmlConfiguration", "Invalid Hibernate XML configuration", "Hibernate",
                "Reports syntax errors and missing mandatory tags in hibernate.cfg.xml files.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Hibernate.InvalidXmlMappings", "Invalid Hibernate XML mappings", "Hibernate",
                "Reports entity mapping inconsistencies in Hibernate hbm.xml files.",
                HighlightSeverity.ERROR, true, "Java");

        // 27. HTML ([-] with subcategory and direct items from Image 5)
        // Subcategory: Accessibility
        addTool("HTML.MissingAssociatedLabel", "Missing associated label", "HTML/Accessibility",
                "Reports form input elements that do not have an associated <label> or aria-label.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.MissingRequiredAltAttribute", "Missing required 'alt' attribute", "HTML/Accessibility",
                "Reports <img>, <area>, and <input type='image'> elements missing an alt attribute.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.MissingRequiredLangAttribute", "Missing required 'lang' attribute", "HTML/Accessibility",
                "Reports <html> root elements missing a 'lang' language attribute.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.MissingRequiredSummaryAttribute", "Missing required 'summary' attribute", "HTML/Accessibility",
                "Reports <table> elements lacking summary information.",
                HighlightSeverity.WARNING, false, "HTML");
        addTool("HTML.MissingRequiredTitleAttribute", "Missing required 'title' attribute", "HTML/Accessibility",
                "Reports elements lacking required explanatory 'title' attributes.",
                HighlightSeverity.WARNING, false, "HTML");
        addTool("HTML.MissingRequiredTitleElement", "Missing required 'title' element", "HTML/Accessibility",
                "Reports HTML <head> sections missing the required <title> element.",
                HighlightSeverity.WARNING, true, "HTML");

        // Direct HTML items (Image 5)
        addTool("HTML.EmptyTag", "Empty tag", "HTML",
                "Reports non-void HTML tags that contain no content or whitespace.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.IncorrectBooleanAttribute", "Incorrect boolean attribute", "HTML",
                "Reports invalid boolean attribute assignments in HTML markup.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.MalformedContentOfScriptTag", "Malformed content of 'script' tag", "HTML",
                "Reports syntax errors inside embedded <script> tags.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("HTML.MismatchedImageSize", "Mismatched image size", "HTML",
                "Reports <img> elements whose width and height attributes differ from actual image pixel dimensions.",
                HighlightSeverity.WARNING, false, "HTML");
        addTool("HTML.MissingClosingTag", "Missing closing tag", "HTML",
                "Reports unclosed HTML elements.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.MissingRequiredAttribute", "Missing required attribute", "HTML",
                "Reports elements missing mandatory specification attributes.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.ObsoleteAttribute", "Obsolete attribute", "HTML",
                "Reports HTML attributes obsolete or deprecated in the HTML5 specification.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.ObsoleteTag", "Obsolete tag", "HTML",
                "Reports HTML tags obsolete or deprecated in the HTML5 specification.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.PresentationalTag", "Presentational tag", "HTML",
                "Reports purely presentational HTML elements (such as <font>, <center>, <b>) that should be replaced with CSS.",
                HighlightSeverity.WARNING, false, "HTML");
        addTool("HTML.RedundantClosingTag", "Redundant closing tag", "HTML",
                "Reports closing tags on void elements (such as <img> or <br>) that require no close tag.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.UnknownAttribute", "Unknown attribute", "HTML",
                "Reports unknown attribute names on standard HTML elements.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.UnknownTag", "Unknown tag", "HTML",
                "Reports unknown HTML tag names not recognized by the HTML specification.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.UnresolvedFileInLink", "Unresolved file in a link", "HTML",
                "Reports href or src attributes referencing local files that do not exist.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.UnresolvedFragmentInLink", "Unresolved fragment in a link", "HTML",
                "Reports anchor #hash fragment identifiers that match no element ID on the target page.",
                HighlightSeverity.WARNING, true, "HTML");
        addTool("HTML.UnresolvedWebLink", "Unresolved web link", "HTML",
                "Reports external HTTP/HTTPS hyperlinks that fail to resolve or return 404.",
                HighlightSeverity.WARNING, false, "HTML");
        addTool("HTML.WrongAttributeValue", "Wrong attribute value", "HTML",
                "Reports attribute values that violate enumerated or formatted specification requirements.",
                HighlightSeverity.WARNING, true, "HTML");

        // 28. HTTP Client ([✓] all 16 inspections from Image 1)
        addTool("HttpClient.AmbiguityEncoding", "Ambiguity Encoding Inspection", "HTTP Client",
                "Reports ambiguous URL encoding in HTTP requests.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.AuthConfigValidation", "Auth configuration validation", "HTTP Client",
                "Validates authentication configuration and credentials in HTTP client files.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.DuplicateImport", "Duplicate import", "HTTP Client",
                "Reports duplicate import declarations in HTTP request files.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.InappropriateProtocolUsage", "Inappropriate HTTP Protocol usage", "HTTP Client",
                "Reports unsupported or malformed HTTP protocol declarations.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.IncorrectHttpHeader", "Incorrect HTTP header", "HTTP Client",
                "Reports malformed or invalid HTTP request header names and values.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.MissingRequestSeparatorHtmlXml", "Missing request separator in HTML/XML body", "HTTP Client",
                "Reports missing blank line separator between headers and HTML/XML request body.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.MissingRequestSeparatorJson", "Missing request separator in JSON body", "HTTP Client",
                "Reports missing blank line separator between headers and JSON request body.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.MissingRequestSeparatorYaml", "Missing request separator in YAML body", "HTTP Client",
                "Reports missing blank line separator between headers and YAML request body.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.PlaceholderInHttpRequest", "'$placeholder' in HTTP Request", "HTTP Client",
                "Reports unreplaced placeholder syntax in HTTP request definitions.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.PossibleRequestName", "Possible request name", "HTTP Client",
                "Suggests converting comments preceding requests into named requests.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.RedundantContentLength", "Redundant 'Content-Length'", "HTTP Client",
                "Reports explicit Content-Length headers that are calculated automatically.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.UnknownHttpMethod", "Unknown HTTP method", "HTTP Client",
                "Reports unrecognized HTTP methods in request declarations.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.UnresolvedAuthIdentifier", "Unresolved Auth identifier", "HTTP Client",
                "Reports authentication identifiers that cannot be resolved in the current environment.",
                HighlightSeverity.ERROR, true, "HTTP Client");
        addTool("HttpClient.UnresolvedEnvironmentVariable", "Unresolved environment variable", "HTTP Client",
                "Reports referenced environment variables missing from active environment configuration.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.VariableShouldBeDoubleQuoted", "Variable should be double-quoted", "HTTP Client",
                "Reports HTTP variable references containing spaces that should be surrounded by double quotes.",
                HighlightSeverity.WARNING, true, "HTTP Client");
        addTool("HttpClient.WhitespaceInUrlInRequest", "Whitespace in URL in request", "HTTP Client",
                "Reports illegal whitespace characters contained within request URLs.",
                HighlightSeverity.WARNING, true, "HTTP Client");

        // 29. Inappropriate gRPC request scheme ([✓] from Image 1)
        addTool("Grpc.RequestSchemaCanBeSubstitutedOrOmitted", "GRPC request schema can be substituted or omitted", "Inappropriate gRPC request scheme",
                "Reports gRPC request schemes that can be substituted or omitted.",
                HighlightSeverity.WARNING, true, "General");

        // 30. Internationalization ([✓] from Image 1)
        addTool("I18n.LossyEncoding", "Lossy encoding", "Internationalization",
                "Reports characters that cannot be represented in the target character encoding without loss.",
                HighlightSeverity.WARNING, true, "General");
        addTool("I18n.NonAsciiCharacters", "Non-ASCII characters", "Internationalization",
                "Reports non-ASCII characters in resource bundles and property files.",
                HighlightSeverity.WARNING, true, "General");

        // 31. Jakarta Data ([✓] from Image 1)
        addTool("JakartaData.IncorrectRepositoryMethodParameter", "Incorrect repository method parameter", "Jakarta Data",
                "Reports Jakarta Data repository method parameters that do not match entity property types or query criteria.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JakartaData.RepositoryMethodErrors", "Jakarta Data repository method errors", "Jakarta Data",
                "Reports syntax, naming, and return type errors in Jakarta Data repository methods.",
                HighlightSeverity.WARNING, true, "Java");

        // 32. Java ([-] with 48 subcategories from Images 2 & 3)
        // Subcategory: Abstraction issues ([-] Indeterminate)
        addTool("Java.Abstraction.FeatureEnvy", "Feature envy", "Java/Abstraction issues",
                "Reports methods that access data of other classes more than their own.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Abstraction.InstanceofChain", "Instanceof chain", "Java/Abstraction issues",
                "Reports chains of 'if-else instanceof' statements that can be refactored with polymorphism.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Assignment issues ([-] Indeterminate)
        addTool("Java.Assignment.ForLoopParameter", "Assignment to for-loop parameter", "Java/Assignment issues",
                "Reports assignments to for-loop iteration variables.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Assignment.MethodParameter", "Assignment to method parameter", "Java/Assignment issues",
                "Reports assignments to incoming method parameters.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Bitwise operation issues ([✓] Checked)
        addTool("Java.Bitwise.IncompatibleMask", "Incompatible bitwise mask expression", "Java/Bitwise operation issues",
                "Reports bitwise expressions that always evaluate to constant or conflicting results.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Bitwise.PointlessExpression", "Pointless bitwise expression", "Java/Bitwise operation issues",
                "Reports bitwise operations that have no effect (e.g. & -1, | 0).",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Class metrics ([ ] Unchecked)
        addTool("Java.ClassMetrics.TooManyFields", "Class with too many fields", "Java/Class metrics",
                "Reports classes declaring more fields than the specified threshold.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.ClassMetrics.TooManyMethods", "Class with too many methods", "Java/Class metrics",
                "Reports classes declaring more methods than the specified threshold.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Class structure ([-] Indeterminate)
        addTool("Java.ClassStructure.ClassInitializer", "Class initializer", "Java/Class structure",
                "Reports static or instance class initializers that can be converted to constructors or field initializers.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.ClassStructure.MultipleTopLevelClasses", "Multiple top-level classes in single file", "Java/Class structure",
                "Reports source files that contain more than one top-level class.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Cloning issues ([-] Indeterminate)
        addTool("Java.Cloning.CloneableWithoutClone", "'Cloneable' class without 'clone()'", "Java/Cloning issues",
                "Reports classes that implement Cloneable but do not override clone().",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Cloning.CloneDoesNotCallSuper", "'clone()' does not call 'super.clone()'", "Java/Cloning issues",
                "Reports clone() methods that do not invoke super.clone().",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Code maturity ([-] Indeterminate)
        addTool("Java.CodeMaturity.DeprecatedApiUsage", "Deprecated API usage", "Java/Code maturity",
                "Reports usage of deprecated classes, methods, and fields.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.CodeMaturity.SystemOutPrintln", "Call to 'System.out.println'", "Java/Code maturity",
                "Reports console output calls that should be replaced with logging framework calls.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Code style issues ([✓] Checked)
        addTool("Java.CodeStyle.UnnecessarySemicolon", "Unnecessary semicolon", "Java/Code style issues",
                "Reports redundant semicolons.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.CodeStyle.RedundantCast", "Redundant type cast", "Java/Code style issues",
                "Reports explicit type casts that are unnecessary.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Compiler issues ([-] Indeterminate)
        addTool("Java.Compiler.UncheckedWarning", "Unchecked warning", "Java/Compiler issues",
                "Reports compiler unchecked conversion and raw type warnings.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Compiler.RawUseParameterizedClass", "Raw use of parameterized class", "Java/Compiler issues",
                "Reports generic classes used without type arguments.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Concurrency annotation issues ([-] Indeterminate)
        addTool("Java.Concurrency.GuardedByViolation", "GuardedBy violation", "Java/Concurrency annotation issues",
                "Reports field access or method invocations that violate @GuardedBy lock annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Concurrency.ImmutableClassMutableField", "Immutable class has mutable field", "Java/Concurrency annotation issues",
                "Reports classes annotated with @Immutable that contain mutable fields.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Control flow issues ([-] Indeterminate)
        addTool("Java.ControlFlow.LoopDoesNotLoop", "Loop statement that does not loop", "Java/Control flow issues",
                "Reports for, while, and do-while loops that execute at most once.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.ControlFlow.BreakWithLabel", "Break statement with label", "Java/Control flow issues",
                "Reports labeled break statements which can complicate control flow.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Data flow ([-] Indeterminate)
        addTool("Java.DataFlow.ConstantConditionsExceptions", "Constant conditions & exceptions", "Java/Data flow",
                "Reports conditions that are always true or false, and values that may cause NullPointerException.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.DataFlow.VariableNeverAssigned", "Variable is never assigned", "Java/Data flow",
                "Reports variables that are declared but never assigned a value.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Declaration redundancy ([✓] Checked)
        addTool("Java.DeclarationRedundancy.UnusedDeclaration", "Unused declaration", "Java/Declaration redundancy",
                "Reports classes, methods, and fields that are never referenced.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.DeclarationRedundancy.RedundantThrows", "Redundant 'throws' clause", "Java/Declaration redundancy",
                "Reports declared exceptions in method signatures that are never thrown.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Dependency issues ([-] Indeterminate)
        addTool("Java.Dependency.CyclicPackageDependency", "Cyclic package dependency", "Java/Dependency issues",
                "Reports cyclic dependencies between packages.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Dependency.MemberAccessAcrossModule", "Package-private member access across module", "Java/Dependency issues",
                "Reports package-private member accesses that cross module boundaries.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Encapsulation ([-] Indeterminate)
        addTool("Java.Encapsulation.PublicField", "Public field", "Java/Encapsulation",
                "Reports public non-final fields that expose class internal state.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Encapsulation.PackageVisibleField", "Package-visible field", "Java/Encapsulation",
                "Reports package-private fields that could be private with accessor methods.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Error handling ([-] Indeterminate)
        addTool("Java.ErrorHandling.EmptyCatchBlock", "Empty 'catch' block", "Java/Error handling",
                "Reports catch blocks that contain no statements.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.ErrorHandling.CaughtExceptionRethrown", "Caught exception is immediately rethrown", "Java/Error handling",
                "Reports catch blocks that only rethrow the caught exception.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Finalization ([-] Indeterminate)
        addTool("Java.Finalization.FinalizeCalledExplicitly", "'finalize()' called explicitly", "Java/Finalization",
                "Reports explicit invocations of finalize().",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Finalization.FinalizeDoesNotCallSuper", "'finalize()' does not call 'super.finalize()'", "Java/Finalization",
                "Reports finalize() implementations that do not call super.finalize().",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Imports ([-] Indeterminate)
        addTool("Java.Imports.UnusedImport", "Unused import", "Java/Imports",
                "Reports import statements that are never referenced.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Imports.OnDemandImport", "On-demand import", "Java/Imports",
                "Reports wildcard imports (e.g. import java.util.*) that can be replaced with single-type imports.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Inheritance issues ([-] Indeterminate)
        addTool("Java.Inheritance.RefactoringNeededSubclass", "Refactoring needed on subclass", "Java/Inheritance issues",
                "Reports inheritance hierarchies where subclasses violate the Liskov substitution principle.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Inheritance.AbstractClassWithoutAbstractMethods", "Abstract class without abstract methods", "Java/Inheritance issues",
                "Reports abstract classes that do not declare any abstract methods.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Initialization ([-] Indeterminate)
        addTool("Java.Initialization.NonFinalFieldInConstructor", "Non-final field initialized in constructor", "Java/Initialization",
                "Reports fields initialized in constructors that could be marked final.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Initialization.StaticFieldInConstructor", "Static field initialized in constructor", "Java/Initialization",
                "Reports static fields assigned inside instance constructors.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Internationalization ([-] Indeterminate)
        addTool("Java.I18n.HardcodedStringLiteral", "Hardcoded string literal", "Java/Internationalization",
                "Reports hardcoded user-visible text literals.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.I18n.CharacterComparison", "Character comparison", "Java/Internationalization",
                "Reports locale-sensitive character comparisons.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Java language level issues ([-] Indeterminate)
        addTool("Java.LanguageLevel.MigrationToJava21", "Language level migration to Java 21", "Java/Java language level issues",
                "Suggests modern Java 21 language constructs (pattern matching, record patterns).",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.LanguageLevel.DiamondOperator", "Diamond operator can be used", "Java/Java language level issues",
                "Reports constructor generic arguments that can use '<>'.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Java language level migration aids ([ ] Unchecked)
        addTool("Java.MigrationAids.EnhancedForLoop", "Replace with enhanced 'for' loop", "Java/Java language level migration aids",
                "Reports classic indexed loops that can be replaced with enhanced for-each loops.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.MigrationAids.ExplicitTypeToVar", "Explicit type can be replaced with 'var'", "Java/Java language level migration aids",
                "Reports local variable declarations where explicit types can be replaced with 'var'.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: JavaBeans issues ([-] Indeterminate)
        addTool("Java.JavaBeans.MissingGetterSetter", "Missing getter or setter for property", "Java/JavaBeans issues",
                "Reports JavaBeans property fields lacking corresponding accessor methods.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.JavaBeans.NonStandardGetterName", "Non-standard getter name", "Java/JavaBeans issues",
                "Reports getter methods that do not follow JavaBean naming conventions.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Javadoc ([✓] Checked)
        addTool("Java.Javadoc.DanglingJavadocComment", "Dangling Javadoc comment", "Java/Javadoc",
                "Reports Javadoc comments that are not attached to any declaration.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Javadoc.DeclarationHasJavadocProblems", "Declaration has Javadoc problems", "Java/Javadoc",
                "Reports missing tags, invalid tag syntax, and unresolved symbol references in Javadoc.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: JUnit ([-] Indeterminate)
        addTool("Java.JUnit.AssertionsInTestMethod", "Assertions in test method", "Java/JUnit",
                "Reports test methods that do not contain assertions.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.JUnit.MalformedSetUpTearDown", "Malformed setUp() or tearDown() method", "Java/JUnit",
                "Reports setUp() and tearDown() methods with invalid signatures.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Logging ([-] Indeterminate)
        addTool("Java.Logging.StringConcatInLog", "String concatenation argument to log call", "Java/Logging",
                "Reports string concatenation inside log calls that should use parameterized messages.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Logging.InvalidLoggerDeclaration", "Invalid logger declaration", "Java/Logging",
                "Reports logger field declarations that are not static, final, or properly named.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Lombok ([ ] Unchecked)
        addTool("Java.Lombok.DataOnEntityClass", "Lombok @Data on entity class", "Java/Lombok",
                "Reports @Data annotations on JPA entities which can cause circular equals/hashCode issues.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Lombok.BuilderDefaultValue", "Lombok @Builder default value", "Java/Lombok",
                "Reports initialized fields in classes annotated with @Builder missing @Builder.Default.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Memory ([-] Indeterminate)
        addTool("Java.Memory.StaticCollectionLeak", "Static collection leak", "Java/Memory",
                "Reports static collection fields that can grow indefinitely.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Memory.AnonymousInnerClassMayBeStatic", "Anonymous inner class may be static", "Java/Memory",
                "Reports anonymous inner classes that do not capture outer instance and could be static.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Method metrics ([ ] Unchecked)
        addTool("Java.MethodMetrics.OverlyComplexMethod", "Overly complex method", "Java/Method metrics",
                "Reports methods whose cyclomatic complexity exceeds threshold.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.MethodMetrics.TooManyParameters", "Method with too many parameters", "Java/Method metrics",
                "Reports methods with parameter counts exceeding threshold.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Modularization issues ([-] Indeterminate)
        addTool("Java.Modularization.UnusedModuleDeclaration", "Unused module declaration", "Java/Modularization issues",
                "Reports unused module-info 'requires' directives.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Modularization.NonExportedPackageReference", "Non-exported package reference", "Java/Modularization issues",
                "Reports references to packages that are not exported by their declaring module.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Naming conventions ([-] Indeterminate)
        addTool("Java.Naming.ClassNamingConvention", "Class naming convention", "Java/Naming conventions",
                "Reports class names that do not follow naming standards.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Naming.MethodNamingConvention", "Method naming convention", "Java/Naming conventions",
                "Reports method names that do not follow camelCase standards.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Numeric issues ([ ] Unchecked)
        addTool("Java.Numeric.IntegerMultiplicationImplicitCast", "Integer multiplication or shift implicit cast to long", "Java/Numeric issues",
                "Reports 32-bit integer arithmetic that may overflow before being assigned to a 64-bit long.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Numeric.FloatingPointEquality", "Floating point equality comparison", "Java/Numeric issues",
                "Reports equality comparison (== or !=) between floating point values.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Packaging issues ([-] Indeterminate)
        addTool("Java.Packaging.PackageMismatch", "Package name does not match directory", "Java/Packaging issues",
                "Reports package statements that do not reflect the physical directory path.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Packaging.ClassOutsideSourceRoot", "Class outside source root", "Java/Packaging issues",
                "Reports Java classes located outside designated source roots.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Performance ([ ] Unchecked)
        addTool("Java.Performance.BoxingInLoop", "Boxing/unboxing inside loop", "Java/Performance",
                "Reports auto-boxing or unboxing operations inside loop bodies.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Performance.StringConcatInLoop", "String concatenation in loop", "Java/Performance",
                "Reports String concatenation in loop bodies that should use StringBuilder.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Portability ([-] Indeterminate)
        addTool("Java.Portability.HardcodedFileSeparator", "Hardcoded file separator", "Java/Portability",
                "Reports hardcoded '/' or '\\' file path separators.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Portability.RuntimeExecCall", "Runtime.exec() call", "Java/Portability",
                "Reports invocations of Runtime.getRuntime().exec() which may be platform-dependent.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Probable bugs ([✓] Checked)
        addTool("Java.ProbableBugs.NullabilityDataFlow", "Nullability and data flow problems", "Java/Probable bugs",
                "Reports potential NullPointerException, contract violations, and unreachable code.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.ProbableBugs.ResultOfMethodCallIgnored", "Result of method call ignored", "Java/Probable bugs",
                "Reports calls to pure or immutable methods where the returned value is ignored.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Properties files ([✓] Checked)
        addTool("Java.PropertiesFiles.UnusedProperty", "Unused property in properties file", "Java/Properties files",
                "Reports property keys defined in properties files that are never referenced.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.PropertiesFiles.DuplicateProperty", "Duplicate property definition", "Java/Properties files",
                "Reports duplicate property keys defined in the same properties file.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Reflective access ([-] Indeterminate)
        addTool("Java.ReflectiveAccess.InvalidClassNameForName", "Class.forName() with invalid class name", "Java/Reflective access",
                "Reports Class.forName() invocations with literal string arguments that cannot be resolved.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.ReflectiveAccess.MethodInvokeAccessibility", "Method.invoke() accessibility violation", "Java/Reflective access",
                "Reports reflective invocations of private or package-private members without setAccessible(true).",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Resource management ([ ] Unchecked)
        addTool("Java.ResourceManagement.AutoCloseableNotClosed", "'AutoCloseable' resource is not closed", "Java/Resource management",
                "Reports AutoCloseable instances that are not managed in a try-with-resources statement.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.ResourceManagement.ChannelNotClosed", "Channel opened but not closed", "Java/Resource management",
                "Reports NIO Channels opened without guaranteed closing in finally blocks.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Security ([-] Indeterminate)
        addTool("Java.Security.InsecureRandom", "Insecure random number generator", "Java/Security",
                "Reports usage of java.util.Random where java.security.SecureRandom is required.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Security.HardcodedEncryptionKey", "Hardcoded encryption key", "Java/Security",
                "Reports cryptographic keys hardcoded directly in source code.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: Serialization issues ([-] Indeterminate)
        addTool("Java.Serialization.SerializableWithoutSerialVersionUID", "'Serializable' class without 'serialVersionUID'", "Java/Serialization issues",
                "Reports Serializable classes that do not declare a serialVersionUID field.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Serialization.NonSerializableFieldInSerializableClass", "Non-serializable field in 'Serializable' class", "Java/Serialization issues",
                "Reports non-transient, non-serializable fields in Serializable classes.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Test frameworks ([-] Indeterminate)
        addTool("Java.TestFrameworks.AssertWithoutMessage", "Assert without message", "Java/Test frameworks",
                "Reports test assertions lacking explanatory failure messages.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.TestFrameworks.AssertionCanBeSimplified", "Assertion can be simplified", "Java/Test frameworks",
                "Reports complex assertions that can be replaced with specialized methods.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: TestNG ([-] Indeterminate)
        addTool("Java.TestNG.TestMethodWithoutAssertions", "TestNG test method without assertions", "Java/TestNG",
                "Reports TestNG @Test methods that do not perform assertions.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.TestNG.ConflictingGroupDefinitions", "Conflicting TestNG group definitions", "Java/TestNG",
                "Reports inconsistent TestNG group dependencies and configurations.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Threading issues ([ ] Unchecked)
        addTool("Java.Threading.DoubleCheckedLocking", "Double-checked locking", "Java/Threading issues",
                "Reports double-checked locking patterns on non-volatile fields.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Java.Threading.SyncOnNonFinalField", "Synchronization on non-final field", "Java/Threading issues",
                "Reports synchronization on mutable non-final fields.",
                HighlightSeverity.WARNING, false, "Java");

        // Subcategory: toString() issues ([✓] Checked)
        addTool("Java.ToString.CallToArray", "Call to 'toString()' on array", "Java/toString() issues",
                "Reports calls to toString() on arrays which yield object identity rather than contents.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.ToString.FieldNotUsedInToString", "Field not used in 'toString()' method", "Java/toString() issues",
                "Reports non-static fields omitted from generated toString() implementations.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Verbose or redundant code constructs ([✓] Checked)
        addTool("Java.VerboseCode.RedundantTypeCast", "Redundant type cast", "Java/Verbose or redundant code constructs",
                "Reports type cast expressions that are unnecessary.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.VerboseCode.UnnecessaryLocalVariable", "Unnecessary local variable", "Java/Verbose or redundant code constructs",
                "Reports local variables that are declared and immediately returned or assigned.",
                HighlightSeverity.WARNING, true, "Java");

        // Subcategory: Visibility ([✓] Checked)
        addTool("Java.Visibility.AccessCanBePrivate", "Access can be private", "Java/Visibility",
                "Reports members whose visibility can be restricted to private.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Java.Visibility.MemberVisibilityCanBeWeakened", "Member visibility can be weakened", "Java/Visibility",
                "Reports package-private or protected members whose visibility can be made more restrictive.",
                HighlightSeverity.WARNING, true, "Java");

        // 33. Java EE ([✓] all 6 inspections from Image 1)
        addTool("JavaEE.AppDescriptorCorrectness", "Java EE application descriptor correctness", "Java EE",
                "Reports configuration and structural errors in application.xml deployment descriptors.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.MimeType", "MIME type", "Java EE",
                "Reports unrecognized or malformed MIME type declarations in web deployment descriptors.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.SecurityRoleNameCorrectness", "Security role name correctness", "Java EE",
                "Reports security-role-ref mappings that reference undefined security roles.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.ServletMapping", "Servlet mapping", "Java EE",
                "Reports servlet url-pattern mappings that are invalid or conflict with other mappings.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.WebXmlErrors", "Web.xml errors", "Java EE",
                "Reports structural and validation errors in web.xml deployment descriptors.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.WebXmlWarnings", "Web.xml warnings", "Java EE",
                "Reports deprecated configurations and potential issues in web.xml files.",
                HighlightSeverity.WARNING, true, "Java");

        // 34. JavaFX ([✓] all 8 inspections from Image 1)
        addTool("JavaFX.ColorOutOfRange", "Color component is out of range", "JavaFX",
                "Reports RGB or HSB color component values outside valid ranges in JavaFX and FXML.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.EventHandlerMethodSignature", "Event handler method signature problems", "JavaFX",
                "Reports FXML event handler methods whose signatures do not match the expected event type.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.RedundantPropertyValue", "JavaFX redundant property values", "JavaFX",
                "Reports FXML property values that match default control values.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.UnusedImports", "JavaFX unused imports", "JavaFX",
                "Reports unused type imports in FXML documents.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.PropertiesFileIncompatibleType", "The value from properties file is incompatible with the attribute type", "JavaFX",
                "Reports property resource bundle values that cannot be converted to the target FXML attribute type.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.UnnecessaryDefaultTag", "Unnecessary default tag", "JavaFX",
                "Reports FXML container default property tags that can be omitted.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.UnresolvedFxId", "Unresolved fx:id attribute reference", "JavaFX",
                "Reports fx:id attributes in FXML that do not correspond to controller fields.",
                HighlightSeverity.WARNING, true, "FXML");
        addTool("JavaFX.UnresolvedStyleClass", "Unresolved style class reference", "JavaFX",
                "Reports CSS style class references in FXML that are not defined in referenced stylesheets.",
                HighlightSeverity.WARNING, true, "FXML");

        // 35. JavaScript and TypeScript ([-] with 26 subcategories from Image 2)
        // Subcategory: Assignment issues ([-] Indeterminate)
        addTool("JS.Assignment.ForLoopParam", "Assignment to for-loop parameter", "JavaScript and TypeScript/Assignment issues",
                "Reports assignments to for-loop iteration variables.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Assignment.FunctionParam", "Assignment to function parameter", "JavaScript and TypeScript/Assignment issues",
                "Reports assignments to incoming function arguments.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Async code and promises ([-] Indeterminate)
        addTool("JS.Async.AwaitOutsideAsync", "Missing 'await' for an async function call", "JavaScript and TypeScript/Async code and promises",
                "Reports async calls without await whose return promise is ignored.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Async.PromiseReturnedWithoutAwait", "Promise returned in non-async function", "JavaScript and TypeScript/Async code and promises",
                "Reports functions returning Promise instances without async keyword.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Bitwise operation issues ([-] Indeterminate)
        addTool("JS.Bitwise.IncompatibleMask", "Incompatible bitwise mask expression", "JavaScript and TypeScript/Bitwise operation issues",
                "Reports bitwise expressions that always evaluate to constant results.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Bitwise.PointlessOperation", "Pointless bitwise expression", "JavaScript and TypeScript/Bitwise operation issues",
                "Reports bitwise operations that have no effect.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Code quality tools ([ ] Unchecked)
        addTool("JS.CodeQuality.ESLint", "ESLint verification", "JavaScript and TypeScript/Code quality tools",
                "Runs ESLint static analyzer rules on JavaScript and TypeScript files.",
                HighlightSeverity.WARNING, false, "JavaScript");
        addTool("JS.CodeQuality.JSHint", "JSHint verification", "JavaScript and TypeScript/Code quality tools",
                "Runs JSHint linter rules on source files.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Code style issues ([-] Indeterminate)
        addTool("JS.CodeStyle.UnnecessarySemicolon", "Unnecessary semicolon", "JavaScript and TypeScript/Code style issues",
                "Reports redundant semicolons.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.CodeStyle.ChainedEquality", "Chained equality", "JavaScript and TypeScript/Code style issues",
                "Reports chained equality comparisons (a == b == c) that behave unexpectedly.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Control flow issues ([-] Indeterminate)
        addTool("JS.ControlFlow.InfiniteLoop", "Infinite loop statement", "JavaScript and TypeScript/Control flow issues",
                "Reports loops that cannot terminate normally.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.ControlFlow.UnneededLoop", "Loop statement that does not loop", "JavaScript and TypeScript/Control flow issues",
                "Reports loop statements that execute at most one iteration.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Data flow ([-] Indeterminate)
        addTool("JS.DataFlow.ConstantCondition", "Constant conditional expression", "JavaScript and TypeScript/Data flow",
                "Reports conditions that evaluate to constant boolean values.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.DataFlow.UnassignedVariable", "Variable is never assigned", "JavaScript and TypeScript/Data flow",
                "Reports variables declared without an initializer that are never written to.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: DOM issues ([ ] Unchecked)
        addTool("JS.DOM.DeprecatedApi", "Deprecated DOM API usage", "JavaScript and TypeScript/DOM issues",
                "Reports calls to deprecated DOM methods and properties.",
                HighlightSeverity.WARNING, false, "JavaScript");
        addTool("JS.DOM.InvalidEventName", "Invalid DOM event name", "JavaScript and TypeScript/DOM issues",
                "Reports event listener names that are not standard DOM event types.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: ES2015 migration aids ([✓] Checked)
        addTool("JS.ES2015.VarCanBeConst", "'var' declaration can be replaced with 'let' or 'const'", "JavaScript and TypeScript/ES2015 migration aids",
                "Reports 'var' variables that can be modernized to block-scoped 'let' or 'const'.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.ES2015.TemplateString", "String concatenation can be converted to template string", "JavaScript and TypeScript/ES2015 migration aids",
                "Reports complex string concatenation expressions that can use ES6 template literals.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Flow type checker ([✓] Checked)
        addTool("JS.Flow.TypeMismatch", "Flow type mismatch", "JavaScript and TypeScript/Flow type checker",
                "Reports type annotation incompatibilities in Flow-annotated code.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Flow.UntypedImport", "Flow untyped import", "JavaScript and TypeScript/Flow type checker",
                "Reports imports of untyped JavaScript modules in Flow-checked files.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Function metrics ([ ] Unchecked)
        addTool("JS.FunctionMetrics.OverlyComplex", "Overly complex function", "JavaScript and TypeScript/Function metrics",
                "Reports functions whose cyclomatic complexity exceeds threshold.",
                HighlightSeverity.WARNING, false, "JavaScript");
        addTool("JS.FunctionMetrics.ParametersCount", "Parameters number exceeds threshold", "JavaScript and TypeScript/Function metrics",
                "Reports functions declaring more arguments than recommended.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: General ([-] Indeterminate)
        addTool("JS.General.DuplicateCase", "Duplicate 'case' label", "JavaScript and TypeScript/General",
                "Reports duplicate case clauses in switch statements.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.General.UnresolvedReference", "Unresolved JavaScript reference", "JavaScript and TypeScript/General",
                "Reports variable or function identifiers that cannot be resolved in scope.",
                HighlightSeverity.WARNING, false, "JavaScript");

        // Subcategory: Imports and dependencies ([✓] Checked)
        addTool("JS.Imports.UnusedImport", "Unused import declaration", "JavaScript and TypeScript/Imports and dependencies",
                "Reports imported symbols that are never referenced.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Imports.DuplicateImport", "Duplicate import", "JavaScript and TypeScript/Imports and dependencies",
                "Reports multiple import statements importing the same module path.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Naming conventions ([✓] Checked)
        addTool("JS.Naming.FunctionNaming", "Function naming convention", "JavaScript and TypeScript/Naming conventions",
                "Reports function names that do not follow camelCase naming conventions.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Naming.VariableNaming", "Variable naming convention", "JavaScript and TypeScript/Naming conventions",
                "Reports variable names that do not follow project naming rules.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Node.js ([✓] Checked)
        addTool("JS.Node.UnresolvedModule", "Unresolved Node.js module", "JavaScript and TypeScript/Node.js",
                "Reports require() calls pointing to missing npm packages or local modules.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Node.RequireCall", "Deprecated 'require' call", "JavaScript and TypeScript/Node.js",
                "Suggests converting CommonJS require calls to ES module imports.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Potentially confusing code constructs ([✓] Checked)
        addTool("JS.Confusing.CommaExpression", "Comma expression", "JavaScript and TypeScript/Potentially confusing code constructs",
                "Reports comma operator usage which may obscure side-effects.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Confusing.NestedTernary", "Nested conditional expression", "JavaScript and TypeScript/Potentially confusing code constructs",
                "Reports ternary expressions nested inside other conditional operators.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Potentially undesirable code constructs ([✓] Checked)
        addTool("JS.Undesirable.WithStatement", "'with' statement", "JavaScript and TypeScript/Potentially undesirable code constructs",
                "Reports deprecated 'with' statements.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Undesirable.CallerCallee", "Use of caller or callee properties", "JavaScript and TypeScript/Potentially undesirable code constructs",
                "Reports arguments.caller and arguments.callee references banned in strict mode.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Probable bugs ([✓] Checked)
        addTool("JS.ProbableBugs.EqualityComparisonWithNaN", "Comparison with NaN", "JavaScript and TypeScript/Probable bugs",
                "Reports equality comparisons with NaN that always evaluate to false.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.ProbableBugs.InfiniteRecursion", "Infinite recursion", "JavaScript and TypeScript/Probable bugs",
                "Reports recursive calls with no reachable base condition.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: React ([✓] Checked)
        addTool("JS.React.MissingKey", "Missing 'key' prop in element list", "JavaScript and TypeScript/React",
                "Reports array map elements missing a unique key prop in JSX.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.React.InvalidHookCall", "Invalid Hook call", "JavaScript and TypeScript/React",
                "Reports React hooks invoked conditionally or outside component render functions.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Security ([✓] Checked)
        addTool("JS.Security.Eval", "Use of 'eval()' function", "JavaScript and TypeScript/Security",
                "Reports calls to dangerous eval() function.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Security.InnerHTML", "Unsafe assignment to innerHTML", "JavaScript and TypeScript/Security",
                "Reports untrusted string assignments to innerHTML or outerHTML.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Switch statement issues ([✓] Checked)
        addTool("JS.Switch.FallThrough", "Fallthrough in 'switch' statement", "JavaScript and TypeScript/Switch statement issues",
                "Reports case clauses in switch statements that fall through without break or return.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Switch.MissingDefault", "Missing 'default' branch in 'switch' statement", "JavaScript and TypeScript/Switch statement issues",
                "Reports switch statements that do not include a default case branch.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Try statement issues ([✓] Checked)
        addTool("JS.Try.EmptyCatch", "Empty 'catch' block", "JavaScript and TypeScript/Try statement issues",
                "Reports catch blocks containing no statements.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Try.FinallyReturn", "'return' inside 'finally' block", "JavaScript and TypeScript/Try statement issues",
                "Reports return statements inside finally blocks that discard thrown exceptions.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: TypeScript ([✓] Checked)
        addTool("JS.TS.ExplicitAny", "Explicit 'any' type", "JavaScript and TypeScript/TypeScript",
                "Reports variables or parameters declared with explicit 'any' type.",
                HighlightSeverity.WARNING, true, "TypeScript");
        addTool("JS.TS.UnnecessaryTypeAssertion", "Unnecessary type assertion", "JavaScript and TypeScript/TypeScript",
                "Reports 'as' type casts where the type is already inferred.",
                HighlightSeverity.WARNING, true, "TypeScript");

        // Subcategory: Unit testing ([✓] Checked)
        addTool("JS.Testing.MissingAssertions", "Test without assertions", "JavaScript and TypeScript/Unit testing",
                "Reports unit test methods that execute without asserting expectations.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Testing.DuplicateTestName", "Duplicate test title", "JavaScript and TypeScript/Unit testing",
                "Reports identical describe or it test suite titles.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Unused symbols ([✓] Checked)
        addTool("JS.Unused.UnusedVariable", "Unused local variable", "JavaScript and TypeScript/Unused symbols",
                "Reports declared local variables that are never read.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Unused.UnusedFunction", "Unused function", "JavaScript and TypeScript/Unused symbols",
                "Reports declared functions that are never called.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // Subcategory: Validity issues ([✓] Checked)
        addTool("JS.Validity.DuplicateProperty", "Duplicate object property name", "JavaScript and TypeScript/Validity issues",
                "Reports duplicate property keys in object literal declarations.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.Validity.ReservedWord", "Use of reserved word as identifier", "JavaScript and TypeScript/Validity issues",
                "Reports JavaScript reserved keywords used as variable or parameter names.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // 36. JPA ([✓] with Kotlin subcategory and 24 direct items from Image 3)
        // Subcategory: Kotlin
        addTool("JPA.Kotlin.ImmutableCollectionProperty", "Immutable collection-like property", "JPA/Kotlin",
                "Reports immutable collection properties in JPA entities that Hibernate cannot update.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("JPA.Kotlin.ImmutableNotNullableProperty", "Immutable not-nullable JPA property", "JPA/Kotlin",
                "Reports non-nullable entity properties in Kotlin missing no-arg initialization.",
                HighlightSeverity.WARNING, true, "Kotlin");

        // Direct JPA items
        addTool("JPA.AssociationMarkedWithColumn", "Association field marked with @Colum", "JPA",
                "Reports relationship fields marked with @Column instead of @JoinColumn.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.EntityAttributeNotMarkedWithAssociation", "Entity attribute is not marked with association annotation", "JPA",
                "Reports entity fields referencing other entity types without relationship annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.EntityHasMoreThanOneIdAttribute", "Entity has more than one id attribute.", "JPA",
                "Reports entities declaring multiple @Id attributes without @IdClass.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.EntityListenerProblems", "Entity listener problems", "JPA",
                "Reports invalid method signatures on JPA entity listener classes.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.EntityListenerWarnings", "Entity listener warnings", "JPA",
                "Reports non-standard lifecycle callback methods in entity listeners.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.ManyToManyCascadeRemove", "For @ManyToMany associations, the REMOVE entity state transition doesn't make sense", "JPA",
                "Reports @ManyToMany associations configured with CascadeType.REMOVE which causes unintended deletions.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.ConverterAnnotation", "JPA converter must be annotated with @Converter annotation", "JPA",
                "Reports AttributeConverter implementations missing the @Converter annotation.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.LombokBuilderConstructor", "Lombok @Builder needs a proper constructor for this class", "JPA",
                "Reports entities with Lombok @Builder lacking an explicit all-args or no-args constructor.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.OrmXmlProblems", "Orm.xml problems", "JPA",
                "Reports mapping and validation errors in JPA orm.xml configuration files.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.PersistenceXmlNotAddedToFacet", "Persistence.xml is not added to facet", "JPA",
                "Reports persistence.xml files that are not registered in the project JPA facet.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.PersistenceXmlProblems", "persistence.xml problems", "JPA",
                "Reports syntax, validation, and missing unit errors in persistence.xml.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.PersistentAttributeSignatureChecks", "Persistent attribute signature checks", "JPA",
                "Reports getter/setter signature mismatches on persistent entity properties.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.PersistentAttributeTypeChecks", "Persistent attribute type checks", "JPA",
                "Reports unsupported attribute types mapped in JPA entities.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.PersistentEntityMissesPrimaryKey", "Persistent entity misses primary key", "JPA",
                "Reports JPA entity classes lacking an @Id or @EmbeddedId attribute.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.PersistentObjectClassSignatureChecks", "Persistent object class signature checks", "JPA",
                "Reports entity classes that are final or lack a public/protected no-argument constructor.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.QueryLanguageChecks", "Query language checks", "JPA",
                "Validates JPQL and HQL query strings for syntax and semantic correctness.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.LazyOneToOneNonOwning", "Specifying FetchType.LAZY for the non-owning side of the @OneToOne", "JPA",
                "Reports FetchType.LAZY on mappedBy side of @OneToOne which is ignored by JPA providers.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.UnresolvedDbReferencesInAnnotations", "Unresolved database references in annotations", "JPA",
                "Reports database table or column names in annotations that cannot be resolved in the dataSource.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.UnresolvedDbReferencesInXml", "Unresolved database references in XML", "JPA",
                "Reports unresolved database table and column references in JPA XML mapping files.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.UnresolvedEntityGraphNames", "Unresolved entity graph names", "JPA",
                "Reports named entity graphs referenced in query hints that do not exist.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.UnresolvedQueriesAndQueryParams", "Unresolved queries and query parameters", "JPA",
                "Reports named queries and query parameter names that cannot be resolved.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.UnresolvedReferencesInQueries", "Unresolved references in queries", "JPA",
                "Reports entity property and alias references in JPQL queries that do not resolve.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.AllArgsConstructorWithoutNoArg", "Using @AllArgsConstructor for JPA entities without defined no-argument", "JPA",
                "Reports JPA entities with Lombok @AllArgsConstructor missing an explicit @NoArgsConstructor.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JPA.BuilderWithoutNoArg", "Using @Builder for JPA entities without defined no-argument constructo", "JPA",
                "Reports JPA entities with Lombok @Builder missing a no-argument constructor required by JPA.",
                HighlightSeverity.WARNING, true, "Java");

        // 37. JRuby ([✓] all 8 inspections from Image 3)
        addTool("JRuby.MissingJavaInterfaceMethod", "Class missing Java interface method implementations", "JRuby",
                "Reports Ruby classes implementing Java interfaces that omit required method implementations.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.DeprecatedMethodUsage", "Deprecated method usage", "JRuby",
                "Reports usage of deprecated Java methods and fields in JRuby code.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("JRuby.IncorrectTopLevelPackage", "Incorrect top-level package specification", "JRuby",
                "Reports invalid top-level Java package imports in JRuby files.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.InvalidImportFormat", "Invalid import format", "JRuby",
                "Reports invalid syntax in 'java_import' statements.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.NoFieldAccessorFound", "No field accessor found", "JRuby",
                "Reports Java field references that do not provide corresponding getter/setter accessors.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.PrivateClassImported", "Private class imported", "JRuby",
                "Reports imports of non-public Java classes.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.SuperclassIsAnInterface", "Superclass is an interface", "JRuby",
                "Reports Ruby classes extending Java interfaces as superclasses instead of implementing them.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("JRuby.UnusedImport", "Unused import", "JRuby",
                "Reports unused Java class imports in JRuby files.",
                HighlightSeverity.WARNING, true, "Ruby");

        // 38. JSON and JSON5 ([✓] all 7 inspections from Image 4)
        addTool("JSON.ComplianceWithSchema", "Compliance with JSON schema", "JSON and JSON5",
                "Validates JSON documents against configured JSON Schema specifications.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("JSON.ComplianceWithStandard", "Compliance with JSON standard", "JSON and JSON5",
                "Reports violations of the standard JSON format (RFC 8259).",
                HighlightSeverity.ERROR, true, "JSON");
        addTool("JSON5.ComplianceWithStandard", "Compliance with JSON5 standard", "JSON and JSON5",
                "Reports syntax errors and invalid tokens according to the JSON5 standard.",
                HighlightSeverity.ERROR, true, "JSON5");
        addTool("JSON.DeprecatedProperty", "Deprecated JSON property", "JSON and JSON5",
                "Reports usage of deprecated properties defined in schema definitions.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("JSON.DuplicateKeysInObjectLiterals", "Duplicate keys in object literals", "JSON and JSON5",
                "Reports duplicate property keys in JSON objects.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("JSON5.DuplicateKeysInObjectLiterals", "Duplicate keys in object literals", "JSON and JSON5",
                "Reports duplicate property keys in JSON5 objects.",
                HighlightSeverity.WARNING, true, "JSON5");
        addTool("JSON.UnresolvedRefAndSchema", "Unresolved '$ref' and '$schema' references", "JSON and JSON5",
                "Reports schema URI references in $ref and $schema properties that cannot be resolved.",
                HighlightSeverity.WARNING, true, "JSON");

        // 39. JSONPath ([✓] all 3 inspections from Image 4)
        addTool("JSONPath.UnknownFunction", "Unknown JSONPath function", "JSONPath",
                "Reports unrecognized function calls in JSONPath expressions.",
                HighlightSeverity.WARNING, true, "JSONPath");
        addTool("JSONPath.UnknownOperator", "Unknown JSONPath operator", "JSONPath",
                "Reports unrecognized filter and comparison operators in JSONPath expressions.",
                HighlightSeverity.WARNING, true, "JSONPath");
        addTool("JSONPath.UnknownPropertyKey", "Unknown property key used for JSONPath evaluate expression", "JSONPath",
                "Reports property keys in JSONPath expressions that do not exist in the target payload.",
                HighlightSeverity.WARNING, true, "JSONPath");

        // 40. JSP ([-] with 13 inspections from Image 4)
        addTool("JSP.AbsolutePaths", "Absolute paths", "JSP",
                "Reports absolute file paths in JSP include and forward actions.",
                HighlightSeverity.WARNING, true, "JSP");
        addTool("JSP.ElDeferredExpressions", "EL deferred expressions inspection", "JSP",
                "Validates deferred syntax (#{...}) in JSP EL expressions.",
                HighlightSeverity.WARNING, true, "JSP");
        addTool("JSP.ElMethodFunctionParamsCount", "EL method function parameters count", "JSP",
                "Reports EL method invocations with incorrect parameter count.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.ElMethodSignature", "EL method signature inspection", "JSP",
                "Reports EL method invocations whose argument types do not match method signatures.",
                HighlightSeverity.WARNING, true, "JSP");
        addTool("JSP.DirectiveInspection", "Jsp directive inspection", "JSP",
                "Reports malformed or unknown attributes in JSP directives.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.ElSpecificationValidation", "JSP EL specification validation", "JSP",
                "Validates Expression Language syntax against the JSP EL specification.",
                HighlightSeverity.WARNING, true, "JSP");
        addTool("JSP.PropertiesInspection", "Jsp properties inspection", "JSP",
                "Reports unresolved JavaBean property references in <jsp:getProperty> and EL expressions.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.ReferencesToClassesFromDefaultPackage", "References to classes from the default package in JSP files", "JSP",
                "Reports classes in the default package referenced from JSP pages.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.SelfIncludingFiles", "Self-including JSP files", "JSP",
                "Reports JSP pages that directly or indirectly include themselves.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.TagBodyContentType", "Tag body content type", "JSP",
                "Reports custom tags whose body content does not match the tag library descriptor.",
                HighlightSeverity.WARNING, true, "JSP");
        addTool("JSP.TagLibraryDescriptor", "Tag library descriptor inspection", "JSP",
                "Validates TLD tag library descriptor XML files.",
                HighlightSeverity.ERROR, true, "JSP");
        addTool("JSP.UnescapedElExpressions", "Unescaped EL Expressions", "JSP",
                "Reports unescaped EL expressions vulnerable to Cross-Site Scripting (XSS).",
                HighlightSeverity.WARNING, false, "JSP");
        addTool("JSP.UnhandledException", "Unhandled Exception in JSP", "JSP",
                "Reports checked exceptions thrown in JSP scriptlets without error page handlers.",
                HighlightSeverity.WARNING, true, "JSP");

        // 41. JUnit ([-] with 16 inspections from Image 5)
        addTool("JUnit.AssertEqualsCalledOnArray", "'assertEquals()' called on array", "JUnit",
                "Reports assertEquals() invocations with array arguments which should use assertArrayEquals().",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.AssertEqualsMayBeAssertSame", "'assertEquals()' may be 'assertSame()'", "JUnit",
                "Reports assertEquals() calls between singleton instances that can use assertSame().",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.ExpectedExceptionNeverThrown", "Expected exception never thrown in test method body", "JUnit",
                "Reports @Test(expected=...) tests whose body cannot throw the expected exception.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.ApiUsageFromMultipleVersions", "JUnit API usage from multiple versions in a single TestCase", "JUnit",
                "Reports test classes mixing annotations and assertions from different JUnit versions (JUnit 3, 4, 5).",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JUnit.AssertionCanBeAssertThat", "JUnit assertion can be 'assertThat()' call", "JUnit",
                "Suggests converting classic assertions to Hamcrest or AssertJ assertThat().",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.MalformedDeclaration", "JUnit malformed declaration", "JUnit",
                "Reports test methods that are static, private, return a value, or declare parameters incorrectly.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JUnit.TestAnnotatedWithIgnoreDisabled", "JUnit test annotated with '@Ignore'/'@Disabled'", "JUnit",
                "Reports disabled or ignored test methods.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.SuperTearDownNotCalledFromFinally", "JUnit 3 'super.tearDown()' is not called from 'finally' block", "JUnit",
                "Reports JUnit 3 tearDown() overrides that do not call super.tearDown() inside a finally block.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.JUnit3TestCanBeJUnit4", "JUnit 3 test can be JUnit 4", "JUnit",
                "Suggests modernizing TestCase subclasses to JUnit 4 @Test annotations.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.JUnit4TestCanBeJUnit5", "JUnit 4 test can be JUnit 5", "JUnit",
                "Suggests migrating JUnit 4 tests to JUnit Jupiter 5 APIs.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.JUnit5ObsoleteAssertions", "JUnit 5 obsolete assertions", "JUnit",
                "Reports deprecated JUnit 4 assertions used within JUnit 5 test classes.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JUnit.MultipleExceptionsDeclaredOnTestMethod", "Multiple exceptions declared on test method", "JUnit",
                "Reports test methods declaring multiple redundant checked exceptions.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.OldStyleTestMethodInJUnit4", "Old style JUnit test method in JUnit 4 class", "JUnit",
                "Reports testXxx() methods in JUnit 4 classes missing the @Test annotation.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JUnit.ParameterizedTestWithoutDataProvider", "Parameterized test class without data provider method", "JUnit",
                "Reports @RunWith(Parameterized.class) classes missing @Parameters factory methods.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("JUnit.RunWithAnnotationAlreadyExistsInParent", "'@RunWith' annotation already exists in a parent class", "JUnit",
                "Reports redundant @RunWith annotations on test subclasses.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JUnit.UsageOfObsoleteAssertMethod", "Usage of obsolete 'junit.framework.Assert' method", "JUnit",
                "Reports calls to deprecated junit.framework.Assert methods.",
                HighlightSeverity.WARNING, false, "Java");

        // 42. JVM languages ([-] with Logging & Test frameworks subcategories and 21 direct items from Image 1)
        // Subcategory: Logging ([-] Indeterminate)
        addTool("JVM.Logging.ConditionMismatch", "Log condition does not match logging call", "JVM languages/Logging",
                "Reports log calls where condition guard doesn't match the logging level.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.Logging.CallNotGuarded", "Logging call not guarded by log condition", "JVM languages/Logging",
                "Reports expensive log calls that are not guarded by a logging level check.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Logging.CallsGuarded", "Logging calls guarded by log condition", "JVM languages/Logging",
                "Reports log calls wrapped with redundant condition guards.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Logging.NonDistinguishableCalls", "Non-distinguishable logging calls", "JVM languages/Logging",
                "Reports multiple identical log statements that cannot be distinguished in log outputs.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Logging.PlaceholdersMismatch", "Number of placeholders does not match number of arguments in logg", "JVM languages/Logging",
                "Reports format string placeholder count mismatches in parameterized logging calls.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Logging.StringTemplate", "String template as argument to logging call", "JVM languages/Logging",
                "Reports string concatenation or template arguments in logging calls that should use placeholders.",
                HighlightSeverity.WARNING, true, "JVM");

        // Subcategory: Test frameworks ([-] Indeterminate)
        addTool("JVM.Testing.AssertEqualsInconvertibleTypes", "'assertEquals()' between objects of inconvertible types", "JVM languages/Test frameworks",
                "Reports assertEquals calls between types that share no common hierarchy.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Testing.FailedLineInTest", "Failed line in test", "JVM languages/Test frameworks",
                "Highlights source lines where tests failed during last execution run.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.Testing.TestOnlyInProduction", "Test-only usage in production code", "JVM languages/Test frameworks",
                "Reports references to @VisibleForTesting symbols outside test roots.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.Testing.TestClassWithoutTests", "Test class without tests", "JVM languages/Test frameworks",
                "Reports test classes that declare no test methods.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.Testing.TestInProductSource", "Test in product source", "JVM languages/Test frameworks",
                "Reports test classes located in production source directories.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.Testing.TestMethodWithoutAssertions", "Test method without assertions", "JVM languages/Test frameworks",
                "Reports test methods that do not verify expectations with assertions.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.Testing.TestCaseWithNonTrivialConstructors", "TestCase with non-trivial constructors", "JVM languages/Test frameworks",
                "Reports JUnit test classes that execute logic inside constructors instead of setup methods.",
                HighlightSeverity.WARNING, false, "JVM");

        // Direct JVM languages items
        addTool("JVM.ApiMustAlreadyBeRemoved", "API must already be removed", "JVM languages",
                "Reports usage of scheduled-for-removal APIs whose planned removal release has passed.",
                HighlightSeverity.ERROR, true, "JVM");
        addTool("JVM.EqualsHashCodeOnUrl", "Call to 'equals()' or 'hashCode()' on 'URL' object", "JVM languages",
                "Reports equals() or hashCode() calls on java.net.URL objects which trigger blocking DNS lookups.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.SystemGetPropertySimplified", "Call to 'System.getProperty(str)' could be simplified", "JVM languages",
                "Reports System.getProperty() calls that can use Boolean.getBoolean() or standard constants.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.ThreadRun", "Call to 'Thread.run()'", "JVM languages",
                "Reports calls to Thread.run() instead of Thread.start().",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.ShouldNotBeExtended", "Class, interface, or method should not be extended", "JVM languages",
                "Reports classes or methods extending types marked non-extendable.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.EmptyMethod", "Empty method (available for Code | Inspect Code)", "JVM languages",
                "Reports methods with empty bodies.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.IllegalDependencyOnInternal", "Illegal dependency on internal package", "JVM languages",
                "Reports dependencies on internal, non-public packages.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.IllegalPackageDependencies", "Illegal package dependencies", "JVM languages",
                "Reports prohibited package dependency relationships configured by project rules.",
                HighlightSeverity.ERROR, true, "JVM");
        addTool("JVM.IncorrectMimeType", "Incorrect MIME Type declaration", "JVM languages",
                "Reports malformed or non-standard MIME type strings.",
                HighlightSeverity.ERROR, true, "JVM");
        addTool("JVM.InspectionSuppression", "Inspection suppression annotation", "JVM languages",
                "Reports redundant or unneeded inspection suppression annotations.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.MethodCanOnlyBeOverridden", "Method can only be overridden", "JVM languages",
                "Reports methods that are intended only for overriding, not direct invocation.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.MissingDeprecatedOnRemoval", "Missing '@Deprecated' annotation on scheduled for removal API", "JVM languages",
                "Reports scheduled-for-removal elements that lack an explicit @Deprecated annotation.",
                HighlightSeverity.ERROR, true, "JVM");
        addTool("JVM.NonSafeStringToSafeMethod", "Non-safe string is passed to safe method", "JVM languages",
                "Reports tainted string arguments passed to methods requiring sanitized inputs.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.NonSafeStringUsedAsSql", "Non-safe string is used as SQL", "JVM languages",
                "Reports unsanitized string concatenation used to construct SQL query strings.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.BlockingInNonBlocking", "Possibly blocking call in non-blocking context", "JVM languages",
                "Reports blocking thread or I/O operations inside reactive or coroutine dispatchers.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.SerializableWithoutSerialVersionUID", "Serializable class without 'serialVersionUID'", "JVM languages",
                "Reports Serializable classes that omit a serialVersionUID field.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.UnknownHttpHeader", "Unknown HTTP header", "JVM languages",
                "Reports unrecognized HTTP header names.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.UnstableApiUsage", "Unstable API Usage", "JVM languages",
                "Reports usage of experimental or unstable library APIs.",
                HighlightSeverity.WARNING, true, "JVM");
        addTool("JVM.UnstableTypeInSignature", "Unstable type is used in signature", "JVM languages",
                "Reports experimental types exposed in public method signatures.",
                HighlightSeverity.WARNING, false, "JVM");
        addTool("JVM.ApiNotAvailableAtLanguageLevel", "Usages of API which isn't available at the configured language level", "JVM languages",
                "Reports APIs requiring a higher language level than the module configuration.",
                HighlightSeverity.ERROR, true, "JVM");
        addTool("JVM.UsagesOfApiStatusObsolete", "Usages of ApiStatus.@Obsolete", "JVM languages",
                "Reports usage of declarations annotated with @ApiStatus.Obsolete.",
                HighlightSeverity.WARNING, true, "JVM");

        // 43. Kotlin ([-] with 11 subcategories and 9 direct items from Image 2)
        // Subcategories
        addTool("Kotlin.Migration.CodeMigration", "Code migration inspection", "Kotlin/Code migration",
                "Reports outdated Kotlin syntax that can be modernized.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Coroutines.RedundantSuspend", "Redundant 'suspend' modifier", "Kotlin/Coroutine inspections",
                "Reports suspend functions that do not perform any suspending calls.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Coroutines.BlockingInSuspend", "Blocking call in suspend function", "Kotlin/Coroutine inspections",
                "Reports blocking calls inside coroutine suspend functions.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.JavaInterop.PlatformType", "Platform type dereference", "Kotlin/Java interop issues",
                "Reports unsafe dereferences on platform types returned from Java calls.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Migration.ApiMigration", "Kotlin API migration", "Kotlin/Migration",
                "Assists with upgrading between major Kotlin language and stdlib versions.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Naming.ClassNaming", "Class naming convention", "Kotlin/Naming conventions",
                "Reports class names that do not follow Kotlin naming conventions.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.Numeric.ImplicitCast", "Implicit numeric cast", "Kotlin/Numeric issues",
                "Reports implicit conversions between numeric types.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Numeric.FloatComparison", "Floating-point comparison", "Kotlin/Numeric issues",
                "Reports exact equality comparisons on floating-point values.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.Other.RedundantVisibility", "Redundant visibility modifier", "Kotlin/Other problems",
                "Reports explicit visibility modifiers that match the default (public).",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Other.UnnecessaryLateinit", "Unnecessary 'lateinit'", "Kotlin/Other problems",
                "Reports lateinit properties initialized immediately at declaration.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.ProbableBugs.InfiniteRecursion", "Infinite recursion", "Kotlin/Probable bugs",
                "Reports recursive calls with no reachable base condition.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.ProbableBugs.NullDereference", "Nullable receiver dereference", "Kotlin/Probable bugs",
                "Reports potential NullPointerException on nullable receiver calls.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.React.MissingKey", "Missing key prop in Kotlin/JS React element", "Kotlin/React",
                "Reports JSX element iterators missing unique keys in Kotlin/JS.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Redundant.RedundantSam", "Redundant SAM constructor", "Kotlin/Redundant constructs",
                "Reports explicit SAM constructor calls that can use lambda expressions.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Redundant.RedundantUnit", "Redundant 'Unit' return type", "Kotlin/Redundant constructs",
                "Reports explicit Unit return types on functions.",
                HighlightSeverity.WARNING, false, "Kotlin");
        addTool("Kotlin.Style.CanBeReplacedWithOperator", "Function call can be replaced with operator", "Kotlin/Style issues",
                "Reports named method calls (e.g. plus, get) that can be written with operator syntax.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.Style.ExplicitType", "Explicit type argument can be inferred", "Kotlin/Style issues",
                "Reports explicit generic type arguments that can be omitted.",
                HighlightSeverity.WARNING, false, "Kotlin");

        // Direct Kotlin leaf tools (all 9 checked from Image 2)
        addTool("Kotlin.DeprecatedLibraryInGradle", "Deprecated library is used in Gradle", "Kotlin",
                "Reports deprecated Kotlin libraries declared in build.gradle dependencies.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.DeprecatedLibraryInMaven", "Deprecated library is used in Maven", "Kotlin",
                "Reports deprecated Kotlin libraries declared in pom.xml dependencies.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.InvalidPropertyKey", "Invalid property key", "Kotlin",
                "Reports unresolved property bundle keys referenced in Kotlin source.",
                HighlightSeverity.ERROR, true, "Kotlin");
        addTool("Kotlin.TestJunitCouldBeUsed", "kotlin-test-junit could be used", "Kotlin",
                "Suggests using kotlin-test-junit integration libraries.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.GradleAndIdePluginsDifferent", "Kotlin Gradle and IDE plugins versions are different", "Kotlin",
                "Reports version mismatch between project Gradle Kotlin plugin and IDE Kotlin plugin.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.LibraryAndGradlePluginDifferent", "Kotlin library and Gradle plugin versions are different", "Kotlin",
                "Reports version mismatch between Kotlin stdlib and Gradle Kotlin plugin.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.MavenPluginMisconfigured", "Kotlin Maven Plugin misconfigured", "Kotlin",
                "Reports configuration issues in kotlin-maven-plugin executions.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.LibraryAndMavenPluginDifferent", "Library and maven plugin versions are different", "Kotlin",
                "Reports version mismatch between Kotlin stdlib and kotlin-maven-plugin.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Kotlin.MavenAndIdePluginsDifferent", "Maven and IDE plugins versions are different", "Kotlin",
                "Reports version mismatch between Maven Kotlin plugin and IDE Kotlin plugin.",
                HighlightSeverity.WARNING, true, "Kotlin");

        // 44. Ktor ([-] with 2 inspections from Image 2)
        addTool("Ktor.ApplicationYaml", "Ktor application.yaml", "Ktor",
                "Validates Ktor application.yaml configuration structure and module references.",
                HighlightSeverity.WARNING, true, "Kotlin");
        addTool("Ktor.OpenApiDocOutdated", "OpenAPI documentation for current module is outdated", "Ktor",
                "Reports OpenAPI documentation files that are out of sync with Ktor route definitions.",
                HighlightSeverity.WARNING, false, "Kotlin");

        // 45. Kubernetes ([✓] all 13 inspections from Image 3)
        addTool("Kubernetes.DeprecatedResourceProperties", "Deprecated Kubernetes resource properties", "Kubernetes",
                "Reports deprecated properties used in Kubernetes YAML manifests.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.DeprecatedResourcePropertyValues", "Deprecated Kubernetes resource property values", "Kubernetes",
                "Reports deprecated property values in Kubernetes resources.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.DeprecatedResources", "Deprecated Kubernetes resources", "Kubernetes",
                "Reports deprecated API versions and resource types in Kubernetes manifests.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.DuplicatedEnvVarDefinitions", "Duplicated EnvVar definitions", "Kubernetes",
                "Reports duplicate environment variable names declared within a container definition.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Kubernetes.InvalidChartYamlValues", "Invalid Chart.yaml values", "Kubernetes",
                "Reports invalid metadata values in Helm Chart.yaml files.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Kubernetes.MissingChartYamlKeys", "Missing Chart.yaml keys", "Kubernetes",
                "Reports missing required keys (apiVersion, name, version) in Helm Chart.yaml.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Kubernetes.MissingKubernetesYamlKeys", "Missing Kubernetes YAML keys", "Kubernetes",
                "Reports missing mandatory fields in Kubernetes resource definitions.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Kubernetes.NonEditableResourceProperties", "Non-editable Kubernetes resource properties", "Kubernetes",
                "Reports modifications to immutable Kubernetes resource properties.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.NonEditableResources", "Non-editable Kubernetes resources", "Kubernetes",
                "Reports update attempts on non-updatable Kubernetes resources.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.UnknownChartYamlKeys", "Unknown Chart.yaml keys", "Kubernetes",
                "Reports unrecognized keys in Helm Chart.yaml files.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.UnknownResources", "Unknown Kubernetes resources", "Kubernetes",
                "Reports Kubernetes YAML documents with unrecognized apiVersion or kind.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Kubernetes.UnknownKubernetesYamlKeys", "Unknown Kubernetes YAML keys", "Kubernetes",
                "Reports unrecognized keys in Kubernetes resource manifests.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Kubernetes.UnknownKubernetesYamlValues", "Unknown Kubernetes YAML values", "Kubernetes",
                "Reports invalid property values according to Kubernetes schema definitions.",
                HighlightSeverity.ERROR, true, "YAML");

        // 46. Language injection ([✓] all 3 inspections from Image 3)
        addTool("LangInjection.AnnotationNotApplicable", "Injection annotation is not applicable", "Language injection",
                "Reports @Language annotations placed on elements that cannot accept injected language fragments.",
                HighlightSeverity.ERROR, true, "General");
        addTool("LangInjection.LanguageMismatch", "Language mismatch", "Language injection",
                "Reports injected code fragments whose syntax violates the expected language dialect.",
                HighlightSeverity.WARNING, true, "General");
        addTool("LangInjection.UnknownLanguageId", "Unknown Language ID", "Language injection",
                "Reports unrecognized language IDs specified in @Language annotations.",
                HighlightSeverity.ERROR, true, "General");

        // 47. Less ([✓] all 3 inspections from Image 3)
        addTool("Less.MissingImport", "Missing import", "Less",
                "Reports @import statements in Less stylesheets referencing non-existent files.",
                HighlightSeverity.WARNING, true, "Less");
        addTool("Less.UnresolvedMixin", "Unresolved mixin", "Less",
                "Reports references to Less mixins that are not declared or imported.",
                HighlightSeverity.WARNING, true, "Less");
        addTool("Less.UnresolvedVariable", "Unresolved variable", "Less",
                "Reports references to undefined Less variables.",
                HighlightSeverity.WARNING, true, "Less");

        // 48. Liquibase ([✓] all 2 inspections from Image 3)
        addTool("Liquibase.DuplicateChangeSetId", "Duplicate changeset's 'id' attribute within one file for the same 'author'.", "Liquibase",
                "Reports changelog files containing duplicate changeSet IDs for the same author.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Liquibase.UnresolvedProperty", "Unresolved Liquibase property", "Liquibase",
                "Reports unresolved property references in Liquibase changelog files.",
                HighlightSeverity.ERROR, true, "XML");

        // 49. Manifest ([✓] all 2 inspections from Image 3)
        addTool("Manifest.MissingFinalNewLine", "Missing final new line", "Manifest",
                "Reports MANIFEST.MF files missing a terminating newline required by JAR specifications.",
                HighlightSeverity.ERROR, true, "Manifest");
        addTool("Manifest.UnknownHeaderName", "Unknown or misspelled header name", "Manifest",
                "Reports unrecognized header names in MANIFEST.MF files.",
                HighlightSeverity.WARNING, true, "Manifest");

        // 50. Markdown ([✓] all 8 inspections from Image 3)
        addTool("Markdown.IncorrectTableFormatting", "Incorrect table formatting", "Markdown",
                "Reports misaligned columns and pipes in Markdown tables.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.IncorrectlyNumberedList", "Incorrectly numbered list item", "Markdown",
                "Reports ordered list items with non-sequential numbering.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.LinksShouldNotContainSpaces", "Links should not contain spaces", "Markdown",
                "Reports URL destinations in Markdown links containing unencoded whitespace.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.OutdatedTableOfContents", "Outdated table of contents section", "Markdown",
                "Reports table of contents blocks that do not reflect document headers.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.TableNoSideBorders", "Table doesn't have side borders", "Markdown",
                "Reports Markdown tables that lack outer boundary vertical pipes.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.UnresolvedFileReferences", "Unresolved file references", "Markdown",
                "Reports relative link targets pointing to non-existent local files.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.UnresolvedHeaderReference", "Unresolved header reference", "Markdown",
                "Reports anchor links pointing to missing section headers.",
                HighlightSeverity.WARNING, true, "Markdown");
        addTool("Markdown.UnresolvedLinkLabel", "Unresolved link label", "Markdown",
                "Reports reference-style links whose label definitions cannot be found.",
                HighlightSeverity.WARNING, true, "Markdown");

        // 51. Maven ([✓] all 16 inspections from Image 4)
        addTool("Maven.DuplicateDependencies", "Duplicate Dependencies", "Maven",
                "Reports dependencies declared multiple times within the same POM.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.DuplicatePluginDeclaration", "Duplicate plugin declaration", "Maven",
                "Reports plugins configured multiple times in build/plugins section.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.ModelInspection", "Maven Model Inspection", "Maven",
                "Validates POM XML documents against official Maven POM schema specifications.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.Version4RequiredFor410Schema", "Maven version 4+ is required for projects with 4.1.0 schema", "Maven",
                "Reports POMs using the 4.1.0 schema running on Maven versions prior to 4.0.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.ModelVersion410RequiredPackaging", "Model version 4.1.0 is required for the packaging", "Maven",
                "Reports packaging types requiring Maven POM model version 4.1.0.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.ModelVersionChildTagDefined", "'modelVersion' child tag should be defined", "Maven",
                "Reports POM files missing the mandatory <modelVersion> child tag.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.MultiModuleDirectoryNotDefined", "Multi-module directory is not defined", "Maven",
                "Reports projects with submodules that do not declare a top-level root directory.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.ParentGroupIdOrArtifactIdMissing", "Parent groupId or artifactId is missing", "Maven",
                "Reports <parent> declarations missing groupId or artifactId tags.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.ParentVersionMissed", "Parent version missed", "Maven",
                "Reports <parent> declarations missing version tag.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.RedundantGroupId", "Redundant groupId", "Maven",
                "Reports child POMs repeating the groupId already declared in their parent POM.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.RedundantVersion", "Redundant version", "Maven",
                "Reports child POMs repeating the version already declared in their parent POM.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.ModulesTagDeprecated", "The <modules> tag is deprecated; use <subprojects> instead", "Maven",
                "Suggests modernizing <modules> to <subprojects> in Maven 4 projects.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.ParentCoordinatesRedundantMaven4", "The parent coordinates are redundant and not required in Maven 4", "Maven",
                "Reports parent POM coordinates that can be omitted under Maven 4.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.UsageOfPropertiesInParentDescription", "Usage of properties in parent description", "Maven",
                "Reports property expressions used inside parent coordinate elements.",
                HighlightSeverity.WARNING, true, "Maven");
        addTool("Maven.WrongModelVersionTag1", "Wrong model version. Model version should be 4.1.0 for this tag", "Maven",
                "Reports tags supported only starting from model version 4.1.0.",
                HighlightSeverity.ERROR, true, "Maven");
        addTool("Maven.WrongModelVersionTag2", "Wrong model version. Model version should be 4.1.0 for this tag (packaging)", "Maven",
                "Reports configuration tags that require model version 4.1.0.",
                HighlightSeverity.ERROR, true, "Maven");

        // 52. Micronaut ([✓] with Micronaut & Micronaut Data subcategories and 5 direct items from Image 4)
        addTool("Micronaut.ExpressionLanguageSyntax", "Incorrect Micronaut Expression Language (MicronautEl) syntax", "Micronaut/Micronaut",
                "Reports syntax errors in Micronaut Expression Language annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Micronaut.Data.IncorrectRepoDeclaration", "Incorrect repository method declaration", "Micronaut/Micronaut Data",
                "Reports repository query methods whose return types cannot match query semantics.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Micronaut.Data.IncorrectRepoParameter", "Incorrect repository method parameter", "Micronaut/Micronaut Data",
                "Reports repository method parameters that do not match entity properties.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Micronaut.Data.IncorrectRepoReturnType", "Incorrect repository method return type", "Micronaut/Micronaut Data",
                "Reports invalid return types for Micronaut Data repository finder methods.",
                HighlightSeverity.WARNING, true, "Java");

        // Direct Micronaut items
        addTool("Micronaut.ApplicationProperties", "Micronaut application.properties", "Micronaut",
                "Validates Micronaut application.properties key-value configurations.",
                HighlightSeverity.WARNING, true, "Properties");
        addTool("Micronaut.ApplicationYaml", "Micronaut application.yaml", "Micronaut",
                "Validates Micronaut application.yaml structure and configuration schemas.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Micronaut.UnresolvedPathVariable", "Unresolved @PathVariable reference", "Micronaut",
                "Reports controller route @PathVariable parameters that do not match URI template variables.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Micronaut.UnresolvedCacheAnnotation1", "Unresolved cache annotation parameter reference", "Micronaut",
                "Reports cache key expressions referencing parameters that do not exist.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Micronaut.UnresolvedCacheAnnotation2", "Unresolved cache annotation parameter reference (Strict)", "Micronaut",
                "Reports missing cache configuration names in caching annotations.",
                HighlightSeverity.ERROR, true, "Java");

        // 53. MongoJS ([✓] all 6 inspections from Image 4)
        addTool("MongoJS.DeprecatedElement1", "Deprecated element", "MongoJS",
                "Reports calls to deprecated Mongo shell commands and functions.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("MongoJS.DeprecatedElement2", "Deprecated element (Legacy)", "MongoJS",
                "Reports usage of obsolete MongoDB JavaScript operators.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("MongoJS.ResolutionProblems1", "Resolution problems", "MongoJS",
                "Reports unresolved collection or database references in MongoJS queries.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("MongoJS.ResolutionProblems2", "Resolution problems (Field)", "MongoJS",
                "Reports unresolved document field paths in Mongo aggregation pipelines.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("MongoJS.StatementWithSideEffects1", "Statement with side effects", "MongoJS",
                "Reports queries whose side effects may impact concurrent operations.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("MongoJS.StatementWithSideEffects2", "Statement with side effects (Mutating)", "MongoJS",
                "Reports uncommitted modifying statements inside read contexts.",
                HighlightSeverity.WARNING, true, "JavaScript");

        // 54. MySQL ([✓])
        addTool("MySQL.SyntaxError", "MySQL dialect syntax error", "MySQL",
                "Reports syntax errors specific to MySQL dialect.",
                HighlightSeverity.ERROR, true, "SQL");

        // 55. OpenAPI specifications ([✓] all 4 inspections from Image 5)
        addTool("OpenAPI.PossibleSpecificationCandidate1", "Possible OpenAPI/Swagger specification candidate", "OpenAPI specifications",
                "Suggests associating OpenAPI schema validation with JSON/YAML documents.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("OpenAPI.PossibleSpecificationCandidate2", "Possible OpenAPI/Swagger specification candidate (JSON)", "OpenAPI specifications",
                "Suggests registering OpenAPI capabilities for REST specification files.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("OpenAPI.UnresolvedReference1", "Unresolved reference", "OpenAPI specifications",
                "Reports $ref schema references that cannot be resolved.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("OpenAPI.UnresolvedReference2", "Unresolved reference (Remote)", "OpenAPI specifications",
                "Reports remote URL $ref targets that fail network or file resolution.",
                HighlightSeverity.ERROR, true, "YAML");

        // 56. Oracle ([✓] all 3 inspections from Image 5)
        addTool("Oracle.ForwardDeclarationWithoutDefinition", "Forward declaration without definition", "Oracle",
                "Reports forward-declared PL/SQL procedures and functions missing implementation bodies.",
                HighlightSeverity.ERROR, true, "SQL");
        addTool("Oracle.MissingBodyForPackageSpecification", "Missing body for package/object type specification", "Oracle",
                "Reports package specifications that lack a corresponding package body.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("Oracle.OverloadingErrors", "Overloading errors", "Oracle",
                "Reports conflicting overloaded subprogram signatures in PL/SQL packages.",
                HighlightSeverity.WARNING, true, "SQL");

        // 57. Pandas ([✓] all 2 inspections from Image 5)
        addTool("Pandas.MethodSeriesToListRecommended", "Method Series.to_list() is recommended", "Pandas",
                "Suggests replacing Series.tolist() with Series.to_list() according to Pandas recommendations.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("Pandas.TruthValueOfDataFrameAmbiguous", "The truth value of a DataFrame is ambiguous", "Pandas",
                "Reports boolean evaluations on DataFrame or Series objects that should use .empty, .bool(), .any(), or .all().",
                HighlightSeverity.WARNING, true, "Python");

        // 58. Pattern validation ([✓] all 3 inspections from Image 5)
        addTool("PatternValidation.NonAnnotatedMethodOverridesPatternMethod", "Non-annotated Method overrides @Pattern Method", "Pattern validation",
                "Reports overriding methods that omit @Pattern annotations present on the overridden method.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("PatternValidation.PatternAnnotationNotApplicable", "Pattern annotation is not applicable", "Pattern validation",
                "Reports @Pattern annotations placed on incompatible field or parameter types.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("PatternValidation.ValidateAnnotatedPatterns", "Validate annotated patterns", "Pattern validation",
                "Validates regular expression syntax defined in @Pattern annotation value attributes.",
                HighlightSeverity.WARNING, true, "Java");

        // 59. PHP ([-] with 20 subcategories from Image 5)
        // Subcategories
        addTool("PHP.Attributes.InvalidAttribute", "Invalid attribute target", "PHP/Attributes",
                "Reports PHP 8 attributes declared on invalid syntax targets.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.CodeSmell.LongMethod", "Overly long method", "PHP/Code smell",
                "Reports PHP functions and methods whose line count exceeds threshold.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.CodeStyle.UnnecessarySemicolon", "Unnecessary semicolon", "PHP/Code style",
                "Reports redundant semicolons in PHP files.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Composer.DuplicateDependency", "Duplicate Composer package dependency", "PHP/Composer",
                "Reports duplicate package requirements in composer.json.",
                HighlightSeverity.WARNING, true, "JSON");
        addTool("PHP.ControlFlow.InfiniteLoop", "Infinite loop", "PHP/Control flow",
                "Reports while/for loops that cannot terminate normally.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.ErrorHandling.EmptyCatch", "Empty 'catch' block", "PHP/Error handling",
                "Reports catch blocks containing no statements.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.General.DuplicateCase", "Duplicate 'case' label", "PHP/General",
                "Reports duplicate case labels in switch statements.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Naming.ClassNaming", "Class naming convention", "PHP/Naming conventions",
                "Reports PHP class names that do not follow PascalCase conventions.",
                HighlightSeverity.WARNING, false, "PHP");
        addTool("PHP.Strict.StrictTypesMissing", "Missing declare(strict_types=1)", "PHP/PHP strict standards",
                "Reports PHP files missing strict types declarations.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Strict.DeprecatedDynamicProperties", "Deprecated dynamic properties", "PHP/PHP strict standards",
                "Reports creation of dynamic properties deprecated in PHP 8.2.",
                HighlightSeverity.WARNING, false, "PHP");
        addTool("PHP.PHPDoc.MissingParamTag", "Missing @param tag", "PHP/PHPDoc",
                "Reports functions missing docblock parameter tags.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.PHPUnit.AssertionWithoutExpectation", "Test without assertions", "PHP/PHPUnit",
                "Reports PHPUnit test methods with no assertion calls.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.ProbableBugs.DivisionByZero", "Division by zero", "PHP/Probable bugs",
                "Reports division or modulo operations where divisor evaluates to zero.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Psalm.TypeMismatch", "Psalm type mismatch", "PHP/Psalm",
                "Reports type inconsistencies detected by Psalm annotations.",
                HighlightSeverity.WARNING, false, "PHP");
        addTool("PHP.Quality.PHPCSFixer", "PHP CS Fixer inspection", "PHP/Quality tools",
                "Runs PHP CS Fixer style inspections.",
                HighlightSeverity.WARNING, false, "PHP");
        addTool("PHP.RegExp.InvalidPattern", "Invalid regular expression", "PHP/Regular expressions",
                "Reports syntax errors in PCRE regex pattern strings.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Assignments.CanUseCoalesce", "Can use null coalescing assignment", "PHP/Replaceable assignments",
                "Suggests using ??= null coalescing assignment operators.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Symfony.UnresolvedService", "Unresolved Symfony service", "PHP/Symfony",
                "Reports service identifiers that cannot be located in the Symfony container.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.TypeCompatibility.TypeMismatch", "Type mismatch in return", "PHP/Type compatibility",
                "Reports return expressions incompatible with declared return types.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Undefined.UndefinedVariable", "Undefined variable", "PHP/Undefined symbols",
                "Reports variables referenced before definition.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.Undefined.UndefinedConstant", "Undefined constant", "PHP/Undefined symbols",
                "Reports constants that are not declared.",
                HighlightSeverity.WARNING, false, "PHP");
        addTool("PHP.Unused.UnusedPrivateField", "Unused private field", "PHP/Unused symbols",
                "Reports private class properties that are never read.",
                HighlightSeverity.WARNING, true, "PHP");

        // 60. PostCSS ([-]) - 5 inspections matching reference
        initPostCssInspections();

        // 61. PostgreSQL ([✓])
        initPostgreSqlInspections();

        // 62. Proofreading ([✓])
        initProofreadingInspections();

        // 63. Properties files ([-]) - 9 inspections matching reference
        initPropertiesFilesInspections();

        // 64. Protocol Buffers ([✓])
        initProtocolBuffersInspections();

        // 64b. Pyramid ([✓])
        initPyramidInspections();

        // 65. Python ([-]) - All inspections matching reference
        initPythonInspections();

        // 66. Qodana ([ ]) - Unchecked
        initQodanaInspections();

        // 67. Quarkus ([-]) - 5 inspections matching reference
        initQuarkusInspections();

        // 68. RBS ([✓]) - 27 inspections across 5 subcategories matching reference
        initRbsInspections();

        // 68b. Reactive Streams ([✓]) - 12 inspections across 3 subcategories matching reference
        initReactiveStreamsInspections();

        // 69. RegExp ([-]) - 15 inspections matching reference
        initRegExpInspections();

        // 70. RELAX NG ([-]) - 2 inspections matching reference
        initRelaxNgInspections();

        // 71. Requirements ([✓]) - 2 inspections matching reference
        initRequirementsInspections();

        // 72. RESTful Web Service (JAX-RS) ([✓]) - 7 inspections matching reference
        initRestfulWebServiceInspections();

        // 73. Ruby ([-]) - 75+ inspections across 14 subcategories matching reference
        initRubyInspections();

        // 74. Rust ([-]) - 161 inspections across Cargo.toml, Lints, and core Rust matching screenshots
        initRustInspections();

        // 75. Sass/SCSS ([✓])
        initSassInspections();

        // 76. sbt ([✓])
        initSbtInspections();

        // 77. Scala ([-])
        initScalaInspections();

        // 78. Security ([✓])
        initSecurityInspections();

        // 79. Shell script ([✓])
        initShellScriptInspections();

        // 80. Spring ([✓])
        initSpringInspections();

        // 81. Spring Data ([✓])
        initSpringDataInspections();

        // 82. Spring Modulith ([✓])
        initSpringModulithInspections();

        // 83. SQL ([-])
        initSqlInspections();

        // 84. SQL server ([✓])
        initSqlServerInspections();

        // 85. Thymeleaf ([✓])
        initThymeleafInspections();

        // 86. TOML ([✓])
        initTomlInspections();

        // 87. Velocity ([✓])
        initVelocityInspections();

        // 88. Version control ([✓])
        initVersionControlInspections();

        // 89. Vue ([✓])
        initVueInspections();

        // 90. XML ([✓])
        initXmlInspections();

        // 91. XPath ([✓])
        initXPathInspections();

        // 92. XSLT ([✓])
        initXsltInspections();

        // 93. YAML ([✓])
        initYamlInspections();
    }

    private void addTool(String id, String displayName, String groupPath, String desc,
                         HighlightSeverity severity, boolean defaultEnabled, String language) {
        addTool(id, displayName, groupPath, desc, severity, defaultEnabled, language, false, false);
    }

    private void addTool(String id, String displayName, String groupPath, String desc,
                         HighlightSeverity severity, boolean defaultEnabled, String language,
                         boolean batchModeOnly, boolean cleanupTool) {
        InspectionTool tool = InspectionTool.builder(id)
                .displayName(displayName)
                .groupPath(groupPath)
                .description(desc)
                .defaultSeverity(severity)
                .defaultEnabled(defaultEnabled)
                .language(language)
                .batchModeOnly(batchModeOnly)
                .cleanupTool(cleanupTool)
                .build();
        toolsById.put(tool.getId(), tool);
    }

    private void initPostCssInspections() {
        addTool("PostCSS.InvalidCustomMedia", "Invalid custom media", "PostCSS",
                "Reports invalid CSS custom media queries according to PostCSS specifications.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("PostCSS.InvalidCustomSelector", "Invalid custom selector", "PostCSS",
                "Reports invalid CSS custom selectors in PostCSS stylesheets.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("PostCSS.InvalidMediaQueryRange", "Invalid media query range", "PostCSS",
                "Reports invalid media query ranges in PostCSS media expressions.",
                HighlightSeverity.ERROR, true, "CSS");
        addTool("PostCSS.InvalidNestedRule", "Invalid nested rule", "PostCSS",
                "Reports invalid CSS nesting syntax in PostCSS rule blocks.",
                HighlightSeverity.WARNING, false, "CSS");
        addTool("PostCSS.UnresolvedCssModuleValue", "Unresolved CSS module value", "PostCSS",
                "Reports unresolved @value definitions in CSS modules.",
                HighlightSeverity.ERROR, true, "CSS");
    }

    private void initPostgreSqlInspections() {
        addTool("PostgreSQL.SelectFromProcedureCall", "Postgres: Select from procedure call", "PostgreSQL",
                "Reports SELECT queries that invoke stored procedures instead of CALL statements in PostgreSQL.",
                HighlightSeverity.WARNING, true, "SQL");
    }

    private void initProofreadingInspections() {
        addTool("Proofreading.Grammar", "Grammar", "Proofreading",
                "Checks sentence structure and grammatical correctness in natural language text and comments.",
                HighlightSeverity.GRAMMAR_ERROR, true, "General");
        addTool("Proofreading.NaturalLanguageDetection", "Natural language detection", "Proofreading",
                "Detects natural languages in comments and text to apply appropriate dictionaries and spell checking rules.",
                HighlightSeverity.WARNING, true, "General");
        addTool("Proofreading.Typo", "Typo", "Proofreading",
                "Reports misspelled words in code identifiers, strings, and comments.",
                HighlightSeverity.TYPO, true, "General");
    }

    private void initPropertiesFilesInspections() {
        addTool("Properties.DuplicateProperty", "Duplicate property (available for Code | Inspect Code)", "Properties files",
                "Reports duplicate property keys in properties files and resource bundles.",
                HighlightSeverity.WARNING, false, "Properties", true, false);
        addTool("Properties.InconsistentResourceBundle", "Inconsistent resource bundle (available for Code | Inspect Code)", "Properties files",
                "Reports resource bundles with inconsistent keys across locale-specific properties files.",
                HighlightSeverity.ERROR, true, "Properties", true, false);
        addTool("Properties.MissingMessageFormatParameter", "Missing message format parameter", "Properties files",
                "Reports missing or mismatched MessageFormat parameters in localized property values.",
                HighlightSeverity.WARNING, true, "Properties");
        addTool("Properties.AlphabeticallyUnsorted", "Properties file or resource bundle is alphabetically unsorted", "Properties files",
                "Reports properties files that are not sorted alphabetically by key.",
                HighlightSeverity.WARNING, false, "Properties");
        addTool("Properties.DelimiterMismatch", "Property key/value delimiter doesn't match code style settings", "Properties files",
                "Reports property key/value delimiters that do not adhere to code style settings.",
                HighlightSeverity.WARNING, true, "Properties");
        addTool("Properties.SuspiciousLocaleLanguages", "Suspicious resource bundle locale languages", "Properties files",
                "Reports unusual or suspicious locale country/language code combinations in resource bundles.",
                HighlightSeverity.WARNING, false, "Properties");
        addTool("Properties.ThreeDotCharacters", "Three dot characters instead of the ellipsis", "Properties files",
                "Suggests replacing three consecutive dots (...) with the unicode ellipsis character (…).",
                HighlightSeverity.WARNING, false, "Properties");
        addTool("Properties.TrailingSpaces", "Trailing spaces in property", "Properties files",
                "Reports trailing whitespace characters at the end of property keys or values.",
                HighlightSeverity.WARNING, true, "Properties");
        addTool("Properties.UnusedProperty", "Unused property", "Properties files",
                "Reports property keys that are not referenced in the project code.",
                HighlightSeverity.WARNING, true, "Properties");
    }

    private void initProtocolBuffersInspections() {
        addTool("Protobuf.DuplicatedImport", "Duplicated import statements", "Protocol Buffers",
                "Reports duplicate import statements in .proto files.",
                HighlightSeverity.WARNING, true, "Protobuf");
    }

    private void initPyramidInspections() {
        addTool("Pyramid.ProjectNotInstalled", "Project is not installed for development", "Pyramid",
                "Reports Pyramid web applications that have not been installed in development mode via 'pip install -e .'.",
                HighlightSeverity.WARNING, true, "Python");
    }

    private void initPythonInspections() {
        // Subcategory Security
        addTool("PySecurityVulnerableApiInspection", "Vulnerable API usage", "Python/Security",
                "Reports Python API calls known to be vulnerable to common security exploits.",
                HighlightSeverity.WARNING, true, "Python");

        // Direct Python inspections matching reference screenshots 2, 3, 4
        addTool("PyByteLiteralNonAsciiInspection", "A byte literal contains a non-ASCII character", "Python",
                "Reports byte literals containing non-ASCII characters without proper escape sequences.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyProtectedMemberInspection", "Accessing a protected member of a class or a module", "Python",
                "Reports access to protected members outside class hierarchy or enclosing module.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInstanceAttributeDefinedOutsideInitInspection", "An instance attribute is defined outside `__init__`", "Python",
                "Reports instance attributes defined in methods other than __init__.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidInterpreterInspection", "An invalid interpreter", "Python",
                "Reports SDK interpreters configured with invalid Python paths.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyAssigningFunctionCallNoReturnInspection", "Assigning function calls that don't return anything", "Python",
                "Reports assigning results of functions that return None or lack return statements.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyAugmentAssignmentInspection", "Assignment can be replaced with augmented assignment", "Python",
                "Suggests replacing 'x = x + 1' with augmented assignment 'x += 1'.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyAssignmentToLoopOrWithParameterInspection", "Assignments to 'for' loop or 'with' statement parameter", "Python",
                "Reports reassignments to loop variables or with statement context targets.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyCallingNonCallableInspection", "Attempt to call a non-callable object", "Python",
                "Reports invocation of objects that do not implement __call__.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyClassSpecificDecoratorOutsideClassInspection", "Class-specific decorator is used outside the class", "Python",
                "Reports @classmethod or @staticmethod decorators used on module-level functions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyClassHasNoInitInspection", "Class has no `__init__` method", "Python",
                "Reports classes that do not declare an __init__ constructor method.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyClassicStyleClassUsageInspection", "Classic style class usage", "Python",
                "Reports old-style class definitions not inheriting from object in Python 2 compatible code.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyIncompatibleVersionInspection", "Code is incompatible with specific Python versions", "Python",
                "Reports language constructs that are incompatible with target Python runtime versions.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyCythonVariableUsedBeforeDeclarationInspection", "Cython variable is used before its declaration", "Python",
                "Reports Cython cdef variables referenced before their declaration statement.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyDeprecationInspection", "Deprecated function, class, or module", "Python",
                "Reports calls to deprecated functions, classes, and imported modules.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyDictDuplicateKeysInspection", "Dictionary contains duplicate keys", "Python",
                "Reports dictionary literals that contain duplicate keys.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyDictCreationInspection", "Dictionary creation can be rewritten by dictionary literal", "Python",
                "Suggests replacing consecutive subscript assignments with an inline dictionary literal.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyStringFormatInspection", "Errors in string formatting operations", "Python",
                "Reports mismatched placeholder counts and invalid specifiers in printf-style and str.format expressions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyExceptionInheritanceInspection", "Exceptions do not inherit from standard 'Exception' class", "Python",
                "Reports custom exception classes that do not derive from BaseException or Exception.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyFileNonAsciiInspection", "File contains non-ASCII character", "Python",
                "Reports non-ASCII characters in source files lacking encoding declarations.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyFirstArgumentReassignedInspection", "First argument of the method is reassigned", "Python",
                "Reports reassignments to 'self' or 'cls' parameters inside method bodies.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyFixtureNotRequestedInspection", "Fixture is not requested by test functions", "Python",
                "Reports pytest fixture definitions that are never requested by test functions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PySetFunctionToLiteralInspection", "Function call can be replaced with set literal", "Python",
                "Suggests replacing set([...]) constructor calls with { ... } set literal syntax.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyGlobalVariableAtModuleLevelInspection", "Global variable is not defined at the module level", "Python",
                "Reports 'global' declarations referencing identifiers that do not exist at module scope.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyImproperFirstParameterInspection", "Improper first parameter", "Python",
                "Reports instance methods missing 'self' parameter or class methods missing 'cls' parameter.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyImproperFutureImportInspection", "Improper position of from __future__ import", "Python",
                "Reports __future__ import statements that are not at the very top of the source file.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInappropriateAccessToPropertiesInspection", "Inappropriate access to properties", "Python",
                "Reports calling property descriptors directly as methods.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyIncompatibleNewAndInitSignaturesInspection", "Incompatible signatures of __new__ and __init__", "Python",
                "Reports mismatched parameter signatures between __new__ and __init__ methods.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyIncompatibleStubPackagesInspection", "Incompatible stub packages", "Python",
                "Reports installed PEP 561 typing stub packages that conflict with library runtime versions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInconsistentIndentationInspection", "Inconsistent indentation", "Python",
                "Reports mixed tabs and spaces or non-standard indentation levels.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInconsistentReturnStatementsInspection", "Inconsistent return statements", "Python",
                "Reports functions where some branches return values while others return implicitly or without an expression.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyPytestMarkParametrizeInspection", "Incorrect arguments in @pytest.mark.parametrize", "Python",
                "Validates argument names and values in pytest parametrization decorators.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyIncorrectCallArgumentsInspection", "Incorrect call arguments", "Python",
                "Reports missing required arguments, unexpected keyword arguments, and redundant parameters.",
                HighlightSeverity.WARNING, true, "Python");

        // CommandLineInspection (matches Image 3)
        addTool("CommandLineInspection", "Incorrect CLI syntax", "Python",
                "Reports the problems if the arguments of the command you type in the console are not in the proper order. The inspection also verifies that option names and arguments are correct.\n" +
                "Do not disable the inspection if you are going to use command-line interfaces like manage.py in Django.",
                HighlightSeverity.WARNING, true, "Python");

        addTool("PyIncorrectDocstringInspection", "Incorrect docstring", "Python",
                "Reports parameter names and return tags in docstrings that do not match function signature.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyIncorrectPropertyDefinitionInspection", "Incorrect property definition", "Python",
                "Reports getter, setter, or deleter declarations that violate @property conventions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyIncorrectTypeInspection", "Incorrect type", "Python",
                "Reports type annotation incompatibilities between actual argument types and expected parameters.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInitReturnsValueInspection", "__init__ method that returns a value", "Python",
                "Reports return statements in __init__ methods that return values other than None.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidAbstractClassDefinitionInspection", "Invalid abstract class definition and usages", "Python",
                "Reports instantiation of abstract classes with unimplemented abstract methods.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidDataClassesUsageInspection", "Invalid definition and usage of Data Classes", "Python",
                "Validates field definitions, default values, and inheritance hierarchy in @dataclass classes.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidNamedTupleDefinitionInspection", "Invalid definition of 'typing.NamedTuple'", "Python",
                "Reports invalid field definitions and default values in typing.NamedTuple subclasses.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidEnumDefinitionInspection", "Invalid Enum definition and usages", "Python",
                "Validates Enum class declarations, duplicate member names, and invalid enum values.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidProtocolDefinitionInspection", "Invalid protocol definitions and usages", "Python",
                "Validates typing.Protocol definitions and structural subtyping conformances.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidTypeHintsInspection", "Invalid type hints definitions and usages", "Python",
                "Reports malformed PEP 484 and PEP 585 type annotations.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidTypedDictInspection", "Invalid TypedDict definition and usages", "Python",
                "Validates typing.TypedDict definitions and required/not-required key access.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidClassVarInspection", "Invalid usage of ClassVar variables", "Python",
                "Reports ClassVar annotations used on instance variables or in unsupported scopes.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidNewStyleTypeParametersInspection", "Invalid usage of new-style type parameters and type aliases", "Python",
                "Validates PEP 695 type parameter syntax ('type X[T] = ...') in Python 3.12+ code.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidNewTypeInspection", "Invalid usage of NewType", "Python",
                "Validates typing.NewType definitions and constructor usage.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidOverrideDecoratorInspection", "Invalid usages of @override decorator", "Python",
                "Reports @override annotations on methods that do not override any base class method.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidSlotsDefinitionInspection", "Invalid usages of classes with '__slots__' definitions", "Python",
                "Reports attribute assignments and inheritance issues involving __slots__ definitions.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyInvalidFinalClassInspection", "Invalid usages of final classes, methods, and variables", "Python",
                "Reports subclassing of @final classes or overriding of @final methods.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyMethodNotDeclaredStaticInspection", "Method is not declared static", "Python",
                "Suggests adding @staticmethod decorator to methods that do not reference instance or class members.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyMethodSignatureOverrideMismatchInspection", "Method signature does not match signature of overridden method", "Python",
                "Reports subclass methods whose parameter signatures are incompatible with overridden base methods.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyMissedCallToSuperInitInspection", "Missed call to '__init__' of the super class", "Python",
                "Reports subclass __init__ methods that fail to invoke super().__init__().",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyMissingAwaitSyntaxInspection", "Missing 'await' syntax in coroutine calls", "Python",
                "Reports async coroutine function calls missing an 'await' expression.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyMissingOrEmptyDocstringInspection", "Missing or empty docstring", "Python",
                "Reports public modules, classes, and functions missing docstrings.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyMissingTypeHintingInspection", "Missing type hinting for function definition", "Python",
                "Reports functions and methods lacking parameter and return type annotations.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyNoEncodingSpecifiedInspection", "No encoding specified for file", "Python",
                "Reports source files that do not declare a '# -*- coding: utf-8 -*-' header.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyNonOptimalListDeclarationInspection", "Non-optimal list declaration", "Python",
                "Suggests replacing list multiplications and inefficient comprehensions with optimal constructors.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyOldStyleClassNewStyleFeaturesInspection", "Old-style class contains new-style class features", "Python",
                "Reports __slots__ or property descriptors used in old-style classes.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyOutdatedPoetryPackageVersionsInspection", "Outdated Poetry package versions", "Python",
                "Reports outdated package version constraints in pyproject.toml Poetry configurations.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyOverloadsInRegularFilesInspection", "Overloads in regular Python files", "Python",
                "Reports @overload declarations in implementation files that lack an implementation function.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyPep8Inspection", "PEP 8 coding style violation", "Python",
                "Reports adherence violations to the PEP 8 Python style guide.",
                HighlightSeverity.WEAK_WARNING, true, "Python");
        addTool("PyPep8NamingInspection", "PEP 8 naming convention violation", "Python",
                "Reports class, function, variable, and constant names violating PEP 8 naming standards.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyProblematicNestingOfDecoratorsInspection", "Problematic nesting of decorators", "Python",
                "Reports incorrect ordering of decorators like @property, @classmethod, and @staticmethod.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTrailingSemicolonInspection", "Prohibited trailing semicolon in a statement", "Python",
                "Reports redundant semicolons at the end of Python statements.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyRedeclaredNamesWithoutUsageInspection", "Redeclared names without usages", "Python",
                "Reports variables or imports that are reassigned or redefined without being read.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyRedundantBooleanVariableCheckInspection", "Redundant boolean variable check", "Python",
                "Reports boolean checks comparing boolean expressions against 'True' or 'False'.",
                HighlightSeverity.WARNING, true, "Python");

        // PyRedundantParenthesesInspection with options (matches Image 4)
        InspectionTool redundantParens = InspectionTool.builder("PyRedundantParenthesesInspection")
                .displayName("Redundant parentheses")
                .groupPath("Python")
                .description("Reports about redundant parentheses in expressions.\nThe IDE provides the quick-fix action to remove the redundant parentheses.")
                .defaultSeverity(HighlightSeverity.WEAK_WARNING)
                .defaultEnabled(true)
                .language("Python")
                .addOption("ignoreArgumentOfPercentOperator", "Ignore argument of % operator", false)
                .addOption("ignoreTuples", "Ignore tuples", false)
                .addOption("ignoreEmptyListsOfBaseClasses", "Ignore empty lists of base classes", false)
                .build();
        toolsById.put(redundantParens.getId(), redundantParens);

        addTool("PyShadowingBuiltinsInspection", "Shadowing built-in names", "Python",
                "Reports local variables or parameters shadowing standard built-in functions or types.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyShadowingNamesFromOuterScopesInspection", "Shadowing names from outer scopes", "Python",
                "Reports local variables shadowing variables defined in enclosing outer scopes.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PySingleQuotedDocstringInspection", "Single quoted docstring", "Python",
                "Suggests replacing single-quoted docstrings with standard triple-quoted docstrings.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyStatementHasNoEffectInspection", "Statement has no effect", "Python",
                "Reports expressions evaluated as statements whose results are unused.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyStubPackagesAdvertiserInspection", "Stub packages advertiser", "Python",
                "Suggests installing community typing stub packages for third-party libraries.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PySuspiciousRelativeImportsInspection", "Suspicious relative imports", "Python",
                "Reports relative import paths that ascend beyond package roots.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyDefaultArgumentMutableInspection", "The default argument is mutable", "Python",
                "Reports mutable default argument values such as lists or dictionaries in function signatures.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyFunctionArgumentEqualToDefaultParameterInspection", "The function argument is equal to the default parameter", "Python",
                "Reports argument values passed to function calls that match parameter default values.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyTooComplexChainedComparisonsInspection", "Too complex chained comparisons", "Python",
                "Reports chained comparison expressions whose evaluation order is ambiguous or unintuitive.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTupleAssignmentBalanceInspection", "Tuple assignment balance is incorrect", "Python",
                "Reports unpacked assignments where the number of targets does not match iterable length.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTupleItemAssignmentInspection", "Tuple item assignment is prohibited", "Python",
                "Reports attempt to mutate immutable tuple items via item assignment.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTypeCastWithImpossibleTypesInspection", "Type cast with impossible types", "Python",
                "Reports typing.cast calls where source and target types share no common subtypes.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTypeInDocstringDoesNotMatchInferredTypeInspection", "Type in docstring does not match inferred type", "Python",
                "Reports discrepancies between static inferred types and docstring type documentation.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyTypingAssertTypeInspection", "typing.assert_type", "Python",
                "Reports typing.assert_type checks where the inferred type does not match expected type.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnboundLocalVariableInspection", "Unbound local variables", "Python",
                "Reports local variables accessed before assignment in enclosing function scope.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnclearExceptionClausesInspection", "Unclear exception clauses", "Python",
                "Reports bare 'except:' clauses catching SystemExit or KeyboardInterrupt unintentionally.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnnecessaryBackslashInspection", "Unnecessary backslash", "Python",
                "Reports redundant line-continuation backslashes inside parentheses, brackets, or braces.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnnecessaryTypeCastInspection", "Unnecessary type cast", "Python",
                "Reports typing.cast calls where the expression already matches the specified target type.",
                HighlightSeverity.WARNING, false, "Python");
        addTool("PyUnreachableCodeInspection", "Unreachable code", "Python",
                "Reports statements positioned after unconditional returns, raises, or breaks.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnresolvedReferencesInspection", "Unresolved references", "Python",
                "Reports references to undefined variables, functions, classes, or modules.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnsatisfiedPackageRequirementsInspection", "Unsatisfied package requirements", "Python",
                "Reports imported packages that are missing from requirements.txt or setup.py.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnusedImportsInspection", "Unused imports", "Python",
                "Reports imported symbols that are not referenced in the file.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyUnusedLocalSymbolsInspection", "Unused local symbols", "Python",
                "Reports local variables or parameters that are never read.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyEqualityToNoneInspection", "Using equality operators to compare with None", "Python",
                "Suggests replacing '== None' and '!= None' with 'is None' and 'is not None'.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyWrongArgumentsToCallSuperInspection", "Wrong arguments to call super", "Python",
                "Reports super(Class, self) calls where Class is not an enclosing class.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("PyWrongOrderOfExceptClausesInspection", "Wrong order of 'except' clauses", "Python",
                "Reports derived exception handlers placed after general base exception handlers.",
                HighlightSeverity.WARNING, true, "Python");
    }

    private void initQodanaInspections() {
        addTool("Qodana.CodeMetrics", "Code metrics", "Qodana",
                "Calculates code complexity, maintainability index, and quality metrics using Qodana linters.",
                HighlightSeverity.WARNING, false, "General");
    }

    private void initQuarkusInspections() {
        addTool("Quarkus.ApplicationProperties", "Invalid Quarkus application.properties configuration", "Quarkus",
                "Validates configuration keys, formats, and environment profiles in Quarkus application.properties.",
                HighlightSeverity.WARNING, true, "Properties");
        addTool("Quarkus.YamlConfiguration", "Invalid Quarkus YAML configuration", "Quarkus",
                "Validates configuration structure, keys, and values in Quarkus application.yaml.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Quarkus.ConfigMappingPrefix", "Missing or empty 'prefix' attribute value in the '@ConfigMapping' annotation", "Quarkus",
                "Reports missing or blank prefix attributes on Quarkus @ConfigMapping interface annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Quarkus.ProducesConsumesMime", "Undeclared '@Produces' / '@Consumes' MIME types for '...", "Quarkus",
                "Reports JAX-RS resource endpoints missing explicit @Produces or @Consumes media types.",
                HighlightSeverity.WARNING, false, "Java");
        addTool("Quarkus.WrongAccessModifier", "Wrong access modifier of bean members with CDI annotations", "Quarkus",
                "Reports private, static, or final modifiers on Quarkus CDI bean injection points and observer methods.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initRbsInspections() {
        // RBS/Code style issues
        addTool("RBS.CodeStyle.LiteralClassReferenced", "Literal class referenced", "RBS/Code style issues",
                "Reports explicit references to literal classes in RBS signatures.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.CodeStyle.SimplifiableBooleanUnion", "Simplifiable boolean union", "RBS/Code style issues",
                "Suggests simplifying 'true | false' type unions to 'bool' in RBS.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.CodeStyle.UnnecessaryParentheses", "Unnecessary parentheses", "RBS/Code style issues",
                "Reports redundant parentheses around type signatures in RBS files.",
                HighlightSeverity.WEAK_WARNING, true, "Ruby");
        addTool("RBS.CodeStyle.UnnecessaryQualifier", "Unnecessary qualifier", "RBS/Code style issues",
                "Reports unnecessary module namespace qualifiers in RBS files.",
                HighlightSeverity.WARNING, true, "Ruby");

        // RBS/Data flow
        addTool("RBS.DataFlow.UnusedInterface", "Unused interface", "RBS/Data flow",
                "Reports RBS interface declarations that are never used or implemented.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.DataFlow.UnusedTypeAlias", "Unused type alias", "RBS/Data flow",
                "Reports RBS type aliases that are never referenced.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.DataFlow.UnusedTypeVariable", "Unused type variable", "RBS/Data flow",
                "Reports generic type variables in RBS declarations that are never used.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.DataFlow.UnusedUseClause", "Unused use clause", "RBS/Data flow",
                "Reports 'use' clauses in RBS files that do not import any referenced types.",
                HighlightSeverity.WARNING, true, "Ruby");

        // RBS/Inheritance issues
        addTool("RBS.Inheritance.ModuleUsedAsSuperclass", "Module used as superclass", "RBS/Inheritance issues",
                "Reports modules specified as superclasses instead of classes in RBS.",
                HighlightSeverity.ERROR, true, "Ruby");

        // RBS/Naming conventions
        addTool("RBS.Naming.UnconventionalInterfaceName", "Unconventional interface name", "RBS/Naming conventions",
                "Reports RBS interface names that do not follow standard Ruby naming conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.Naming.UnconventionalTypeAliasName", "Unconventional type alias name", "RBS/Naming conventions",
                "Reports RBS type alias names that do not follow PascalCase conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.Naming.UnconventionalTypeVariableName", "Unconventional type variable name", "RBS/Naming conventions",
                "Reports type variable names that do not conform to standard single uppercase letter style.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.Naming.UnconventionalUseAliasName", "Unconventional use alias name", "RBS/Naming conventions",
                "Reports alias names in 'use' clauses that do not conform to naming conventions.",
                HighlightSeverity.WARNING, true, "Ruby");

        // RBS/Probable bugs
        addTool("RBS.Bugs.BadTypeArgumentType", "Bad type argument type", "RBS/Probable bugs",
                "Reports invalid type arguments passed to generic types in RBS.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.BadTypeVariableDefaultType", "Bad type variable default type", "RBS/Probable bugs",
                "Reports default type values that do not satisfy type variable upper bounds.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.ConflictingTypeVariableCount", "Conflicting type variable count", "RBS/Probable bugs",
                "Reports mismatched type parameter counts in reopened classes or modules in RBS.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.ConflictingTypeVariableVariance", "Conflicting type variable variance", "RBS/Probable bugs",
                "Reports conflicting covariance/contravariance annotations on type variables.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.CyclicClassModuleAliasDeclaration", "Cyclic class/module alias declaration", "RBS/Probable bugs",
                "Reports cyclic alias declarations resulting in infinite recursive definitions.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.DuplicateDeclaration", "Duplicate declaration", "RBS/Probable bugs",
                "Reports duplicate method, member, or type declarations in the same RBS scope.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.DuplicateKeywordParameter", "Duplicate keyword parameter", "RBS/Probable bugs",
                "Reports duplicate keyword parameter names in RBS method signatures.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("RBS.Bugs.InconsistentClassModuleAlias", "Inconsistent class/module alias", "RBS/Probable bugs",
                "Reports class aliases referencing modules or module aliases referencing classes.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.IncorrectInclusionCall", "Incorrect inclusion call", "RBS/Probable bugs",
                "Reports invalid 'include', 'extend', or 'prepend' declarations in RBS.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.IncorrectTypeArgumentCount", "Incorrect type argument count", "RBS/Probable bugs",
                "Reports parameterized type references with wrong number of type arguments.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.IncorrectTypeArgumentVariance", "Incorrect type argument variance", "RBS/Probable bugs",
                "Reports type arguments violating variance annotations of generic parameters.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.InvalidMethodOverload", "Invalid method overload", "RBS/Probable bugs",
                "Reports method overloads that violate RBS signature rules or types.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.InvalidTypeArgumentUsage", "Invalid type argument usage", "RBS/Probable bugs",
                "Reports invalid type arguments used in type application positions.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.Bugs.UnresolvedReference", "Unresolved reference", "RBS/Probable bugs",
                "Reports references to classes, modules, or types that cannot be resolved in RBS.",
                HighlightSeverity.ERROR, true, "Ruby");
    }

    private void addTool(InspectionTool tool) {
        if (tool != null) {
            toolsById.put(tool.getId(), tool);
        }
    }

    private void initReactiveStreamsInspections() {
        // Reactive Streams/Common
        addTool("ReactiveStreams.Common.ClassImplementsPublisher", "Class implements Publisher", "Reactive Streams/Common",
                "Reports classes directly implementing the Reactive Streams Publisher interface.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Common.ClassImplementsSubscriber", "Class implements Subscriber", "Reactive Streams/Common",
                "Reports classes directly implementing the Reactive Streams Subscriber interface.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Common.ReturnNullOrNullableFromLambda", "Return null or something nullable from a lambda in transformation method", "Reactive Streams/Common",
                "Reports lambdas returning null or nullable values inside Reactive transformation operators.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Common.ThrowStatementInReactiveOperator", "Throw statement in Reactive operator", "Reactive Streams/Common",
                "Reports explicit throw statements inside Reactive operators instead of emitting error signals.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Common.TooLongSameMethodsChain", "Too long same methods chain", "Reactive Streams/Common",
                "Reports excessively long chains of identical reactive method calls.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Common.UnusedPublisher", "Unused publisher", "Reactive Streams/Common",
                "Reports reactive publishers created but never subscribed to or returned.",
                HighlightSeverity.WARNING, true, "Java");

        // Reactive Streams/Mutiny
        addTool("ReactiveStreams.Mutiny.CallingSubscribeInReactiveMethods", "Calling 'subscribe' in \"reactive\" methods", "Reactive Streams/Mutiny",
                "Reports calls to 'subscribe' inside methods that return reactive types in Mutiny.",
                HighlightSeverity.WARNING, true, "Java");

        // Reactive Streams/Reactor
        addTool("ReactiveStreams.Reactor.CallingSubscribeInReactiveMethods", "Calling 'subscribe' in \"reactive\" methods", "Reactive Streams/Reactor",
                "Reports calls to 'subscribe' inside methods that return Mono or Flux publishers.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Reactor.CallingTransformationOnReceiverWithPublisher", "Calling transformation function on receiver with Mono/Flux publisher", "Reactive Streams/Reactor",
                "Reports transformation calls on receivers with Mono or Flux publishers.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Reactor.UnfinishedStepVerifier", "Unfinished StepVerifier", "Reactive Streams/Reactor",
                "Reports Reactor StepVerifier test instances that are not terminated with verify().",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Reactor.UnnecessaryDebugInitialization", "Unnecessary debug initialization", "Reactive Streams/Reactor",
                "Reports redundant Hooks.onOperatorDebug() calls when Reactor debug agent is already attached.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("ReactiveStreams.Reactor.ZipContainsParameterWithMonoVoidType", "Zip contains parameter with Mono<Void> type", "Reactive Streams/Reactor",
                "Reports Mono.zip calls containing Mono<Void> parameters that never emit values.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initRegExpInspections() {
        addTool("RegExp.AnonymousCapturingGroup", "Anonymous capturing group or numeric back reference", "RegExp",
                "Reports capturing groups that are anonymous when named groups are preferred.",
                HighlightSeverity.WARNING, false, "RegExp");
        addTool("RegExp.BeginOrEndAnchorInUnexpectedPosition", "Begin or end anchor in unexpected position", "RegExp",
                "Reports '^' or '$' anchors placed at positions where they cannot match.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.ConsecutiveSpaces", "Consecutive spaces", "RegExp",
                "Reports multiple consecutive space characters in regular expressions.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.DuplicateBranchInAlternation", "Duplicate branch in alternation", "RegExp",
                "Reports identical branches within regular expression alternations.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.DuplicateCharacterInCharacterClass", "Duplicate character in character class", "RegExp",
                "Reports duplicate character literals inside character classes.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.EmptyBranchInAlternation", "Empty branch in alternation", "RegExp",
                "Reports empty branches in alternations that match empty strings unconditionally.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.EscapedMetaCharacter", "Escaped meta character", "RegExp",
                "Reports unnecessary escaping of characters that are not metacharacters in context.",
                HighlightSeverity.WEAK_WARNING, true, "RegExp");
        addTool("RegExp.OctalEscape", "Octal escape", "RegExp",
                "Reports legacy octal escape sequences in regular expressions.",
                HighlightSeverity.WEAK_WARNING, true, "RegExp");
        addTool("RegExp.RedundantClassElements", "Redundant '\\d', '[:digit:]', or '\\D' class elements", "RegExp",
                "Reports redundant character class shorthand elements in regular expressions.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.RedundantCharacterEscape", "Redundant character escape", "RegExp",
                "Reports redundant backslash escapes for characters that do not require escaping.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.RedundantNestedCharacterClass", "Redundant nested character class", "RegExp",
                "Reports character classes nested inside other character classes without set operations.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.RegularExpressionCanBeSimplified", "Regular expression can be simplified", "RegExp",
                "Suggests simplifications for verbose or redundant regular expression patterns.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.SingleCharacterAlternation", "Single character alternation", "RegExp",
                "Suggests replacing single-character alternation '(a|b)' with character class '[ab]'.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.SuspiciousBackReference", "Suspicious back reference", "RegExp",
                "Reports back references referencing groups that cannot have matched yet.",
                HighlightSeverity.WARNING, true, "RegExp");
        addTool("RegExp.UnnecessaryNonCapturingGroup", "Unnecessary non-capturing group", "RegExp",
                "Reports non-capturing groups '(?:...)' that have no effect on operator precedence.",
                HighlightSeverity.WARNING, true, "RegExp");
    }

    private void initRelaxNgInspections() {
        addTool("RelaxNg.UnresolvedReference", "Unresolved reference", "RELAX NG",
                "Reports references to undefined patterns, elements, or defines in RELAX NG schemas.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("RelaxNg.UnusedDefine", "Unused define", "RELAX NG",
                "Reports define declarations in RELAX NG schemas that are never referenced.",
                HighlightSeverity.WEAK_WARNING, false, "XML");
    }

    private void initRequirementsInspections() {
        addTool("Requirements.RequirementNotSatisfied", "Requirement is not satisfied", "Requirements",
                "Reports packages in requirements.txt that are not installed in the active environment.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("Requirements.RequirementIsOutdated", "Requirement is outdated", "Requirements",
                "Reports installed package versions that do not satisfy version bounds specified in requirements.txt.",
                HighlightSeverity.WARNING, true, "Python");
    }

    private void initRestfulWebServiceInspections() {
        addTool("JaxRs.GetMethodReturnsVoid", "@GET annotated method returns a void value", "RESTful Web Service (JAX-RS)",
                "Reports JAX-RS resource methods annotated with @GET that have a void return type.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JaxRs.IncorrectPathUriTemplate", "Incorrect @Path URI template", "RESTful Web Service (JAX-RS)",
                "Reports invalid URI template syntax in JAX-RS @Path annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JaxRs.IncorrectDefaultValueParameter", "Incorrect value of @DefaultValue parameter", "RESTful Web Service (JAX-RS)",
                "Reports @DefaultValue annotations with values that cannot be parsed to the target parameter type.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("JaxRs.IncorrectWadlConfiguration", "Incorrect WADL configuration", "RESTful Web Service (JAX-RS)",
                "Reports configuration errors in WADL generator or descriptor settings.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JaxRs.PathClassWithoutResourceMethods", "@Path class without resource methods", "RESTful Web Service (JAX-RS)",
                "Reports classes annotated with @Path that do not declare any resource or sub-resource methods.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JaxRs.MultipleHttpMethodAnnotations", "Resource method with multiple HTTP method annotations", "RESTful Web Service (JAX-RS)",
                "Reports resource methods annotated with more than one HTTP method designator (@GET, @POST, etc.).",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JaxRs.UnresolvedPathParamReference", "Unresolved @PathParam reference", "RESTful Web Service (JAX-RS)",
                "Reports @PathParam parameter values that do not match any path template variables in @Path.",
                HighlightSeverity.ERROR, true, "Java");
    }

    private void initRubyInspections() {
        initRubyCodeMetricsAndStyleInspections();
        initRubyControlFlowAndDataFlowInspections();
        initRubyGemsAndGeneralInspections();
        initRubyNamingConventionsInspections();
        initRubyProbableBugsInspections();
        initRubyRailsAndRbsAndYardInspections();
    }

    private void initRubyCodeMetricsAndStyleInspections() {
        // Ruby/Code metrics
        addTool("Ruby.Metrics.TooManyInstanceVariables", "Class/module with too many instance variables", "Ruby/Code metrics",
                "Reports classes or modules declaring an excessive number of instance variables.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Metrics.TooManyMethods", "Class/module with too many methods", "Ruby/Code metrics",
                "Reports classes or modules with too many methods defined.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/Code style issues
        addTool("Ruby.Style.StringArrayLiteral", "Array of string literals instead of '%w'", "Ruby/Code style issues",
                "Suggests using %w[...] literal syntax instead of an array of string literals.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.BlockInsteadOfMethodReference", "Block used instead of method reference", "Ruby/Code style issues",
                "Suggests using '&:method' syntax instead of a block with a single method call.",
                HighlightSeverity.WEAK_WARNING, true, "Ruby");
        addTool("Ruby.Style.ClassVariableUsage", "Class variable usage", "Ruby/Code style issues",
                "Reports usage of class variables (@@var) which can lead to unexpected behavior across inheritance.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.ComplexCallChain", "Complex call chain", "Ruby/Code style issues",
                "Reports excessively complex method call chains violating the Law of Demeter.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.ConditionalWithParentheses", "Conditional expression with parentheses", "Ruby/Code style issues",
                "Reports redundant parentheses around if/unless/while conditional expressions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.HashWithStringsAsKeys", "Hash with strings as keys", "Ruby/Code style issues",
                "Suggests using symbols instead of strings as hash keys for memory efficiency.",
                HighlightSeverity.WEAK_WARNING, false, "Ruby");
        addTool("Ruby.Style.IncorrectParenthesesInMethodDefinition", "Incorrect parentheses in method definition", "Ruby/Code style issues",
                "Reports missing or inappropriate parentheses around method definition parameter lists.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.InterpolatedVariableWithoutBraces", "Interpolated variable without braces", "Ruby/Code style issues",
                "Suggests using braces #{...} for variable interpolation in double-quoted strings.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.LegacyHashSyntax", "Legacy hash syntax", "Ruby/Code style issues",
                "Suggests using symbol: value syntax instead of legacy '=>' hash rocket syntax.",
                HighlightSeverity.WEAK_WARNING, false, "Ruby");
        addTool("Ruby.Style.RedundantParenthesesInNoArgCall", "Redundant parentheses in no-arg method call", "Ruby/Code style issues",
                "Reports redundant empty parentheses in method calls with no arguments.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Style.UnnecessaryDoubleQuotes", "Unnecessary use of double quotes in string", "Ruby/Code style issues",
                "Suggests using single quotes for strings that do not contain interpolation or escape sequences.",
                HighlightSeverity.WEAK_WARNING, true, "Ruby");
        addTool("Ruby.Style.WhitespaceBeforeParenthesesInMethodCall", "Whitespace before parentheses in method call", "Ruby/Code style issues",
                "Reports whitespace between method name and open parenthesis in method calls.",
                HighlightSeverity.WARNING, true, "Ruby");
    }

    private void initRubyControlFlowAndDataFlowInspections() {
        // Ruby/Control flow issues
        addTool("Ruby.ControlFlow.CaseWithoutElse", "'case' statement without 'else' block", "Ruby/Control flow issues",
                "Reports 'case' statements that do not have an 'else' branch.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.IncorrectJumpContext", "Incorrect context for jump statement", "Ruby/Control flow issues",
                "Reports 'break', 'next', or 'redo' used outside a loop or iterator block.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.NegativeCondition", "Negative condition in control flow statement", "Ruby/Control flow issues",
                "Suggests using 'unless' or positive conditions instead of negated expressions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.NestedTernary", "Nested ternary operator", "Ruby/Control flow issues",
                "Reports nested ternary operator expressions which reduce code readability.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.SimplifiableIf", "Simplifiable 'if' statement", "Ruby/Control flow issues",
                "Reports 'if' statements that can be simplified using modifier syntax or ternary operators.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.UnconventionalForLoop", "Unconventional 'for' loop", "Ruby/Control flow issues",
                "Suggests using 'each' iterator blocks instead of 'for' loops in Ruby.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.ControlFlow.UnlessWithElse", "'unless' statement with 'else'", "Ruby/Control flow issues",
                "Reports 'unless' statements with an 'else' branch which can be confusing to read.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/Data flow
        addTool("Ruby.DataFlow.FrozenObjectModification", "Frozen object modification", "Ruby/Data flow",
                "Reports attempts to modify frozen objects or strings.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("Ruby.DataFlow.NilDereference", "'nil' dereference", "Ruby/Data flow",
                "Reports method invocations on expressions that may evaluate to nil.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.DataFlow.RedundantSafeNavigation", "Redundant safe navigation", "Ruby/Data flow",
                "Reports redundant '&.' safe navigation operator when the target receiver cannot be nil.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.DataFlow.UnreachableCode", "Unreachable code", "Ruby/Data flow",
                "Reports code that can never be executed due to previous jump or return statements.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.DataFlow.UnusedLocalVariableOrParameter", "Unused local variable/parameter", "Ruby/Data flow",
                "Reports local variables or method parameters that are never read or referenced.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.DataFlow.VariableParameterScope", "Variable/parameter scope", "Ruby/Data flow",
                "Reports variables used outside their intended scope or shadowed by block parameters.",
                HighlightSeverity.WARNING, true, "Ruby");
    }

    private void initRubyGemsAndGeneralInspections() {
        // Ruby/Gems and gem management
        addTool("Ruby.Gems.Brakeman", "Brakeman (available for Code | Inspect Code)", "Ruby/Gems and gem management",
                "Runs Brakeman security scanner to identify security vulnerabilities in Ruby on Rails code.",
                HighlightSeverity.WARNING, true, "Ruby", true, false);
        addTool("Ruby.Gems.MissingGem", "Missing gem", "Ruby/Gems and gem management",
                "Reports required gems in Gemfile that are not installed in current environment.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Gems.MissingRbenvGemset", "Missing Rbenv gemset", "Ruby/Gems and gem management",
                "Reports configured rbenv gemsets that do not exist locally.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Gems.RuboCop", "RuboCop", "Ruby/Gems and gem management",
                "Runs RuboCop linter and style checker on Ruby source files.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Gems.ShouldaTestWithIncorrectFileName", "Shoulda test with incorrect file name", "Ruby/Gems and gem management",
                "Reports Shoulda test files that do not follow the standard naming pattern.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/General
        addTool("Ruby.General.NoRubyInterpreterConfigured", "No Ruby interpreter configured for the project", "Ruby/General",
                "Reports when no Ruby SDK or interpreter is configured for the project.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/Inheritance issues
        addTool("Ruby.Inheritance.ModuleUsedAsSuperclass", "Module used as superclass", "Ruby/Inheritance issues",
                "Reports modules specified as superclasses in class declarations.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/Language level migration aids
        addTool("Ruby.Migration.DeprecatedSyntax", "Deprecated syntax", "Ruby/Language level migration aids",
                "Reports deprecated Ruby syntax constructs that are obsolete in modern Ruby versions.",
                HighlightSeverity.WARNING, true, "Ruby");
    }

    private void initRubyNamingConventionsInspections() {
        addTool("Ruby.Naming.UnconventionalClassModuleName", "Unconventional class/module name", "Ruby/Naming conventions",
                "Reports class or module names that do not follow PascalCase conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalClassMethodName", "Unconventional class method name", "Ruby/Naming conventions",
                "Reports class method names that do not follow snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalClassVariableName", "Unconventional class variable name", "Ruby/Naming conventions",
                "Reports class variable names that do not follow @@snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalConstantName", "Unconventional constant name", "Ruby/Naming conventions",
                "Reports constant names that do not follow SCREAMING_SNAKE_CASE or PascalCase.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalGlobalVariableName", "Unconventional global variable name", "Ruby/Naming conventions",
                "Reports global variable names that do not follow $snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalInstanceMethodName", "Unconventional instance method name", "Ruby/Naming conventions",
                "Reports instance method names that do not follow snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalInstanceVariableName", "Unconventional instance variable name", "Ruby/Naming conventions",
                "Reports instance variable names that do not follow @snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalLocalVariableName", "Unconventional local variable name", "Ruby/Naming conventions",
                "Reports local variable names that do not follow snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Naming.UnconventionalParameterName", "Unconventional parameter name", "Ruby/Naming conventions",
                "Reports method parameter names that do not follow snake_case conventions.",
                HighlightSeverity.WARNING, true, "Ruby");
    }

    private void initRubyProbableBugsInspections() {
        addTool("Ruby.Bugs.AssignmentInConditional", "Assignment expression in conditional", "Ruby/Probable bugs",
                "Reports assignments inside conditional expressions which may be intended as equality checks.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.DuplicateKeyInHash", "Duplicate key in hash", "Ruby/Probable bugs",
                "Reports duplicate key literals defined in hash literals.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.DynamicConstantAssignment", "Dynamic constant assignment", "Ruby/Probable bugs",
                "Reports constant assignments inside method bodies or dynamic blocks.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("Ruby.Bugs.EmptyElseBlock", "Empty 'else' block", "Ruby/Probable bugs",
                "Reports empty 'else' branches in 'if', 'unless', or 'case' statements.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.EmptyRescueBlock", "Empty 'rescue' block", "Ruby/Probable bugs",
                "Reports empty 'rescue' clauses that swallow exceptions silently.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.ExpressionSubstitutionInSingleQuotedString", "Expression substitution in single-quoted string", "Ruby/Probable bugs",
                "Reports #{...} interpolation syntax in single-quoted strings where interpolation does not occur.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.IncorrectHashCall", "Incorrect 'Hash[...]' call", "Ruby/Probable bugs",
                "Reports invalid argument formats passed to Hash[] constructor.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.IncorrectCallArgumentCount", "Incorrect call argument count", "Ruby/Probable bugs",
                "Reports method calls with too few or too many arguments.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.InvalidCallToProtectedPrivateMethod", "Invalid call to protected/private method", "Ruby/Probable bugs",
                "Reports calls to private or protected methods from outside the valid context.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.MismatchedArgumentType", "Mismatched argument type", "Ruby/Probable bugs",
                "Reports method arguments whose inferred types do not match RBS or YARD type signatures.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.MismatchedConstantType", "Mismatched constant type", "Ruby/Probable bugs",
                "Reports constant initializers whose types do not match RBS type signatures.",
                HighlightSeverity.WARNING, true, "Ruby");

        // RubyMismatchedGlobalVariableType with 2 options matching screenshot 5
        addTool(InspectionTool.builder("RubyMismatchedGlobalVariableType")
                .displayName("Mismatched global variable type")
                .groupPath("Ruby/Probable bugs")
                .description("Reports global variable assignments whose inferred types don't match the global variable's expected type. The expected constant type is taken from the RBS type signature.")
                .defaultSeverity(HighlightSeverity.WARNING)
                .defaultEnabled(true)
                .language("Ruby")
                .addOption("checkTypesFromRbs", "Check types from RBS", true)
                .addOption("checkNilability", "Check nilability", true)
                .build());

        addTool("Ruby.Bugs.MismatchedParameterType", "Mismatched parameter type", "Ruby/Probable bugs",
                "Reports method parameter default values or usages conflicting with type signatures.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.MismatchedReturnType", "Mismatched return type", "Ruby/Probable bugs",
                "Reports method return expressions whose types do not match RBS or YARD signatures.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.MismatchedVariableType", "Mismatched variable type", "Ruby/Probable bugs",
                "Reports variable assignments whose types do not match declared RBS types.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.SuperCallWithNoSuperclass", "'super' call with no superclass defined", "Ruby/Probable bugs",
                "Reports 'super' calls in classes that do not inherit from any superclass.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.UnbalancedBrackets", "Unbalanced brackets", "Ruby/Probable bugs",
                "Reports mismatched or unclosed parentheses, brackets, or braces.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.UnexpectedArgumentInMethodCall", "Unexpected argument in method call", "Ruby/Probable bugs",
                "Reports unexpected arguments passed to methods that take no parameters.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Bugs.UnresolvedReference", "Unresolved reference", "Ruby/Probable bugs",
                "Reports references to classes, modules, methods, or variables that cannot be resolved.",
                HighlightSeverity.WARNING, true, "Ruby");
    }

    private void initRubyRailsAndRbsAndYardInspections() {
        // Ruby/Rails
        addTool("Ruby.Rails.DeprecatedFeature", "Deprecated feature", "Ruby/Rails",
                "Reports usage of deprecated Ruby on Rails APIs and features.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Rails.FindByInView", "'find' or 'find_by' method call in view", "Ruby/Rails",
                "Reports database query methods called directly from view templates.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Rails.MailboxMissingProcessMethod", "Mailbox missing 'process' method", "Ruby/Rails",
                "Reports Action Mailbox classes that do not define a 'process' method.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Rails.MailboxWithNoRoutes", "Mailbox with no routes", "Ruby/Rails",
                "Reports Action Mailbox classes with no inbound email routing configured.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Rails.UndefinedChannel", "Undefined channel", "Ruby/Rails",
                "Reports Action Cable channel subscriptions referencing non-existent channel classes.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Rails.UntranslatedI18nProperty", "Untranslated i18n property", "Ruby/Rails",
                "Reports missing translation keys in Rails I18n translation bundles.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/RBS
        addTool("Ruby.Rbs.MissingTypeSignature", "Missing type signature", "Ruby/RBS",
                "Reports Ruby classes, modules, or methods lacking corresponding RBS type signatures.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/Redundant code
        addTool("Ruby.Redundant.ExpressionCanBeSimplified", "Expression can be simplified", "Ruby/Redundant code",
                "Suggests simplifications for boolean or mathematical expressions.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Redundant.RedundantReturnStatement", "Redundant 'return' statement", "Ruby/Redundant code",
                "Reports redundant 'return' statements at the end of method bodies.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Redundant.RedundantSemicolon", "Redundant semicolon", "Ruby/Redundant code",
                "Reports unnecessary semicolons used at the ends of lines in Ruby code.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Redundant.RedundantVariableUsedAsReturnValue", "Redundant variable used as 'return' value", "Ruby/Redundant code",
                "Reports local variables assigned immediately before being returned.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Redundant.ThenInMultilineBlock", "'then' in multiline 'if'/'unless' block", "Ruby/Redundant code",
                "Reports redundant 'then' keywords in multiline 'if' or 'unless' statements.",
                HighlightSeverity.WARNING, true, "Ruby");

        // Ruby/YARD
        addTool("Ruby.Yard.IncorrectTagUsage", "Incorrect tag usage", "Ruby/YARD",
                "Reports invalid or misplaced YARD doc tags and annotations.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.Yard.MissingParamTag", "Missing '@param' tag in method parameter", "Ruby/YARD",
                "Reports method parameters that do not have documented @param tags in YARD documentation.",
                HighlightSeverity.WEAK_WARNING, true, "Ruby");
        addTool("Ruby.Yard.MissingReturnTag", "Missing '@return' tag in method", "Ruby/YARD",
                "Reports methods with return values lacking @return tags in YARD documentation.",
                HighlightSeverity.WEAK_WARNING, true, "Ruby");
    }

    private void initRustInspections() {
        initRustCargoTomlInspections();
        initRustLintsNamingConventionsInspections();
        initRustLintsOtherInspections();
        initRustCoreInspectionsPart1();
        initRustCoreInspectionsPart2();
        initRustCoreInspectionsPart3();
        initRustCoreInspectionsPart4();
    }

    private void initRustCargoTomlInspections() {
        // Rust/Cargo.toml (all enabled [✓])
        addTool("RsCargoCrateNotFound", "Crate not found", "Rust/Cargo.toml",
                "Reports crate names in Cargo.toml dependencies that cannot be found on crates.io.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsCargoCyclicFeatureDependency", "Cyclic feature dependency", "Rust/Cargo.toml",
                "Reports cyclic dependencies between features declared in Cargo.toml.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCargoDuplicateKey", "Duplicate key", "Rust/Cargo.toml",
                "Reports duplicate keys declared in the same Cargo.toml table.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCargoInvalidCrateVersion", "Invalid crate version", "Rust/Cargo.toml",
                "Reports invalid semver requirement strings in Cargo.toml dependency declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCargoNewerCrateVersion", "Newer crate version available", "Rust/Cargo.toml",
                "Reports dependency requirements that have newer compatible versions available on crates.io.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsCargoSchemaViolation", "Schema violation", "Rust/Cargo.toml",
                "Reports invalid table keys or value types violating Cargo.toml schema specification.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCargoUnclosedStringLiteral", "Unclosed string literal", "Rust/Cargo.toml",
                "Reports string literals in Cargo.toml that are missing closing quotes.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCargoUnnecessaryPackageField", "Unnecessary `package` field", "Rust/Cargo.toml",
                "Reports redundant 'package' field in Cargo.toml dependency tables when crate name matches.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsCargoUnusedDependency", "Unused dependency", "Rust/Cargo.toml",
                "Reports declared dependencies in Cargo.toml that are never imported or used in the crate.",
                HighlightSeverity.WARNING, true, "Rust");
    }

    private void initRustLintsNamingConventionsInspections() {
        // Rust/Lints/Naming conventions (all enabled [✓])
        addTool("RsArgumentNamingConvention", "Argument naming convention", "Rust/Lints/Naming conventions",
                "Reports function and method argument names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsAssocTypeNamingConvention", "Associated type naming convention", "Rust/Lints/Naming conventions",
                "Reports associated type names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsConstantNamingConvention", "Constant naming convention", "Rust/Lints/Naming conventions",
                "Reports constant names that do not follow SCREAMING_SNAKE_CASE convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsEnumNamingConvention", "Enum naming convention", "Rust/Lints/Naming conventions",
                "Reports enum names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsEnumVariantNamingConvention", "Enum variant naming convention", "Rust/Lints/Naming conventions",
                "Reports enum variant names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsFieldNamingConvention", "Field naming convention", "Rust/Lints/Naming conventions",
                "Reports struct and union field names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsFunctionNamingConvention", "Function naming convention", "Rust/Lints/Naming conventions",
                "Reports function names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsLifetimeNamingConvention", "Lifetime naming convention", "Rust/Lints/Naming conventions",
                "Reports lifetime parameter names that do not follow standard lowercase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsMacroNamingConvention", "Macro naming convention", "Rust/Lints/Naming conventions",
                "Reports macro names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsMethodNamingConvention", "Method naming convention", "Rust/Lints/Naming conventions",
                "Reports method names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsModuleNamingConvention", "Module naming convention", "Rust/Lints/Naming conventions",
                "Reports module names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsSelfConvention", "Self convention", "Rust/Lints/Naming conventions",
                "Reports methods taking 'self' whose names do not follow standard Rust self conventions (as_*, to_*, into_*).",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsStaticConstantNamingConvention", "Static constant naming convention", "Rust/Lints/Naming conventions",
                "Reports static variable names that do not follow SCREAMING_SNAKE_CASE convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsStructNamingConvention", "Struct naming convention", "Rust/Lints/Naming conventions",
                "Reports struct names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsTraitNamingConvention", "Trait naming convention", "Rust/Lints/Naming conventions",
                "Reports trait names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsTypeAliasNamingConvention", "Type alias naming convention", "Rust/Lints/Naming conventions",
                "Reports type alias names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsTypeParameterNamingConvention", "Type parameter naming convention", "Rust/Lints/Naming conventions",
                "Reports generic type parameter names that do not follow UpperCamelCase convention.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsVariableNamingConvention", "Variable naming convention", "Rust/Lints/Naming conventions",
                "Reports local variable and binding names that do not follow snake_case convention.",
                HighlightSeverity.WARNING, true, "Rust");
    }

    private void initRustLintsOtherInspections() {
        // Rust/Lints (all enabled [✓])
        addTool("RsCastCanBeReplacedWithLiteralSuffix", "Cast can be replaced with literal suffix", "Rust/Lints",
                "Reports numeric cast expressions (e.g. '0 as u64') that can be replaced with a literal suffix (e.g. '0u64').",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDeprecatedElement", "Deprecated element", "Rust/Lints",
                "Reports usage of functions, structs, or traits annotated with #[deprecated].",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsIneffectivePathStatements", "Ineffective path statements", "Rust/Lints",
                "Reports path statements in function bodies that have no side effects.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsLiveness", "Liveness analysis", "Rust/Lints",
                "Reports unused local variables and parameters.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsNonShorthandFieldPattern", "Non-shorthand field pattern", "Rust/Lints",
                "Reports struct field pattern matches where field name and binding name match but are written out fully.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsRedundantMustUse", "Redundant `#[must_use]`", "Rust/Lints",
                "Reports #[must_use] attributes applied to types or functions where it has no effect.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsRedundantSemicolons", "Redundant semicolons", "Rust/Lints",
                "Reports extra consecutive semicolons in statements or items.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnknownCrateTypes", "Unknown crate types", "Rust/Lints",
                "Reports unknown crate types specified in #![crate_type] attribute.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnnecessaryCast", "Unnecessary cast", "Rust/Lints",
                "Reports numeric or pointer casts where expression already has the target type.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnnecessaryLifetimeAnnotations", "Unnecessary lifetime annotations", "Rust/Lints",
                "Reports explicit lifetime parameters that can be elided according to lifetime elision rules.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnnecessaryParentheses", "Unnecessary parentheses", "Rust/Lints",
                "Reports redundant parentheses around expressions in if, while, or match statements.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnnecessaryPathPrefix", "Unnecessary path prefix", "Rust/Lints",
                "Reports redundant 'self::' or 'crate::' path prefixes when the item is already in scope.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnnecessaryReturn", "Unnecessary return", "Rust/Lints",
                "Reports explicit 'return' statements at the end of function bodies that can be trailing expressions.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnreachableCode", "Unreachable code", "Rust/Lints",
                "Reports code statements following diverging expressions (panic!, return, break, continue).",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnreachablePatterns", "Unreachable patterns", "Rust/Lints",
                "Reports match arms or patterns that can never be reached because preceding patterns match all cases.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnusedMustUse", "Unused `#[must_use]`", "Rust/Lints",
                "Reports expressions returning values marked #[must_use] that are ignored without inspection.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnusedMut", "Unused `mut` modifier", "Rust/Lints",
                "Reports variable bindings declared with 'mut' that are never mutated.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnusedImport", "Unused import", "Rust/Lints",
                "Reports 'use' declarations that are not referenced in the module.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnusedLabels", "Unused labels", "Rust/Lints",
                "Reports loop labels that are never referenced by break or continue statements.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnwrapCanBeReplacedWithQuestion", "Unwrap can be replaced with `?`", "Rust/Lints",
                "Suggests replacing unwrap() calls with the '?' try operator in functions returning Result or Option.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsWhileTrueCanBeReplacedWithLoop", "`while true` can be replaced with `loop`", "Rust/Lints",
                "Suggests replacing 'while true { ... }' with idiomatically preferred 'loop { ... }'.",
                HighlightSeverity.WARNING, true, "Rust");
    }

    private void initRustCoreInspectionsPart1() {
        // Direct Rust inspections (Part 1: A - D)
        addTool("RsNonConstantValueInConstantExpression", "A non-constant value was used in a constant expression", "Rust",
                "Reports expressions in constant evaluation positions that depend on runtime values.",
                HighlightSeverity.WARNING, false, "Rust");
        addTool("RsAdditionalNonAutoTraitInTraitObject", "Additional non-auto trait in trait object", "Rust",
                "Reports trait objects declaring more than one non-auto trait.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsAmbiguousMethodCall", "Ambiguous method call (experimental)", "Rust",
                "Reports method calls that could resolve to multiple in-scope traits.",
                HighlightSeverity.WARNING, false, "Rust");
        addTool("RsAnonymousFunctionParameters", "Anonymous function parameters not allowed", "Rust",
                "Reports function parameter declarations lacking parameter names in fn definitions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsApproximatedConstants", "Approximated constants", "Rust",
                "Reports literals that approximate mathematical constants (e.g. 3.14159) instead of std::f64::consts.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsAsyncNonMoveClosureWithParameters", "`async` non-`move` closure with parameters (unsupported)", "Rust",
                "Reports async non-move closures with parameters which are not yet supported by rustc.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCallNotFunction", "Attempt to call not a function", "Rust",
                "Reports call expressions on items or expressions that are not functions or do not implement Fn traits.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsAttributeError", "Attribute error", "Rust",
                "Reports misplaced, unknown, or malformed attributes on Rust items.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsAttributeWithoutParentheses", "Attribute without parentheses", "Rust",
                "Reports attributes that require parentheses for arguments but are missing them.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsBaseExpressionRequiredAfterDotDot", "Base expression required after `..`", "Rust",
                "Reports struct update syntax where '..' is not followed by a base expression.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsAsyncBlockingSleep", "Blocking `sleep` function cannot be used in `async` context", "Rust",
                "Reports std::thread::sleep calls inside async functions or blocks instead of tokio/async sleep.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsSimplifyBooleanExpression", "Boolean expression can be simplified", "Rust",
                "Suggests simplifying boolean expressions using De Morgan's laws or boolean identities.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsBorrowChecker", "Borrow checker errors", "Rust",
                "Reports borrow checker ownership and borrowing violations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCopyAndDropImplemented", "Both `Copy` and `Drop` implemented", "Rust",
                "Reports types that implement both Copy and Drop traits which is forbidden in Rust.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidCharLiteralLength", "Char literal empty or too long", "Rust",
                "Reports char literals which are empty or too long.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCircularModules", "Circular modules", "Rust",
                "Reports cyclic module dependencies resulting in compiler recursion errors.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCommaSeparatedTraitBounds", "Comma-separated trait bounds", "Rust",
                "Reports trait bounds separated by commas instead of '+' in type parameter declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCompileErrorMacro", "`compile_error!` macro", "Rust",
                "Reports invocations of compile_error! macro emitting compiler errors.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCompilerFeatureUnavailable", "Compiler feature is unavailable", "Rust",
                "Reports usage of unstable compiler features on stable toolchains without feature gate.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsComplexPatternInFunction", "Complex pattern in function", "Rust",
                "Reports complex pattern matches in function parameter lists that reduce signature readability.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsConstGenericArgumentWithoutBraces", "Const generic argument expression without braces", "Rust",
                "Reports const generic expressions passed as type arguments that are missing enclosing braces.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsContinueBreakOutsideLoop", "`continue` / `break` used outside `loop` / `while`", "Rust",
                "Reports 'continue' or 'break' statements placed outside loops.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCopyTypeDropped", "`Copy` type dropped", "Rust",
                "Reports explicit calls to std::mem::drop on types implementing Copy.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsCrateMisplacedInPath", "`crate` misplaced in path", "Rust",
                "Reports 'crate' keyword placed at positions other than the leading segment of a path.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsCrateInPaths", "`crate` in paths", "Rust",
                "Reports invalid usage of the 'crate' identifier in paths.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDanglingElse", "Dangling `else`", "Rust",
                "Reports ambiguous 'else' branches in nested if expressions.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDbgUsage", "`#[dbg]` usage", "Rust",
                "Reports leftover dbg! macro invocations in production code.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDefaultParameterValues", "Default parameter values (unsupported)", "Rust",
                "Reports attempts to specify default parameter values which are not supported in Rust.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDefaultTypeParametersSyntax", "Default type parameters syntax", "Rust",
                "Reports invalid syntax in default type parameter declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDefaultQualifierNotAllowed", "`default` qualifier not allowed", "Rust",
                "Reports 'default' keyword on items outside specialization contexts.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDefaultsForConstParameters", "Defaults for const parameters not allowed", "Rust",
                "Reports default values assigned to const generic parameters where unsupported.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDeprecatedInclusiveRangeSyntax", "Deprecated `...` syntax", "Rust",
                "Reports deprecated '...' inclusive range pattern syntax and suggests using '..='.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDeriveNotAllowed", "`#[derive]` not allowed", "Rust",
                "Reports #[derive] attributes on items other than structs, enums, or unions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDerivedTraitNotImplemented", "Derived trait not implemented", "Rust",
                "Reports types missing required trait implementations demanded by #[derive].",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDetachedFile", "Detached file", "Rust",
                "Reports Rust source files that are not included in the crate module tree.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDoubleNegation", "Double negation", "Rust",
                "Reports double negative boolean or numeric expressions (e.g. '!!x' or '- -x').",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDuplicateDefinition", "Duplicate definition", "Rust",
                "Reports duplicate item declarations in the same module scope.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDuplicateHashKey", "Duplicate hash key", "Rust",
                "Reports duplicate key literals in hash map constructor expressions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDuplicateMacroPattern", "Duplicate macro pattern", "Rust",
                "Reports identical pattern arms in macro_rules! definitions.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsDuplicateTraitMethodParameterName", "Duplicate trait method parameter name", "Rust",
                "Reports duplicate parameter names in trait method signatures.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsDynamicEnvironmentCapture", "Dynamic environment capture in `fn`", "Rust",
                "Reports named 'fn' items attempting to capture variables from enclosing environment.",
                HighlightSeverity.ERROR, true, "Rust");
    }

    private void initRustCoreInspectionsPart2() {
        // Direct Rust inspections (Part 2: E - Inherent)
        addTool("RsEntryPointIsAsync", "Entry point is async", "Rust",
                "Reports 'main' functions declared as 'async' without an async runtime entry attribute like #[tokio::main].",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsEqualityAssertionCanBeSimplified", "Equality assertion can be simplified", "Rust",
                "Suggests replacing assert!(a == b) with assert_eq!(a, b) for better test failure output.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsExperimentalInspections", "Experimental inspections", "Rust",
                "Enables experimental Rust inspections for early testing.",
                HighlightSeverity.WARNING, false, "Rust");
        addTool("RsExplicitCallToDrop", "Explicit call to `drop`", "Rust",
                "Reports explicit calls to Drop::drop and suggests calling std::mem::drop instead.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsExternCrateSelfMissingAs", "`extern crate self` missing `as <name>`", "Rust",
                "Reports 'extern crate self;' missing 'as' renaming clause.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsExternalLinter", "External linter (available for Code | Inspect Code)", "Rust",
                "Runs Cargo Check or Clippy external linters for comprehensive analysis.",
                HighlightSeverity.WARNING, true, "Rust", true, false);
        addTool("RsFailedLineInTest", "Failed line in test", "Rust",
                "Highlights source lines where assertions failed during test execution.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsForeignTraitImplementation", "Foreign trait implementation", "Rust",
                "Reports orphan rule violations where both trait and type are external to current crate.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsFormatMacroErrors", "Format macro error", "Rust",
                "Reports errors in the use of macros that support formatting.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsForwardDeclaredIdentifierForParameter", "Forward-declared identifier for parameter with default value", "Rust",
                "Reports forward-declared identifiers referencing subsequent parameters with defaults.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsFunctionCannotBeVariadic", "Function cannot be variadic", "Rust",
                "Reports variadic function declarations outside 'extern \"C\"' blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsGenericArgumentsMustBeSpecifiedBeforeConstraints", "Generic arguments must be specified before the first constraint", "Rust",
                "Reports generic type or lifetime arguments placed after equality or associated type bounds.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIfConditionIsConstant", "`if` condition is constant", "Rust",
                "Reports 'if' conditions that evaluate to a constant true or false boolean value.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsImmutableReassigned", "Immutable reassigned", "Rust",
                "Reports reassignments to variables that were declared immutable.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsImmutableReferenceReassigned", "Immutable reassigned", "Rust",
                "Reports assignments through immutable references.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsImmutableVariableReassigned", "Immutable variable reassigned", "Rust",
                "Reports reassignments to immutable variables or binding patterns.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsImplTraitNotAllowed", "`impl Trait` not allowed", "Rust",
                "Reports 'impl Trait' return types in positions where they are forbidden by language syntax.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsImplMemberOrderDiffersFromTrait", "`impl` member order differs from trait", "Rust",
                "Reports trait implementation items whose order differs from the trait declaration.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsInclusiveRangeWithoutEndBound", "Inclusive range without an end bound", "Rust",
                "Reports inclusive ranges '..=' that omit an ending bound.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIncorrectConstSyntax", "Incorrect const syntax", "Rust",
                "Reports syntax errors in const or static declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIncorrectFunctionSyntax", "Incorrect function syntax", "Rust",
                "Reports syntax errors in function declarations or qualifiers.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIncorrectOrderOfParameters", "Incorrect order of lifetime/type/const parameters", "Rust",
                "Reports generic parameter lists where lifetimes do not precede types and consts.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIncorrectTypeAliasSyntax", "Incorrect type alias syntax", "Rust",
                "Reports syntax errors in type alias declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIncorrectVisibilityRestriction", "Incorrect visibility restriction", "Rust",
                "Reports invalid path expressions inside 'pub(...)' visibility qualifiers.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsIndexExpressionHasWrongType", "Index expression has a wrong type", "Rust",
                "Reports indexing expressions with indices not implementing the required Index trait.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInherentImplBlockNotAllowed", "Inherent `impl` block not allowed for item", "Rust",
                "Reports inherent impl blocks declared for trait types or foreign types.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInherentImplOutsideCrate", "Inherent `impl` defined outside type's containing crate", "Rust",
                "Reports inherent impl blocks for types defined in another crate.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInherentImplForDynAutoTrait", "Inherent `impl` for dyn auto trait", "Rust",
                "Reports inherent impl blocks on dynamic auto traits (e.g. dyn Send + Sync).",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInlineNotAllowed", "`#[inline]` not allowed", "Rust",
                "Reports #[inline] attributes on items other than functions or methods.",
                HighlightSeverity.ERROR, true, "Rust");
    }

    private void initRustCoreInspectionsPart3() {
        // Direct Rust inspections (Part 3: Invalid - Re-export)
        addTool("RsInvalidBreakOrContinue", "Invalid 'break' or 'continue'", "Rust",
                "Reports 'break' or 'continue' statements with invalid loop labels or values.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidTryUsage", "Invalid '?' usage", "Rust",
                "Reports '?' operator used in functions that do not return Result, Option, or Try types.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidCopyImplementation", "Invalid 'Copy' implementation", "Rust",
                "Reports Copy trait implementations for types that contain non-Copy fields.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidDropImplementation", "Invalid 'Drop' implementation", "Rust",
                "Reports Drop trait implementations on types where Drop is not allowed or specialization is attempted.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidSelfImport", "Invalid `self` import", "Rust",
                "Reports invalid 'self' use statements in module imports.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidAccessOfPrivateItem", "Invalid access of private item", "Rust",
                "Reports access to private fields, methods, or modules outside their visible scope.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidCrateName", "Invalid crate name", "Rust",
                "Reports crate names containing forbidden characters or matching reserved keywords.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidFieldsInStruct", "Invalid fields in struct", "Rust",
                "Reports invalid or duplicate field declarations in struct definitions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidFieldsInStructPattern", "Invalid fields in struct or tuple struct pattern", "Rust",
                "Reports non-existent field names used in struct or tuple struct pattern matches.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidLabelName", "Invalid label name", "Rust",
                "Reports loop label identifiers that do not begin with a single quote or are malformed.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidLifetimeName", "Invalid lifetime name", "Rust",
                "Reports lifetime identifiers that match reserved names or lack required apostrophe.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidLiteralSuffix", "Invalid literal suffix", "Rust",
                "Reports numeric or string literals with unrecognized suffix types.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidMacroCall", "Invalid macro call", "Rust",
                "Reports macro invocations with invalid syntax or delimiters.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidMacroDefinition", "Invalid macro definition", "Rust",
                "Reports syntax errors in macro_rules! patterns or templates.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsInvalidMacroVariableType", "Invalid macro variable type", "Rust",
                "Reports invalid designator types (e.g. $x:invalid) in macro parameter patterns.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsPlaceExpression", "Invalid place expression", "Rust",
                "Reports invalid place expressions. For example: `1 + 1 = 2`.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsItemCannotBeInsideItem", "Item cannot be inside another item", "Rust",
                "Reports item declarations placed inside invalid parent item blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsLifetimeBoundsInParentheses", "Lifetime bounds in parentheses", "Rust",
                "Reports lifetime bounds enclosed in unnecessary parentheses.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsLiteralOutOfRange", "Literal out of range", "Rust",
                "Reports numeric literals that exceed the maximum or minimum bounds of their inferred type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNumericLiteralOutOfRange", "Literal out of range", "Rust",
                "Reports float or integer literals whose magnitude cannot be represented in the specified type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMainFunctionNotFound", "Main function not found", "Rust",
                "Reports binary crates lacking a 'main' entry point function.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMainFunctionWithGenericParameters", "`main` function with generic parameters", "Rust",
                "Reports 'main' entry point functions declared with generic type or lifetime parameters.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsManualImplementationOfFnTraits", "Manual implementation of `Fn`, `FnMut`, or `FnOnce`", "Rust",
                "Reports manual implementations of Fn, FnMut, or FnOnce traits without feature gate.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMatchCanBeReplacedWithMethodCall", "Match expression can be replaced with a method call", "Rust",
                "Suggests replacing verbose match expressions with Option or Result methods like unwrap_or, map, or and_then.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsMisorderedGenericArguments", "Misordered generic arguments", "Rust",
                "Reports generic argument lists where lifetimes, types, and const arguments are out of order.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMisplacedEllipsisArgument", "Misplaced `...` argument", "Rust",
                "Reports '...' argument positioned incorrectly in function parameter declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMissingDynInTraitObjects", "Missing `dyn` in trait objects", "Rust",
                "Reports bare trait objects lacking the 'dyn' keyword.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMissingElse", "Missing `else`", "Rust",
                "Reports 'if' expressions used in value-producing contexts lacking an 'else' branch.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsMissingFeatures", "Missing features", "Rust",
                "Reports items requiring Cargo feature flags that are not enabled in Cargo.toml.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsMissingParenthesesForChainedComparison", "Missing parentheses for chained comparison", "Rust",
                "Reports chained comparison expressions (e.g. 'a < b < c') which are not supported without parentheses.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMissingStructFieldType", "Missing struct field type", "Rust",
                "Reports struct field declarations that omit a type annotation.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMissingTypeForConstant", "Missing type for constant", "Rust",
                "Reports const or static declarations missing an explicit type annotation.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsModuleDeclaredOutsideModRs", "Module declared outside mod.rs", "Rust",
                "Reports submodule declarations outside the containing directory or mod.rs file.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMultipleLifetimeBounds", "Multiple lifetime bounds for trait object", "Rust",
                "Reports trait objects with more than one lifetime bound specified.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsMultipleRelaxedDefaultBounds", "Multiple relaxed default bounds for type parameter", "Rust",
                "Reports type parameters declaring more than one '?Sized' relaxed bound.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNestedImplTraitNotAllowed", "Nested `impl Trait` not allowed", "Rust",
                "Reports nested 'impl Trait' types inside other 'impl Trait' bounds.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNestedLifetimeQuantification", "Nested lifetime quantification", "Rust",
                "Reports higher-ranked trait bounds (for<...>) with nested lifetime quantifications.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNoBoxingForAsyncRecursion", "No boxing for async recursion", "Rust",
                "Reports recursive async functions that do not box their return future, causing infinite size.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNoFileFoundForModule", "No file found for module", "Rust",
                "Reports 'mod foo;' declarations where the corresponding foo.rs or foo/mod.rs file does not exist.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNonExhaustiveMatch", "Non-exhaustive match", "Rust",
                "Reports match expressions that do not cover all possible patterns of the target type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNonExistentFieldAccess", "Non-existent field access", "Rust",
                "Reports field access expressions accessing fields that do not exist on the type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNonInlineModuleMissingPath", "Non-inline module declaration missing path attribute", "Rust",
                "Reports non-inline module declarations inside non-standard paths lacking a #[path] attribute.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNonShorthandFieldInitialization", "Non-shorthand field initialization", "Rust",
                "Reports struct initializers where field name and variable name match but are written as 'field: field'.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsNonStringAbiLiteral", "Non-string ABI literal", "Rust",
                "Reports 'extern' declarations using ABI designators that are not string literals.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsNonStructuralMatchTypeUsedForConstGeneric", "Non-structural-match type used for const generic param", "Rust",
                "Reports types not deriving PartialEq and Eq used as const generic parameters.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsObjectTypeWithNoTraits", "Object type with no traits", "Rust",
                "Reports 'dyn' keyword followed by empty trait bounds.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnderscoreOnRhsOfAssignment", "`_` on the right-hand side of assignment", "Rust",
                "Reports '_' wildcard pattern used on the right-hand side of an assignment expression.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsFunctionParametersAreSized", "Parameter type with unknown size", "Rust",
                "Reports function parameters whose types are unknown at compile time.\n\nInspection ID: RsFunctionParametersAreSized",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsPrintlnMacroCanBeSimplified", "`println!` macro can be simplified", "Rust",
                "Reports println!(\"\") calls that can be simplified to println!().",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsProcMacroDefinedOutsideProcMacroCrate", "Proc macro defined outside `proc-macro` crate", "Rust",
                "Reports procedural macro definitions inside crates that are not configured with crate-type = [\"proc-macro\"].",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsPublicItemInProcMacroCrate", "Public item in proc-macro crate", "Rust",
                "Reports public items in proc-macro crates that are not procedural macro functions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsReexportOfPrivateItem", "Re-export of a private item", "Rust",
                "Reports 'pub use' statements re-exporting items that are private to current module.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsRedundantColonColon", "Redundant `::`", "Rust",
                "Reports redundant leading '::' path prefixes.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsRedundantElse", "Redundant `else`", "Rust",
                "Reports redundant 'else' blocks after a branch that breaks or returns.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsReferenceDropped", "Reference dropped", "Rust",
                "Reports calls to std::mem::drop on reference types which has no effect.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsRepeatedDiscriminantValue", "Repeated discriminant value", "Rust",
                "Reports enum variants with repeated explicit discriminant values.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsRepeatedIdentifierInPattern", "Repeated identifier in pattern", "Rust",
                "Reports duplicate identifier bindings in match patterns.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsReprIntTypeMissingForEnum", "`#[repr(inttype)]` missing for enum", "Rust",
                "Reports fieldless enums missing repr(u8/i32/...) attribute when required.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsReservedKeywordUsedAsIdentifier", "Reserved keyword used as an identifier", "Rust",
                "Reports usage of reserved Rust keywords as identifiers.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsReservedLifetimeName", "Reserved lifetime name", "Rust",
                "Reports usage of reserved lifetime names like 'static.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsReturnCanBeLifted", "`return` can be lifted", "Rust",
                "Reports 'return' statements that can be lifted out of match or if arms.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsReturnMustHaveValue", "`return` must have a value", "Rust",
                "Reports return statements with no value in functions that declare a non-unit return type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsRust2018EditionViolation", "Rust 2018 edition violation", "Rust",
                "Reports idiom and syntax violations against the Rust 2018 edition.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsRust2024EditionViolation", "Rust 2024 edition violation", "Rust",
                "Reports idiom and syntax violations against the Rust 2024 edition.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSelfSuperMisplacedInPath", "`self` / `super` misplaced in path", "Rust",
                "Reports 'self' or 'super' path components positioned anywhere other than the start of a path.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSelfFunctionParameterNotAllowed", "`self` function parameter not allowed", "Rust",
                "Reports 'self' parameter used in functions where method receiver is not permitted.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSelfInStaticMethod", "`self` in static method", "Rust",
                "Reports 'self' parameter used in static methods or functions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSelfUnavailableInContext", "`self` unavailable in context", "Rust",
                "Reports 'self' referenced in contexts where no self instance exists.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSizedUnsizedTraitImplementedExplicitly", "`Sized` / `Unsized` trait implemented explicitly", "Rust",
                "Reports explicit implementations of auto traits Sized or ?Sized.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsStructInheritance", "Struct inheritance (unsupported)", "Rust",
                "Reports unsupported struct inheritance declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSupertraitIsNotImplemented", "Supertrait is not implemented", "Rust",
                "Reports trait implementations where required supertraits are not implemented.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsSuspiciousAssignment", "Suspicious assignment", "Rust",
                "Reports suspicious assignments such as self-assignment or assigning to a parameter without effect.",
                HighlightSeverity.WARNING, true, "Rust");
    }

    private void initRustCoreInspectionsPart4() {
        addTool("RsThreadRngGenCanBeReplacedWithRandom", "`thread_rng().gen()` can be replaced with `random()`", "Rust",
                "Suggests replacing thread_rng().gen() with the simpler rand::random() function.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsToStringShouldNotBeImplementedDirectly", "`ToString` should not be implemented directly", "Rust",
                "Suggests implementing Display instead of implementing ToString directly.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsTraitExpected", "Trait expected", "Rust",
                "Reports type names used where a trait is expected in trait bounds or impl blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTraitImplementationIssue", "Trait implementation issue", "Rust",
                "Reports missing or mismatched items in trait implementation blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTraitSafetyMismatch", "Trait safety mismatch", "Rust",
                "Reports unsafe trait implemented without 'unsafe impl' or safe trait implemented with 'unsafe'.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTryMacroUsage", "`try!` macro usage", "Rust",
                "Suggests replacing try! macro invocations with the '?' operator.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsTupleLikeUnion", "Tuple-like union", "Rust",
                "Reports union declarations using tuple struct syntax which is not supported.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTypeChecker", "Type checker", "Rust",
                "Reports type errors, type mismatches, and unsized type violations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTypeCheckerExperimental", "Type checker (experimental)", "Rust",
                "Experimental type checking analysis for complex trait projections and higher-ranked bounds.",
                HighlightSeverity.ERROR, false, "Rust");
        addTool("RsTraitObligations", "Type does not implement trait", "Rust",
                "Reports the use of types that do not implement the necessary traits.\n\nInspection ID: RsTraitObligations",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsTraitObligationsExperimental", "Type does not implement trait (experimental)", "Rust",
                "Experimental trait obligation solver checking unsatisfied trait bounds.",
                HighlightSeverity.ERROR, false, "Rust");
        addTool("RsTypeMismatchedTraitAssociatedType", "Type mismatched the trait's associated type", "Rust",
                "Reports associated type values that do not match the expected trait definition.",
                HighlightSeverity.ERROR, false, "Rust");
        addTool("RsTypePlaceholderUsedInItemSignature", "Type placeholder used in item signature", "Rust",
                "Reports '_' type wildcards used in function or item signature definitions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnclosedTextLiteral", "Unclosed text literal", "Rust",
                "Reports unclosed character or string literal tokens.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUndeclaredLabel", "Undeclared label", "Rust",
                "Reports loop labels referenced in break/continue that have not been declared.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUndeclaredLifetimeName", "Undeclared lifetime name", "Rust",
                "Reports lifetime parameters referenced without being declared in generic parameter list.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnionExpressionFieldsCount", "Union expression fields count", "Rust",
                "Reports union expressions initializing more or fewer than exactly one field.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnlabeledContinueBreakInWhileLoop", "Unlabeled `continue` / `break` in `while` loop condition", "Rust",
                "Reports unlabeled continue or break statements positioned inside while loop condition expressions.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnlabeledContinueBreakInLabeledBlock", "Unlabeled `continue` / `break` in labeled block", "Rust",
                "Reports unlabeled continue or break statements inside labeled blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnnecessaryVisibilityQualifier", "Unnecessary visibility qualifier", "Rust",
                "Reports 'pub' visibility qualifiers on items in private modules or trait declarations.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnreachableLabel", "Unreachable label", "Rust",
                "Reports loop labels that cannot be reached by control flow.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnresolvedMethod", "Unresolved method", "Rust",
                "Reports method invocations that cannot be resolved on the receiver type.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnresolvedMethodExperimental", "Unresolved method (experimental)", "Rust",
                "Experimental method resolution considering ambiguous trait candidates.",
                HighlightSeverity.ERROR, false, "Rust");
        addTool("RsUnresolvedPath", "Unresolved path", "Rust",
                "Reports module, type, or item paths that cannot be resolved in current scope.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnresolvedPathExperimental", "Unresolved path (experimental)", "Rust",
                "Experimental path resolution with auto-import candidate suggestions.",
                HighlightSeverity.ERROR, false, "Rust");
        addTool("RsUnsafeCStringPointer", "Unsafe CString pointer", "Rust",
                "Reports CString::as_ptr calls where the CString is immediately dropped, creating dangling pointers.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsUnsafeInherentImplementation", "Unsafe inherent implementation", "Rust",
                "Reports 'unsafe impl' on inherent (non-trait) implementation blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnsafeItemInSafeContext", "Unsafe item in safe context", "Rust",
                "Reports calls to unsafe functions or dereferencing raw pointers outside 'unsafe' blocks.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnsafeModule", "Unsafe module", "Rust",
                "Reports 'unsafe mod' declarations which are not permitted in Rust syntax.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnstableItem", "Unstable item", "Rust",
                "Reports usage of standard library items that require nightly compiler feature gates.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUnsupportedLetExpression", "Unsupported `let` expression", "Rust",
                "Reports 'let' expressions used outside 'if let' or 'while let' guard contexts.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUseOfDynImplTraitInTypeParameterBounds", "Use of `dyn` / `impl Trait` in type parameter bounds", "Rust",
                "Reports 'dyn Trait' or 'impl Trait' used as bounds on generic type parameters.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsUseOfIncrementDecrementOperator", "Use of increment/decrement operator (unsupported)", "Rust",
                "Reports ++ or -- operators which do not exist in Rust syntax (use += 1 or -= 1).",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsVariadicParametersForNonCAbiFunction", "Variadic parameters for non-C ABI function", "Rust",
                "Reports '...' variadic parameter lists on functions that do not use 'extern \"C\"'.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsWithCallCanBeReplacedWithThreadLocalStableMethod", "`with` call can be replaced with thread local stable method", "Rust",
                "Suggests replacing thread-local with() closures with stable LocalKey accessor methods.",
                HighlightSeverity.WARNING, true, "Rust");
        addTool("RsWrongAssociatedTypeArguments", "Wrong associated type arguments", "Rust",
                "Reports associated type bindings with missing or unexpected generic arguments.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsWrongNumberOfArguments", "Wrong number of arguments", "Rust",
                "Reports function or method calls invoked with fewer or more arguments than expected.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsWrongNumberOfGenericArguments", "Wrong number of generic arguments", "Rust",
                "Reports generic items supplied with too few or too many generic arguments.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsWrongNumberOfLifetimeParameters", "Wrong number of lifetime parameters", "Rust",
                "Reports items invoked with an incorrect number of lifetime parameters.",
                HighlightSeverity.ERROR, true, "Rust");
        addTool("RsWrongNumberOfTypeOrConstParameters", "Wrong number of type or const parameters", "Rust",
                "Reports items invoked with an incorrect number of type or const parameters.",
                HighlightSeverity.ERROR, true, "Rust");
    }

    private void initSassInspections() {
        // Sass/SCSS ([✓] all 4 enabled matching screenshot)
        addTool("Sass.MissingImport", "Missing import", "Sass/SCSS",
                "Reports missing or unresolvable Sass/SCSS @import and @use rules.",
                HighlightSeverity.WARNING, true, "SCSS");
        addTool("Sass.UnresolvedMixin", "Unresolved mixin", "Sass/SCSS",
                "Reports references to Sass mixins (@include) that cannot be resolved.",
                HighlightSeverity.WARNING, true, "SCSS");
        addTool("Sass.UnresolvedPlaceholderSelector", "Unresolved placeholder selector", "Sass/SCSS",
                "Reports Sass placeholder selectors (%placeholder) used in @extend that cannot be resolved.",
                HighlightSeverity.WARNING, true, "SCSS");
        addTool("Sass.UnresolvedVariable", "Unresolved variable", "Sass/SCSS",
                "Reports Sass/SCSS variables that are not defined in the current or imported scopes.",
                HighlightSeverity.WARNING, true, "SCSS");
    }

    private void initSbtInspections() {
        // sbt ([✓] both enabled matching screenshot)
        addTool("Sbt.NewerVersionAvailable", "Newer stable version for library dependency is available", "sbt",
                "Reports library dependencies in sbt build files for which newer stable versions are available.",
                HighlightSeverity.WARNING, true, "sbt");
        addTool("Sbt.ReplaceProjectWithProjectIn", "Replace Project() with project.in()", "sbt",
                "Suggests replacing legacy Project(...) definitions with project.in(...) syntax in sbt build files.",
                HighlightSeverity.WARNING, true, "sbt");
    }

    private void initSecurityInspections() {
        // Security ([✓] all 5 enabled matching screenshot)
        addTool("Security.LinkWithUnencryptedProtocol", "Link with unencrypted protocol", "Security",
                "Reports HTTP links that should use secure HTTPS protocol to prevent tampering and eavesdropping.",
                HighlightSeverity.WARNING, true, "Security");
        addTool("Security.MaliciousDependency", "Malicious dependency", "Security",
                "Reports dependencies flagged as known malicious packages in public security advisories.",
                HighlightSeverity.WARNING, true, "Security");
        addTool("Security.VulnerableApiUsage", "Vulnerable API usage", "Security",
                "Reports invocations of security-sensitive APIs known to contain vulnerabilities or unsafe defaults.",
                HighlightSeverity.WARNING, true, "Security");
        addTool("Security.VulnerableDeclaredDependency", "Vulnerable declared dependency", "Security",
                "Reports declared package dependencies containing known CVE security vulnerabilities.",
                HighlightSeverity.WARNING, true, "Security");
        addTool(InspectionTool.builder("Security.VulnerableImportedDependency")
                .displayName("Vulnerable imported dependency")
                .groupPath("Security")
                .description("Reports transitive imported dependencies containing security vulnerabilities. Available during batch inspection.")
                .defaultSeverity(HighlightSeverity.WARNING)
                .defaultEnabled(true)
                .language("Security")
                .batchModeOnly(true)
                .build());
    }

    private void initShellScriptInspections() {
        // Shell script ([✓] enabled matching screenshot)
        addTool("Shell.ShellCheck", "ShellCheck", "Shell script",
                "Runs ShellCheck static analysis to detect errors, bugs, and stylistic issues in shell scripts.",
                HighlightSeverity.ERROR, true, "Shell");
    }

    private void initSpringInspections() {
        // Spring ([✓] all 10 subcategories enabled matching screenshots)
        initSpringAopInspections();
        initSpringBootInspections();
        initSpringCloudInspections();
        initSpringCloudStreamInspections();
        initSpringCoreCodeInspectionsPart1();
        initSpringCoreCodeInspectionsPart2();
        initSpringCoreXmlInspectionsPart1();
        initSpringCoreXmlInspectionsPart2();
        initSpringCoreDirectInspections();
        initSpringDataSubInspections();
        initSpringIntegrationInspections();
        initSpringMvcInspections();
        initSpringSecurityInspections();
        initSpringWebSocketInspections();
    }

    private void initSpringAopInspections() {
        // Spring AOP ([✓] 5 tools)
        addTool("Spring.Aop.IncorrectJdkProxiedBeanType", "Incorrect JDK-proxied bean type", "Spring/Spring AOP",
                "Reports bean types that are incorrectly configured for JDK dynamic proxies.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Aop.IncorrectAdviceAdvisor", "Incorrect Spring AOP advice or advisor element", "Spring/Spring AOP",
                "Validates that AOP advice elements define required pointcut or pointcut-ref attributes.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Aop.IncorrectAspectPointcut", "Incorrect Spring AOP aspect or pointcut element", "Spring/Spring AOP",
                "Reports structural and syntactic issues in Spring AOP aspect and pointcut definitions.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Aop.IncorrectPointcutExpression", "Incorrect Spring AOP pointcut expression", "Spring/Spring AOP",
                "Validates the syntax and validity of Spring AOP pointcut expression strings.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Aop.MissingAspectjAutoproxy", "Missing aspectj-autoproxy", "Spring/Spring AOP",
                "Ensures that aspectj-autoproxy is configured when using AspectJ annotations.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringBootInspections() {
        // Spring Boot ([✓] 6 tools)
        addTool("Spring.Boot.InvalidConfigurationProperties", "Invalid @ConfigurationProperties", "Spring/Spring Boot",
                "Reports invalid Spring Boot @ConfigurationProperties binding and prefix definitions.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Boot.InvalidMetadataJson", "Invalid additional-spring-configuration-metadata.json", "Spring/Spring Boot",
                "Validates additional-spring-configuration-metadata.json format, property types, and structure.",
                HighlightSeverity.ERROR, true, "JSON");
        addTool("Spring.Boot.InvalidPropertiesConfiguration", "Invalid properties configuration", "Spring/Spring Boot",
                "Reports malformed keys and value syntax in Spring Boot application.properties.",
                HighlightSeverity.ERROR, true, "Properties");
        addTool("Spring.Boot.InvalidSetup", "Invalid Spring Boot application setup", "Spring/Spring Boot",
                "Reports invalid @SpringBootApplication setup, conflicting annotations, and bootstrap issues.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Boot.InvalidYamlConfiguration", "Invalid YAML configuration", "Spring/Spring Boot",
                "Validates application.yml and application.yaml configuration files against configuration metadata.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Spring.Boot.SuspiciousHooksOperatorDebug", "Suspicious Hooks.onOperatorDebug() usage", "Spring/Spring Boot",
                "Warns about potentially performance-degrading Project Reactor debugging hooks in production.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringCloudInspections() {
        // Spring Cloud ([✓] 1 tool)
        addTool("Spring.Cloud.BootstrapConfiguration", "Bootstrap configuration included in application context", "Spring/Spring Cloud",
                "Reports bootstrap configuration classes included directly in the application context.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringCloudStreamInspections() {
        // Spring Cloud Stream ([✓] 2 tools)
        addTool("Spring.CloudStream.StreamHandlerMethodError", "Stream handler method error", "Spring/Spring Cloud Stream",
                "Reports inconsistency errors and invalid signatures in Spring Cloud Stream handler methods.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.CloudStream.UnresolvedMessageChannel", "Unresolved message channel", "Spring/Spring Cloud Stream",
                "Reports unresolved channel attributes in annotations like @StreamListener, @SendTo, @Output, and @Input.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringCoreCodeInspectionsPart1() {
        // Spring Core > Code Part 1 ([✓] 14 tools)
        addTool("Spring.Core.Code.CacheAnnotationsOnInterfaces", "Cache* annotations defined on interfaces/interface methods", "Spring/Spring Core/Code",
                "Reports @Cacheable or @CacheEvict placed on interface declarations instead of concrete bean implementations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.CacheableSelfInvocation", "@Cacheable self-invocation method calls", "Spring/Spring Core/Code",
                "Reports internal self-invocation calls to @Cacheable methods that bypass Spring AOP caching proxies.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.ConfigurationProxyMethods", "@Configuration proxyMethods usage warnings", "Spring/Spring Core/Code",
                "Reports inter-bean calls between @Bean methods on @Configuration classes where proxyBeanMethods is false.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectAsyncMethodSignature", "Incorrect @Async method signature", "Spring/Spring Core/Code",
                "Reports @Async methods whose return type is not void, Future, CompletableFuture, or ListenableFuture.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectScheduledMethodSignature", "Incorrect @Scheduled method signature", "Spring/Spring Core/Code",
                "Reports @Scheduled methods that have parameters or declare a non-void return type.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectAutowiring", "Incorrect autowiring in Spring bean components", "Spring/Spring Core/Code",
                "Reports unresolved, ambiguous, or un-injectable dependency injection points in Spring bean components.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectRequiredCacheNames", "Incorrect required cache names definition", "Spring/Spring Core/Code",
                "Reports cache operations where cacheNames or value attribute is missing or empty.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectSpringComponentAutowiring", "Incorrect Spring component autowiring or injection points", "Spring/Spring Core/Code",
                "Reports injection points that do not match available Spring candidate beans.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectCachePutAndCacheable", "Incorrect usage of @CachePut and @Cacheable on class", "Spring/Spring Core/Code",
                "Reports @CachePut and @Cacheable applied concurrently on class or method levels in conflicting manners.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectlyConfiguredCaching", "Incorrectly configured 'caching' annotation", "Spring/Spring Core/Code",
                "Reports invalid attributes or configuration in @Caching composite cache annotations.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectEventListenerMethods", "Incorrectly configured @EventListener methods", "Spring/Spring Core/Code",
                "Reports @EventListener methods with invalid parameters or mismatched payload events.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.IncorrectProfileExpression", "Incorrectly configured @Profile expression", "Spring/Spring Core/Code",
                "Validates @Profile expression syntax, operators, and profile name tokens.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectDependsOnBean", "Incorrectly referenced bean in @DependsOn annotation", "Spring/Spring Core/Code",
                "Reports bean names specified in @DependsOn that cannot be resolved in the bean factory.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectLookupBean", "Incorrectly referenced bean in @Lookup annotation", "Spring/Spring Core/Code",
                "Reports bean names specified in @Lookup that do not resolve to valid beans in context.",
                HighlightSeverity.ERROR, true, "Java");
    }

    private void initSpringCoreCodeInspectionsPart2() {
        // Spring Core > Code Part 2 ([✓] 14 tools)
        addTool("Spring.Core.Code.IncorrectMockitoBean", "Incorrectly referenced bean in @MockitoBean, @MockitoSpyBean", "Spring/Spring Core/Code",
                "Reports bean names in @MockitoBean or @MockitoSpyBean that do not match defined Spring beans.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.IncorrectScheduledBean", "Incorrectly referenced bean in @Scheduled annotation", "Spring/Spring Core/Code",
                "Reports scheduler bean references in @Scheduled that cannot be found in the application context.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.InvalidPlatformTransactionManager", "Invalid 'PlatformTransactionManager' declaration in @Transactional", "Spring/Spring Core/Code",
                "Reports transactionManager references in @Transactional that do not resolve to PlatformTransactionManager beans.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.InvalidContextConfiguration", "Invalid @ContextConfiguration", "Spring/Spring Core/Code",
                "Reports missing or contradictory class, location, or loader attributes in @ContextConfiguration.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.InvalidDirtiesContextMode", "Invalid @DirtiesContext 'mode' configuration", "Spring/Spring Core/Code",
                "Reports invalid HierarchyMode or MethodMode configurations in @DirtiesContext annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.InvalidSqlAndSqlGroup", "Invalid @Sql and @SqlGroup configurations", "Spring/Spring Core/Code",
                "Reports missing SQL script paths or conflicting execution phase settings in @Sql configurations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.InvalidComponentScanPackage", "Invalid package in @ComponentScan or its meta-annotations", "Spring/Spring Core/Code",
                "Reports basePackages in @ComponentScan that do not exist or contain no Spring components.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.InvalidTransactionalLifecycle", "Invalid transactional lifecycle method declaration", "Spring/Spring Core/Code",
                "Reports @Transactional annotations placed on private, static, or final methods where proxies cannot intercept.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.NonRecommendedFieldInjections", "Non-recommended field injections", "Spring/Spring Core/Code",
                "Reports @Autowired on fields instead of constructor-based dependency injection.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.RequiredPropertyNotInjected", "@Required Spring bean property is not injected", "Spring/Spring Core/Code",
                "Reports required bean properties annotated with @Required that are not configured.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.TransactionalSelfInvocation", "@Transactional self-invocation method calls", "Spring/Spring Core/Code",
                "Reports self-invocation calls to @Transactional methods that bypass transactional AOP proxies.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.UnknownInitDestroyMethod", "Unknown init/destroy method in the @Bean annotation", "Spring/Spring Core/Code",
                "Reports initMethod or destroyMethod names specified in @Bean that do not exist on the target bean class.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Core.Code.UnresolvedImportResource", "Unresolved file references in @ImportResource locations", "Spring/Spring Core/Code",
                "Reports file or classpath locations in @ImportResource that cannot be resolved.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.Code.UnresolvedPropertySource", "Unresolved file references in @PropertySource and @TestPropertySource", "Spring/Spring Core/Code",
                "Reports property resource locations in @PropertySource that cannot be found.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringCoreXmlInspectionsPart1() {
        // Spring Core > XML Part 1 ([✓] 12 tools)
        addTool("Spring.Core.Xml.ConflictingBeanAttribute", "Conflicting Spring bean attribute", "Spring/Spring Core/XML",
                "Reports conflicting bean attributes such as 'autowire' with explicit 'constructor-arg' in XML context.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.DuplicatedBeanNames", "Duplicated bean names in XML-based application context", "Spring/Spring Core/XML",
                "Reports duplicate bean id or name definitions within the same XML application context scope.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.InactiveProfileHighlighting", "Inactive profile highlighting", "Spring/Spring Core/XML",
                "Highlights XML elements assigned to inactive Spring profiles.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.InconsistentInjectionValue", "Inconsistent injection value in XML application context", "Spring/Spring Core/XML",
                "Reports property value types in XML bean definitions that cannot be converted to target property types.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectConstructorInjection", "Incorrect constructor injection in XML Spring bean", "Spring/Spring Core/XML",
                "Reports constructor-arg tags with mismatched indexes, names, or unresolvable types.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectInjectedBeanType", "Incorrect injected bean type", "Spring/Spring Core/XML",
                "Reports ref bean attributes that point to beans incompatible with target injection point types.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectNonPublicMethodFactory", "Incorrect non-public method referenced in a \"factory-method\" attribute", "Spring/Spring Core/XML",
                "Reports factory-method attributes referencing non-public methods on factory classes.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.IncorrectReferenceToAbstractBean", "Incorrect reference to abstract bean", "Spring/Spring Core/XML",
                "Reports bean references pointing to beans defined with abstract='true' which cannot be instantiated.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectResourceType", "Incorrect resource type", "Spring/Spring Core/XML",
                "Reports imported XML configuration resources whose type does not match expected schema.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectXmlContext", "Incorrect Spring Core XML-based application context", "Spring/Spring Core/XML",
                "Reports structural XML validation and schema errors in Spring Core bean definitions.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectXmlBeanAutowiring", "Incorrect XML Spring bean autowiring", "Spring/Spring Core/XML",
                "Reports XML beans configured with autowire='byType' or 'byName' that cannot be uniquely resolved.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectUtilSchemaBeans", "Incorrectly configured 'util' schema beans defined in XML application context", "Spring/Spring Core/XML",
                "Reports invalid attributes and syntax for util:list, util:map, util:set, and util:constant beans.",
                HighlightSeverity.ERROR, true, "XML");
    }

    private void initSpringCoreXmlInspectionsPart2() {
        // Spring Core > XML Part 2 ([✓] 12 tools)
        addTool("Spring.Core.Xml.IncorrectXmlBeanLookupMethod", "Incorrectly configured XML bean lookup-method", "Spring/Spring Core/XML",
                "Reports lookup-method elements referencing non-existent or invalid methods on bean classes.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.IncorrectDefinedMethodFactory", "Incorrectly defined method referenced in a \"factory-method\" attribute", "Spring/Spring Core/XML",
                "Reports factory-method attributes referencing methods that do not exist on the bean or factory-bean.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.InjectionValueViolatesSchema", "Injection value in XML application context violates schema", "Spring/Spring Core/XML",
                "Reports values injected into XML beans that violate property constraints or validation schemas.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.InvalidFilterInComponentScan", "Invalid filter definition in XML-based component scan", "Spring/Spring Core/XML",
                "Reports invalid include-filter or exclude-filter configurations in context:component-scan elements.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.InvalidNonAbstractBeanInstantiation", "Invalid non-abstract bean instantiation", "Spring/Spring Core/XML",
                "Reports XML bean definitions lacking both 'class' and 'factory-bean' attributes unless marked abstract.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.MissingRequiredPropertyInXml", "Missing @Required property injections in the spring XML application context", "Spring/Spring Core/XML",
                "Reports missing property elements in XML bean definitions for properties annotated with @Required.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.BeanNameViolatesConventions", "Spring bean name violates conventions", "Spring/Spring Core/XML",
                "Reports bean names in XML definitions that violate standard camelCase naming conventions.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.UnassignableInjectionPointType", "Unassignable injection point type in XML application context", "Spring/Spring Core/XML",
                "Reports XML bean property injection points that cannot be assigned from target bean types.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.UnknownBeanScope", "Unknown <bean> scope", "Spring/Spring Core/XML",
                "Reports unrecognized scope attribute values on <bean> elements in XML context.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Core.Xml.UnnecessaryAutowiredDependency", "Unnecessary autowired dependency in XML application context", "Spring/Spring Core/XML",
                "Reports unnecessary autowired dependencies in XML context where explicit wiring is already defined.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.UnparsedCustomSpringBeans", "Unparsed custom Spring beans", "Spring/Spring Core/XML",
                "Reports custom namespace XML tags that cannot be parsed by registered namespace handlers.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Spring.Core.Xml.UnresolvedPlaceholdersInXml", "Unresolved placeholders configured in the Spring XML application context", "Spring/Spring Core/XML",
                "Reports ${...} property placeholders in XML contexts that cannot be resolved from PropertySources.",
                HighlightSeverity.WARNING, true, "XML");
    }

    private void initSpringCoreDirectInspections() {
        // Spring Core direct inspections ([✓] 3 tools)
        addTool("Spring.Core.IncorrectInjectingSpelStaticField", "Incorrect injecting of SpEL in a static field", "Spring/Spring Core",
                "Reports @Value annotations containing SpEL expressions placed on static fields.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.IncorrectSpelSyntax", "Incorrect Spring Expression Language (SpEL) syntax", "Spring/Spring Core",
                "Validates Spring Expression Language (SpEL) syntax and reports parse errors in expressions.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Core.UnresolvedSpringHandlers", "Unresolved file or class reference in 'spring.handlers'", "Spring/Spring Core",
                "Reports unresolved handler class references in META-INF/spring.handlers descriptor files.",
                HighlightSeverity.ERROR, true, "Java");
    }

    private void initSpringDataSubInspections() {
        // Spring Data subcategory under Spring ([✓] 6 tools)
        addTool("Spring.Data.PageMustHavePageable", "Query-returning Page must have a Pageable parameter", "Spring/Spring Data",
                "Reports repository query methods returning Page<T> that lack a required Pageable parameter.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Data.MongoDbJsonUnresolvedFields", "Spring Data MongoDB JSON unresolved fields", "Spring/Spring Data",
                "Reports unresolved document fields in Spring Data MongoDB @Query JSON filter strings.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Data.RepositoryMethodErrors", "Spring Data repository method errors", "Spring/Spring Data",
                "Reports syntax and semantic errors in derived Spring Data repository query method names.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Data.RepositoryMethodParametersErrors", "Spring Data repository method parameters errors", "Spring/Spring Data",
                "Reports parameter count and type mismatches in Spring Data repository query methods.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Data.RepositoryMethodReturnTypeErrors", "Spring Data repository method return type errors", "Spring/Spring Data",
                "Reports incompatible return types for repository query methods based on query structure.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Data.UpdateDeleteAnnotatedModifying", "Update/Delete queries must be annotated with @Modifying", "Spring/Spring Data",
                "Reports UPDATE or DELETE queries in Spring Data repositories lacking the required @Modifying annotation.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringIntegrationInspections() {
        // Spring Integration ([✓] 4 tools)
        addTool("Spring.Integration.IncorrectChannelAttribute", "Incorrect 'channel' attribute in an endpoint method annotation", "Spring/Spring Integration",
                "Checks annotations such as @Gateway, @ServiceActivator, and @Filter for invalid message channel references.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Integration.IncorrectEndpointMethod", "Incorrect Spring Integration endpoint method", "Spring/Spring Integration",
                "Validates endpoint method declarations in Spring Integration messaging beans.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Integration.IncorrectXmlContext", "Incorrect Spring Integration XML-based application context", "Spring/Spring Integration",
                "Reports configuration issues within Spring Integration XML-based application contexts.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Integration.Deprecated21", "Spring Integration 2.1 deprecations", "Spring/Spring Integration",
                "Flags XML elements and attributes deprecated in Spring Integration 2.1+.",
                HighlightSeverity.WARNING, true, "XML");
    }

    private void initSpringMvcInspections() {
        // Spring MVC ([✓] 3 tools)
        addTool("Spring.Mvc.PathVariableMismatch", "Mismatch in @PathVariable declarations and usages", "Spring/Spring MVC",
                "Reports mismatches between URL template path variables and @PathVariable method parameter names.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Mvc.NonVoidInitBinder", "Non-void @InitBinder method", "Spring/Spring MVC",
                "Reports @InitBinder controller methods that return non-void values which are ignored by Spring MVC.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.Mvc.UnresolvedView", "Unresolved view reference", "Spring/Spring MVC",
                "Reports controller methods returning view names that cannot be resolved by configured view resolvers.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringSecurityInspections() {
        // Spring Security ([✓] 4 tools)
        addTool("Spring.Security.DebugModeActivated", "Debug mode is activated in the Spring Security configuration", "Spring/Spring Security",
                "Reports activated security debug mode which can expose sensitive credentials in logs.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Security.IncorrectBeansReferenced", "Incorrect configuration of Spring beans referenced in @PreAuthorize, @PostAuthorize, @PreFilter, and @PostFilter annotations", "Spring/Spring Security",
                "Reports invalid or unresolved Spring bean references in SpEL expressions within Spring Security annotations.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Spring.Security.IncorrectXmlContext", "Incorrect Spring Security XML-based application context", "Spring/Spring Security",
                "Reports configuration issues within Spring Security XML-based application contexts.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Spring.Security.SelfInvocation", "@PreFilter/@PreAuthorize/@PostFilter self-invocation", "Spring/Spring Security",
                "Reports self-invocation calls to methods secured by @PreAuthorize, @PostAuthorize, @PreFilter, or @PostFilter.",
                HighlightSeverity.WARNING, true, "Java");
    }

    private void initSpringWebSocketInspections() {
        // Spring WebSocket ([✓] 1 tool)
        addTool("Spring.WebSocket.IncorrectXmlContext", "Incorrect Spring WebSocket XML-based application context", "Spring/Spring WebSocket",
                "Reports configuration issues within Spring WebSocket XML-based application contexts.",
                HighlightSeverity.ERROR, true, "XML");
    }

    private void initSpringDataInspections() {
        // Separate top-level category: Spring Data ([✓] enabled matching screenshot)
        addTool("SpringData.JdbcAssociatedDbElements", "Spring Data JDBC associated DB elements", "Spring Data",
                "Validates database tables, columns, and relationships referenced in Spring Data JDBC entities.",
                HighlightSeverity.ERROR, true, "Java");
    }

    private void initSpringModulithInspections() {
        // Separate top-level category: Spring Modulith ([✓] all 3 enabled matching screenshot)
        addTool("SpringModulith.EventListenerCanBeSimplified", "Event listener declaration can be simplified", "Spring Modulith",
                "Suggests simplifying domain event listener method declarations in Spring Modulith modules.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("SpringModulith.InvalidDependencyDeclaration", "Invalid dependency declaration", "Spring Modulith",
                "Reports invalid or prohibited inter-module dependencies violating Modulith architecture boundaries.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("SpringModulith.RestrictedModuleApiUsage", "Restricted module API usage", "Spring Modulith",
                "Reports access to internal, non-exposed package members of another application module.",
                HighlightSeverity.ERROR, true, "Java");
    }

    private void initSqlInspections() {
        // SQL ([-] indeterminate matching screenshot with 3 disabled items)
        initSqlInspectionsPart1();
        initSqlInspectionsPart2();
    }

    private void initSqlInspectionsPart1() {
        addTool("SqlAddNotNullColumn", "Adding not null column without default value", "SQL",
                "Reports attempts to add a NOT NULL column to an existing table without specifying a default value.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlAggregate", "Aggregate-related problems", "SQL",
                "Detects aggregate functions and expressions used improperly in SELECT and GROUP BY clauses.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlAmbiguousColumn", "Ambiguous reference", "SQL",
                "Reports column or table references in queries that resolve to multiple ambiguous candidates.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlAutoIncrementDuplicate", "Auto-increment duplicate", "SQL",
                "Reports table definitions declaring multiple auto-increment columns.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlCheckUsingColumns", "Check using clause columns", "SQL",
                "Validates that column names in JOIN ... USING (column) exist in all joined tables.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlShadowingAlias", "Column is shadowed by alias", "SQL",
                "Reports column names shadowed by alias definitions in the same scope.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlGroupBy", "Column should be in group by clause", "SQL",
                "Reports non-aggregated columns in SELECT expressions that are missing from GROUP BY.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlConstantCondition", "Constant expression", "SQL",
                "Reports constant expressions in WHERE or HAVING clauses that always evaluate to true or false.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlSingleSessionTemporaryTable", "Create a temporary table without a single session mode", "SQL",
                "Reports temporary tables created when single session mode is not enabled in database console.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlCurrentSchema", "Current console schema introspected", "SQL",
                "Reports queries referencing schemas that have not been introspected in database console.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlWithoutWhere", "Delete or update statement without where clauses", "SQL",
                "Reports DELETE or UPDATE statements lacking a WHERE clause that would modify all rows.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlDeprecatedType", "Deprecated type", "SQL",
                "Reports usage of deprecated SQL data types in table or variable declarations.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlDuplicateColumn", "Duplicating column name in SELECT", "SQL",
                "Reports duplicate column names or aliases in SELECT projection lists.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlDerivedTableAlias", "Each derived table should have alias", "SQL",
                "Reports derived subqueries in FROM clauses that do not declare an alias identifier.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlExcessiveJoin", "Excessive JOIN count", "SQL",
                "Reports queries that join more tables than the recommended threshold.",
                HighlightSeverity.WARNING, false, "SQL");
        addTool("SqlSignature", "Function signature", "SQL",
                "Validates SQL built-in and user-defined function invocations against their parameter signatures.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlIdentifier", "Identifier should be quoted", "SQL",
                "Reports SQL reserved words used as identifier names without proper quoting.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlIllFormedStatement", "Ill-formed date/time literals", "SQL",
                "Reports malformed date, time, and timestamp literal strings in SQL statements.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlIllegalCursorState", "Illegal cursor state", "SQL",
                "Reports cursor operations invoked on closed or un-opened cursor handles.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlType", "Implicit string truncation", "SQL",
                "Reports string literals assigned to columns or variables whose length exceeds capacity.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlDropIndexedColumn", "Index is dependent on column", "SQL",
                "Reports attempts to drop or alter table columns that are indexed by existing database indexes.",
                HighlightSeverity.WARNING, true, "SQL");
    }

    private void initSqlInspectionsPart2() {
        addTool("SqlInsertIntoNonNull", "Insert NULL into NOT NULL column", "SQL",
                "Reports INSERT statements explicitly inserting NULL into columns with NOT NULL constraints.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlInsertIntoGeneratedColumn", "Insertion into generated columns", "SQL",
                "Reports attempts to manually insert values into GENERATED ALWAYS or identity columns.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlMisleadingReference", "Misleading references", "SQL",
                "Reports ambiguous or misleading table and column references across correlated subqueries.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlMissingColumnAliases", "Missing column aliases", "SQL",
                "Reports expressions in SELECT lists that lack explicit column aliases.",
                HighlightSeverity.WARNING, false, "SQL");
        addTool("SqlMissingReturnStatement", "Missing return statement", "SQL",
                "Reports stored functions and procedures lacking a required RETURN statement.",
                HighlightSeverity.ERROR, true, "SQL");
        addTool("SqlMultipleLimitClauses", "Multiple row limiting/offset clauses in queries", "SQL",
                "Reports queries containing multiple conflicting LIMIT, OFFSET, or TOP clauses.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlNamedArguments", "Named arguments should be used", "SQL",
                "Suggests using named arguments for stored procedure calls with many parameters.",
                HighlightSeverity.WARNING, false, "SQL");
        addTool("SqlNoDataSource", "No data sources configured", "SQL",
                "Reports SQL files and fragments not associated with any configured database data source.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlRedundantAlias", "Redundant alias expressions", "SQL",
                "Reports column or table aliases that are identical to the source identifier.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlRedundantCoalesce", "Redundant code in COALESCE call", "SQL",
                "Reports COALESCE arguments that appear after a non-nullable expression and will never be evaluated.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlRedundantElseNull", "Redundant ELSE NULL clause", "SQL",
                "Reports CASE expressions with explicit 'ELSE NULL' clauses which are default behavior.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlRedundantOrderingDirection", "Redundant ordering direction", "SQL",
                "Reports explicit ASC ordering direction in ORDER BY clauses which is default.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlRedundantLimit", "Redundant row limiting in queries", "SQL",
                "Reports redundant row limiting clauses like FETCH and LIMIT in queries. Example (PostgreSQL):\n\nCREATE TABLE foo(a INT);\n\nSELECT * FROM foo WHERE EXISTS(SELECT * FROM foo LIMIT 2);\nSELECT * FROM foo WHERE EXISTS(SELECT * FROM foo FETCH FIRST 2 ROWS ONLY);\n\nTo fix the warning, you can add OFFSET to limiting clauses. If OFFSET is missing, then LIMIT is redundant because the usage of LIMIT does not influence the operation result of EXISTS. In case with OFFSET, we skip first N rows and this will influence the output.\n\nSELECT * FROM foo WHERE EXISTS(SELECT * FROM foo OFFSET 1 ROW LIMIT 2);\nSELECT * FROM foo WHERE EXISTS(SELECT * FROM foo OFFSET 1 ROW FETCH FIRST 2 ROWS ONLY);",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlDialect", "SQL dialect detection", "SQL",
                "Detects SQL syntax features that do not match the assigned SQL dialect.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlSourceModification", "SQL source modification detection", "SQL",
                "Monitors synchronization status between local database scripts and server definitions.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlStatementWithSideEffects", "Statement with side effects", "SQL",
                "Reports SQL statements with side effects inside read-only transactions or functions.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlSuspiciousCodeInTriggers", "Suspicious code in triggers", "SQL",
                "Reports potentially problematic constructs inside database trigger bodies.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlTypesCompatibility", "Types compatibility", "SQL",
                "Validates data types compatibility across expressions, comparisons, and function calls in SQL queries.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlUnicodeStringLiteral", "Unicode usage in SQL", "SQL",
                "Reports national Unicode characters in string literals lacking required N prefix.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlUnreachableCode", "Unreachable code", "SQL",
                "Reports SQL statements that will never be executed due to prior RETURN, RAISE, or unconditional branch statements.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlResolve", "Unresolved reference", "SQL",
                "Reports table, column, procedure, or schema names that cannot be resolved in database context.",
                HighlightSeverity.ERROR, true, "SQL");
        addTool("SqlUnsafeJoinInDelete", "Unsafe 'join' clause in 'delete' statement", "SQL",
                "Reports JOIN clauses in DELETE statements that may cause unintended rows to be deleted.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlUnusedCte", "Unused common table expression", "SQL",
                "Reports common table expressions (WITH clauses) that are never referenced in the main query.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlUnusedSubqueryItem", "Unused subquery item", "SQL",
                "Reports columns or aliases defined in subqueries that are never referenced in outer query.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlUnusedVariable", "Unused variable", "SQL",
                "Reports SQL declared local variables that are never read or referenced in the routine body.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlGotoStatements", "Usages of GOTO statements", "SQL",
                "Reports usages of GOTO statements in SQL procedures and scripts.",
                HighlightSeverity.WARNING, false, "SQL");
        addTool("SqlTransactionInTriggers", "Use of transaction management statements in triggers", "SQL",
                "Reports COMMIT, ROLLBACK, or SAVEPOINT statements inside trigger definitions.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlCaseCoalesce", "Using CASE instead of COALESCE function and vice versa", "SQL",
                "Suggests replacing CASE expressions with COALESCE function calls or vice versa for cleaner syntax.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlCaseConditional", "Using CASE instead of conditional function and vice versa", "SQL",
                "Suggests replacing CASE expressions with NULLIF, IIF, or IFNULL conditional functions and vice versa.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlNamedAndPositionalArguments", "Using of named and positional arguments", "SQL",
                "Reports mixing named and positional arguments in procedure and function calls where forbidden.",
                HighlightSeverity.ERROR, true, "SQL");
        addTool("SqlValuesClauseCardinality", "VALUES clause cardinality", "SQL",
                "Reports mismatched column counts across different row tuples in a VALUES clause.",
                HighlightSeverity.ERROR, true, "SQL");
    }

    private void initSqlServerInspections() {
        // SQL server ([✓] all 2 enabled matching screenshot)
        addTool("SqlServer.BuiltinFunctions", "Builtin functions", "SQL server",
                "Reports SQL Server built-in functions called with incorrect arguments or unsupported options.",
                HighlightSeverity.WARNING, true, "SQL");
        addTool("SqlServer.OrderByInQueries", "ORDER BY in queries", "SQL server",
                "Reports ORDER BY clauses in views, inline functions, derived tables, and subqueries without TOP or OFFSET.",
                HighlightSeverity.ERROR, true, "SQL");
    }

    private void initThymeleafInspections() {
        // Thymeleaf ([✓] all 3 enabled matching screenshot)
        addTool("Thymeleaf.DialectExtensionsErrors", "Thymeleaf Dialect Extensions errors", "Thymeleaf",
                "Reports syntax and structural errors in custom Thymeleaf dialect attributes and element processors.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Thymeleaf.UnresolvedMessageResourceKeys", "Unresolved message resource keys", "Thymeleaf",
                "Reports Thymeleaf #{...} message keys that cannot be resolved in message property bundles.",
                HighlightSeverity.ERROR, true, "HTML");
        addTool("Thymeleaf.UnresolvedReferencesInExpressionVariables", "Unresolved references in Thymeleaf expression variables", "Thymeleaf",
                "Reports Thymeleaf ${...} and *{...} expression variables that cannot be resolved in model context.",
                HighlightSeverity.WARNING, true, "HTML");
    }

    private void initTomlInspections() {
        // TOML ([✓] 1 enabled matching screenshot)
        addTool("Toml.UnresolvedReference", "Unresolved reference", "TOML",
                "Reports unresolved table or array references and invalid key paths in TOML configuration files.",
                HighlightSeverity.WARNING, true, "TOML");
    }

    private void initVelocityInspections() {
        // Velocity ([✓] all 5 enabled matching screenshot)
        addTool("Velocity.DirectiveArgumentsInspection", "Directive arguments inspection", "Velocity",
                "Validates argument count and types passed to Apache Velocity template directives.",
                HighlightSeverity.WARNING, true, "Velocity");
        addTool("Velocity.FileReferencesInspection", "File references inspection", "Velocity",
                "Validates #parse and #include directive file paths to ensure target velocity template files exist.",
                HighlightSeverity.WARNING, true, "Velocity");
        addTool("Velocity.ReferencesInspection", "References inspection", "Velocity",
                "Reports unresolved context variable and property references in Velocity template files.",
                HighlightSeverity.WARNING, true, "Velocity");
        addTool("Velocity.TypesInspection", "Types inspection", "Velocity",
                "Checks types in method calls and property access within Velocity expressions.",
                HighlightSeverity.WARNING, true, "Velocity");
        addTool("Velocity.WellFormednessInspection", "Well-formedness inspection", "Velocity",
                "Reports unclosed directives, unmatched #end statements, and syntactic malformations in Velocity templates.",
                HighlightSeverity.ERROR, true, "Velocity");
    }

    private void initVersionControlInspections() {
        // Version control ([✓] 1 enabled matching screenshot)
        addTool("Vcs.IgnoreFileDuplicates", "Ignore file duplicates", "Version control",
                "Reports duplicate rules and entries in .gitignore, .hgignore, and other ignore files.",
                HighlightSeverity.WARNING, true, "General");
    }

    private void initVueInspections() {
        // Vue ([✓] all 6 enabled matching screenshot)
        addTool("Vue.DataFunction", "Data function", "Vue",
                "Reports Vue component 'data' properties that are not declared as a function returning an object.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.DeprecatedSymbol", "Deprecated symbol", "Vue",
                "Reports usage of deprecated Vue APIs, lifecycle hooks, and template directives.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.DuplicateTemplateScriptTag", "Duplicate template/script tag", "Vue",
                "Reports duplicate <template>, <script>, or <style> tags in a single Vue component file.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.MissingComponentImport", "Missing component import", "Vue",
                "Reports Vue components used in template that are not imported or registered in script.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.UnrecognizedDirective", "Unrecognized directive", "Vue",
                "Reports unknown or unrecognized Vue v- directives on template elements.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.UnrecognizedSlot", "Unrecognized slot", "Vue",
                "Reports slot names used on child components that are not defined in child's template.",
                HighlightSeverity.WARNING, true, "Vue");
    }

    private void initXmlInspections() {
        // XML ([✓] all 14 enabled matching screenshot)
        addTool("Xml.DeprecatedApiUsage", "Deprecated API usage in XML", "XML",
                "Reports references to deprecated XML elements, attributes, or API tags in XML configurations.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.DeprecatedSymbol", "Deprecated symbol", "XML",
                "Reports symbols and entities deprecated in schemas or DTD specifications.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.DuplicateIdAttribute", "Duplicate 'id' attribute", "XML",
                "Reports duplicate xml:id or id attributes within the same XML document scope.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.EmptyElementContent", "Empty element content", "XML",
                "Reports XML elements with empty bodies that should either contain content or be self-closing.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.FailedExternalValidation", "Failed external validation", "XML",
                "Reports validation errors against external DTD, XSD schema, or RelaxNG specifications.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.RedundantAttributeWithDefaultValue", "Redundant attribute with default value", "XML",
                "Reports XML attributes explicitly set to values that match their DTD or schema default.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.UnboundNamespacePrefix", "Unbound namespace prefix", "XML",
                "Reports XML element or attribute namespace prefixes that have no corresponding xmlns declaration.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.UnresolvedIdReference", "Unresolved 'id' reference", "XML",
                "Reports IDREF or IDREFS attributes referencing non-existent ID targets.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.UnresolvedDtdReference", "Unresolved DTD reference", "XML",
                "Reports DOCTYPE declarations pointing to missing or unreachable external DTD files.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.UnresolvedFileReference", "Unresolved file reference", "XML",
                "Reports file system or resource paths in XML attributes that cannot be resolved.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.UnresolvedReferences", "Unresolved references", "XML",
                "Reports general unresolved symbol, bean, or entity references in XML descriptor files.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.UnusedSchemaDeclaration", "Unused schema declaration", "XML",
                "Reports XML schema namespace declarations (xsi:schemaLocation) that are not used.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xml.WrongRootElement", "Wrong root element", "XML",
                "Reports XML documents whose root element does not match expected schema or doctype root.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xml.Highlighting", "XML highlighting", "XML",
                "Performs comprehensive batch validation and highlighting of XML syntax and structure.",
                HighlightSeverity.WARNING, true, "XML", true, false);
    }

    private void initXPathInspections() {
        // XPath ([✓] all 5 enabled matching screenshot)
        addTool("XPath.HardcodedNamespacePrefix", "Hardcoded namespace prefix", "XPath",
                "Reports hardcoded namespace prefixes in XPath expressions that should be resolved dynamically.",
                HighlightSeverity.WARNING, true, "XPath");
        addTool("XPath.ImplicitTypeConversion", "Implicit type conversion", "XPath",
                "Reports implicit conversions between node-sets, strings, numbers, and booleans in XPath expressions.",
                HighlightSeverity.WARNING, true, "XPath");
        addTool("XPath.RedundantTypeConversion", "Redundant type conversion", "XPath",
                "Reports explicit string(), number(), or boolean() conversions that are already performed implicitly.",
                HighlightSeverity.WARNING, true, "XPath");
        addTool("XPath.UnknownElementOrAttributeName", "Unknown element or attribute name", "XPath",
                "Reports element or attribute step names in XPath expressions that do not exist in associated schema.",
                HighlightSeverity.WARNING, true, "XPath");
        addTool("XPath.PredicateWithIndex0", "XPath predicate with index 0", "XPath",
                "Reports XPath position predicates with index [0], since XPath indexing is 1-based.",
                HighlightSeverity.WARNING, true, "XPath");
    }

    private void initXsltInspections() {
        // XSLT ([✓] all 4 enabled matching screenshot)
        addTool("Xslt.IncorrectDeclaration", "Incorrect declaration", "XSLT",
                "Reports invalid xsl:output, xsl:key, or xsl:namespace declarations in XSLT stylesheets.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xslt.IncorrectTemplateInvocation", "Incorrect template invocation", "XSLT",
                "Reports xsl:call-template instructions with invalid names or mismatched xsl:with-param arguments.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("Xslt.ShadowedVariable", "Shadowed variable", "XSLT",
                "Reports local xsl:variable declarations that shadow global variables or parameters.",
                HighlightSeverity.WARNING, true, "XML");
        addTool("Xslt.UnusedVariableOrParameter", "Unused variable or parameter", "XSLT",
                "Reports xsl:variable or xsl:param definitions that are never referenced within template scope.",
                HighlightSeverity.WARNING, true, "XML");
    }

    private void initYamlInspections() {
        // YAML ([✓] all 7 enabled matching screenshot)
        addTool("Yaml.DeprecatedKey", "Deprecated YAML key", "YAML",
                "Reports deprecated keys in YAML files based on schema definitions.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Yaml.DuplicatedKeys", "Duplicated YAML keys", "YAML",
                "Reports duplicate keys within the same YAML mapping.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Yaml.RecursiveAlias", "Recursive alias", "YAML",
                "Reports circular alias references that cause infinite recursion when resolving YAML anchors.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Yaml.SuspiciousTypeMismatch", "Suspicious type mismatch", "YAML",
                "Reports values whose types do not conform to expected schema types in YAML documents.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Yaml.UnresolvedAlias", "Unresolved alias", "YAML",
                "Reports YAML alias references (*alias) that cannot be resolved to any anchor (&anchor).",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("Yaml.UnusedAnchor", "Unused anchor", "YAML",
                "Reports YAML anchors (&anchor) that are defined but never referenced by any alias.",
                HighlightSeverity.WARNING, true, "YAML");
        addTool("Yaml.ValidationByJsonSchema", "Validation by JSON Schema", "YAML",
                "Validates YAML documents against associated JSON schemas and reports validation errors.",
                HighlightSeverity.WARNING, true, "YAML");
    }

    // =========================================================================
    // 77. Scala Hierarchy (Screenshots 1-5 Parity)
    // =========================================================================

    private void initScalaInspections() {
        initScalaAkkaInspections();
        initScalaCodeStyleInspections();
        initScalaCollectionsInspections();
        initScalaDirectiveInspections();
        initScalaGeneralInspectionsPart1();
        initScalaGeneralInspectionsPart2();
        initScalaGeneralInspectionsPart3();
        initScalaMethodSignatureInspections();
        initScalaPlayAndPropertiesInspections();
        initScalaScaladocAndSpecs2Inspections();
        initScalaSyntacticAndWorksheetInspections();
    }

    private void initScalaAkkaInspections() {
        // Scala/Akka ([✓] 4 tools, all Warning, all enabled)
        addTool("Scala.Akka.ActorMutableState", "Actor mutable state", "Scala/Akka",
                "Reports mutable state (such as vars or mutable collections) in Akka Actor classes that can lead to race conditions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Akka.AppropriateActorConstructorNotFound", "Appropriate Actor constructor not found", "Scala/Akka",
                "Reports Props creations where no matching constructor exists on the target Actor class.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Akka.CouldBeReplacedWithFactoryMethodCall", "Could be replaced with a factory method call", "Scala/Akka",
                "Reports Props instantiations that can be replaced with calls to existing companion factory methods.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Akka.DynamicInvocationCouldBeReplacedWithConstructorCall", "Dynamic invocation could be replaced with a constructor call", "Scala/Akka",
                "Reports Props invocations using reflection or class name strings that can use direct typed constructors.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCodeStyleInspections() {
        // Scala/Code style ([✓] 2 tools, all Warning, all enabled)
        addTool("Scala.CodeStyle.ScalaStyleInspection", "Scala style inspection", "Scala/Code style",
                "Inspects Scala files according to scalastyle rules and configuration.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.CodeStyle.Scala2SyntaxWithXsource3", "Scala 2 syntax with -Xsource:3", "Scala/Code style",
                "Reports Scala 2 syntax constructs that can be migrated to Scala 3 syntax when compiling with -Xsource:3.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCollectionsInspections() {
        initScalaCollectionsComparing();
        initScalaCollectionsIndicesAndMaps();
        initScalaCollectionsOptionsAndOther();
        initScalaCollectionsSimplifications();
        initScalaCollectionsSizeAndDirect();
    }

    private void initScalaCollectionsComparing() {
        // Scala/Collections/Comparing ([-] 5 tools, 1 disabled)
        addTool("Scala.Collections.Comparing.ComparingLengthToLengthCompare", "Comparing length to lengthCompare", "Scala/Collections/Comparing",
                "Suggests comparing collection length with lengthCompare for better performance on linear sequences.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.Collections.Comparing.CorrespondsSameElementsOnUnsortedCollection", "Corresponds/sameElements on unsorted collection", "Scala/Collections/Comparing",
                "Reports corresponds or sameElements calls on unsorted collections like Set or Map where order is non-deterministic.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Comparing.EqualsOnArraysAndIterators", "Equals on arrays and iterators", "Scala/Collections/Comparing",
                "Reports == or equals calls on arrays and iterators that check reference identity rather than content equality.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Comparing.EqualsOnCollectionsOfDifferentKinds", "Equals on collections of different kinds", "Scala/Collections/Comparing",
                "Reports == or equals comparisons between collections of different kinds (e.g., List and Set) that always evaluate to false.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Comparing.SameElementsOnCollectionsOfSameKind", "SameElements onCollections of a same kind", "Scala/Collections/Comparing",
                "Reports sameElements invocations on collections of the same type where == or equals is clearer and more idiomatic.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCollectionsIndicesAndMaps() {
        // Scala/Collections/Indices ([✓] 4 tools)
        addTool("Scala.Collections.Indices.AccessToFirstElementByIndex", "Access to first element by index", "Scala/Collections/Indices",
                "Suggests replacing collection(0) with collection.head.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Indices.AccessToLastElementByIndex", "Access to last element by index", "Scala/Collections/Indices",
                "Suggests replacing collection(collection.size - 1) with collection.last.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Indices.ConstructingRangeForSeqIndices", "Constructing range for seq indices", "Scala/Collections/Indices",
                "Suggests replacing 0 until seq.length with seq.indices.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Indices.ManuallyZippingWithIndices", "Manually zipping with indices", "Scala/Collections/Indices",
                "Suggests replacing seq.zip(seq.indices) with seq.zipWithIndex.",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Collections/Maps ([✓] 7 tools)
        addTool("Scala.Collections.Maps.EmptinessCheckOnGetToContains", "Emptiness check on Get to Contains", "Scala/Collections/Maps",
                "Suggests replacing map.get(k).isDefined or map.get(k).nonEmpty with map.contains(k).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.ExtractingKeysManually", "Extracting keys manually", "Scala/Collections/Maps",
                "Suggests replacing map.map(_._1) with map.keys or map.keySet.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.ExtractingValuesManually", "Extracting values manually", "Scala/Collections/Maps",
                "Suggests replacing map.map(_._2) with map.values.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.GetAndGetOrElseToGetOrElse", "Get and getOrElse to getOrElse", "Scala/Collections/Maps",
                "Suggests replacing map.get(k).getOrElse(v) with map.getOrElse(k, v).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.LiftToGet", "Lift to Get", "Scala/Collections/Maps",
                "Suggests replacing map.lift(k) with map.get(k).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.RedundantGetWhenGettingValueFromMap", "Redundant get when getting a value from Map", "Scala/Collections/Maps",
                "Suggests replacing map.get(k).get with map(k).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Maps.UnitReturnTypeInArgumentOfMap", "Unit return type in the argument of map", "Scala/Collections/Maps",
                "Reports map calls where the transformation function returns Unit, suggesting foreach instead.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCollectionsOptionsAndOther() {
        // Scala/Collections/Options ([-] 8 tools, 2 disabled)
        addTool("Scala.Collections.Options.ChangeToFilter", "Change to filter", "Scala/Collections/Options",
                "Suggests replacing if (p(x)) opt else None with opt.filter(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.EmulatedOptionX", "Emulated Option(x)", "Scala/Collections/Options",
                "Suggests replacing if (x != null) Some(x) else None with Option(x).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.EqualsSomeXToContainsX", "Equals Some(x) to contains(x)", "Scala/Collections/Options",
                "Suggests replacing opt == Some(x) with opt.contains(x).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.GetOrElseNullToOrNull", "GetOrElse(null) to orNull", "Scala/Collections/Options",
                "Suggests replacing opt.getOrElse(null) with opt.orNull.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.MapAndGetOrElseFalseToExists", "Map and getOrElse(false) to exists", "Scala/Collections/Options",
                "Suggests replacing opt.map(p).getOrElse(false) with opt.exists(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.MapAndGetOrElseToFold", "Map and getOrElse to fold", "Scala/Collections/Options",
                "Suggests replacing opt.map(f).getOrElse(default) with opt.fold(default)(f).",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.Collections.Options.RedundantHeadOptionOrLastOption", "Redundant headOption or lastOption", "Scala/Collections/Options",
                "Reports redundant headOption or lastOption calls on an expression that is already an Option.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Options.SomeToOption", "Some to Option", "Scala/Collections/Options",
                "Suggests replacing Some(x) with Option(x) when x may be null.",
                HighlightSeverity.WARNING, false, "Scala");

        // Scala/Collections/Other ([✓] 2 tools)
        addTool("Scala.Collections.Other.FilterAfterSort", "Filter after sort", "Scala/Collections/Other",
                "Suggests performing filter before sort to reduce the number of elements sorted.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Other.UnzipForExtractingSingleElement", "Unzip for extracting a single element", "Scala/Collections/Other",
                "Suggests replacing seq.unzip._1 with seq.map(_._1) to avoid allocating unnecessary temporary collections.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCollectionsSimplifications() {
        // Scala/Collections/Simplifications: filter and exists ([-] 8 tools, 1 disabled)
        addTool("Scala.Collections.SimplificationsFilterAndExists.ExistsSimplifiableToContains", "Exists simplifiable to contains", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.exists(_ == x) with seq.contains(x).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.FilterAndContainsToIntersectOrDiff", "Filter and contains to intersect or diff", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.filter(other.contains) with seq.intersect(other) or seq.diff(other).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.FilterAndEmptinessCheckToExistsForall", "Filter and emptiness check to exists/forall", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.filter(p).nonEmpty with seq.exists(p) or seq.filter(p).isEmpty with !seq.exists(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.FilterAndHeadOptionToFind", "Filter and headOption to find", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.filter(p).headOption with seq.find(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.FilterAndSizeToCount", "Filter and size to count", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.filter(p).size with seq.count(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.FindAndEmptinessCheckToExists", "Find and emptiness check to exists", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.find(p).isDefined with seq.exists(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.MapAndContainsTrueFalseToExistsOrNotForall", "Map and contains(true/false) to exists or !forall", "Scala/Collections/Simplifications: filter and exists",
                "Suggests replacing seq.map(p).contains(true) with seq.exists(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsFilterAndExists.RedundantContainsInFilter", "Redundant contains in filter", "Scala/Collections/Simplifications: filter and exists",
                "Reports redundant contains checks inside filter predicates.",
                HighlightSeverity.WARNING, false, "Scala");

        // Scala/Collections/Simplifications: find and map to apply ([✓] 1 tool)
        addTool("Scala.Collections.SimplificationsFindAndMapToApply.FindAndMapToGet", "Find and map to get", "Scala/Collections/Simplifications: find and map to apply",
                "Suggests replacing map.find(_._1 == k).map(_._2) with map.get(k).",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Collections/Simplifications: forall and exists ([✓] 1 tool)
        addTool("Scala.Collections.SimplificationsForallAndExists.DoubleNegationInForallAndExists", "Double negation in forall and exists", "Scala/Collections/Simplifications: forall and exists",
                "Suggests replacing !seq.forall(!p) with seq.exists(p) or !seq.exists(!p) with seq.forall(p).",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Collections/Simplifications: other ([✓] 15 tools)
        addTool("Scala.Collections.SimplificationsOther.CollectAndHeadOptionToCollectFirst", "Collect and headOption to collectFirst", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.collect(pf).headOption with seq.collectFirst(pf).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ConversionToSetAndBackToDistinct", "Conversion to Set and back to distinct", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.toSet.toSeq with seq.distinct.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.DropAndTakeToSlice", "Drop and take to slice", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.drop(from).take(until - from) with seq.slice(from, until).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.EmulatedHeadOptionOrLastOption", "Emulated headOption or lastOption", "Scala/Collections/Simplifications: other",
                "Suggests replacing if (seq.nonEmpty) Some(seq.head) else None with seq.headOption.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.FoldSimplifiableToForall", "Fold simplifiable to forall", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.foldLeft(true)((acc, x) => acc && p(x)) with seq.forall(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.IndexBoundsCheck", "Index bounds check", "Scala/Collections/Simplifications: other",
                "Suggests replacing 0 <= i && i < seq.length with seq.isDefinedAt(i).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.MakeArrayToString", "Make Array to String", "Scala/Collections/Simplifications: other",
                "Suggests replacing new String(arr) or arr.mkString with arr.mkString for consistent array serialization.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.MapAndFlattenToFlatMap", "Map and flatten to flatMap", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.map(f).flatten with seq.flatMap(f).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ReplaceToWithUntil", "Replace to with until", "Scala/Collections/Simplifications: other",
                "Suggests replacing 0 to len - 1 with 0 until len.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ReplaceWithFlatten", "Replace with flatten", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.flatMap(identity) with seq.flatten.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ReverseTakeAndReverseToTakeRight", "Reverse, take and reverse to takeRight", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.reverse.take(n).reverse with seq.takeRight(n).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ReverseAndFindToFindLast", "Reverse and find to findLast", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.reverse.find(p) with seq.findLast(p).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.ReverseAndIteratorToReverseIterator", "Reverse and iterator to reverseIterator", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.reverse.iterator with seq.reverseIterator.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.SimplifiableFoldOrReduceMethod", "Simplifiable fold or reduce method", "Scala/Collections/Simplifications: other",
                "Suggests replacing fold or reduce invocations with specialized methods like sum, product, min, or max.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SimplificationsOther.SortedAndHeadLastToMaxMin", "Sorted and head/last to max/min", "Scala/Collections/Simplifications: other",
                "Suggests replacing seq.sorted.head with seq.min or seq.sorted.last with seq.max.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaCollectionsSizeAndDirect() {
        // Scala/Collections/Size ([✓] 2 tools)
        addTool("Scala.Collections.Size.SimplifiableEmptyCheck", "Simplifiable empty check", "Scala/Collections/Size",
                "Suggests replacing seq.size == 0 with seq.isEmpty and seq.size > 0 with seq.nonEmpty.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.Size.SizeToLengthOnArraysAndStrings", "Size to length on arrays and strings", "Scala/Collections/Size",
                "Suggests replacing .size with .length on arrays and strings.",
                HighlightSeverity.WARNING, true, "Scala");

        // Direct under Scala/Collections (1 enabled, 1 disabled -> Collections is indeterminate [-])
        addTool("Scala.Collections.RedundantCollectionConversion", "Redundant collection conversion", "Scala/Collections",
                "Reports collection conversion calls (like .toList or .toSeq) that produce the same collection type or are immediately converted again.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Collections.SideEffectsInMonadicTransformation", "Side effects in a monadic transformation", "Scala/Collections",
                "Reports side-effecting operations like println or mutable updates inside map, flatMap, and filter operations.",
                HighlightSeverity.WARNING, false, "Scala");
    }

    private void initScalaDirectiveInspections() {
        // Scala/Directive ([✓] 1 tool)
        addTool("Scala.Directive.NewerStableVersionForLibraryDependencyIsAvailable", "Newer stable version for library dependency is available", "Scala/Directive",
                "Reports newer stable library dependency versions available in maven repositories.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaGeneralInspectionsPart1() {
        // Scala/General (Part 1 of 3: tools 1 to 22)
        addTool("Scala.General.AbsoluteImport", "Absolute import", "Scala/General",
                "Reports package-relative import paths that can be made absolute with _root_.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.AbstractValueInTrait", "Abstract value in trait", "Scala/General",
                "Reports abstract val definitions in traits that can lead to null pointer errors during trait initialization.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.AdvancedLanguageFeatures", "Advanced language features", "Scala/General",
                "Reports usage of advanced Scala language features (such as higherKinds, reflectiveCalls, existentials) without explicit language imports.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.AlphanumericDefinitionUsedAsInfixOperator", "Alphanumeric definition used as infix operator is not declared 'infix'", "Scala/General",
                "Reports alphanumeric method definitions used as infix operators without the 'infix' modifier in Scala 3.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.BasePackageDeclaration", "Base package declaration", "Scala/General",
                "Reports package statements that do not align with configured base package conventions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.CaseClassParameter", "Case class parameter", "Scala/General",
                "Reports redundant val modifiers on case class constructor parameters.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ClassDoesNotCorrespondToFileName", "Class doesn't correspond to file name", "Scala/General",
                "Reports toplevel classes or objects whose names do not match the containing Scala source file name.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ClassParameterShadowsSuperclassVar", "Class parameter shadows superclass var", "Scala/General",
                "Reports constructor parameters that shadow mutable var fields of superclasses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ComparingUnrelatedTypes", "Comparing unrelated types", "Scala/General",
                "Reports equality checks between expressions of unrelated types that will always evaluate to false.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ConvertExpressionToSAM", "Convert expression to Single Abstract Method (SAM)", "Scala/General",
                "Suggests converting anonymous class instances to lambda expressions when targeting single abstract method interfaces.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.DeclarationAccessCanBeWeaker", "Declaration access can be weaker", "Scala/General",
                "Reports members whose visibility can be made private or protected based on usage analysis.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.DefinitionAndOverriddenMemberHaveDifferentTargetName", "Definition and the overridden member have different @targetName", "Scala/General",
                "Reports overriding members whose @targetName annotation does not match the superclass member's @targetName.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.DefinitionMissesTargetNameAnnotation", "Definition misses a @targetName annotation", "Scala/General",
                "Reports overriding members that omit a @targetName annotation when the overridden member specifies one.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.DefinitionWithOperatorNameDoesNotHaveTargetName", "Definition with an operator name doesn't have @targetName", "Scala/General",
                "Reports operator-named definitions in Scala 3 that lack a corresponding @targetName annotation.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.DeprecatedIdentifier", "Deprecated identifier", "Scala/General",
                "Reports identifiers that use deprecated keywords or syntax.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.DeprecatedKindProjectorSyntax", "Deprecated kind-projector syntax", "Scala/General",
                "Reports legacy kind-projector type lambda syntax that can be updated to modern syntax.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.DeprecatedPackageObject", "Deprecated package object", "Scala/General",
                "Reports package object declarations deprecated in Scala 3 in favor of toplevel definitions.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.DoubleNegation", "Double negation", "Scala/General",
                "Reports redundant double boolean negation expressions like !!x.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.EmptyTargetNameExternalName", "Empty @targetName external name", "Scala/General",
                "Reports @targetName annotations with empty external names.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.FieldFromDelayedInit", "Field from DelayedInit", "Scala/General",
                "Reports fields accessed in classes extending App or DelayedInit that may not yet be initialized.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.FinalModifierRedundantForToplevelObjects", "'final' modifier is redundant for toplevel objects", "Scala/General",
                "Reports redundant 'final' modifiers on toplevel object declarations.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.FloatingPointLiteralEndingWithDot", "Floating point literal ending with '.'", "Scala/General",
                "Reports floating point numbers written with a trailing dot (e.g. 1.) which can cause syntactic ambiguity.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaGeneralInspectionsPart2() {
        // Scala/General (Part 2 of 3: tools 23 to 45)
        addTool("Scala.General.IsInstanceOf", "isInstanceOf", "Scala/General",
                "Reports isInstanceOf type checks that can be replaced with pattern matching or polymorphism.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.KindProjectorSimplifyType", "Kind Projector: simplify type", "Scala/General",
                "Suggests simplifying kind-projector type projections and type lambdas.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.KindProjectorUseCorrectLambdaKeyword", "Kind Projector: Use correct lambda keyword", "Scala/General",
                "Reports incorrect or outdated lambda keyword usage in kind-projector syntax.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.LegacyStringFormatting", "Legacy string formatting", "Scala/General",
                "Suggests converting legacy String.format or concatenation to string interpolators.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.LoopVariableNotUpdatedInsideLoop", "Loop variable not updated inside loop", "Scala/General",
                "Reports while loops where the loop condition variable is not updated within the body, causing an infinite loop.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.MalformedFormatString", "Malformed format string", "Scala/General",
                "Reports format strings with invalid specifiers, unbalanced arguments, or mismatched types.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.MarkInnerCaseObjectsAsFinal", "Mark inner case objects as final", "Scala/General",
                "Suggests adding 'final' modifier to inner case objects to improve pattern match exhaustiveness.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.MatchStatementConvertibleToPatternMatchingAnonymousFunction", "Match statement convertible to pattern matching anonymous function", "Scala/General",
                "Suggests converting match statements to pattern-matching partial functions (e.g., x => x match { ... }).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.MemberHasTargetNameWhenOverriddenMemberDoesNot", "Member has @targetName annotation when the overridden member doesn't", "Scala/General",
                "Reports overriding members that add a @targetName annotation when the superclass member does not define one.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.MissingTypeAnnotation", "Missing type annotation", "Scala/General",
                "Reports public declarations lacking explicit return type annotations.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("MultipleTargetNameAnnotations", "Multiple @targetName annotations", "Scala/General",
                "Reports the usage of multiple @targetName annotations on a single element.\n\n" +
                "All but the last @targetName annotation are ignored. Consider using at most one annotation per definition.\n\n" +
                "The quick-fix removes selected @targetName annotation.\n\n" +
                "Inspection ID: MultipleTargetNameAnnotations",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.MultipleArgListsInAnnotationConstructor", "Multiple arg lists in annotation constructor", "Scala/General",
                "Reports annotation invocations specifying multiple argument lists which is unsupported by the Scala compiler.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.MultipleTargetsForTargetName", "Multiple targets for @targetName", "Scala/General",
                "Reports multiple definitions having the same @targetName in the same scope, which causes JVM name collisions.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.NameBooleanParameters", "Name boolean parameters", "Scala/General",
                "Suggests using named arguments for boolean parameters to improve call-site readability.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.NestedStatefulMonads", "Nested stateful monads", "Scala/General",
                "Reports deeply nested stateful monad transformers that can be restructured or flattened.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.NoTailRecursionAnnotation", "No tail recursion annotation", "Scala/General",
                "Suggests adding @tailrec annotation to tail-recursive method definitions to guarantee optimization.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.NonLocalReturnStatement", "Non-local return statement", "Scala/General",
                "Reports return statements inside closures and anonymous functions that cause non-local returns.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.NonValueFieldAccessedInHashCode", "Non-value field is accessed in 'hashCode()'", "Scala/General",
                "Reports mutable var fields accessed in hashCode() implementations which breaks hash map consistency.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.NotImplementedCode", "Not implemented code", "Scala/General",
                "Reports usages of ??? or NotImplementedError in production code paths.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.OldStyleSyntaxForAggregateContextBoundsIsDeprecated", "Old style syntax for aggregate context bounds is deprecated", "Scala/General",
                "Reports deprecated old-style context bound syntax in Scala 3.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.RedundantBlock", "Redundant block", "Scala/General",
                "Reports redundant curly brace blocks wrapping single expressions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.RedundantCastInspection", "Redundant cast inspection", "Scala/General",
                "Reports redundant asInstanceOf casts where the target type is already guaranteed.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.RedundantClassParameterClause", "Redundant class parameter clause", "Scala/General",
                "Reports redundant empty constructor parameter clauses on class declarations.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaGeneralInspectionsPart3() {
        // Scala/General (Part 3 of 3: tools 46 to 67)
        addTool("Scala.General.RedundantConversionInspection", "Redundant conversion inspection", "Scala/General",
                "Reports redundant type conversion invocations that convert an expression to its own type.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ReferenceMustBePrefixed", "Reference must be prefixed", "Scala/General",
                "Reports references that should be qualified with explicit package or class prefixes to prevent ambiguity.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.RelativeImport", "Relative import", "Scala/General",
                "Reports relative package imports that can be rewritten to absolute imports.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.ScalaDeprecation", "Scala deprecation", "Scala/General",
                "Reports usages of deprecated Scala standard library symbols and compiler features.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.SimplifyBooleanExpression", "Simplify boolean expression", "Scala/General",
                "Reports complex boolean expressions that can be simplified using boolean algebra laws.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.SuspiciousForwardReference", "Suspicious forward reference", "Scala/General",
                "Reports references to fields before their declaration point in class bodies.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.SuspiciousShadowingByTypeParameter", "Suspicious shadowing by a Type Parameter", "Scala/General",
                "Reports method or inner class type parameters that shadow type parameters in enclosing scopes.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.SuspiciousShadowingByVariablePattern", "Suspicious shadowing by a Variable Pattern", "Scala/General",
                "Reports variable pattern names in match clauses that shadow existing local variables.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.TrivialMatchCanBeSimplified", "Trivial match can be simplified", "Scala/General",
                "Suggests simplifying boolean or single-case match expressions with if-else or direct assignments.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.TypeAnnotationRequired", "Type annotation required", "Scala/General",
                "Reports declarations that require explicit type annotations according to configured coding rules.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.TypeCheckCanBePatternMatching", "Type check can be pattern matching", "Scala/General",
                "Suggests replacing isInstanceOf checks combined with asInstanceOf casts by pattern matching.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnmatchedTag", "Unmatched tag", "Scala/General",
                "Reports unmatched opening or closing XML literals in Scala XML code blocks.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.General.UnnecessaryBracesInImportInspection", "Unnecessary braces in import inspection", "Scala/General",
                "Reports redundant curly braces in import statements containing only a single import target.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnnecessaryParentheses", "Unnecessary parentheses", "Scala/General",
                "Reports redundant parentheses in expressions that do not affect operator precedence.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnnecessaryPartialFunction", "Unnecessary partial function", "Scala/General",
                "Reports partial function literals that can be simplified into standard lambda functions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnreachableCode", "Unreachable code", "Scala/General",
                "Reports code that cannot be reached during execution following returns, throws, or unconditional branches.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnusedDeclaration", "Unused declaration", "Scala/General",
                "Reports declarations that are never used across the project or local scope.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UnusedExpression", "Unused expression", "Scala/General",
                "Reports expressions whose evaluated values are never used and have no side effects.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.UseOfPostfixMethodCall", "Use of postfix method call", "Scala/General",
                "Reports postfix method call syntax which requires explicit compiler feature flag and is discouraged.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.General.VarCouldBeVal", "'var' could be a 'val'", "Scala/General",
                "Reports mutable var definitions that are never reassigned after initialization and can be made val.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.ViewBoundsAreDeprecated", "View bounds are deprecated", "Scala/General",
                "Reports view bound syntax (e.g., [T <% Ordered[T]]) deprecated in Scala 2.13 and removed in Scala 3.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.General.WrongPackageStatement", "Wrong package statement", "Scala/General",
                "Reports package declarations that do not match the directory structure of the file.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaMethodSignatureInspections() {
        // Scala/Method signature ([✓] 16 tools, all enabled matching media_1790826033450.png)
        addTool("Scala.MethodSignature.OverrideAbstractMember", "Abstract method implementation without override keyword", "Scala/Method signature",
                "Reports abstract method implementations that omit the override keyword.",
                HighlightSeverity.NO_HIGHLIGHTING, true, "Scala");
        addTool("Scala.MethodSignature.AccessorLikeMethodIsEmptyParen", "Accessor-like method has empty parameter clause", "Scala/Method signature",
                "Reports accessor-like methods declared with empty parentheses () which should be parameterless according to conventions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.AccessorLikeMethodIsUnit", "Accessor-like method has Unit result type", "Scala/Method signature",
                "Reports accessor-like methods that return Unit instead of an accessor value.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.ApparentResultTypeRefinement", "Apparent result type refinement; is an assignment missing?", "Scala/Method signature",
                "Reports apparent result type refinement where an '=' assignment might be missing.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.EmptyParenMethodAccessedAsParameterless", "Empty-paren method accessed as parameterless", "Scala/Method signature",
                "Reports empty-paren methods called without parentheses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.EmptyParenMethodOverriddenAsParameterless", "Empty-paren Scala method overridden as parameterless", "Scala/Method signature",
                "Reports empty-paren Scala methods overridden as parameterless in subclasses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.JavaAccessorEmptyParenCall", "Java accessor method called with empty argument clause", "Scala/Method signature",
                "Reports Java accessor methods invoked with empty parentheses ().",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.JavaAccessorMethodOverriddenAsEmptyParen", "Java accessor method overridden with empty argument clause", "Scala/Method signature",
                "Reports parameterless Java accessor methods overridden with empty argument clauses ().",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.JavaMutatorMethodAccessedAsParameterless", "Java mutator method accessed as parameterless", "Scala/Method signature",
                "Reports Java mutator methods called without parentheses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.JavaMutatorMethodOverriddenAsParameterless", "Java mutator method overridden as parameterless", "Scala/Method signature",
                "Reports Java mutator methods overridden as parameterless in Scala classes.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.UnitMethodIsParameterless", "Method with Unit result type is parameterless", "Scala/Method signature",
                "Reports side-effecting methods returning Unit that are declared without parentheses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.MutatorLikeMethodIsParameterless", "Mutator-like named method is parameterless", "Scala/Method signature",
                "Reports mutator-like named methods (e.g., set*, init*) declared without parentheses.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.ParameterlessMemberOverriddenAsEmptyParen", "Parameterless Scala member overridden as empty-paren", "Scala/Method signature",
                "Reports parameterless Scala members overridden with empty argument clauses ().",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.ProcedureDeclaration", "Procedure syntax in method declaration", "Scala/Method signature",
                "Reports procedure syntax method declarations deprecated in Scala 3.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.ProcedureDefinition", "Procedure syntax in method definition", "Scala/Method signature",
                "Reports procedure syntax method definitions deprecated in Scala 3.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.MethodSignature.TypedParameterWithoutParen", "Typed parameter without parenthesis in functional literal", "Scala/Method signature",
                "Reports typed parameters in function literals that lack enclosing parentheses.",
                HighlightSeverity.NO_HIGHLIGHTING, true, "Scala");
    }

    private void initScalaPlayAndPropertiesInspections() {
        // Scala/Play ([✓] 4 tools, all Warning, all enabled matching media_1790826033450.png)
        addTool("Scala.Play.BadFileNameInspection", "Play file name inspection", "Scala/Play",
                "Validates Play template and route file names to ensure they are valid Scala identifiers.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Play.RoutingActionInspection", "Play Routing action inspection", "Scala/Play",
                "Validates Play framework route action methods, parameter counts, and signatures.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Play.RoutingUrlClashInspection", "Play Routing URL clash inspection", "Scala/Play",
                "Reports conflicting route URLs in Play framework routes files.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Play.UnresolvedResource", "Play unresolved resource inspection", "Scala/Play",
                "Reports unresolved static resources and assets in Play framework templates and route files.",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Properties files ([✓] 1 tool, Error !, enabled matching media_1790826033450.png)
        addTool("Scala.PropertiesFiles.InvalidPropertyKey", "Invalid property key", "Scala/Properties files",
                "Reports unresolved or invalid property keys in Scala property bundles and i18n references.",
                HighlightSeverity.ERROR, true, "Scala");

        // Scala/Resource leaks ([✓] 1 tool, Warning ▲, enabled matching media_1790826033450.png)
        addTool("Scala.ResourceLeaks.SourceNotClosed", "Source is not closed", "Scala/Resource leaks",
                "Reports scala.io.Source instances that are created but not closed.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaScaladocAndSpecs2Inspections() {
        // Scala/Scaladoc ([✓] 7 tools, all Warning, all enabled matching media_1790826033450.png)
        addTool("Scala.Scaladoc.HeaderTagsUnbalanced", "Header tags unbalanced", "Scala/Scaladoc",
                "Reports unbalanced = header markup tags in Scaladoc comments.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.InlinedTag", "Inlined tag", "Scala/Scaladoc",
                "Reports incorrectly placed inline tags in Scaladoc comments.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.MissingTagParameterDescription", "Missing tag parameter description", "Scala/Scaladoc",
                "Reports Scaladoc @param or @tparam tags lacking parameter descriptions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.TagUnclosed", "Tag unclosed", "Scala/Scaladoc",
                "Reports unclosed HTML or Scaladoc formatting tags.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.UnknownParameter", "Unknown parameter", "Scala/Scaladoc",
                "Reports @param or @tparam tags in Scaladoc comments that do not correspond to any method parameter.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.UnknownTag", "Unknown tag", "Scala/Scaladoc",
                "Reports unrecognized @ tags in Scaladoc comments.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Scaladoc.WrongScaladocElement", "Wrong scaladoc element", "Scala/Scaladoc",
                "Reports syntax and parser errors in Scaladoc comments.",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Specs2 ([✓] 1 tool, Warning ▲, enabled matching media_1790826063653.png)
        addTool("Scala.Specs2.Specs2Matchers", "Specs2 matchers", "Scala/Specs2",
                "Validates specs2 matcher usages, assertion structures, and verification blocks.",
                HighlightSeverity.WARNING, true, "Scala");
    }

    private void initScalaSyntacticAndWorksheetInspections() {
        // Scala/Syntactic clarification ([-] 2 tools: 1 enabled, 1 disabled matching media_1790826063653.png)
        addTool("Scala.SyntacticClarification.AutoTupling", "Auto-tupling", "Scala/Syntactic clarification",
                "Reports implicit auto-tupling conversions where multiple arguments are converted into a single Tuple.",
                HighlightSeverity.WARNING, false, "Scala");
        addTool("Scala.SyntacticClarification.ConvertNullInitializerToUnderscore", "Null initializer can be replaced by _", "Scala/Syntactic clarification",
                "Suggests replacing explicit null initialization of var fields with default wildcard _ initialization.",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Syntactic simplification ([✓] 8 tools, all Warning, all enabled matching media_1790826063653.png)
        addTool("Scala.SyntacticSimplification.ConvertibleToMethodValue", "Anonymous function convertible to a method value", "Scala/Syntactic simplification",
                "Suggests replacing anonymous functions like (x => foo(x)) with method values (foo _ or foo).",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.AppliedTypeLambdaCanBeSimplified", "Applied Type Lambda can be simplified", "Scala/Syntactic simplification",
                "Suggests simplifying applied type lambdas in Scala type expressions.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.RedundantDefaultArgument", "Argument duplicates corresponding parameter default value", "Scala/Syntactic simplification",
                "Reports explicit method call arguments that match the default parameter values.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.PostfixUnaryOperation", "Postfix unary operation", "Scala/Syntactic simplification",
                "Reports postfix unary operations that can lead to subtle operator precedence bugs.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.RedundantNewOnCaseClass", "Redundant new on case class", "Scala/Syntactic simplification",
                "Suggests removing redundant 'new' keywords when instantiating case classes.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.RemoveRedundantReturn", "Redundant return", "Scala/Syntactic simplification",
                "Reports redundant 'return' statements at the end of method bodies.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.ScalaUnnecessarySemicolon", "Scala unnecessary semicolon inspection", "Scala/Syntactic simplification",
                "Reports redundant semicolons at line endings in Scala source code.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.SyntacticSimplification.FunctionTupleSyntacticSugar", "Syntactic sugar", "Scala/Syntactic simplification",
                "Suggests using idiomatic syntactic sugar for tuples and function types (e.g. (A, B) instead of Tuple2[A, B]).",
                HighlightSeverity.WARNING, true, "Scala");

        // Scala/Worksheet ([✓] 2 tools: 1 Warning, 1 Error, all enabled matching media_1790826063653.png)
        addTool("Scala.Worksheet.AmmoniteUnresolvedLibrary", "Ammonite unresolved import", "Scala/Worksheet",
                "Reports unresolved $ivy dependencies or imports in Ammonite scripts.",
                HighlightSeverity.WARNING, true, "Scala");
        addTool("Scala.Worksheet.WorksheetPackageDeclaration", "Worksheet package declaration", "Scala/Worksheet",
                "Reports package declarations inside worksheets which are not allowed.",
                HighlightSeverity.ERROR, true, "Scala");
    }
}

