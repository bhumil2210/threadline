package threadline.java.visibility;

/** Runnable twin of scenarios/java/visibility.yaml. Keep the code in sync with the YAML. */
public class Main {
    static boolean stop = false; // a plain field

    public static void main(String[] args) throws Exception {
        Thread worker = new Thread(() -> {
            long spins = 0;
            while (!stop) { // the JIT may read stop once and never again
                spins++;
            }
            System.out.println("worker stopped after " + spins + " spins");
        }, "worker");
        worker.start();
        Thread.sleep(1_000);
        stop = true; // main writes the flag
        System.out.println("main set stop = true");
        worker.join(); // can wait forever
    }
}
