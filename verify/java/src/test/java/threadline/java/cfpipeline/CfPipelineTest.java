package threadline.java.cfpipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/cf-pipeline.yaml. Asserts facts, never timings. */
@DisplayName("cf-pipeline")
class CfPipelineTest {

    @Test
    @DisplayName("stages run in order, all on the same common-pool thread, while the joiner WAITs")
    void stagesRunInOrderOnOnePoolThread() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        List<String> log = new CopyOnWriteArrayList<>();
        String here = Thread.currentThread().getName();

        CompletableFuture<Void> chain = CompletableFuture
                .supplyAsync(() -> {
                    Facts.await(gate); // stages get attached before this completes
                    log.add("fetchUser@" + Thread.currentThread().getName());
                    return new Main.User(42, "Ada");
                })
                .thenApply(user -> {
                    log.add("thenApply@" + Thread.currentThread().getName());
                    return user.name();
                })
                .thenAccept(name -> log.add("thenAccept@" + Thread.currentThread().getName() + ":" + name));

        Thread joiner = new Thread(chain::join, "joiner");
        joiner.start();
        Facts.awaitState(joiner, Thread.State.WAITING);
        gate.countDown();
        joiner.join();

        assertEquals(3, log.size(), log.toString());
        assertTrue(log.get(0).startsWith("fetchUser@"));
        assertTrue(log.get(1).startsWith("thenApply@"));
        assertTrue(log.get(2).startsWith("thenAccept@") && log.get(2).endsWith(":Ada"));
        String thread = log.get(0).substring(log.get(0).indexOf('@') + 1);
        assertEquals(List.of(thread, thread, thread + ":Ada"), log.stream().map(l -> l.substring(l.indexOf('@') + 1)).toList());
        assertNotEquals(here, thread);
        if (ForkJoinPool.getCommonPoolParallelism() > 1) {
            assertTrue(thread.startsWith("ForkJoinPool.commonPool-worker-"), thread);
        }
    }

    @Test
    @DisplayName("common-pool threads are daemons, so main must join() or the JVM may exit early")
    void commonPoolThreadsAreDaemons() {
        boolean daemon = CompletableFuture.supplyAsync(() -> Thread.currentThread().isDaemon()).join();
        assertTrue(daemon);
    }

    @Test
    @DisplayName("main prints Ada")
    void mainPrintsAda() throws Exception {
        assertEquals("Ada", Facts.stdout(() -> Main.main(new String[0])));
    }
}
