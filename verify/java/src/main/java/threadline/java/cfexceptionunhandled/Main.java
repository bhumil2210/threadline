package threadline.java.cfexceptionunhandled;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runnable twin of scenarios/java/cf-exception-unhandled.yaml. Keep the code in sync with the YAML. */
public class Main {
    static class ShopDown extends RuntimeException {
        ShopDown(String shop) {
            super("shop " + shop + " is down");
        }
    }

    /** Shared by main and the tests, so never shut down; a daemon thread lets the JVM exit. */
    static final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "io-1");
        t.setDaemon(true);
        return t;
    });

    static CompletableFuture<Void> last;

    public static void main(String[] args) throws Exception {
        var order = CompletableFuture.supplyAsync(() -> priceAt("A"), io) // throws: shop is down
            .thenAccept(p -> System.out.println("price " + p)); // skipped
        System.out.println("main moves on");
        // nobody calls join(), get() or exceptionally(): the error is never seen
        last = order;
        Thread.sleep(300); // give io-1 time to fail
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
