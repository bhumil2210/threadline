package threadline.java.visibilityvolatile;

/** Runnable twin of scenarios/java/visibility-volatile.yaml. Keep the code in sync with the YAML. */
public class Main {
    static volatile boolean stop = false; // every read sees the latest write

    public static void main(String[] args) throws Exception {
        Thread worker = new Thread(() -> {
            long spins = 0;
            while (!stop) { // reads stop from memory on every loop
                spins++;
            }
            System.out.println("worker stopped after " + spins + " spins");
        }, "worker");
        worker.start();
        Thread.sleep(1_000);
        stop = true; // main writes the flag
        System.out.println("main set stop = true");
        worker.join(); // returns soon after
    }
}
