package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class GemMetadataTest {

    @Test
    void testToRubyModuleName() {
        assertEquals("Untitled1", GemMetadata.toRubyModuleName("untitled1"));
        assertEquals("MyGem", GemMetadata.toRubyModuleName("my_gem"));
        assertEquals("Foo::Bar", GemMetadata.toRubyModuleName("foo-bar"));
        assertEquals("FooBar::Baz", GemMetadata.toRubyModuleName("foo_bar-baz"));
        assertEquals("Foo::Bar::Baz", GemMetadata.toRubyModuleName("foo-bar-baz"));
        assertEquals("Untitled1", GemMetadata.toRubyModuleName(""));
        assertEquals("Untitled1", GemMetadata.toRubyModuleName(null));
        assertEquals("Untitled1", GemMetadata.toRubyModuleName("   "));
    }

    @Test
    void testToFlatModuleName() {
        assertEquals("Untitled1", GemMetadata.toFlatModuleName("untitled1"));
        assertEquals("MyGem", GemMetadata.toFlatModuleName("my_gem"));
        assertEquals("FooBar", GemMetadata.toFlatModuleName("foo-bar"));
        assertEquals("FooBarBaz", GemMetadata.toFlatModuleName("foo_bar-baz"));
    }

    @Test
    void testGenerateLibFile() {
        String code = GemMetadata.generateLibFile("my_gem", "MyGem");
        assertTrue(code.contains("require_relative \"my_gem/version\""));
        assertTrue(code.contains("module MyGem"));
        assertTrue(code.contains("class Error < StandardError; end"));

        String nestedCode = GemMetadata.generateLibFile("foo-bar", "Foo::Bar");
        assertTrue(nestedCode.contains("module Foo"));
        assertTrue(nestedCode.contains("module Bar"));
    }

    @Test
    void testGenerateVersionFile() {
        String code = GemMetadata.generateVersionFile("MyGem");
        assertTrue(code.contains("module MyGem"));
        assertTrue(code.contains("VERSION = \"0.1.0\""));

        String nestedCode = GemMetadata.generateVersionFile("Foo::Bar");
        assertTrue(nestedCode.contains("module Foo"));
        assertTrue(nestedCode.contains("module Bar"));
        assertTrue(nestedCode.contains("VERSION = \"0.1.0\""));
    }

    @Test
    void testGenerateGemspec() {
        String gemspec = GemMetadata.generateGemspec("demo_gem", "DemoGem", "Alice", "alice@example.com", false);
        assertTrue(gemspec.contains("spec.name = \"demo_gem\""));
        assertTrue(gemspec.contains("spec.version = DemoGem::VERSION"));
        assertTrue(gemspec.contains("spec.authors = [\"Alice\"]"));
        assertTrue(gemspec.contains("spec.email = [\"alice@example.com\"]"));
        assertFalse(gemspec.contains("spec.extensions ="));

        String gemspecWithExt = GemMetadata.generateGemspec("demo_gem", "DemoGem", "Alice", "alice@example.com", true);
        assertTrue(gemspecWithExt.contains("spec.extensions = [\"ext/demo_gem/extconf.rb\"]"));
    }

    @Test
    void testGenerateGemfile() {
        String gemfileMinitest = GemMetadata.generateGemfile("demo_gem", "minitest");
        assertTrue(gemfileMinitest.contains("gemspec"));
        assertTrue(gemfileMinitest.contains("gem \"minitest\""));
        assertFalse(gemfileMinitest.contains("gem \"rspec\""));

        String gemfileRspec = GemMetadata.generateGemfile("demo_gem", "rspec");
        assertTrue(gemfileRspec.contains("gem \"rspec\""));
        assertFalse(gemfileRspec.contains("gem \"minitest\""));
    }

    @Test
    void testGenerateRakefile() {
        String rakefileMinitest = GemMetadata.generateRakefile("minitest", false);
        assertTrue(rakefileMinitest.contains("Rake::TestTask.new(:test)"));
        assertTrue(rakefileMinitest.contains("task default: :test"));

        String rakefileRspec = GemMetadata.generateRakefile("rspec", false);
        assertTrue(rakefileRspec.contains("RSpec::Core::RakeTask.new(:spec)"));
        assertTrue(rakefileRspec.contains("task default: :spec"));
    }

    @Test
    void testGenerateMinitestFiles() {
        String helper = GemMetadata.generateMinitestHelper("demo");
        assertTrue(helper.contains("require \"demo\""));
        assertTrue(helper.contains("require \"minitest/autorun\""));

        String test = GemMetadata.generateMinitestTest("demo_gem", "DemoGem");
        assertTrue(test.contains("class TestDemoGem < Minitest::Test"));
        assertTrue(test.contains("refute_nil ::DemoGem::VERSION"));
    }

    @Test
    void testGenerateRspecFiles() {
        String dotRspec = GemMetadata.generateRspecDotFile();
        assertEquals("--require spec_helper\n", dotRspec);

        String helper = GemMetadata.generateRspecHelper("demo");
        assertTrue(helper.contains("require \"demo\""));
        assertTrue(helper.contains("RSpec.configure do |config|"));

        String spec = GemMetadata.generateRspecSpec("DemoGem");
        assertTrue(spec.contains("RSpec.describe DemoGem do"));
        assertTrue(spec.contains("expect(DemoGem::VERSION).not_to be nil"));
    }

    @Test
    void testGenerateCodeOfConduct() {
        String coc = GemMetadata.generateCodeOfConduct("test@example.com");
        assertTrue(coc.contains("Contributor Covenant Code of Conduct"));
        assertTrue(coc.contains("test@example.com"));
    }

    @Test
    void testGenerateMitLicense() {
        String license = GemMetadata.generateMitLicense("Test Developer");
        assertTrue(license.contains("MIT License"));
        assertTrue(license.contains(String.valueOf(Year.now().getValue())));
        assertTrue(license.contains("Test Developer"));
    }

    @Test
    void testGenerateExecutable() {
        String exe = GemMetadata.generateExecutable("demo_gem");
        assertTrue(exe.startsWith("#!/usr/bin/env ruby"));
        assertTrue(exe.contains("require \"demo_gem\""));
    }

    @Test
    void testGenerateCExtensionFiles() {
        String extconf = GemMetadata.generateExtconfRb("demo_gem");
        assertTrue(extconf.contains("create_makefile(\"demo_gem/demo_gem\")"));

        String header = GemMetadata.generateCExtensionHeader("demo_gem");
        assertTrue(header.contains("#ifndef DEMO_GEM_H"));
        assertTrue(header.contains("#include \"ruby.h\""));

        String source = GemMetadata.generateCExtensionSource("demo_gem", "DemoGem");
        assertTrue(source.contains("#include \"demo_gem.h\""));
        assertTrue(source.contains("Init_demo_gem(void)"));
        assertTrue(source.contains("rb_define_module(\"DemoGem\")"));
    }

    @Test
    void testGenerateReadme() {
        String readme = GemMetadata.generateReadme("demo_gem", "DemoGem", "minitest");
        assertTrue(readme.contains("# DemoGem"));
        assertTrue(readme.contains("rake test"));

        String readmeRspec = GemMetadata.generateReadme("demo_gem", "DemoGem", "rspec");
        assertTrue(readmeRspec.contains("rake spec"));
    }

    @Test
    void testGenerateGitignore() {
        String gitignore = GemMetadata.generateGitignore();
        assertTrue(gitignore.contains("/.bundle/"));
        assertTrue(gitignore.contains("*.gem"));
        assertTrue(gitignore.contains(".idea/"));
        assertTrue(gitignore.contains(".rspec_status"));
    }
}
