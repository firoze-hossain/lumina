package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RubyMetadataTest {

    @Test
    void testConstants() {
        assertEquals("[No interpreter selected]", RubyMetadata.NO_INTERPRETER_LABEL);
    }

    @Test
    void testDiscovery() {
        List<RubyMetadata.RubyInstallation> list = RubyMetadata.fetchRubyInstallations(true);
        assertNotNull(list);
        List<RubyMetadata.RubyInstallation> discovered = RubyMetadata.discoverSystemRubies();
        assertNotNull(discovered);
    }

    @Test
    void testCreateInstallationFromPath() {
        RubyMetadata.RubyInstallation inst = RubyMetadata.createInstallationFromPath("/usr/bin/ruby");
        assertNotNull(inst);
        assertEquals("/usr/bin/ruby", inst.executable());
        assertNotNull(inst.version());
        assertTrue(inst.label().startsWith("ruby-"));
        assertTrue(inst.displayLabel().contains("/usr/bin/ruby"));
        assertEquals(inst.displayLabel(), inst.toString());
    }

    @Test
    void testGenerateSampleCode() {
        String code = RubyMetadata.generateSampleCode();
        assertNotNull(code);
        assertTrue(code.contains("def print_hi(name)"));
        assertTrue(code.contains("puts \"Hi, #{name}\""));
        assertTrue(code.contains("print_hi('Ruby')"));
    }

    @Test
    void testGenerateGitignore() {
        String gitignore = RubyMetadata.generateGitignore();
        assertNotNull(gitignore);
        assertTrue(gitignore.contains("/.bundle/"));
        assertTrue(gitignore.contains("/vendor/bundle/"));
        assertTrue(gitignore.contains("*.gem"));
        assertTrue(gitignore.contains("*.rbc"));
        assertTrue(gitignore.contains(".idea/"));
        assertTrue(gitignore.contains(".env"));
    }

    @Test
    void testGenerateIdeaModulesXml() {
        String modulesXml = RubyMetadata.generateIdeaModulesXml("my-ruby-app");
        assertNotNull(modulesXml);
        assertTrue(modulesXml.contains("<project version=\"4\">"));
        assertTrue(modulesXml.contains("my-ruby-app.iml"));
    }

    @Test
    void testGenerateIdeaIml() {
        String iml = RubyMetadata.generateIdeaIml();
        assertNotNull(iml);
        assertTrue(iml.contains("type=\"RUBY_MODULE\""));
        assertTrue(iml.contains("NewModuleRootManager"));
        assertTrue(iml.contains("sourceFolder"));
    }

    @Test
    void testGenerateIdeaMiscXml() {
        String miscXml = RubyMetadata.generateIdeaMiscXml("ruby-3.3.0");
        assertNotNull(miscXml);
        assertTrue(miscXml.contains("project-jdk-name=\"ruby-3.3.0\""));
        assertTrue(miscXml.contains("project-jdk-type=\"RUBY_SDK\""));
    }
}
