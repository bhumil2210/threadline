package threadline.java.collectionsconcurrentmap;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Runnable twin of scenarios/java/collections-concurrent-map.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final Map<String, Integer> counts = new ConcurrentHashMap<>();

    static void countWords(List<String> words) {
        for (String w : words) {
            counts.merge(w, 1, Integer::sum); // atomic for each key
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> countWords(List.of("a", "a")), "T1");
        Thread t2 = new Thread(() -> countWords(List.of("b", "a")), "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(new TreeMap<>(counts)); // always {a=3, b=1}
    }
}
