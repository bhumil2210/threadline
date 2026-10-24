package threadline.java.waitreleaseslock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/wait-releases-lock.yaml. Asserts facts, never timings. */
@DisplayName("wait-releases-lock")
class WaitReleasesLockTest {

    @BeforeEach
    void reset() {
        Main.ready = false;
    }

    @Test
    @DisplayName("wait() is WAITING without the lock; after notifyAll it is BLOCKED until the lock is free")
    void waitReleasesAndReacquires() throws Exception {
        Thread waiter = new Thread(() -> {
            try {
                Main.waiter();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        String out = Facts.stdout(() -> {
            waiter.start();
            Facts.awaitState(waiter, Thread.State.WAITING);
            synchronized (Main.lock) { // we get in while waiter is inside its synchronized block
                Main.ready = true;
                Main.lock.notifyAll();
                Facts.awaitState(waiter, Thread.State.BLOCKED); // woken, but needs the lock back
            }
            waiter.join(5_000);
            assertFalse(waiter.isAlive());
        });
        assertEquals("waiter: ready", out);
    }

    @Test
    @DisplayName("main prints waiter: ready")
    void mainRuns() throws Exception {
        assertEquals("waiter: ready", Facts.stdout(() -> Main.main(new String[0])));
    }

    @Test
    @DisplayName("wait() outside synchronized on that object throws IllegalMonitorStateException")
    void waitNeedsTheMonitor() {
        assertThrows(IllegalMonitorStateException.class, () -> Main.lock.wait());
    }
}
