package threadline.java.cfthreadhopping;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/** Runnable twin of scenarios/java/cf-thread-hopping.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        var ioPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "io-1"));
        var cpuPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "cpu-1"));

        var f = CompletableFuture.supplyAsync(() -> slowCall(), ioPool);
        var a = f.thenApply(r -> parse(r));               // non-async: no thread of its own
        var b = f.thenApplyAsync(r -> parse(r), cpuPool); // async: submitted to cpuPool

        var done = CompletableFuture.completedFuture("cached");
        done.thenApply(r -> parse(r));            // already done: runs right here

        CompletableFuture.allOf(a, b).join();
        ioPool.shutdown();
        cpuPool.shutdown();
    }

    static String slowCall() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "fresh";
    }

    static String parse(String r) {
        System.out.println("parse(" + r + ") on " + Thread.currentThread().getName());
        return r.toUpperCase();
    }
}
