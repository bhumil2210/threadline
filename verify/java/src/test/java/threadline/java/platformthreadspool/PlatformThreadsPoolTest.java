package threadline.java.platformthreadspool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/platform-threads-pool.yaml. Asserts facts, never timings. */
@DisplayName("platform-threads-pool")
class PlatformThreadsPoolTest {

    @Test
    @DisplayName("a sleeping task keeps its pool thread: the third task waits in the queue")
    void sleepingTaskHoldsItsThread() throws Exception {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CountDownLatch twoAsleep = new CountDownLatch(2);
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                twoAsleep.countDown();
                Thread.sleep(500);
                return null;
            });
        }
        twoAsleep.await();
        assertEquals(1, pool.getQueue().size(), "task 3 waits although both running tasks are only sleeping");
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("each task runs both halves on the same platform thread")
    void taskStaysOnItsThread() throws Exception {
        List<String> lines = Facts.stdout(() -> Main.main(new String[0])).lines().toList();
        assertEquals("done", lines.getLast());
        Map<String, List<String>> byTask = lines.stream().filter(l -> l.startsWith("task "))
                .collect(Collectors.groupingBy(l -> l.substring(0, 6), Collectors.mapping(l -> l.substring(10), Collectors.toList())));
        assertEquals(3, byTask.size(), lines.toString());
        byTask.forEach((task, threads) -> {
            assertEquals(2, threads.size());
            assertEquals(threads.get(0), threads.get(1), task + " moved threads");
        });
        assertEquals(2, byTask.values().stream().map(t -> t.get(0)).distinct().count(), "only 2 threads");
    }
}
