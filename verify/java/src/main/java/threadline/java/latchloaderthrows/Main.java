package threadline.java.latchloaderthrows;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/** Runnable twin of scenarios/java/latch-loader-throws.yaml. Keep the code in sync with the YAML. */
public class Main {
    static CountDownLatch startLoaders() {
        CountDownLatch ready = new CountDownLatch(3);
        for (String name : List.of("db", "cache", "config")) {
            new Thread(() -> {
                load(name);        // config throws here
                ready.countDown(); // so config never counts down
            }, name).start();
        }
        return ready;
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch ready = startLoaders();
        ready.await(); // stuck at 1: waits forever
        System.out.println("all loaded, start serving"); // never printed
    }

    static void load(String what) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (what.equals("config")) throw new IllegalStateException("config file missing");
    }
}
