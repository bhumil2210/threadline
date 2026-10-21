package threadline.java.cfexceptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/cf-exceptions.yaml. Keep the code in sync with the YAML. */
public class Main {
    static class ShopDown extends RuntimeException {
        ShopDown(String shop) {
            super("shop " + shop + " is down");
        }
    }

    static final AtomicInteger threadNo = new AtomicInteger();
    /** Shared by main and the tests, so never shut down; daemon threads let the JVM exit. */
    static final ExecutorService io =
            Executors.newFixedThreadPool(2, Main::ioThread);

    static Thread ioThread(Runnable r) {
        Thread t = new Thread(r, "io-" + threadNo.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    public static void main(String[] args) {
        var price = CompletableFuture.supplyAsync(() -> priceAt("A"), io) // throws: shop is down
            .thenApply(p -> p * 2)        // skipped: there is no value
            .exceptionally(ex -> -1);     // runs only on failure: fallback -1

        var label = CompletableFuture.supplyAsync(() -> priceAt("B"), io) // works
            .handle((p, ex) -> ex == null ? "€" + p : "n/a"); // always runs

        System.out.println(price.join() + " " + label.join()); // -1 €12
    }

    static int priceAt(String shop) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (shop.equals("A")) throw new ShopDown(shop);
        return 12;
    }
}
