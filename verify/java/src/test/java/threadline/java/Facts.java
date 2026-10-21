package threadline.java;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

/** Small helpers shared by the scenario fact tests. */
public final class Facts {
    private Facts() {}

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Waits (up to 5 s) until the thread reaches the given state. Fails otherwise. */
    public static void awaitState(Thread t, Thread.State state) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (t.getState() != state) {
            assertTrue(System.nanoTime() < deadline, t.getName() + " never reached " + state + ", is " + t.getState());
            Thread.sleep(1);
        }
    }

    /** Runs code and returns what it printed to System.out, trimmed. */
    public static String stdout(ThrowingRunnable code) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));
        try {
            code.run();
        } finally {
            System.setOut(original);
        }
        return out.toString().trim();
    }

    public static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
