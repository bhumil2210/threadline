package threadline.java;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Every scenarios/java/*.yaml has a twin Main.java whose code contains the YAML's code, line for line. */
class ScenarioSyncTest {
    private static final Path SCENARIOS = Path.of("../../scenarios/java");
    private static final Path SOURCES = Path.of("src/main/java/threadline/java");

    @TestFactory
    Stream<DynamicTest> everyScenarioHasAMatchingTwin() throws IOException {
        try (Stream<Path> files = Files.list(SCENARIOS)) {
            return files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList().stream()
                    .map(yaml -> DynamicTest.dynamicTest(yaml.getFileName().toString(), () -> check(yaml)));
        }
    }

    private static void check(Path yaml) throws IOException {
        List<String> lines = Files.readAllLines(yaml);
        String id = lines.stream().filter(l -> l.startsWith("id: ")).findFirst().orElseThrow().substring(4).trim();
        Path twin = SOURCES.resolve(id.replace("-", "")).resolve("Main.java");
        assertTrue(Files.exists(twin), "missing twin " + twin + " for " + yaml);

        List<String> twinLines = Files.readAllLines(twin).stream().map(String::trim).toList();
        int from = 0;
        for (String codeLine : codeBlock(lines)) {
            int at = indexOf(twinLines, codeLine, from);
            assertTrue(at >= 0, yaml.getFileName() + ": code line not found in order in " + twin + ": " + codeLine);
            from = at + 1;
        }
    }

    /** Non-blank, trimmed lines of the YAML `code: |` block. */
    private static List<String> codeBlock(List<String> lines) {
        List<String> code = new ArrayList<>();
        boolean in = false;
        for (String l : lines) {
            if (l.startsWith("code: |")) {
                in = true;
            } else if (in && !l.isEmpty() && !l.startsWith(" ")) {
                break;
            } else if (in && !l.isBlank()) {
                code.add(l.trim());
            }
        }
        return code;
    }

    private static int indexOf(List<String> haystack, String needle, int from) {
        for (int i = from; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) return i;
        }
        return -1;
    }
}
