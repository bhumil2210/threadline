package threadline.java.waitvssleep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/wait-vs-sleep.yaml. Asserts facts, never timings. */
@DisplayName("wait-vs-sleep")
class WaitVsSleepTest {

    @Test
    @DisplayName("napper sleeps (TIMED_WAITING) with the lock, so other is BLOCKED until it leaves")
    void sleepKeepsTheLock() throws Exception {
        Thread napper = new Thread(() -> {
            try {
                Main.napper();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "napper");
        Thread other = new Thread(Main::other, "other");
        String out = Facts.stdout(() -> {
            napper.start();
            Facts.awaitState(napper, Thread.State.TIMED_WAITING);
            other.start();
            Facts.awaitState(other, Thread.State.BLOCKED);
            assertEquals(Thread.State.TIMED_WAITING, napper.getState(), "napper is still asleep, holding the lock");
            napper.join();
            other.join();
        });
        assertEquals("other got the lock", out);
    }

    @Test
    @DisplayName("sleep() is static: it always sleeps the calling thread")
    void sleepIsStatic() throws Exception {
        assertTrue(java.lang.reflect.Modifier.isStatic(Thread.class.getMethod("sleep", long.class).getModifiers()));
    }
}
