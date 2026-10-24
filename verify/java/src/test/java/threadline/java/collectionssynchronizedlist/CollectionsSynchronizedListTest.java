package threadline.java.collectionssynchronizedlist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/collections-synchronized-list.yaml. Asserts facts, never timings. */
@DisplayName("collections-synchronized-list")
class CollectionsSynchronizedListTest {

    @Test
    @DisplayName("add() locks the list itself, so another thread's add is BLOCKED meanwhile")
    void addLocksTheList() throws Exception {
        Main.list.clear();
        Thread t2 = new Thread(() -> Main.list.add(1), "T2");
        synchronized (Main.list) { // stand in for T1 inside add()
            t2.start();
            Facts.awaitState(t2, Thread.State.BLOCKED);
            assertEquals(0, Main.list.size());
        }
        t2.join();
        assertEquals(1, Main.list.size());
    }

    @Test
    @DisplayName("no element is lost")
    void nothingLost() throws Exception {
        for (int trial = 0; trial < 100; trial++) {
            Main.list.clear();
            CountDownLatch go = new CountDownLatch(1);
            Runnable task = () -> {
                Facts.await(go);
                Main.addMany();
            };
            Thread t1 = new Thread(task, "T1");
            Thread t2 = new Thread(task, "T2");
            t1.start();
            t2.start();
            go.countDown();
            t1.join();
            t2.join();
            assertEquals(2_000, Main.list.size());
        }
    }
}
