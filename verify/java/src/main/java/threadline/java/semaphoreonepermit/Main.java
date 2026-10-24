package threadline.java.semaphoreonepermit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/semaphore-one-permit.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final AtomicInteger inside = new AtomicInteger();
    static final AtomicInteger maxInside = new AtomicInteger();
    static final CountDownLatch done = new CountDownLatch(4);

    public static void main(String[] args) throws Exception {
        Semaphore permits = new Semaphore(1); // one at a time, like a lock
        for (int i = 1; i <= 4; i++) {
            int id = i;
            new Thread(() -> {
                permits.acquireUninterruptibly(); // waits while the permit is taken
                try { callApi(id); }
                finally { permits.release(); }    // hand the permit back
            }, "worker-" + id).start();
        }
        done.await();
        System.out.println("max inside callApi: " + maxInside.get());
    }

    static void callApi(int id) {
        maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            inside.decrementAndGet();
            done.countDown();
        }
    }
}
