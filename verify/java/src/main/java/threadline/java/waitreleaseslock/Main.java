package threadline.java.waitreleaseslock;

/** Runnable twin of scenarios/java/wait-releases-lock.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final Object lock = new Object();
    static boolean ready = false;

    static void waiter() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {
                lock.wait(); // lets go of the lock while it waits
            }
            System.out.println("waiter: ready");
        }
    }

    static void notifier() {
        synchronized (lock) { // gets in right away
            ready = true;
            lock.notifyAll(); // wakes waiter, which still needs the lock back
        }
    }

    public static void main(String[] args) throws Exception {
        Thread waiter = new Thread(() -> {
            try {
                waiter();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        Thread notifier = new Thread(Main::notifier, "notifier");
        waiter.start();
        Thread.sleep(100);
        notifier.start();
        waiter.join();
        notifier.join();
    }
}
