package threadline.java.interruptswallowed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/interrupt-swallowed.yaml. Asserts facts, never timings. */
@DisplayName("interrupt-swallowed")
class InterruptSwallowedTest {

    @Test
    @DisplayName("swallowing InterruptedException clears the flag, so the loop never ends")
    void swallowedInterruptKeepsLooping() throws Exception {
        Thread worker = Main.newWorker();
        worker.setDaemon(true); // it never stops; don't keep the test JVM alive
        worker.start();
        Facts.awaitState(worker, Thread.State.TIMED_WAITING);
        worker.interrupt();
        worker.join(2_500); // longer than two of its 1 s sleeps
        assertTrue(worker.isAlive(), "worker should still be looping");
        assertFalse(worker.isInterrupted(), "the flag was cleared when sleep() threw");
    }

    @Test
    @DisplayName("restoring the flag in catch lets the same loop stop")
    void restoringTheFlagStops() throws Exception {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // the fix
                }
            }
        }, "worker");
        worker.setDaemon(true);
        worker.start();
        Facts.awaitState(worker, Thread.State.TIMED_WAITING);
        worker.interrupt();
        worker.join(5_000);
        assertFalse(worker.isAlive());
    }
}
