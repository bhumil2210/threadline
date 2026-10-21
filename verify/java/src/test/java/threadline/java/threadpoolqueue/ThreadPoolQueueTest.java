package threadline.java.threadpoolqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/thread-pool-queue.yaml. Asserts facts, never timings. */
@DisplayName("thread-pool-queue")
class ThreadPoolQueueTest {

    @Test
    @DisplayName("2 tasks run while the other 2 wait in the queue; submit() returns without waiting")
    void extraTasksWaitInTheQueue() throws Exception {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch twoRunning = new CountDownLatch(2);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        Set<String> started = ConcurrentHashMap.newKeySet();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            int id = i;
            futures.add(pool.submit(() -> {
                started.add("task " + id);
                maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                twoRunning.countDown();
                Facts.await(gate);
                inside.decrementAndGet();
            }));
        }
        assertFalse(futures.get(0).isDone(), "submit() returned before the task finished");
        twoRunning.await();
        assertEquals(2, started.size(), "only 2 tasks have started");
        assertEquals(2, pool.getQueue().size(), "the other 2 are waiting in the queue");
        gate.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(4, started.size());
        assertEquals(2, maxInside.get(), "never more than 2 tasks at once");
    }

    @Test
    @DisplayName("4 tasks reuse the same 2 pool threads")
    void tasksReuseTwoThreads() throws Exception {
        List<String> lines = Facts.stdout(() -> Main.main(new String[0])).lines().toList();
        assertEquals(4, lines.size(), String.join("\n", lines));
        Set<String> threads = lines.stream().map(l -> l.substring(l.indexOf(" on ") + 4)).collect(Collectors.toSet());
        assertEquals(2, threads.size(), "only 2 distinct threads: " + threads);
        threads.forEach(t -> assertTrue(t.matches("pool-\\d+-thread-[12]"), t));
    }
}
