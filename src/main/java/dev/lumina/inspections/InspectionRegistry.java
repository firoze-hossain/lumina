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

        // 33. Java EE ([-])
        addTool("JavaEE.ServletMapping", "Invalid servlet mapping", "Java EE",
                "Validates servlet url-pattern mappings in web.xml.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JavaEE.EjbBinding", "Unresolved EJB reference", "Java EE",
                "Reports EJB lookup references that cannot be resolved in JNDI context.",
                HighlightSeverity.WARNING, false, "Java");

        // 34. JavaFX ([-])
        addTool("JavaFX.RedundantPropertyValue", "JavaFX redundant property values", "JavaFX",
                "Reports FXML property values that match default control values.",
                HighlightSeverity.WEAK_WARNING, true, "FXML");
        addTool("JavaFX.UnusedImport", "JavaFX unused imports", "JavaFX",
                "Reports unused type imports in FXML documents.",
                HighlightSeverity.WARNING, false, "FXML");

        // 35. JavaScript and TypeScript ([-])
        addTool("JS.UnusedVariable", "Unused JavaScript variable", "JavaScript and TypeScript",
                "Reports variables that are declared but never read.",
                HighlightSeverity.WARNING, true, "JavaScript");
        addTool("JS.ExplicitAny", "Usage of explicit 'any' type", "JavaScript and TypeScript",
                "Reports TypeScript 'any' type annotations where specific types can be inferred.",
                HighlightSeverity.WEAK_WARNING, false, "TypeScript");

        // 36. JPA ([-])
        addTool("JPA.EntityIdentifier", "Entity without primary key", "JPA",
                "Reports JPA entity classes missing an @Id or @EmbeddedId attribute.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("JPA.QueryCheck", "JPA Query parameter mismatch", "JPA",
                "Reports named parameters in query string that do not match setParameter calls.",
                HighlightSeverity.WARNING, false, "Java");

        // 37. JRuby ([✓])
        addTool("JRuby.JavaInterop", "JRuby Java interop problem", "JRuby",
                "Reports invalid Java class calls and package references in JRuby code.",
                HighlightSeverity.WARNING, true, "Ruby");

        // 38. JSON and JSON5 ([✓])
        addTool("JSON.SyntaxError", "JSON syntax error", "JSON and JSON5",
                "Reports syntax errors, missing commas, and unexpected tokens in JSON documents.",
                HighlightSeverity.ERROR, true, "JSON");

        // 39. JSONPath ([✓])
        addTool("JSONPath.SyntaxValidation", "Invalid JSONPath syntax", "JSONPath",
                "Validates JSONPath queries and filter expressions.",
                HighlightSeverity.ERROR, true, "JSONPath");

        // 40. JSP ([✓])
        addTool("JSP.DirectiveSyntax", "JSP directive syntax", "JSP",
                "Validates JSP page and taglib directives.",
                HighlightSeverity.ERROR, true, "JSP");

        // 41. JUnit ([✓])
        addTool("JUnit.MalformedTestMethod", "Malformed test method", "JUnit",
                "Reports test methods that are static, private, or have return values.",
                HighlightSeverity.WARNING, true, "Java");

        // 42. JVM languages ([✓])
        addTool("JVM.SignatureClash", "Bridge method signature clash", "JVM languages",
                "Reports conflicting bytecode signatures in multi-language projects.",
                HighlightSeverity.ERROR, true, "JVM");

        // 43. Kotlin ([-])
        addTool("Kotlin.RedundantNullCheck", "Redundant null check", "Kotlin",
                "Reports unnecessary null checks on non-nullable Kotlin expressions.",
                HighlightSeverity.WEAK_WARNING, true, "Kotlin");
        addTool("Kotlin.UnusedSymbol", "Unused Kotlin symbol", "Kotlin",
                "Reports Kotlin functions, classes, and properties that are never used.",
                HighlightSeverity.WARNING, false, "Kotlin");

        // 44. Ktor ([✓])
        addTool("Ktor.RoutingConflict", "Conflicting routing path", "Ktor",
                "Reports conflicting or overlapping endpoint route definitions in Ktor applications.",
                HighlightSeverity.WARNING, true, "Kotlin");

        // 45. Kubernetes ([✓])
        addTool("Kubernetes.ResourceValidation", "Kubernetes manifest validation", "Kubernetes",
                "Validates Kubernetes resource YAML against official schemas.",
                HighlightSeverity.ERROR, true, "YAML");

        // 46. Language injection ([✓])
        addTool("LangInjection.SyntaxError", "Injected language syntax error", "Language injection",
                "Reports syntax errors inside injected language fragments (SQL, RegExp, HTML, etc.).",
                HighlightSeverity.ERROR, true, "General");

        // 47. Less ([✓])
        addTool("Less.VariableReference", "Unresolved Less variable", "Less",
                "Reports Less variables that are referenced without declaration.",
                HighlightSeverity.ERROR, true, "Less");

        // 48. Liquibase ([✓])
        addTool("Liquibase.ChangeSetId", "Duplicate changeset id", "Liquibase",
                "Reports changelog files containing duplicate changeSet IDs.",
                HighlightSeverity.ERROR, true, "XML");

        // 49. Manifest ([✓])
        addTool("Manifest.BundleHeader", "Invalid OSGi / JAR manifest header", "Manifest",
                "Validates MANIFEST.MF header syntax and exported package formats.",
                HighlightSeverity.WARNING, true, "Manifest");

        // 50. Markdown ([✓])
        addTool("Markdown.BrokenLink", "Broken Markdown link", "Markdown",
                "Reports references to local files or headers that do not exist.",
                HighlightSeverity.WARNING, true, "Markdown");

        // 51. Maven ([✓])
        addTool("Maven.DuplicateDependency", "Duplicate dependency declaration", "Maven",
                "Reports dependencies declared multiple times in pom.xml.",
                HighlightSeverity.WARNING, true, "Maven");

        // 52. Micronaut ([✓])
        addTool("Micronaut.EndpointValidation", "Invalid Micronaut endpoint mapping", "Micronaut",
                "Validates Micronaut controller URI templates and parameter bindings.",
                HighlightSeverity.WARNING, true, "Java");

        // 53. MongoDB ([✓])
        addTool("MongoDB.QuerySyntax", "MongoDB shell query syntax", "MongoDB",
                "Validates MongoDB aggregation pipelines and JSON query documents.",
                HighlightSeverity.ERROR, true, "JSON");

        // 54. MySQL ([✓])
        addTool("MySQL.SyntaxError", "MySQL dialect syntax error", "MySQL",
                "Reports syntax errors specific to MySQL dialect.",
                HighlightSeverity.ERROR, true, "SQL");

        // 55. OpenAPI specifications ([✓]) - Selected in screenshot
        addTool("OpenAPI.SchemaValidation", "OpenAPI schema validation", "OpenAPI specifications",
                "Validates OpenAPI / Swagger 3.x specification documents against official schemas.",
                HighlightSeverity.ERROR, true, "YAML");
        addTool("OpenAPI.MissingResponse", "Missing response code", "OpenAPI specifications",
                "Reports operations missing success (2xx) or default response codes.",
                HighlightSeverity.WARNING, true, "YAML");

        // 56. Oracle ([✓])
        addTool("Oracle.PLSQLSyntax", "Oracle PL/SQL syntax validation", "Oracle",
                "Validates PL/SQL packages, procedures, and trigger definitions.",
                HighlightSeverity.ERROR, true, "SQL");

        // 57. Pandas ([✓])
        addTool("Pandas.DeprecatedMethod", "Deprecated Pandas API", "Pandas",
                "Reports calls to deprecated methods in Pandas dataframes.",
                HighlightSeverity.WARNING, true, "Python");

        // 58. Pattern validation ([✓])
        addTool("PatternValidation.SyntaxError", "Pattern validation syntax", "Pattern validation",
                "Validates search and replace structural pattern templates.",
                HighlightSeverity.ERROR, true, "General");

        // 59. PHP ([-])
        addTool("PHP.UndefinedVariable", "Undefined PHP variable", "PHP",
                "Reports variables referenced before assignment in PHP scripts.",
                HighlightSeverity.WARNING, true, "PHP");
        addTool("PHP.DocblockType", "PHPDoc type mismatch", "PHP",
                "Reports inconsistencies between PHPDoc comments and method signatures.",
                HighlightSeverity.WEAK_WARNING, false, "PHP");

        // 60. PostCSS ([✓])
        addTool("PostCSS.PluginSyntax", "PostCSS plugin syntax", "PostCSS",
                "Validates PostCSS custom directives and nesting rules.",
                HighlightSeverity.WARNING, true, "CSS");

        // 61. PostgreSQL ([✓])
        addTool("PostgreSQL.SyntaxValidation", "PostgreSQL syntax validation", "PostgreSQL",
                "Validates queries and stored functions against PostgreSQL syntax.",
                HighlightSeverity.ERROR, true, "SQL");

        // 62. Proofreading ([-])
        addTool("Proofreading.Typo", "Typo in identifier or comment", "Proofreading",
                "Reports misspelled words in code identifiers, strings, and comments.",
                HighlightSeverity.TYPO, true, "General");
        addTool("Proofreading.Grammar", "Grammar error", "Proofreading",
                "Checks sentence structure and grammatical correctness in comments.",
                HighlightSeverity.GRAMMAR_ERROR, false, "General");

        // 63. Properties files ([✓])
        addTool("Properties.UnusedProperty", "Unused property key", "Properties files",
                "Reports property keys defined in .properties files that are never referenced.",
                HighlightSeverity.WARNING, true, "Properties");

        // 64. Protocol Buffers ([✓])
        addTool("Protobuf.FieldNumber", "Duplicate field number", "Protocol Buffers",
                "Reports duplicate field tag numbers within the same protobuf message.",
                HighlightSeverity.ERROR, true, "Protobuf");

        // 65. Python ([-])
        addTool("Python.UnresolvedReference", "Unresolved Python reference", "Python",
                "Reports variable, function, or module names that cannot be resolved.",
                HighlightSeverity.WARNING, true, "Python");
        addTool("Python.TypeCheck", "Type annotation mismatch", "Python",
                "Reports arguments that do not match expected PEP 484 type annotations.",
                HighlightSeverity.WARNING, false, "Python");

        // 66. Qodana ([ ]) - Unchecked
        addTool("Qodana.SanityCheck", "Qodana configuration sanity check", "Qodana",
                "Validates qodana.yaml inspection profiles and baseline paths.",
                HighlightSeverity.WARNING, false, "YAML");

        // 67. Quarkus ([-])
        addTool("Quarkus.ConfigProperty", "Unresolved Quarkus config property", "Quarkus",
                "Reports @ConfigProperty keys missing in application.properties.",
                HighlightSeverity.WARNING, true, "Java");
        addTool("Quarkus.PanacheQuery", "Panache entity query check", "Quarkus",
                "Validates HQL queries in PanacheEntity repository methods.",
                HighlightSeverity.WARNING, false, "Java");

        // 68. RBS ([-])
        addTool("RBS.TypeDefinition", "RBS type definition problem", "RBS",
                "Validates Ruby signature definitions in RBS files.",
                HighlightSeverity.ERROR, true, "Ruby");
        addTool("RBS.UnusedType", "Unused RBS type alias", "RBS",
                "Reports type aliases in RBS files that are never referenced.",
                HighlightSeverity.WEAK_WARNING, false, "Ruby");

        // 69. RegExp ([-])
        addTool("RegExp.SyntaxError", "Regular expression syntax error", "RegExp",
                "Reports syntax errors, unescaped characters, and invalid ranges in regex patterns.",
                HighlightSeverity.ERROR, true, "RegExp");
        addTool("RegExp.RedundantEscape", "Redundant character escape", "RegExp",
                "Reports unnecessary backslash escape sequences in regular expressions.",
                HighlightSeverity.WEAK_WARNING, false, "RegExp");

        // 70. RELAX NG ([✓])
        addTool("RelaxNG.PatternSyntax", "RELAX NG grammar validation", "RELAX NG",
                "Validates XML documents against RELAX NG compact or XML schemas.",
                HighlightSeverity.ERROR, true, "XML");

        // 71. Requirements ([✓])
        addTool("Requirements.PackageVersion", "Unresolved package requirement", "Requirements",
                "Reports packages in requirements.txt that cannot be satisfied in current environment.",
                HighlightSeverity.WARNING, true, "Python");

        // 72. RESTful Web Service (JAX-RS) ([✓])
        addTool("JaxRs.HttpMethod", "Missing HTTP method annotation", "RESTful Web Service (JAX-RS)",
                "Reports resource methods with path mapping missing @GET, @POST, etc.",
                HighlightSeverity.WARNING, true, "Java");

        // 73. Ruby ([-])
        addTool("Ruby.UnusedLocalVariable", "Unused Ruby local variable", "Ruby",
                "Reports local variables in Ruby blocks that are never used.",
                HighlightSeverity.WARNING, true, "Ruby");
        addTool("Ruby.StyleGuide", "Ruby style guideline violation", "Ruby",
                "Checks adherence to standard community Ruby formatting conventions.",
                HighlightSeverity.WEAK_WARNING, false, "Ruby");

        // 74. Rust ([✓])
        addTool("Rust.MoveError", "Use of moved value", "Rust",
                "Reports borrowing and ownership errors where a value is used after being moved.",
                HighlightSeverity.ERROR, true, "Rust");

        // 75. Sass/SCSS ([✓])
        addTool("Sass.VariableScope", "Unresolved Sass variable", "Sass/SCSS",
                "Reports variables in SCSS files that are not in scope.",
                HighlightSeverity.ERROR, true, "SCSS");

        // 76. sbt ([✓])
        addTool("Sbt.SettingKey", "Unresolved sbt setting key", "sbt",
                "Validates build.sbt setting expressions and dependency keys.",
                HighlightSeverity.WARNING, true, "Scala");

        // 77. Scala ([-]) - Selected in screenshot
        addTool("Scala.TypeMismatch", "Scala expression type mismatch", "Scala",
                "Reports compile-time type incompatibilities in Scala expressions.",
                HighlightSeverity.ERROR, true, "Scala");
        addTool("Scala.UnusedImport", "Unused Scala import", "Scala",
                "Reports unused package and class imports in Scala sources.",
                HighlightSeverity.WARNING, false, "Scala");

        // 78. Security ([-])
        addTool("Security.SqlInjection", "Potential SQL injection vulnerability", "Security",
                "Reports string concatenation used to construct dynamic SQL queries from untrusted input.",
                HighlightSeverity.ERROR, true, "Security");
        addTool("Security.InsecureAlgorithm", "Use of weak cryptographic algorithm", "Security",
                "Reports usage of MD5, DES, or SHA-1 for cryptographic security purposes.",
                HighlightSeverity.WARNING, false, "Security");

        // 79. Shell script ([✓])
        addTool("Shell.UnquotedVariable", "Unquoted variable expansion", "Shell script",
                "Reports variable expansions in Bash scripts that should be quoted to prevent word splitting.",
                HighlightSeverity.WARNING, true, "Shell");

        // 80. Spring ([-])
        addTool("Spring.AutowiredDependency", "Unsatisfied @Autowired bean dependency", "Spring",
                "Reports Spring bean dependencies that cannot be resolved in application context.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("Spring.RequestMapping", "Duplicate URL mapping in Spring controllers", "Spring",
                "Reports conflicting route mappings in @RequestMapping and @GetMapping annotations.",
                HighlightSeverity.WARNING, false, "Java");

        // 81. Spring Data ([-])
        addTool("SpringData.MethodName", "Invalid Spring Data query method name", "Spring Data",
                "Validates derived query method names against entity property names.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("SpringData.CustomImplementation", "Missing custom repository implementation", "Spring Data",
                "Reports custom repository interfaces missing required implementation classes.",
                HighlightSeverity.WARNING, false, "Java");

        // 82. Spring Modulith ([-])
        addTool("SpringModulith.ModuleViolation", "Application module boundary violation", "Spring Modulith",
                "Reports internal package references between segregated Spring Modulith components.",
                HighlightSeverity.ERROR, true, "Java");
        addTool("SpringModulith.EventExternalization", "Unpublished internal domain event", "Spring Modulith",
                "Reports domain events intended for external publication lacking appropriate annotations.",
                HighlightSeverity.WARNING, false, "Java");

        // 83. SQL ([-])
        addTool("SQL.SyntaxError", "SQL dialect syntax error", "SQL",
                "Reports syntax errors, unresolved tables, and column mismatches in SQL queries.",
                HighlightSeverity.ERROR, true, "SQL");
        addTool("SQL.MissingJoinCondition", "Missing JOIN condition", "SQL",
                "Reports Cartesian product joins resulting from missing ON conditions.",
                HighlightSeverity.WARNING, false, "SQL");

        // 84. SQL server ([✓])
        addTool("SqlServer.TsqlSyntax", "T-SQL syntax validation", "SQL server",
                "Validates Microsoft SQL Server specific T-SQL statements and table hints.",
                HighlightSeverity.ERROR, true, "SQL");

        // 85. Thymeleaf ([✓])
        addTool("Thymeleaf.ExpressionSyntax", "Thymeleaf standard expression syntax", "Thymeleaf",
                "Validates Thymeleaf ${...}, *{...}, and #{...} expressions in HTML templates.",
                HighlightSeverity.ERROR, true, "HTML");

        // 86. TOML ([✓])
        addTool("TOML.SyntaxValidation", "TOML syntax error", "TOML",
                "Reports syntax errors, duplicate table keys, and malformed values in TOML files.",
                HighlightSeverity.ERROR, true, "TOML");

        // 87. Velocity ([✓])
        addTool("Velocity.DirectiveValidation", "Velocity VTL directive syntax", "Velocity",
                "Validates Apache Velocity template language directives and variable interpolations.",
                HighlightSeverity.ERROR, true, "Velocity");

        // 88. Version control ([✓])
        addTool("Vcs.CommitMessage", "Empty or malformed commit message", "Version control",
                "Checks commit messages against configured length and format conventions.",
                HighlightSeverity.WARNING, true, "General");

        // 89. Vue ([-])
        addTool("Vue.ComponentBinding", "Invalid Vue component template binding", "Vue",
                "Reports unresolved property references in Vue single file components.",
                HighlightSeverity.WARNING, true, "Vue");
        addTool("Vue.UnusedComponent", "Unused local Vue component", "Vue",
                "Reports components registered in Vue file that are not used in template.",
                HighlightSeverity.WEAK_WARNING, false, "Vue");

        // 90. XML ([-])
        addTool("XML.WellFormedness", "XML well-formedness error", "XML",
                "Reports XML syntax errors, mismatched start/end tags, and unclosed elements.",
                HighlightSeverity.ERROR, true, "XML");
        addTool("XML.UnusedNamespace", "Unused XML namespace declaration", "XML",
                "Reports xmlns namespace prefixes that are declared but never used in document.",
                HighlightSeverity.WARNING, false, "XML");

        // 91. XPath ([✓])
        addTool("XPath.ExpressionSyntax", "XPath expression syntax", "XPath",
                "Reports syntax errors and invalid axis specifiers in XPath expressions.",
                HighlightSeverity.ERROR, true, "XPath");

        // 92. XSLT ([✓])
        addTool("XSLT.TemplateMatch", "XSLT template match validation", "XSLT",
                "Validates XSLT transformation template matches and output definitions.",
                HighlightSeverity.ERROR, true, "XML");

        // 93. YAML ([✓])
        addTool("YAML.IndentationError", "YAML syntax and indentation error", "YAML",
                "Reports indentation errors, bad tab characters, and malformed keys in YAML files.",
                HighlightSeverity.ERROR, true, "YAML");
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
}
