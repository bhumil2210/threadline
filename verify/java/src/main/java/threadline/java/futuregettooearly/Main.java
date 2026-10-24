package threadline.java.futuregettooearly;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Runnable twin of scenarios/java/future-get-too-early.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final List<String> log = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<String> f = pool.submit(() -> slowCall()); // returns right away
        String result = f.get();                          // main blocks at once
        doOtherWork();                                    // starts only after slowCall
        System.out.println(result);
        pool.shutdown();
    }

    static String slowCall() throws InterruptedException {
        Thread.sleep(200);
        log.add("slowCall done");
        return "data";
    }

    static void doOtherWork() throws InterruptedException {
        log.add("doOtherWork start");
        Thread.sleep(50);
    }
}
