package threadline.java.syncvsatomic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Facts behind scenarios/java/sync-vs-atomic.yaml. Asserts facts, never timings. */
@DisplayName("sync-vs-atomic")
class SyncVsAtomicTest {

    @BeforeEach
    void reset() {
        Main.syncCount = 0;
        Main.atomicCount.set(0);
    }

    @Test
    @DisplayName("sync-2 is BLOCKED while another thread holds the monitor, and enters once it is released")
    void syncThreadBlocksOnHeldMonitor() throws Exception {
        Thread s2 = new Thread(Main::syncIncrement, "sync-2");
        synchronized (Main.class) { // static synchronized methods lock Main.class
            s2.start();
            awaitState(s2, Thread.State.BLOCKED);
            assertEquals(0, Main.syncCount, "sync-2 must not run its increment while blocked");
        }
        s2.join();
        assertEquals(1, Main.syncCount);
    }

    @Test
    @DisplayName("CAS fails and writes nothing when the value changed after it was read")
    void casFailsOnStaleValue() {
        int seen = Main.atomicCount.get(); // atomic-2 reads 0
        Main.atomicIncrement(); // atomic-1 gets in first: 0 -> 1
        assertFalse(Main.atomicCount.compareAndSet(seen, seen + 1), "CAS 0->1 must fail when count is 1");
        assertEquals(1, Main.atomicCount.get(), "a failed CAS writes nothing");
        assertTrue(Main.atomicCount.compareAndSet(1, 2), "retrying with the fresh value succeeds");
    }

    @Test
    @DisplayName("neither version loses updates under contention")
    void noLostUpdates() throws Exception {
        int perThread = 1_000_000;
        runPair(perThread, Main::syncIncrement);
        runPair(perThread, Main::atomicIncrement);
        assertEquals(2 * perThread, Main.syncCount);
        assertEquals(2 * perThread, Main.atomicCount.get());
    }

    @Test
    @DisplayName("atomic threads are never seen BLOCKED or WAITING under contention")
    void atomicThreadsNeverBlock() throws Exception {
        Set<Thread.State> seen = ConcurrentHashMap.newKeySet();
        CountDownLatch go = new CountDownLatch(1);
        Runnable task = () -> {
            awaitQuietly(go);
            for (int i = 0; i < 5_000_000; i++) Main.atomicIncrement();
        };
        Thread a1 = new Thread(task, "atomic-1");
        Thread a2 = new Thread(task, "atomic-2");
        a1.start();
        a2.start();
        go.countDown();
        while (a1.isAlive() || a2.isAlive()) {
            for (Thread t : new Thread[] {a1, a2}) if (t.isAlive()) seen.add(t.getState());
            Thread.onSpinWait();
        }
        a1.join();
        a2.join();
        seen.retainAll(EnumSet.of(Thread.State.BLOCKED, Thread.State.WAITING, Thread.State.TIMED_WAITING));
        assertEquals(Set.of(), seen);
    }

    @Test
    @DisplayName("main prints 2 2")
    void mainPrintsTwoTwo() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        assertEquals("2 2", out.toString().trim());
    }

    private static void runPair(int perThread, Runnable increment) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        Runnable task = () -> {
            awaitQuietly(go);
            for (int i = 0; i < perThread; i++) increment.run();
        };
        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        go.countDown();
        t1.join();
        t2.join();
    }

    private static void awaitState(Thread t, Thread.State state) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (t.getState() != state) {
            assertTrue(System.nanoTime() < deadline, t.getName() + " never reached " + state + ", is " + t.getState());
            Thread.sleep(1);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
