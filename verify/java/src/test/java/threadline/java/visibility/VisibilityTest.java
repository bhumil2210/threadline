package threadline.java.visibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/visibility.yaml. Asserts facts, never timings. */
@DisplayName("visibility")
class VisibilityTest {

    @Test
    @DisplayName("with a plain field, worker never sees stop = true and the program hangs")
    void plainFlagIsNeverSeen() throws Exception {
        // Its own JVM, because the hang depends on the JIT compiling the loop.
        Facts.Forked run = Facts.fork(Main.class, 5_000);
        assertFalse(run.exited(), "expected a hang, got: " + run.out());
        assertTrue(run.out().contains("main set stop = true"), run.out());
        assertFalse(run.out().contains("worker stopped"), run.out());
    }
}
