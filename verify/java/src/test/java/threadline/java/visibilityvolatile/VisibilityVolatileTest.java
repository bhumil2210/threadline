package threadline.java.visibilityvolatile;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/visibility-volatile.yaml. Asserts facts, never timings. */
@DisplayName("visibility-volatile")
class VisibilityVolatileTest {

    @Test
    @DisplayName("with volatile, worker sees stop = true and the program exits")
    void volatileFlagIsSeen() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 20_000);
        assertTrue(run.exited(), run.out());
        assertTrue(run.out().contains("worker stopped after"), run.out());
    }
}
