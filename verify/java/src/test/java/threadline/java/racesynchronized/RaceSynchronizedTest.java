package threadline.java.racesynchronized;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/race-synchronized.yaml. Asserts facts, never timings. */
@DisplayName("race-synchronized")
class RaceSynchronizedTest {

    @BeforeEach
    void reset() {
        Main.count = 0;
    }

    @Test
    @DisplayName("T2 is BLOCKED while another thread holds the monitor, and enters once it is released")
    void blocksOnHeldMonitor() throws Exception {
        Thread t2 = new Thread(Main::increment, "T2");
        synchronized (Main.class) { // static synchronized methods lock Main.class
            t2.start();
            Facts.awaitState(t2, Thread.State.BLOCKED);
            assertEquals(0, Main.count, "T2 must not run its increment while blocked");
        }
        t2.join();
        assertEquals(1, Main.count);
    }

    @Test
    @DisplayName("no update is lost under contention")
    void noLostUpdates() throws Exception {
        int perThread = 1_000_000;
        CountDownLatch go = new CountDownLatch(1);
        Runnable task = () -> {
            Facts.await(go);
            for (int i = 0; i < perThread; i++) Main.increment();
        };
        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        go.countDown();
        t1.join();
        t2.join();
        assertEquals(2 * perThread, Main.count);
    }

    @Test
    @DisplayName("main prints 2")
    void mainPrintsTwo() throws Exception {
        assertEquals("2", Facts.stdout(() -> Main.main(new String[0])));
    }
}
