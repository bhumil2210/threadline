package threadline.java.threadpoolfour;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Runnable twin of scenarios/java/thread-pool-four.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 1; i <= 4; i++) {
            int id = i;
            pool.submit(() -> work("task " + id));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
    }

    static void work(String name) {
        System.out.println(name + " on " + Thread.currentThread().getName());
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
