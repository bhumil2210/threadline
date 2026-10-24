package threadline.java.commonpoolownexecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/common-pool-own-executor.yaml. Asserts facts, never timings. */
@DisplayName("common-pool-own-executor")
class CommonPoolOwnExecutorTest {

    @Test
    @DisplayName("with its own executor, quickTask runs on io-1 before any report finishes")
    void quickTaskDoesNotWait() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 30_000, "-Djava.util.concurrent.ForkJoinPool.common.parallelism=2");
        assertTrue(run.exited(), run.out());
        List<String> events = run.lines().stream().map(l -> l.substring(l.indexOf(' ') + 1)).toList();
        int quick = events.indexOf("io-1 quickTask");
        assertTrue(quick >= 0, run.out());
        int firstEnd = -1;
        for (int i = 0; i < events.size(); i++) if (events.get(i).matches(".* report \\d end")) { firstEnd = i; break; }
        assertTrue(quick < firstEnd, "quickTask should not wait for the stream:\n" + run.out());
    }

    @Test
    @DisplayName("a later *Async stage without an executor goes back to the common pool")
    void executorIsNotInherited() {
        String thread = java.util.concurrent.CompletableFuture.supplyAsync(() -> 1, Main.ioPool)
                .thenApplyAsync(x -> Thread.currentThread().getName())
                .join();
        assertTrue(thread.startsWith("ForkJoinPool.commonPool-worker-"), thread);
    }
}
