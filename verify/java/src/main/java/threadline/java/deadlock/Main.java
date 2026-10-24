package threadline.java.deadlock;

/** Runnable twin of scenarios/java/deadlock.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final Object A = new Object();
    static final Object B = new Object();

    static void lockAThenB() {
        synchronized (A) {
            pause(); // meanwhile T2 takes B
            synchronized (B) { // BLOCKED: T2 holds B
                System.out.println("T1 done");
            }
        }
    }

    static void lockBThenA() {
        synchronized (B) {
            pause();
            synchronized (A) { // BLOCKED: T1 holds A
                System.out.println("T2 done");
            }
        }
    }

    public static void main(String[] args) {
        new Thread(Main::lockAThenB, "T1").start();
        new Thread(Main::lockBThenA, "T2").start(); // both wait forever
    }

    static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
