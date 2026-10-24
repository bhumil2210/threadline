package threadline.java.platformthreadspool;

import java.util.concurrent.Executors;

/** Runnable twin of scenarios/java/platform-threads-pool.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 3; i++) {
                int id = i + 1;
                executor.submit(() -> {
                    compute(id);          // runs on a pool thread
                    Thread.sleep(200);    // blocks: the pool thread sleeps too
                    compute(id);          // same thread, which was never free
                    return null;
                });
            }
        } // close() waits for every task
        System.out.println("done");
    }

    static synchronized void compute(int task) {
        System.out.println("task " + task + " on " + Thread.currentThread().getName());
    }
}
