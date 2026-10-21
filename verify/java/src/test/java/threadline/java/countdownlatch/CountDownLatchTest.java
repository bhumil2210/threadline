package threadline.java.countdownlatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/count-down-latch.yaml. Asserts facts, never timings. */
@DisplayName("count-down-latch")
class CountDownLatchTest {

    @Test
    @DisplayName("await() keeps the caller WAITING until the count reaches 0, not before")
    void awaitReleasesOnlyAtZero() throws Exception {
        CountDownLatch ready = new CountDownLatch(3);
        Thread waiter = new Thread(() -> {
            try {
                ready.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "main-like");
        waiter.start();

        Facts.awaitState(waiter, Thread.State.WAITING);
        ready.countDown();
        ready.countDown();
        assertEquals(1, ready.getCount());
        Facts.awaitState(waiter, Thread.State.WAITING); // still waiting with 1 left
        ready.countDown();
        waiter.join(5_000);
        assertEquals(Thread.State.TERMINATED, waiter.getState(), "released at 0");
    }

    @Test
    @DisplayName("a latch is single-use: it stays at 0 and can't be reset")
    void latchIsSingleUse() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        latch.countDown(); // no effect at 0
        assertEquals(0, latch.getCount());
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS), "await returns at once, forever after");
    }

    @Test
    @DisplayName("main prints once everything is loaded")
    void mainPrints() throws Exception {
        assertEquals("all loaded, start serving", Facts.stdout(() -> Main.main(new String[0])));
    }
}
