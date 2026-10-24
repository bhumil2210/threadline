package threadline.java.deadlocklockorder;

/** Runnable twin of scenarios/java/deadlock-lock-order.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final Object A = new Object();
    static final Object B = new Object();

    static void lockAThenB(String who) {
        synchronized (A) { // the second thread waits here, holding nothing
            pause();
            synchronized (B) {
                System.out.println(who + " done");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> lockAThenB("T1"), "T1");
        Thread t2 = new Thread(() -> lockAThenB("T2"), "T2"); // same order as T1
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
