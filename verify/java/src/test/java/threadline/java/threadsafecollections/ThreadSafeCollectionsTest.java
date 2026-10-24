package threadline.java.threadsafecollections;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/thread-safe-collections.yaml. Asserts facts, never timings. */
@DisplayName("thread-safe-collections")
class ThreadSafeCollectionsTest {

    @Test
    @DisplayName("two threads adding to one ArrayList lose elements or crash")
    void arrayListBreaks() throws Exception {
        int trials = 500;
        for (int trial = 1; trial <= trials; trial++) {
            Main.list.clear();
            AtomicReference<Throwable> crash = new AtomicReference<>();
            CountDownLatch go = new CountDownLatch(1);
            Runnable task = () -> {
                Facts.await(go);
                Main.addMany();
            };
            Thread t1 = new Thread(task, "T1");
            Thread t2 = new Thread(task, "T2");
            for (Thread t : new Thread[] {t1, t2}) {
                t.setUncaughtExceptionHandler((th, ex) -> crash.set(ex));
                t.start();
            }
            go.countDown();
            t1.join();
            t2.join();
            if (crash.get() != null || Main.list.size() < 2_000) {
                System.out.printf("trial %d: size %d, crash %s%n", trial, Main.list.size(), crash.get());
                return;
            }
        }
        fail("ArrayList survived " + trials + " trials of concurrent adds");
    }
}
