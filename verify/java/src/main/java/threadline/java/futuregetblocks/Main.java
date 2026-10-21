package threadline.java.futuregetblocks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Runnable twin of scenarios/java/future-get-blocks.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<String> f = pool.submit(() -> slowCall()); // returns right away
        doOtherWork();                                    // main is still free
        String result = f.get();                          // main blocks here
        System.out.println(result);
        pool.shutdown();
    }

    static String slowCall() throws InterruptedException {
        Thread.sleep(200);
        return "data";
    }

    static void doOtherWork() throws InterruptedException {
        Thread.sleep(50);
    }
}
