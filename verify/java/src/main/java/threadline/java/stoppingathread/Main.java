package threadline.java.stoppingathread;

/** Runnable twin of scenarios/java/stopping-a-thread.yaml. Keep the code in sync with the YAML. */
public class Main {
    static Thread newWorker() {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(10_000); // TIMED_WAITING
            } catch (InterruptedException e) {
                System.out.println("woken early, cleaning up");
            }
        }, "worker");
        return worker;
    }

    public static void main(String[] args) throws Exception {
        Thread worker = newWorker();
        worker.start();
        Thread.sleep(100);
        worker.interrupt(); // asks worker to stop
        worker.join();
    }
}
