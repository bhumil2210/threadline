package threadline.java.threadpoolfour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/thread-pool-four.yaml. Asserts facts, never timings. */
@DisplayName("thread-pool-four")
class ThreadPoolFourTest {

    @Test
    @DisplayName("with 4 threads, all 4 tasks run at once and nothing waits in the queue")
    void allTasksRunAtOnce() throws Exception {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        CountDownLatch allRunning = new CountDownLatch(4);
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                allRunning.countDown();
                Facts.await(gate);
            });
        }
        allRunning.await(); // would hang if fewer than 4 could run together
        assertEquals(0, pool.getQueue().size());
        gate.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("each task gets its own pool thread")
    void fourThreads() throws Exception {
        Set<String> threads = Facts.stdout(() -> Main.main(new String[0])).lines()
                .map(l -> l.substring(l.indexOf(" on ") + 4)).collect(Collectors.toSet());
        assertEquals(4, threads.size(), threads.toString());
    }
}
