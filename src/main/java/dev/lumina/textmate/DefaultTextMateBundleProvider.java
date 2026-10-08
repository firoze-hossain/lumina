package dev.lumina.textmate;

import java.util.ArrayList;
import java.util.List;

/**
 * Default provider supplying the complete catalog of 62 built-in TextMate syntax bundles in Lumina IDE.
 * Dynamically discovered and registered by TextMateBundlesManager.
 */
public class DefaultTextMateBundleProvider implements TextMateBundleProvider {

    public static final List<String> BUILT_IN_BUNDLE_NAMES = List.of(
            "adoc", "bat", "bicep", "bicepparam", "clojure",
            "cmake", "coffeescript", "cpp", "csharp", "css",
            "dart", "diff", "docker", "erlang", "fsharp",
            "git-base", "go", "groovy", "handlebars", "hcl",
            "hlsl", "html", "ini", "java", "javascript",
            "json", "jsp", "julia", "kconfig", "kotlin",
            "latex", "less", "log", "lua", "make",
            "markdown-basics", "markdown-math", "mdx", "objective-c", "perl",
            "php", "powershell", "pug", "python", "r",
            "razor", "restructuredtext", "ruby", "rust", "scss",
            "search-result", "shaderlab", "shellscript", "sql", "swift",
            "terraform", "twig", "typescript-basics", "vb", "viml",
            "xml", "yaml"
    );

    @Override
    public String getProviderName() {
        return "Bundled TextMate Provider";
    }

    @Override
    public List<TextMateBundle> getBundles() {
        List<TextMateBundle> bundles = new ArrayList<>(BUILT_IN_BUNDLE_NAMES.size());
        for (String name : BUILT_IN_BUNDLE_NAMES) {
            bundles.add(TextMateBundle.builtIn(name));
        }
        return bundles;
    }
}
