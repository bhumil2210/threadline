package threadline.java.stoppingathread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/stopping-a-thread.yaml. Asserts facts, never timings. */
@DisplayName("stopping-a-thread")
class StoppingAThreadTest {

    @Test
    @DisplayName("a sleeping worker is TIMED_WAITING, and interrupt() makes sleep throw at once")
    void interruptWakesASleepingThread() throws Exception {
        String out = Facts.stdout(() -> {
            Thread worker = Main.newWorker();
            worker.start();
            Facts.awaitState(worker, Thread.State.TIMED_WAITING);
            worker.interrupt();
            worker.join(5_000); // far less than the 10 s sleep
            assertFalse(worker.isAlive(), "worker should have stopped right after interrupt()");
        });
        assertEquals("woken early, cleaning up", out);
    }

    @Test
    @DisplayName("Thread.stop() no longer works: it throws, or is gone from the JDK")
    void stopIsGone() throws Exception {
        java.lang.reflect.Method stop;
        try {
            stop = Thread.class.getMethod("stop");
        } catch (NoSuchMethodException removed) {
            return; // removed in newer JDKs
        }
        Thread t = new Thread(() -> {});
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> stop.invoke(t));
        assertTrue(e.getCause() instanceof UnsupportedOperationException, String.valueOf(e.getCause()));
    }
}
