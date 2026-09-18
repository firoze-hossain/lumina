package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenArchetypeMetadataTest {

    @Test
    void testDefaultCatalogs() {
        List<MavenArchetypeMetadata.CatalogEntry> catalogs = MavenArchetypeMetadata.defaultCatalogs();
        assertNotNull(catalogs);
        assertEquals(3, catalogs.size());

        MavenArchetypeMetadata.CatalogEntry internal = catalogs.get(0);
        assertEquals("Internal", internal.name());
        assertEquals("System", internal.type());
        assertTrue(internal.isSystem());

        MavenArchetypeMetadata.CatalogEntry defaultLocal = catalogs.get(1);
        assertEquals("Default Local", defaultLocal.name());
        assertEquals("System", defaultLocal.type());
        assertTrue(defaultLocal.isSystem());
        assertNotNull(defaultLocal.location());
        assertTrue(defaultLocal.location().contains(".m2"));

        MavenArchetypeMetadata.CatalogEntry mavenCentral = catalogs.get(2);
        assertEquals("Maven Central", mavenCentral.name());
        assertEquals("System", mavenCentral.type());
        assertTrue(mavenCentral.isSystem());
        assertEquals("https://repo.maven.apache.org/maven2", mavenCentral.location());
    }

    @Test
    void testResolveDefaultLocalRepo() {
        String repo = MavenArchetypeMetadata.resolveDefaultLocalRepo();
        assertNotNull(repo);
        assertFalse(repo.isBlank());
        assertTrue(repo.contains(".m2") || repo.contains("repository"));
    }

    @Test
    void testGetArchetypesForInternalCatalog() {
        MavenArchetypeMetadata.CatalogEntry internal = new MavenArchetypeMetadata.CatalogEntry("Internal", "System", "", true);
        List<String> list = MavenArchetypeMetadata.getArchetypesForCatalog(internal);
        assertNotNull(list);
        assertEquals(10, list.size());
        assertEquals("org.apache.maven.archetypes:maven-archetype-archetype", list.get(0));
        assertEquals("org.apache.maven.archetypes:maven-archetype-j2ee-simple", list.get(1));
        assertEquals("org.apache.maven.archetypes:maven-archetype-plugin", list.get(2));
        assertTrue(list.contains("org.apache.maven.archetypes:maven-archetype-quickstart"));
        assertTrue(list.contains("org.apache.maven.archetypes:maven-archetype-webapp"));
    }

    @Test
    void testGetArchetypesForMavenCentralCatalog() {
        MavenArchetypeMetadata.CatalogEntry central = new MavenArchetypeMetadata.CatalogEntry("Maven Central", "System", "https://repo.maven.apache.org/maven2", true);
        List<String> list = MavenArchetypeMetadata.getArchetypesForCatalog(central);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        // Matches IntelliJ IDEA Image 2 exactly:
        assertEquals("ai.platon.pulsar:browser4-plugin-archetype", list.get(0));
        assertEquals("ai.raics:spark-scala-archetype_2.12", list.get(1));
        assertEquals("ai.raics:spark-scala-archetype_2.13", list.get(2));
        assertEquals("ai.wanaku.sdk:capabilities-archetypes-java-tool", list.get(3));
        assertEquals("am.ik.archetype:elm-spring-boot-blank-archetype", list.get(4));
        assertEquals("am.ik.archetype:graalvm-blank-archetype", list.get(5));
        assertTrue(list.contains("org.apache.maven.archetypes:maven-archetype-quickstart"));
    }

    @Test
    void testCatalogSwitchingProducesDistinctLists() {
        MavenArchetypeMetadata.CatalogEntry internal = new MavenArchetypeMetadata.CatalogEntry("Internal", "System", "", true);
        MavenArchetypeMetadata.CatalogEntry central = new MavenArchetypeMetadata.CatalogEntry("Maven Central", "System", "https://repo.maven.apache.org/maven2", true);

        List<String> internalList = MavenArchetypeMetadata.getArchetypesForCatalog(internal);
        List<String> centralList = MavenArchetypeMetadata.getArchetypesForCatalog(central);

        assertNotNull(internalList);
        assertNotNull(centralList);
        assertNotEquals(internalList, centralList);
        assertEquals(10, internalList.size());
        assertTrue(centralList.size() > 10);
        assertFalse(internalList.contains("ai.platon.pulsar:browser4-plugin-archetype"));
        assertTrue(centralList.contains("ai.platon.pulsar:browser4-plugin-archetype"));
    }

    @Test
    void testParseCatalogXmlContent() {
        String xml = """
                <archetype-catalog>
                  <archetypes>
                    <archetype>
                      <groupId>zebra.group</groupId>
                      <artifactId>zebra-arch</artifactId>
                      <version>1.0</version>
                    </archetype>
                    <archetype>
                      <groupId>alpha.group</groupId>
                      <artifactId>alpha-arch</artifactId>
                      <version>2.0</version>
                    </archetype>
                  </archetypes>
                </archetype-catalog>
                """;
        List<String> parsed = MavenArchetypeMetadata.parseCatalogXmlContent(xml);
        assertNotNull(parsed);
        assertEquals(2, parsed.size());
        assertEquals("alpha.group:alpha-arch", parsed.get(0));
        assertEquals("zebra.group:zebra-arch", parsed.get(1));
    }

    @Test
    void testParseCatalogXml(@TempDir Path tempDir) throws IOException {
        Path catalogFile = tempDir.resolve("archetype-catalog.xml");
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <archetype-catalog>
                  <archetypes>
                    <archetype>
                      <groupId>com.mycompany</groupId>
                      <artifactId>my-custom-archetype</artifactId>
                      <version>1.0.0</version>
                      <description>Custom description</description>
                    </archetype>
                    <archetype>
                      <groupId>org.sample</groupId>
                      <artifactId>sample-archetype</artifactId>
                      <version>2.0</version>
                    </archetype>
                  </archetypes>
                </archetype-catalog>
                """;
        Files.writeString(catalogFile, xml);

        List<String> archetypes = MavenArchetypeMetadata.parseCatalogXml(catalogFile);
        assertNotNull(archetypes);
        assertEquals(2, archetypes.size());
        assertTrue(archetypes.contains("com.mycompany:my-custom-archetype"));
        assertTrue(archetypes.contains("org.sample:sample-archetype"));
    }

    @Test
    void testDiscoverLocalArchetypes(@TempDir Path tempDir) throws IOException {
        // Create a mock local maven repository structure
        Path archetypeDir = tempDir.resolve("org/example/archetypes/example-archetype/1.0");
        Files.createDirectories(archetypeDir);
        Files.createFile(archetypeDir.resolve("example-archetype-1.0.jar"));

        List<String> found = MavenArchetypeMetadata.discoverLocalArchetypes(tempDir.toString());
        assertNotNull(found);
        assertTrue(found.contains("org.example.archetypes:example-archetype"));
    }

    @Test
    void testGetVersionsForArchetype() {
        List<String> quickstartVersions = MavenArchetypeMetadata.getVersionsForArchetype("org.apache.maven.archetypes:maven-archetype-quickstart");
        assertNotNull(quickstartVersions);
        assertTrue(quickstartVersions.contains("1.4"));
        assertTrue(quickstartVersions.contains("RELEASE"));

        List<String> quarkusVersions = MavenArchetypeMetadata.getVersionsForArchetype("io.quarkus:quarkus-archetype");
        assertNotNull(quarkusVersions);
        assertTrue(quarkusVersions.contains("3.18.1"));

        List<String> fallbackVersions = MavenArchetypeMetadata.getVersionsForArchetype("unknown.group:custom-archetype");
        assertNotNull(fallbackVersions);
        assertFalse(fallbackVersions.isEmpty());
        assertTrue(fallbackVersions.contains("RELEASE"));
    }

    @Test
    void testIdeaMetadataGeneration() {
        String modulesXml = MavenArchetypeMetadata.generateIdeaModulesXml("my-app");
        assertNotNull(modulesXml);
        assertTrue(modulesXml.contains("my-app.iml"));
        assertTrue(modulesXml.contains("ProjectModuleManager"));

        String miscXml = MavenArchetypeMetadata.generateIdeaMiscXml("21");
        assertNotNull(miscXml);
        assertTrue(miscXml.contains("languageLevel=\"JDK_21\""));
        assertTrue(miscXml.contains("project-jdk-name=\"21\""));
        assertTrue(miscXml.contains("MavenProjectsManager"));

        String vcsXml = MavenArchetypeMetadata.generateIdeaVcsXml();
        assertNotNull(vcsXml);
        assertTrue(vcsXml.contains("VcsDirectoryMappings"));
        assertTrue(vcsXml.contains("vcs=\"Git\""));
    }
}
