package threadline.java.daemonthread;

/** Runnable twin of scenarios/java/daemon-thread.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        Thread worker = new Thread(() -> download(), "worker");
        worker.setDaemon(true);             // the JVM won't wait for a daemon thread
        worker.start();
        System.out.println("main goes on");
        // main ends here: the JVM exits and stops worker mid-download
    }

    static void download() {
        try {
            Thread.sleep(3_000);
            System.out.println("worker done");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("cleanup"); // never printed: daemon threads get no finally at exit
        }
    }
}
