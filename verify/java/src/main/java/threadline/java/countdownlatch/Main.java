package threadline.java.countdownlatch;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/** Runnable twin of scenarios/java/count-down-latch.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) throws Exception {
        CountDownLatch ready = new CountDownLatch(3);
        for (String name : List.of("db", "cache", "config")) {
            new Thread(() -> {
                load(name);
                ready.countDown(); // one fewer to wait for
            }, name).start();
        }
        ready.await(); // main waits until the count reaches 0
        System.out.println("all loaded, start serving");
    }

    static void load(String what) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
