package threadline.java.interruptswallowed;

/** Runnable twin of scenarios/java/interrupt-swallowed.yaml. Keep the code in sync with the YAML. */
public class Main {
    static Thread newWorker() {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    // swallowed: throwing it cleared the flag
                }
            }
        }, "worker");
        return worker;
    }

    public static void main(String[] args) throws Exception {
        Thread worker = newWorker();
        worker.start();
        Thread.sleep(100);
        worker.interrupt();
        worker.join(); // waits forever
    }
}
