package threadline.java.interruptflagcheck;

/** Runnable twin of scenarios/java/interrupt-flag-check.yaml. Keep the code in sync with the YAML. */
public class Main {
    static long sink;

    static Thread newWorker() {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                crunch(); // CPU work, never blocks
            }
            System.out.println("saw the flag, stopping");
        }, "worker");
        return worker;
    }

    public static void main(String[] args) throws Exception {
        Thread worker = newWorker();
        worker.start();
        Thread.sleep(100);
        worker.interrupt(); // only sets a flag
        worker.join();
    }

    static void crunch() {
        for (int i = 0; i < 10_000; i++) sink += i;
    }
}
