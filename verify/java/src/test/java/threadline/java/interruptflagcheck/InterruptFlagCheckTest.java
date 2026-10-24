package threadline.java.interruptflagcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/interrupt-flag-check.yaml. Asserts facts, never timings. */
@DisplayName("interrupt-flag-check")
class InterruptFlagCheckTest {

    @Test
    @DisplayName("a busy worker stays RUNNABLE; it stops once its loop checks the interrupt flag")
    void loopSeesTheFlag() throws Exception {
        String out = Facts.stdout(() -> {
            Thread worker = Main.newWorker();
            worker.start();
            Facts.sleep(50);
            assertEquals(Thread.State.RUNNABLE, worker.getState(), "crunch() never blocks");
            worker.interrupt();
            worker.join(5_000);
            assertFalse(worker.isAlive(), "worker should stop at its next flag check");
        });
        assertEquals("saw the flag, stopping", out);
    }

    @Test
    @DisplayName("Thread.interrupted() reads the flag and clears it; isInterrupted() only reads")
    void interruptedClears() {
        Thread.currentThread().interrupt();
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(Thread.currentThread().isInterrupted(), "isInterrupted() leaves the flag set");
        assertTrue(Thread.interrupted());
        assertFalse(Thread.currentThread().isInterrupted(), "interrupted() cleared it");
    }
}
