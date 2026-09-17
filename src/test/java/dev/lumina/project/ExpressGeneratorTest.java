package dev.lumina.project;

import dev.lumina.run.RunConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressGeneratorTest {

    @Test
    void testExpressMetadataEngines() {
        List<ExpressMetadata.ViewEngine> viewEngines = ExpressMetadata.getViewEngines();
        assertNotNull(viewEngines);
        assertEquals(8, viewEngines.size());

        List<String> viewNames = viewEngines.stream().map(ExpressMetadata.ViewEngine::name).toList();
        assertTrue(viewNames.contains("Dust"));
        assertTrue(viewNames.contains("EJS"));
        assertTrue(viewNames.contains("Handlebars"));
        assertTrue(viewNames.contains("Hogan.js"));
        assertTrue(viewNames.contains("Pug (Jade)"));
        assertTrue(viewNames.contains("Twig"));
        assertTrue(viewNames.contains("Vash"));
        assertTrue(viewNames.contains("None"));

        assertEquals("Pug (Jade)", ExpressMetadata.getDefaultViewEngine().name());

        List<ExpressMetadata.StylesheetEngine> stylesheetEngines = ExpressMetadata.getStylesheetEngines();
        assertNotNull(stylesheetEngines);
        assertEquals(5, stylesheetEngines.size());

        List<String> sheetNames = stylesheetEngines.stream().map(ExpressMetadata.StylesheetEngine::name).toList();
        assertTrue(sheetNames.contains("Plain CSS"));
        assertTrue(sheetNames.contains("Stylus"));
        assertTrue(sheetNames.contains("LESS"));
        assertTrue(sheetNames.contains("Compass"));
        assertTrue(sheetNames.contains("SASS"));

        assertEquals("Plain CSS", ExpressMetadata.getDefaultStylesheetEngine().name());

        assertEquals("Pug (Jade)", ExpressMetadata.findViewEngine("Pug (Jade)").name());
        assertEquals("EJS", ExpressMetadata.findViewEngine("ejs").name());
        assertEquals("Plain CSS", ExpressMetadata.findStylesheetEngine("Plain CSS").name());
        assertEquals("SASS", ExpressMetadata.findStylesheetEngine("sass").name());
    }

    @Test
    void testExpressCliDisplayFormatting() {
        String formatted = ExpressMetadata.formatCliDisplay("4.16.1");
        assertNotNull(formatted);
        assertTrue(formatted.startsWith("npx --package express-generator express"));
        assertTrue(formatted.endsWith("4.16.1"));

        List<String> versions = ExpressMetadata.fetchAllVersions(false);
        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        assertTrue(versions.contains("4.16.1"));
    }

    @Test
    void testGenerateExpressPugAndPlainCss(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-express-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EXPRESS,
                "my-express-app",
                tempDir,
                true,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "my-express-app",
                "com.example.myexpressapp",
                "21",
                "", "", "", "", "", "1.0.0", "",
                "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1",
                "React", "/usr/local/bin/node", "5.1.0", false,
                "/usr/local/bin/node", "4.16.1", "Pug (Jade)", "Plain CSS"
        );

        ProjectGenerator.generate(spec, log::add);

        // Check bin/www
        Path www = projectDir.resolve("bin/www");
        assertTrue(Files.exists(www), "bin/www should exist");
        String wwwContent = Files.readString(www);
        assertTrue(wwwContent.contains("my-express-app:server"));
        assertTrue(wwwContent.contains("http.createServer(app)"));

        // Check app.js
        Path appJs = projectDir.resolve("app.js");
        assertTrue(Files.exists(appJs), "app.js should exist");
        String appContent = Files.readString(appJs);
        assertTrue(appContent.contains("app.set('view engine', 'pug')"));
        assertTrue(appContent.contains("app.use(logger('dev'))"));
        assertTrue(appContent.contains("app.use('/', indexRouter)"));
        assertTrue(appContent.contains("app.use('/users', usersRouter)"));

        // Check routes
        assertTrue(Files.exists(projectDir.resolve("routes/index.js")));
        assertTrue(Files.exists(projectDir.resolve("routes/users.js")));

        // Check views
        assertTrue(Files.exists(projectDir.resolve("views/layout.pug")));
        assertTrue(Files.exists(projectDir.resolve("views/index.pug")));
        assertTrue(Files.exists(projectDir.resolve("views/error.pug")));

        // Check stylesheets
        assertTrue(Files.exists(projectDir.resolve("public/stylesheets/style.css")));

        // Check package.json
        Path pkgJson = projectDir.resolve("package.json");
        assertTrue(Files.exists(pkgJson));
        String pkgContent = Files.readString(pkgJson);
        assertTrue(pkgContent.contains("\"start\": \"node ./bin/www\""));
        assertTrue(pkgContent.contains("\"express\":"));
        assertTrue(pkgContent.contains("\"pug\":"));

        // Check git
        assertTrue(Files.exists(projectDir.resolve(".git")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));

        // Verify RunConfiguration detection
        List<RunConfiguration> configs = RunConfiguration.detect(projectDir);
        assertFalse(configs.isEmpty(), "Should detect npm run configurations");
        assertTrue(configs.stream().anyMatch(c -> c.label().contains("npm start")));
    }

    @Test
    void testGenerateExpressEjsAndSass(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("express-ejs-sass");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EXPRESS,
                "express-ejs-sass",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "express-ejs-sass",
                "com.example.expressejssass",
                "21",
                "", "", "", "", "", "1.0.0", "",
                "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1",
                "React", "/usr/local/bin/node", "5.1.0", false,
                "/opt/homebrew/bin/node", "4.16.1", "EJS", "SASS"
        );

        ProjectGenerator.generate(spec, log::add);

        // Check app.js
        String appContent = Files.readString(projectDir.resolve("app.js"));
        assertTrue(appContent.contains("app.set('view engine', 'ejs')"));
        assertTrue(appContent.contains("sassMiddleware"));

        // Check views
        assertTrue(Files.exists(projectDir.resolve("views/index.ejs")));
        assertTrue(Files.exists(projectDir.resolve("views/error.ejs")));

        // Check stylesheets
        assertTrue(Files.exists(projectDir.resolve("public/stylesheets/style.sass")));
        assertTrue(Files.exists(projectDir.resolve("public/stylesheets/style.css")));

        // Check package.json
        String pkgContent = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgContent.contains("\"ejs\":"));
        assertTrue(pkgContent.contains("\"node-sass-middleware\":"));
    }

    @Test
    void testGenerateExpressHandlebarsAndStylus(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("express-hbs-stylus");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EXPRESS,
                "express-hbs-stylus",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "express-hbs-stylus",
                "com.example.expresshbsstylus",
                "21",
                "", "", "", "", "", "1.0.0", "",
                "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1",
                "React", "/usr/local/bin/node", "5.1.0", false,
                "/usr/bin/node", "4.16.1", "Handlebars", "Stylus"
        );

        ProjectGenerator.generate(spec, log::add);

        // Check app.js
        String appContent = Files.readString(projectDir.resolve("app.js"));
        assertTrue(appContent.contains("app.set('view engine', 'hbs')"));
        assertTrue(appContent.contains("stylus.middleware"));

        // Check views
        assertTrue(Files.exists(projectDir.resolve("views/layout.hbs")));
        assertTrue(Files.exists(projectDir.resolve("views/index.hbs")));
        assertTrue(Files.exists(projectDir.resolve("views/error.hbs")));

        // Check stylesheets
        assertTrue(Files.exists(projectDir.resolve("public/stylesheets/style.styl")));

        // Check package.json
        String pkgContent = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgContent.contains("\"hbs\":"));
        assertTrue(pkgContent.contains("\"stylus\":"));
    }

    @Test
    void testGenerateExpressNoneViewEngineAndLess(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("express-no-view-less");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.EXPRESS,
                "express-no-view-less",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "express-no-view-less",
                "com.example.expressnoviewless",
                "21",
                "", "", "", "", "", "1.0.0", "",
                "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1",
                "React", "/usr/local/bin/node", "5.1.0", false,
                "/usr/bin/node", "4.16.1", "None", "LESS"
        );

        ProjectGenerator.generate(spec, log::add);

        // Check app.js
        String appContent = Files.readString(projectDir.resolve("app.js"));
        assertFalse(appContent.contains("app.set('view engine'"));
        assertTrue(appContent.contains("lessMiddleware"));

        // Check views: views directory should not exist when None is selected
        assertFalse(Files.exists(projectDir.resolve("views")));
        assertTrue(Files.exists(projectDir.resolve("public/index.html")));

        // Check stylesheets
        assertTrue(Files.exists(projectDir.resolve("public/stylesheets/style.less")));

        // Check package.json
        String pkgContent = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgContent.contains("\"less-middleware\":"));
    }
}
