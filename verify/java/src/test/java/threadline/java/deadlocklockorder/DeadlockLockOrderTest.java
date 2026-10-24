package threadline.java.deadlocklockorder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/deadlock-lock-order.yaml. Asserts facts, never timings. */
@DisplayName("deadlock-lock-order")
class DeadlockLockOrderTest {

    @Test
    @DisplayName("same lock order: the second thread waits on A while holding nothing, then both finish")
    void sameOrderFinishes() throws Exception {
        Thread t2 = new Thread(() -> Main.lockAThenB("T2"), "T2");
        String out = Facts.stdout(() -> {
            synchronized (Main.A) { // stand in for T1 holding A
                t2.start();
                Facts.awaitState(t2, Thread.State.BLOCKED);
                var info = ManagementFactory.getThreadMXBean().getThreadInfo(new long[] {t2.threadId()}, true, false)[0];
                assertEquals(0, info.getLockedMonitors().length, "T2 holds no lock while it waits for A");
            }
            t2.join(5_000);
        });
        assertEquals("T2 done", out);
    }

    @Test
    @DisplayName("main finishes with both threads done")
    void mainFinishes() throws Exception {
        List<String> lines = Facts.stdout(() -> Main.main(new String[0])).lines().toList();
        Set<String> done = lines.stream().collect(Collectors.toSet());
        assertEquals(Set.of("T1 done", "T2 done"), done);
    }
}
