package threadline.java.virtualthreads;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Runnable twin of scenarios/java/virtual-threads.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        ThreadFactory vts = Thread.ofVirtual().name("vt-", 1).factory();
        try (var executor = Executors.newThreadPerTaskExecutor(vts)) {
            for (int i = 0; i < 3; i++) {
                executor.submit(() -> {
                    compute();            // runs on a carrier thread
                    Thread.sleep(200);    // blocks: the virtual thread unmounts
                    compute();            // remounts, maybe on another carrier
                    return null;
                });
            }
        } // close() waits for every task
        System.out.println("done");
    }

    /** Prints which virtual thread is running and which carrier it is mounted on. */
    static void compute() {
        System.out.println(carrierInfo());
    }

    /** e.g. "vt-1 on ForkJoinPool-1-worker-2". Parsed from VirtualThread.toString(). */
    static String carrierInfo() {
        Thread t = Thread.currentThread();
        String s = t.toString();
        int at = s.lastIndexOf('@');
        return t.getName() + " on " + (at >= 0 ? s.substring(at + 1) : "?");
    }
}
