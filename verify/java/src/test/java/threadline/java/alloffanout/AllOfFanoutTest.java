package threadline.java.alloffanout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/allof-fanout.yaml. Asserts facts, never timings. */
@DisplayName("allof-fanout")
class AllOfFanoutTest {

    @Test
    @DisplayName("all three calls run at the same time")
    void callsRunTogether() {
        ExecutorService io = Executors.newFixedThreadPool(3);
        CyclicBarrier allRunning = new CyclicBarrier(3);
        var calls = new CompletableFuture<?>[3];
        for (int i = 0; i < 3; i++) {
            calls[i] = CompletableFuture.runAsync(() -> {
                try {
                    allRunning.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("not all calls were running at once", e);
                }
            }, io);
        }
        CompletableFuture.allOf(calls).join();
        io.shutdown();
    }

    @Test
    @DisplayName("allOf completes only after the slowest call; then join() on each returns without waiting")
    void allOfWaitsForTheSlowest() {
        ExecutorService io = Executors.newFixedThreadPool(3);
        CountDownLatch bGate = new CountDownLatch(1);
        var a = CompletableFuture.supplyAsync(() -> 12, io);
        var b = CompletableFuture.supplyAsync(() -> {
            Facts.await(bGate);
            return 15;
        }, io);
        var c = CompletableFuture.supplyAsync(() -> 9, io);
        var all = CompletableFuture.allOf(a, b, c);

        a.join();
        c.join();
        assertFalse(all.isDone(), "a and c are done, but b is not, so allOf waits");
        bGate.countDown();
        all.join();
        assertTrue(a.isDone() && b.isDone() && c.isDone(), "after allOf, every join() is instant");
        assertEquals(9, Math.min(a.join(), Math.min(b.join(), c.join())));
        io.shutdown();
    }

    @Test
    @DisplayName("if one call fails, allOf still waits for the rest before failing")
    void allOfWaitsEvenOnFailure() {
        ExecutorService io = Executors.newFixedThreadPool(2);
        CountDownLatch bGate = new CountDownLatch(1);
        var a = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("shop A down");
        }, io);
        var b = CompletableFuture.supplyAsync(() -> {
            Facts.await(bGate);
            return 15;
        }, io);
        var all = CompletableFuture.allOf(a, b);
        a.exceptionally(ex -> null).join();
        assertFalse(all.isDone(), "a failed, but allOf still waits for b");
        bGate.countDown();
        assertTrue(all.handle((v, ex) -> ex != null).join(), "then allOf fails");
        io.shutdown();
    }

    @Test
    @DisplayName("main prints the cheapest price, 9")
    void mainPrintsCheapest() throws Exception {
        assertEquals("9", Facts.stdout(() -> Main.main(new String[0])));
    }
}
