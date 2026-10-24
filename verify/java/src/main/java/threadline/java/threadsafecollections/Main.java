package threadline.java.threadsafecollections;

import java.util.ArrayList;
import java.util.List;

/** Runnable twin of scenarios/java/thread-safe-collections.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final List<Integer> list = new ArrayList<>(); // not thread-safe

    static void addMany() {
        for (int i = 0; i < 1_000; i++) {
            list.add(i); // read size, write slot [size], then size = size + 1
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(Main::addMany, "T1");
        Thread t2 = new Thread(Main::addMany, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(list.size()); // often less than 2000, or add() throws
    }
}
