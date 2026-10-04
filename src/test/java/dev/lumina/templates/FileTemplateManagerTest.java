package dev.lumina.templates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileTemplateManagerTest {

    private FileTemplateManager manager;

    @BeforeEach
    void setUp() {
        manager = FileTemplateManager.getInstance();
        manager.setCurrentScheme("Default");
    }

    @Test
    void testBuiltinTemplatesPresent() {
        List<FileTemplate> files = manager.getTemplates(FileTemplateCategory.FILES);
        assertFalse(files.isEmpty(), "Files category must not be empty");

        FileTemplate annotationType = manager.findTemplateByName("AnnotationType", FileTemplateCategory.FILES);
        assertNotNull(annotationType, "AnnotationType template must exist");
        assertTrue(annotationType.getText().contains("public @interface ${NAME}"), "AnnotationType must have @interface");
        assertTrue(annotationType.getText().contains("#parse(\"File Header.java\")"), "AnnotationType must parse File Header");

        FileTemplate enumTemplate = manager.findTemplateByName("Enum", FileTemplateCategory.FILES);
        assertNotNull(enumTemplate, "Enum template must exist");
        assertTrue(enumTemplate.getText().contains("public enum ${NAME}"), "Enum must declare public enum");

        FileTemplate groovyClass = manager.findTemplateByName("Groovy Class", FileTemplateCategory.FILES);
        assertNotNull(groovyClass, "Groovy Class template must exist");
        assertTrue(groovyClass.getText().contains("class ${NAME}"), "Groovy Class must declare class ${NAME}");
    }

    @Test
    void testIncludesCategory() {
        List<FileTemplate> includes = manager.getTemplates(FileTemplateCategory.INCLUDES);
        assertFalse(includes.isEmpty(), "Includes category must not be empty");

        FileTemplate header = manager.findTemplateByName("File Header", FileTemplateCategory.INCLUDES);
        assertNotNull(header, "File Header template must exist");
        assertTrue(header.getText().contains("Created by ${USER} on ${DATE}"), "File Header must contain user and date macro");
    }

    @Test
    void testDynamicTemplateModificationAndRevert() {
        FileTemplate template = manager.findTemplateByName("Class", FileTemplateCategory.FILES);
        assertNotNull(template);

        String original = template.getText();
        template.setText("// custom modified text\n" + original);
        assertTrue(template.isModified(), "Template should be marked modified");

        manager.revertTemplate(template);
        assertEquals(original, template.getText(), "Reverted template must match original default");
        assertFalse(template.isModified(), "Reverted template should no longer be marked modified");
    }

    @Test
    void testAddAndRemoveCustomTemplate() {
        FileTemplate custom = new FileTemplate("MyCustomTemplate", "txt", "", FileTemplateCategory.FILES,
                "Custom content ${NAME}", "Test description", "generic", false);

        manager.addTemplate(custom);
        FileTemplate found = manager.findTemplateByName("MyCustomTemplate", FileTemplateCategory.FILES);
        assertNotNull(found, "Custom template should be discoverable in manager");
        assertFalse(found.isBuiltin(), "Custom template must not be marked builtin");

        boolean removed = manager.removeTemplate(custom);
        assertTrue(removed, "Custom template should be removable");
        assertNull(manager.findTemplateByName("MyCustomTemplate", FileTemplateCategory.FILES), "Removed template should not exist");
    }

    @Test
    void testVelocityRenderingWithParseAndConditions() {
        FileTemplate annotationType = manager.findTemplateByName("AnnotationType", FileTemplateCategory.FILES);
        assertNotNull(annotationType);

        Map<String, Object> vars = Map.of(
                "PACKAGE_NAME", "com.example.annotations",
                "NAME", "MyMarker",
                "USER", "firoze"
        );

        String rendered = manager.renderTemplate(annotationType, vars);
        assertTrue(rendered.contains("package com.example.annotations;"), "Rendered text must have package statement");
        assertTrue(rendered.contains("Created by firoze"), "Rendered text must include header with expanded user");
        assertTrue(rendered.contains("public @interface MyMarker {"), "Rendered text must include public @interface MyMarker");
    }

    @Test
    void testCatchStatementBodyTemplate() {
        FileTemplate catchBody = manager.findTemplateByName("Catch Statement Body", FileTemplateCategory.CODE);
        assertNotNull(catchBody, "Catch Statement Body template must exist in Code category");
        assertEquals("throw new RuntimeException(${EXCEPTION});", catchBody.getText().trim(),
                "Catch Statement Body must match IntelliJ default");
        assertFalse(catchBody.isReformatCode(), "Reformat code should be false by default matching IntelliJ");
        assertTrue(catchBody.getVariables().containsKey("EXCEPTION"), "Must contain EXCEPTION variable");
        assertTrue(catchBody.getVariables().containsKey("EXCEPTION_TYPE"), "Must contain EXCEPTION_TYPE variable");
    }

    @Test
    void testTestNgTestMethodTemplate() {
        FileTemplate testng = manager.findTemplateByName("TestNG Test Method", FileTemplateCategory.CODE);
        assertNotNull(testng, "TestNG Test Method template must exist in Code category");
        assertTrue(testng.getText().contains("@org.testng.annotations.Test"), "Must contain TestNG annotation");
        assertTrue(testng.getText().contains("public void test${NAME}()"), "Must contain test method signature");
        assertTrue(testng.getText().contains("${BODY}"), "Must contain ${BODY}");
        assertFalse(testng.isReformatCode(), "Reformat code should be false by default");
        assertTrue(testng.getVariables().containsKey("NAME"), "Must contain NAME variable");
        assertTrue(testng.getVariables().containsKey("BODY"), "Must contain BODY variable");
    }

    @Test
    void testOtherCategoryGroupedTemplates() {
        List<FileTemplate> other = manager.getTemplates(FileTemplateCategory.OTHER);
        assertFalse(other.isEmpty(), "Other category must contain templates");

        FileTemplate pomXml = manager.findTemplateByName("pom.xml", FileTemplateCategory.OTHER);
        assertNotNull(pomXml, "pom.xml template must exist");
        assertEquals("Maven", pomXml.getGroup(), "pom.xml must belong to Maven group");
        assertEquals("xml", pomXml.getExtension(), "pom.xml extension must be xml");

        FileTemplate springConfig = manager.findTemplateByName("Spring Config", FileTemplateCategory.OTHER);
        assertNotNull(springConfig, "Spring Config must exist");
        assertEquals("Spring", springConfig.getGroup(), "Spring Config must belong to Spring group");

        FileTemplate jpaPersist = manager.findTemplateByName("persistence.xml", FileTemplateCategory.OTHER);
        assertNotNull(jpaPersist, "persistence.xml must exist");
        assertEquals("JPA", jpaPersist.getGroup(), "persistence.xml must belong to JPA group");
    }
}
