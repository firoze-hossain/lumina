package dev.lumina.project;

import java.time.Year;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service providing dynamic Ruby Gem naming, configuration, and templates
 * matching IntelliJ IDEA and Bundler.
 */
public final class GemMetadata {

    private GemMetadata() {
    }

    /**
     * Converts a gem name (e.g. "untitled1", "my_gem", "foo-bar", "foo_bar-baz")
     * into a canonical Ruby module name (e.g. "Untitled1", "MyGem", "Foo::Bar", "FooBar::Baz").
     */
    public static String toRubyModuleName(String gemName) {
        if (gemName == null || gemName.isBlank()) {
            return "Untitled1";
        }
        String clean = gemName.trim();
        String[] parts = clean.split("-");
        return Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .map(GemMetadata::camelCase)
                .collect(Collectors.joining("::"));
    }

    /**
     * Returns a flat identifier without "::", suitable for C function names and test class names.
     */
    public static String toFlatModuleName(String gemName) {
        String mod = toRubyModuleName(gemName);
        return mod.replace("::", "");
    }

    private static String camelCase(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generates lib/<name>.rb with nested module structure matching Bundler.
     */
    public static String generateLibFile(String name, String moduleName) {
        String[] modules = moduleName.split("::");
        StringBuilder sb = new StringBuilder();
        sb.append("# frozen_string_literal: true\n\n");
        sb.append("require_relative \"").append(name).append("/version\"\n\n");
        for (int i = 0; i < modules.length; i++) {
            sb.append("  ".repeat(i)).append("module ").append(modules[i]).append("\n");
        }
        int indent = modules.length;
        sb.append("  ".repeat(indent)).append("class Error < StandardError; end\n");
        sb.append("  ".repeat(indent)).append("# Your code goes here...\n");
        for (int i = modules.length - 1; i >= 0; i--) {
            sb.append("  ".repeat(i)).append("end\n");
        }
        return sb.toString();
    }

    /**
     * Generates lib/<name>/version.rb.
     */
    public static String generateVersionFile(String moduleName) {
        String[] modules = moduleName.split("::");
        StringBuilder sb = new StringBuilder();
        sb.append("# frozen_string_literal: true\n\n");
        for (int i = 0; i < modules.length; i++) {
            sb.append("  ".repeat(i)).append("module ").append(modules[i]).append("\n");
        }
        int indent = modules.length;
        sb.append("  ".repeat(indent)).append("VERSION = \"0.1.0\"\n");
        for (int i = modules.length - 1; i >= 0; i--) {
            sb.append("  ".repeat(i)).append("end\n");
        }
        return sb.toString();
    }

    /**
     * Generates <name>.gemspec.
     */
    public static String generateGemspec(String name, String moduleName, String author, String email, boolean hasCExtension) {
        String authorName = (author != null && !author.isBlank()) ? author : System.getProperty("user.name", "TODO: Write your name");
        String emailAddr = (email != null && !email.isBlank()) ? email : "TODO: Write your email";
        String extLine = hasCExtension ? "  spec.extensions = [\"ext/" + name + "/extconf.rb\"]\n" : "";

        return """
                # frozen_string_literal: true

                require_relative "lib/%s/version"

                Gem::Specification.new do |spec|
                  spec.name = "%s"
                  spec.version = %s::VERSION
                  spec.authors = ["%s"]
                  spec.email = ["%s"]

                  spec.summary = "Write a short summary, because RubyGems requires one."
                  spec.description = "Write a longer description or delete this line."
                  spec.homepage = "https://example.com"
                  spec.license = "MIT"
                  spec.required_ruby_version = ">= 3.0.0"

                  spec.metadata["allowed_push_host"] = "TODO: Set to your gem server 'https://example.com'"

                  spec.metadata["homepage_uri"] = spec.homepage
                  spec.metadata["source_code_uri"] = "https://example.com"
                  spec.metadata["changelog_uri"] = "https://example.com"

                  # Specify which files should be added to the gem when it is released.
                  # The `git ls-files -z` loads the files in the RubyGem that have been added into git.
                  gemspec = File.basename(__FILE__)
                  spec.files = IO.popen(%%w[git ls-files -z], chdir: __dir__, err: IO::NULL) do |ls|
                    ls.readlines("\\x0", chomp: true).reject do |f|
                      (f == gemspec) ||
                        f.start_with?(*%%w[bin/ test/ spec/ features/ .git appveyor Gemfile])
                    end
                  end
                  spec.bindir = "exe"
                  spec.executables = spec.files.grep(%%r{\\Aexe/}) { |f| File.basename(f) }
                  spec.require_paths = ["lib"]
                %s
                  # Uncomment to register a new dependency of your gem
                  # spec.add_dependency "example-gem", "~> 1.0"

                  # For more information and examples about making a new gem, check out our
                  # guide at: https://bundler.io/guides/creating_gem.html
                end
                """.formatted(name, name, moduleName, authorName, emailAddr, extLine);
    }

    /**
     * Generates Gemfile.
     */
    public static String generateGemfile(String name, String testFramework) {
        boolean rspec = "rspec".equalsIgnoreCase(testFramework);
        String testDep = rspec ? "gem \"rspec\", \"~> 3.12\"" : "gem \"minitest\", \"~> 5.16\"";

        return """
                # frozen_string_literal: true

                source "https://rubygems.org"

                # Specify your gem's dependencies in %s.gemspec
                gemspec

                gem "rake", "~> 13.0"
                %s
                """.formatted(name, testDep);
    }

    /**
     * Generates Rakefile.
     */
    public static String generateRakefile(String testFramework, boolean hasCExtension) {
        boolean rspec = "rspec".equalsIgnoreCase(testFramework);
        if (rspec) {
            return """
                    # frozen_string_literal: true

                    require "bundler/gem_tasks"
                    require "rspec/core/rake_task"

                    RSpec::Core::RakeTask.new(:spec)

                    task default: :spec
                    """;
        } else {
            return """
                    # frozen_string_literal: true

                    require "bundler/gem_tasks"
                    require "rake/testtask"

                    Rake::TestTask.new(:test) do |t|
                      t.libs << "test"
                      t.libs << "lib"
                      t.test_files = FileList["test/**/test_*.rb"]
                    end

                    task default: :test
                    """;
        }
    }

    /**
     * Generates test/test_helper.rb (for Minitest).
     */
    public static String generateMinitestHelper(String name) {
        return """
                # frozen_string_literal: true

                $LOAD_PATH.unshift File.expand_path("../lib", __dir__)
                require "%s"

                require "minitest/autorun"
                """.formatted(name);
    }

    /**
     * Generates test/test_<name>.rb (for Minitest).
     */
    public static String generateMinitestTest(String name, String moduleName) {
        String testClass = "Test" + toFlatModuleName(name);
        return """
                # frozen_string_literal: true

                require "test_helper"

                class %s < Minitest::Test
                  def test_that_it_has_a_version_number
                    refute_nil ::%s::VERSION
                  end

                  def test_it_does_something_useful
                    assert false
                  end
                end
                """.formatted(testClass, moduleName);
    }

    /**
     * Generates .rspec (for RSpec).
     */
    public static String generateRspecDotFile() {
        return "--require spec_helper\n";
    }

    /**
     * Generates spec/spec_helper.rb (for RSpec).
     */
    public static String generateRspecHelper(String name) {
        return """
                # frozen_string_literal: true

                require "%s"

                RSpec.configure do |config|
                  # Enable flags like --only-failures and --next-failure
                  config.example_status_persistence_file_path = ".rspec_status"

                  # Disable RSpec exposing methods globally on `Module` and `main`
                  config.disable_monkey_patching!

                  config.expect_with :rspec do |c|
                    c.syntax = :expect
                  end
                end
                """.formatted(name);
    }

    /**
     * Generates spec/<name>_spec.rb (for RSpec).
     */
    public static String generateRspecSpec(String moduleName) {
        return """
                # frozen_string_literal: true

                RSpec.describe %s do
                  it "has a version number" do
                    expect(%s::VERSION).not_to be nil
                  end

                  it "does something useful" do
                    expect(false).to eq(true)
                  end
                end
                """.formatted(moduleName, moduleName);
    }

    /**
     * Generates Contributor Covenant Code of Conduct.
     */
    public static String generateCodeOfConduct(String contactEmail) {
        String email = (contactEmail != null && !contactEmail.isBlank()) ? contactEmail : "[INSERT CONTACT METHOD]";
        return """
                # Contributor Covenant Code of Conduct

                ## Our Pledge

                We as members, contributors, and leaders pledge to make participation in our
                community a harassment-free experience for everyone, regardless of age, body
                size, visible or invisible disability, ethnicity, sex characteristics, gender
                identity and expression, level of experience, education, socio-economic status,
                nationality, personal appearance, race, caste, color, religion, or sexual
                identity and orientation.

                We pledge to act and interact in ways that contribute to an open, welcoming,
                diverse, inclusive, and healthy community.

                ## Our Standards

                Examples of behavior that contributes to a positive environment for our
                community include:

                * Demonstrating empathy and kindness toward other people
                * Being respectful of differing opinions, viewpoints, and experiences
                * Giving and gracefully accepting constructive feedback
                * Accepting responsibility and apologizing to those affected by our mistakes,
                  and learning from the experience
                * Focusing on what is best not just for us as individuals, but for the
                  overall community

                ## Enforcement Responsibilities

                Community leaders are responsible for clarifying and enforcing our standards of
                acceptable behavior and will take appropriate and fair corrective action in
                response to any behavior that they deem inappropriate, threatening, offensive,
                or harmful.

                ## Scope

                This Code of Conduct applies within all community spaces, and also applies when
                an individual is officially representing the community in public spaces.

                ## Enforcement

                Instances of abusive, harassing, or otherwise unacceptable behavior may be
                reported to the community leaders responsible for enforcement at %s.
                All complaints will be reviewed and investigated promptly and fairly.

                ## Attribution

                This Code of Conduct is adapted from the [Contributor Covenant](https://www.contributor-covenant.org),
                version 2.1, available at
                https://www.contributor-covenant.org/version/2/1/code_of_conduct.html
                """.formatted(email);
    }

    /**
     * Generates MIT License with current year and author.
     */
    public static String generateMitLicense(String author) {
        int year = Year.now().getValue();
        String owner = (author != null && !author.isBlank()) ? author : System.getProperty("user.name", "TODO: Write your name");
        return """
                MIT License

                Copyright (c) %d %s

                Permission is hereby granted, free of charge, to any person obtaining a copy
                of this software and associated documentation files (the "Software"), to deal
                in the Software without restriction, including without limitation the rights
                to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                copies of the Software, and to permit persons to whom the Software is
                furnished to do so, subject to the following conditions:

                The above copyright notice and this permission notice shall be included in all
                copies or substantial portions of the Software.

                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                SOFTWARE.
                """.formatted(year, owner);
    }

    /**
     * Generates executable CLI stub in exe/<name>.
     */
    public static String generateExecutable(String name) {
        return """
                #!/usr/bin/env ruby
                # frozen_string_literal: true

                require "%s"

                # CLI executable entry point
                puts "Hello from %s!"
                """.formatted(name, name);
    }

    /**
     * Generates extconf.rb for C extension boilerplate.
     */
    public static String generateExtconfRb(String name) {
        return """
                # frozen_string_literal: true

                require "mkmf"

                create_makefile("%s/%s")
                """.formatted(name, name);
    }

    /**
     * Generates C header file ext/<name>/<name>.h.
     */
    public static String generateCExtensionHeader(String name) {
        String guard = name.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        return """
                #ifndef %s_H
                #define %s_H 1

                #include "ruby.h"

                #endif /* %s_H */
                """.formatted(guard, guard, guard);
    }

    /**
     * Generates C source file ext/<name>/<name>.c.
     */
    public static String generateCExtensionSource(String name, String flatModuleName) {
        return """
                #include "%s.h"

                VALUE rb_m%s;

                void
                Init_%s(void)
                {
                  rb_m%s = rb_define_module("%s");
                }
                """.formatted(name, flatModuleName, name, flatModuleName, flatModuleName);
    }

    /**
     * Generates README.md matching Bundler gem output.
     */
    public static String generateReadme(String name, String moduleName, String testFramework) {
        String testTask = "rspec".equalsIgnoreCase(testFramework) ? "rake spec" : "rake test";
        return """
                # %s

                Welcome to your new gem! In this directory, you'll find the files you need to be able to package up your Ruby library into a gem. Put your Ruby code in the file `lib/%s`. To experiment with that code, run `bin/console` for an interactive prompt.

                ## Installation

                Install the gem and add to the application's Gemfile by executing:

                    $ bundle add %s

                If bundler is not being used to manage dependencies, install the gem by executing:

                    $ gem install %s

                ## Usage

                TODO: Write usage instructions here

                ## Development

                After checking out the repo, run `bin/setup` to install dependencies. Then, run `%s` to run the tests. You can also run `bin/console` for an interactive prompt that will allow you to experiment.

                ## Contributing

                Bug reports and pull requests are welcome on GitHub at https://github.com/example/%s.
                """.formatted(moduleName, name, name, name, testTask, name);
    }

    /**
     * Generates standard .gitignore for Gem projects.
     */
    public static String generateGitignore() {
        return """
                /.bundle/
                /vendor/bundle/
                /lib/*.bundle
                /lib/*.so
                /lib/*.dll
                /lib/*.dylib
                /ext/**/*.o
                /ext/**/Makefile
                *.gem
                *.rbc
                .idea/
                .env
                .rspec_status
                """;
    }
}
