package threadline.java.composevscombine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/compose-vs-combine.yaml. Keep the code in sync with the YAML. */
public class Main {
    record User(int id, String name) {}

    record Order(int id) {}

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
        // thenCompose: step 2 needs step 1's result, so they run one after another
        Order last = fetchUser(42)
            .thenCompose(user -> fetchLastOrder(user))
            .join();

        // thenCombine: two independent calls run at the same time, then merge
        String page = fetchUser(42)
            .thenCombine(fetchWeather(), (user, weather) -> render(user, weather))
            .join();

        System.out.println(last + " " + page);
    }

    static CompletableFuture<User> fetchUser(int id) {
        return CompletableFuture.supplyAsync(() -> slow(new User(id, "Ada"), 100), io);
    }

    static CompletableFuture<Order> fetchLastOrder(User user) {
        return CompletableFuture.supplyAsync(() -> slow(new Order(7), 150), io);
    }

    static CompletableFuture<String> fetchWeather() {
        return CompletableFuture.supplyAsync(() -> slow("sunny", 150), io);
    }

    static String render(User user, String weather) {
        return user.name() + ", it's " + weather;
    }

    static <T> T slow(T value, long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return value;
    }
}
