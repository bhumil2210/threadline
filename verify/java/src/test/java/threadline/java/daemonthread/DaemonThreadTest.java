package threadline.java.daemonthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/daemon-thread.yaml. Asserts facts, never timings. */
@DisplayName("daemon-thread")
class DaemonThreadTest {

    @Test
    @DisplayName("the JVM exits when main ends, without waiting for the daemon or running its finally")
    void jvmDoesNotWaitForDaemons() throws Exception {
        // The worker sleeps 3 s, so exiting within 2.5 s means the JVM did not wait for it.
        Facts.Forked run = Facts.fork(Main.class, 2_500);
        assertTrue(run.exited(), "the JVM should exit while the daemon is still sleeping: " + run.out());
        assertEquals("main goes on", run.out(), "no \"worker done\" and no \"cleanup\"");
    }

    @Test
    @DisplayName("setDaemon must be called before start()")
    void setDaemonBeforeStart() throws Exception {
        Thread worker = new Thread(() -> Facts.sleep(200), "worker");
        worker.start();
        assertThrows(IllegalThreadStateException.class, () -> worker.setDaemon(true));
        worker.join();
    }

    @Test
    @DisplayName("executor threads are normal threads; common-pool threads are daemons")
    void poolThreadKinds() throws Exception {
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            assertEquals(false, pool.submit(() -> Thread.currentThread().isDaemon()).get());
        } finally {
            pool.shutdown();
        }
        assertEquals(true, java.util.concurrent.CompletableFuture.supplyAsync(() -> Thread.currentThread().isDaemon()).join());
    }
}
