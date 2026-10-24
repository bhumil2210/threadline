package threadline.java.collectionsconcurrentmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/collections-concurrent-map.yaml. Asserts facts, never timings. */
@DisplayName("collections-concurrent-map")
class CollectionsConcurrentMapTest {

    @BeforeEach
    void reset() {
        Main.counts.clear();
    }

    @Test
    @DisplayName("merge on another key runs alongside; merge on the same key is BLOCKED until the first is done")
    void mergeLocksOnlyItsKey() throws Exception {
        Main.counts.put("a", 0);
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread t1 = new Thread(() -> Main.counts.merge("a", 1, (old, one) -> {
            inside.countDown();
            Facts.await(release); // hold key a's bin lock
            return old + one;
        }), "T1");
        t1.start();
        inside.await();

        Thread other = new Thread(() -> Main.countWords(java.util.List.of("b")), "T2");
        other.start();
        other.join(5_000);
        assertFalse(other.isAlive(), "merge(b) must not wait for a merge on a");

        Thread same = new Thread(() -> Main.countWords(java.util.List.of("a")), "T2");
        same.start();
        Facts.awaitState(same, Thread.State.BLOCKED);
        release.countDown();
        t1.join();
        same.join();
        assertEquals(Map.of("a", 2, "b", 1), Main.counts);
    }

    @Test
    @DisplayName("main prints {a=3, b=1}")
    void mainCounts() throws Exception {
        assertEquals("{a=3, b=1}", Facts.stdout(() -> Main.main(new String[0])));
    }

    @Test
    @DisplayName("get-then-put on a ConcurrentHashMap still loses updates")
    void getThenPutLosesUpdates() throws Exception {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        for (int trial = 1; trial <= 50; trial++) {
            map.put("a", 0);
            CountDownLatch go = new CountDownLatch(1);
            Runnable task = () -> {
                Facts.await(go);
                for (int i = 0; i < 100_000; i++) map.put("a", map.get("a") + 1);
            };
            Thread t1 = new Thread(task, "T1");
            Thread t2 = new Thread(task, "T2");
            t1.start();
            t2.start();
            go.countDown();
            t1.join();
            t2.join();
            if (map.get("a") < 200_000) return;
        }
        fail("get-then-put never lost an update");
    }
}
