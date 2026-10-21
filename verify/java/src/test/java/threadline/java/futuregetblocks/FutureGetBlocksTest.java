package threadline.java.futuregetblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/future-get-blocks.yaml. Asserts facts, never timings. */
@DisplayName("future-get-blocks")
class FutureGetBlocksTest {

    @Test
    @DisplayName("submit() returns at once; get() leaves the caller WAITING until the task finishes")
    void getBlocksTheCaller() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch gate = new CountDownLatch(1);
        AtomicReference<String> taskThread = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();

        Thread caller = new Thread(() -> {
            Future<String> f = pool.submit(() -> {
                taskThread.set(Thread.currentThread().getName());
                Facts.await(gate);
                return "data";
            });
            assertFalse(f.isDone(), "submit() returned before the task finished");
            try {
                result.set(f.get());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, "main-like");
        caller.start();

        Facts.awaitState(caller, Thread.State.WAITING);
        assertEquals(null, result.get(), "no result while the task is still running");
        gate.countDown();
        caller.join();
        assertEquals("data", result.get());
        assertTrue(taskThread.get().matches("pool-\\d+-thread-1"), taskThread.get());
        assertNotEquals("main-like", taskThread.get());
        pool.shutdown();
    }

    @Test
    @DisplayName("main prints the result")
    void mainPrintsData() throws Exception {
        assertEquals("data", Facts.stdout(() -> Main.main(new String[0])));
    }
}
