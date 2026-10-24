package threadline.java.threadrunbymistake;

/** Runnable twin of scenarios/java/thread-run-by-mistake.yaml. Keep the code in sync with the YAML. */
public class Main {
    public static void main(String[] args) {
        Thread worker = new Thread(() -> download(), "worker");
        worker.run();                       // a plain method call: runs here, on main
        System.out.println("main goes on"); // only after download() returns
        System.out.println(worker.getState()); // NEW: worker never started
    }

    static void download() {
        System.out.println("download on " + Thread.currentThread().getName());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("download done");
    }
}
