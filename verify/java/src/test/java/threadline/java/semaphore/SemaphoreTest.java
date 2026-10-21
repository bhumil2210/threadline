package threadline.java.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/semaphore.yaml. Asserts facts, never timings. */
@DisplayName("semaphore")
class SemaphoreTest {

    @Test
    @DisplayName("with 2 permits, 2 threads get in and 2 wait; each release lets exactly one more in")
    void permitsCapConcurrency() throws Exception {
        Semaphore permits = new Semaphore(2);
        Set<String> entered = ConcurrentHashMap.newKeySet();
        Map<String, CountDownLatch> leave = new ConcurrentHashMap<>();
        List<Thread> workers = IntStream.rangeClosed(1, 4).mapToObj(i -> {
            String name = "worker-" + i;
            leave.put(name, new CountDownLatch(1));
            return new Thread(() -> {
                permits.acquireUninterruptibly();
                try {
                    entered.add(name);
                    Facts.await(leave.get(name)); // stay inside until the test lets this one go
                } finally {
                    permits.release();
                }
            }, name);
        }).toList();
        workers.forEach(Thread::start);

        waitUntil(() -> entered.size() == 2);
        assertEquals(0, permits.availablePermits());
        for (Thread t : workers) {
            if (!entered.contains(t.getName())) Facts.awaitState(t, Thread.State.WAITING);
        }

        // One thread leaves: exactly one waiter gets its permit, the other keeps waiting.
        String firstOut = entered.iterator().next();
        leave.get(firstOut).countDown();
        waitUntil(() -> entered.size() == 3);
        assertEquals(0, permits.availablePermits(), "the freed permit was taken at once");
        Thread stillWaiting = workers.stream().filter(t -> !entered.contains(t.getName())).findFirst().orElseThrow();
        Facts.awaitState(stillWaiting, Thread.State.WAITING);

        leave.values().forEach(CountDownLatch::countDown);
        for (Thread t : workers) t.join(5_000);
        assertEquals(4, entered.size(), "everyone got in eventually");
        assertEquals(2, permits.availablePermits(), "all permits returned");
    }

    @Test
    @DisplayName("main never has more than 2 threads inside callApi")
    void mainCapsAtTwo() throws Exception {
        assertEquals("max inside callApi: 2", Facts.stdout(() -> Main.main(new String[0])));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("condition never became true");
            Thread.sleep(1);
        }
    }
}
