package dev.lumina.run;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses JUnit XML reports produced by Maven Surefire
 * (target/surefire-reports) and Gradle (build/test-results/test) into a
 * simple model the Tests tool window can render.
 */
public final class TestReport {

    public enum Status { PASSED, FAILED, ERROR, SKIPPED }

    public record Case(String className, String method, Status status,
                       double time, String message) {
    }

    public record Suite(String name, double time, List<Case> cases) {
        public long passed() {
            return cases.stream().filter(c -> c.status() == Status.PASSED).count();
        }
        public long failed() {
            return cases.stream().filter(c -> c.status() == Status.FAILED
                    || c.status() == Status.ERROR).count();
        }
        public long skipped() {
            return cases.stream().filter(c -> c.status() == Status.SKIPPED).count();
        }
    }

    private TestReport() {
    }

    /** Report directories for the given project, in priority order. */
    public static List<Path> reportDirs(Path projectRoot) {
        return List.of(
                projectRoot.resolve("target/surefire-reports"),
                projectRoot.resolve("build/test-results/test"));
    }

    /**
     * Parse all TEST-*.xml files modified at or after sinceMillis.
     * Files older than the current run are ignored so stale results
     * from previous runs never show up.
     */
    public static List<Suite> parse(Path projectRoot, long sinceMillis) {
        List<Suite> suites = new ArrayList<>();
        for (Path dir : reportDirs(projectRoot)) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.getFileName().toString().endsWith(".xml"))
                        .filter(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toMillis()
                                        >= sinceMillis;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .sorted()
                        .forEach(p -> {
                            Suite s = parseFile(p);
                            if (s != null && !s.cases().isEmpty()) suites.add(s);
                        });
            } catch (Exception ignored) {
            }
            if (!suites.isEmpty()) break;   // don't mix Maven and Gradle output
        }
        return suites;
    }

    private static Suite parseFile(Path xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            Document doc = factory.newDocumentBuilder().parse(xml.toFile());
            Element root = doc.getDocumentElement();
            if (!"testsuite".equals(root.getTagName())) return null;

            String suiteName = root.getAttribute("name");
            double suiteTime = parseDouble(root.getAttribute("time"));

            List<Case> cases = new ArrayList<>();
            NodeList nodes = root.getElementsByTagName("testcase");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element tc = (Element) nodes.item(i);
                String cls = tc.getAttribute("classname");
                String method = tc.getAttribute("name");
                double time = parseDouble(tc.getAttribute("time"));

                Status status = Status.PASSED;
                String message = "";
                NodeList failure = tc.getElementsByTagName("failure");
                NodeList error = tc.getElementsByTagName("error");
                NodeList skipped = tc.getElementsByTagName("skipped");
                if (failure.getLength() > 0) {
                    status = Status.FAILED;
                    message = describe((Element) failure.item(0));
                } else if (error.getLength() > 0) {
                    status = Status.ERROR;
                    message = describe((Element) error.item(0));
                } else if (skipped.getLength() > 0) {
                    status = Status.SKIPPED;
                    message = ((Element) skipped.item(0)).getAttribute("message");
                }
                cases.add(new Case(cls, method, status, time, message));
            }
            return new Suite(suiteName, suiteTime, cases);
        } catch (Exception e) {
            return null;
        }
    }

    private static String describe(Element failureOrError) {
        String msg = failureOrError.getAttribute("message");
        String type = failureOrError.getAttribute("type");
        String head = (type.isBlank() ? "" : type + ": ") + msg;
        String body = failureOrError.getTextContent();
        if (body != null && !body.isBlank()) {
            // keep the first few stack lines; the console has the full text
            String[] lines = body.strip().split("\n");
            StringBuilder sb = new StringBuilder(head);
            for (int i = 0; i < Math.min(lines.length, 6); i++) {
                sb.append('\n').append(lines[i].strip());
            }
            return sb.toString();
        }
        return head;
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}