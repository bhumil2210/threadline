package threadline.java.waitvssleep;

/** Runnable twin of scenarios/java/wait-vs-sleep.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final Object lock = new Object();

    static void napper() throws InterruptedException {
        synchronized (lock) {
            Thread.sleep(1_000); // sleeps, still holding the lock
        }
    }

    static void other() {
        synchronized (lock) { // BLOCKED until napper leaves
            System.out.println("other got the lock");
        }
    }

    public static void main(String[] args) throws Exception {
        Thread napper = new Thread(() -> {
            try {
                napper();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "napper");
        Thread other = new Thread(Main::other, "other");
        napper.start();
        Thread.sleep(100);
        other.start();
        napper.join();
        other.join();
    }
}
