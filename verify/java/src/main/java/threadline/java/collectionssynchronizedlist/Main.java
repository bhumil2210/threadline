package threadline.java.collectionssynchronizedlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runnable twin of scenarios/java/collections-synchronized-list.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final List<Integer> list = Collections.synchronizedList(new ArrayList<>());

    static void addMany() {
        for (int i = 0; i < 1_000; i++) {
            list.add(i); // locks the list: one add at a time
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(Main::addMany, "T1");
        Thread t2 = new Thread(Main::addMany, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(list.size()); // always 2000
    }
}
