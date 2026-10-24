package threadline.java.racesynchronized;

/** Runnable twin of scenarios/java/race-synchronized.yaml. Keep the code in sync with the YAML. */
public class Main {
    static int count = 0;

    static synchronized void increment() {
        count++; // only one thread inside at a time
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(Main::increment, "T1");
        Thread t2 = new Thread(Main::increment, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count); // always 2
    }
}
