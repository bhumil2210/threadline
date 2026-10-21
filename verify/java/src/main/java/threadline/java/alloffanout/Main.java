package threadline.java.alloffanout;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/allof-fanout.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final AtomicInteger threadNo = new AtomicInteger();
    /** Shared by main and the tests, so never shut down; daemon threads let the JVM exit. */
    static final ExecutorService io =
            Executors.newFixedThreadPool(3, Main::ioThread);

    static final Map<String, Integer> PRICES = Map.of("A", 12, "B", 15, "C", 9);
    static final Map<String, Integer> DELAY_MS = Map.of("A", 40, "B", 100, "C", 60);

    static Thread ioThread(Runnable r) {
        Thread t = new Thread(r, "io-" + threadNo.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    public static void main(String[] args) {
        var a = CompletableFuture.supplyAsync(() -> priceAt("A"), io);
        var b = CompletableFuture.supplyAsync(() -> priceAt("B"), io);
        var c = CompletableFuture.supplyAsync(() -> priceAt("C"), io);

        CompletableFuture.allOf(a, b, c).join(); // waits for the slowest
        int cheapest = Math.min(a.join(), Math.min(b.join(), c.join())); // no waiting now
        System.out.println(cheapest);
    }

    static int priceAt(String shop) {
        try {
            Thread.sleep(DELAY_MS.get(shop));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return PRICES.get(shop);
    }
}
