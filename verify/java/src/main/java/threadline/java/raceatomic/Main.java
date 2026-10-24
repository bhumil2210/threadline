package threadline.java.raceatomic;

import java.util.concurrent.atomic.AtomicInteger;

/** Runnable twin of scenarios/java/race-atomic.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final AtomicInteger count = new AtomicInteger();

    static void increment() {
        int seen;
        do {
            seen = count.get(); // read
        } while (!count.compareAndSet(seen, seen + 1)); // write only if unchanged
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(Main::increment, "T1");
        Thread t2 = new Thread(Main::increment, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count.get()); // always 2
    }
}
