package threadline.java.syncvsatomic;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/sync-vs-atomic.yaml. Keep the code in sync with the YAML. */
public class Main {
    static int syncCount = 0;
    static final AtomicInteger atomicCount = new AtomicInteger();

    static synchronized void syncIncrement() {
        syncCount++; // only one thread inside at a time
    }

    static void atomicIncrement() {
        int seen;
        do {
            seen = atomicCount.get(); // read
        } while (!atomicCount.compareAndSet(seen, seen + 1)); // write only if unchanged
    }

    public static void main(String[] args) throws Exception {
        Thread s1 = new Thread(Main::syncIncrement, "sync-1");
        Thread s2 = new Thread(Main::syncIncrement, "sync-2");
        Thread a1 = new Thread(Main::atomicIncrement, "atomic-1");
        Thread a2 = new Thread(Main::atomicIncrement, "atomic-2");
        for (Thread t : List.of(s1, s2, a1, a2)) t.start();
        for (Thread t : List.of(s1, s2, a1, a2)) t.join();
        System.out.println(syncCount + " " + atomicCount.get()); // always "2 2"
    }
}
