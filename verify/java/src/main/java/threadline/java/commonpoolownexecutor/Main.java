package threadline.java.commonpoolownexecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runnable twin of scenarios/java/common-pool-own-executor.yaml. Keep the code in sync with the YAML.
 * Run with -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 to match the scenario.
 */
public class Main {
    static final long T0 = System.nanoTime();

    static final ExecutorService ioPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "io-1");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws Exception {
        Thread http = new Thread(Main::handleRequest, "http-1");
        http.start();
        // A batch job on main:
        List.of(1, 2, 3, 4, 5, 6).parallelStream()
            .forEach(id -> buildReport(id)); // slow and blocking: holds a common-pool thread
        log("stream done");
        http.join();
    }

    static void handleRequest() {
        pause(50); // the request arrives just after the stream starts
        // Meanwhile, a request handler on http-1:
        String r = CompletableFuture.supplyAsync(() -> quickTask(), ioPool) // its own pool
            .join(); // runs at once on io-1
        log("got " + r);
    }

    static void buildReport(int id) {
        log("report " + id + " start");
        pause(300); // e.g. a database query
        log("report " + id + " end");
    }

    static String quickTask() {
        log("quickTask");
        return "ok";
    }

    /** One line per event: "<ms> <thread> <what>". Order matters to the tests, not the times. */
    static synchronized void log(String what) {
        System.out.println((System.nanoTime() - T0) / 1_000_000 + " " + Thread.currentThread().getName() + " " + what);
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
