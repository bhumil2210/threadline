package threadline.java.raceatomic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/race-atomic.yaml. Asserts facts, never timings. */
@DisplayName("race-atomic")
class RaceAtomicTest {

    @BeforeEach
    void reset() {
        Main.count.set(0);
    }

    @Test
    @DisplayName("CAS fails and writes nothing when the value changed after it was read")
    void casFailsOnStaleValue() {
        int seen = Main.count.get(); // T2 reads 0
        Main.increment(); // T1 gets in first: 0 -> 1
        assertFalse(Main.count.compareAndSet(seen, seen + 1), "CAS 0->1 must fail when count is 1");
        assertEquals(1, Main.count.get(), "a failed CAS writes nothing");
        assertTrue(Main.count.compareAndSet(1, 2), "retrying with the fresh value succeeds");
    }

    @Test
    @DisplayName("no update is lost, and the threads are never BLOCKED or WAITING while incrementing")
    void noLostUpdatesAndNoBlocking() throws Exception {
        int perThread = 5_000_000;
        Set<Thread.State> seen = ConcurrentHashMap.newKeySet();
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch pastGate = new CountDownLatch(2);
        Runnable task = () -> {
            Facts.await(go);
            pastGate.countDown();
            for (int i = 0; i < perThread; i++) Main.increment();
        };
        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        go.countDown();
        // Threads are legitimately WAITING on the start gate; only sample once both are past it.
        pastGate.await();
        while (t1.isAlive() || t2.isAlive()) {
            for (Thread t : new Thread[] {t1, t2}) if (t.isAlive()) seen.add(t.getState());
            Thread.onSpinWait();
        }
        t1.join();
        t2.join();
        assertEquals(2 * perThread, Main.count.get());
        seen.retainAll(EnumSet.of(Thread.State.BLOCKED, Thread.State.WAITING, Thread.State.TIMED_WAITING));
        assertEquals(Set.of(), seen);
    }

    @Test
    @DisplayName("main prints 2")
    void mainPrintsTwo() throws Exception {
        assertEquals("2", Facts.stdout(() -> Main.main(new String[0])));
    }
}
