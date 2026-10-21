package threadline.java.cfthreadhopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/cf-thread-hopping.yaml. Asserts facts, never timings. */
@DisplayName("cf-thread-hopping")
class CfThreadHoppingTest {
    private final ExecutorService ioPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "io-1"));
    private final ExecutorService cpuPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "cpu-1"));

    @AfterEach
    void shutdown() {
        ioPool.shutdown();
        cpuPool.shutdown();
    }

    @Test
    @DisplayName("thenApply attached before completion runs on the thread that completes the future")
    void thenApplyRunsOnCompletingThread() {
        CountDownLatch gate = new CountDownLatch(1);
        var f = CompletableFuture.supplyAsync(() -> {
            Facts.await(gate);
            return "fresh";
        }, ioPool);
        var ranOn = f.thenApply(r -> Thread.currentThread().getName());
        assertTrue(!ranOn.isDone(), "attaching thenApply runs nothing while f is pending");
        gate.countDown();
        assertEquals("io-1", ranOn.join());
    }

    @Test
    @DisplayName("thenApplyAsync with an executor runs on that executor's thread")
    void thenApplyAsyncRunsOnGivenExecutor() {
        CountDownLatch gate = new CountDownLatch(1);
        var f = CompletableFuture.supplyAsync(() -> {
            Facts.await(gate);
            return "fresh";
        }, ioPool);
        var ranOn = f.thenApplyAsync(r -> Thread.currentThread().getName(), cpuPool);
        gate.countDown();
        assertEquals("cpu-1", ranOn.join());
    }

    @Test
    @DisplayName("thenApply on an already-completed future runs right away, on the caller")
    void thenApplyOnCompletedFutureRunsOnCaller() {
        AtomicReference<String> ranOn = new AtomicReference<>();
        var done = CompletableFuture.completedFuture("cached");
        var result = done.thenApply(r -> {
            ranOn.set(Thread.currentThread().getName());
            return r;
        });
        assertTrue(result.isDone(), "already ran by the time thenApply returned");
        assertEquals(Thread.currentThread().getName(), ranOn.get());
    }

    @Test
    @DisplayName("main shows parse on the caller, io-1 and cpu-1")
    void mainShowsThreeThreads() throws Exception {
        Set<String> threads = Facts.stdout(() -> Main.main(new String[0])).lines()
                .map(l -> l.substring(l.indexOf(" on ") + 4))
                .collect(Collectors.toSet());
        assertEquals(Set.of(Thread.currentThread().getName(), "io-1", "cpu-1"), threads);
    }
}
