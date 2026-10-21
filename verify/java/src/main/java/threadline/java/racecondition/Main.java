package threadline.java.racecondition;

/** Runnable twin of scenarios/java/race-condition.yaml. Keep the code in sync with the YAML. */
public class Main {
    static int count = 0;

    static void increment() {
        count++; // read count, add 1, write count
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(Main::increment, "T1");
        Thread t2 = new Thread(Main::increment, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count); // can print 1
    }
}
