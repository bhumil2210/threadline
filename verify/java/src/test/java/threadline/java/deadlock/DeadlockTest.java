package threadline.java.deadlock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/deadlock.yaml. Asserts facts, never timings. */
@DisplayName("deadlock")
class DeadlockTest {

    @Test
    @DisplayName("opposite lock order: both threads end up BLOCKED, and the JVM reports a deadlock")
    void oppositeOrderDeadlocks() throws Exception {
        Thread t1 = new Thread(Main::lockAThenB, "T1");
        Thread t2 = new Thread(Main::lockBThenA, "T2");
        t1.setDaemon(true); // they never finish; don't keep the test JVM alive
        t2.setDaemon(true);
        t1.start();
        t2.start();
        Facts.awaitState(t1, Thread.State.BLOCKED);
        Facts.awaitState(t2, Thread.State.BLOCKED);

        long[] deadlocked = ManagementFactory.getThreadMXBean().findMonitorDeadlockedThreads();
        assertNotNull(deadlocked, "no deadlock found");
        var ids = Arrays.stream(deadlocked).boxed().collect(Collectors.toSet());
        assertTrue(ids.contains(t1.threadId()) && ids.contains(t2.threadId()), "T1 and T2 should be deadlocked");
    }
}
