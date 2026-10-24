package threadline.java.startingathread;

/** Runnable twin of scenarios/java/starting-a-thread.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        Thread worker = new Thread(() -> download(), "worker");
        worker.start();                     // a new thread runs download()
        System.out.println("main goes on"); // main doesn't wait
        // main ends here; the JVM waits for worker
    }

    static void download() {
        System.out.println("download on " + Thread.currentThread().getName());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("worker done");
    }
}
