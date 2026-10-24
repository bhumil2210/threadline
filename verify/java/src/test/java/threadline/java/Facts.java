package threadline.java;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    /** What a program printed (stdout and stderr) in its own JVM, and whether it exited before the timeout. */
    public record Forked(boolean exited, String out) {
        public List<String> lines() {
            return out.lines().toList();
        }
    }

    /**
     * Runs a main class in a fresh JVM with the same classpath, and kills it after the timeout.
     * Use it for facts about JVM exit, JIT behavior or JVM-wide settings like the common pool size.
     */
    public static Forked fork(Class<?> main, long timeoutMs, String... jvmArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.addAll(List.of(jvmArgs));
        cmd.addAll(List.of("-cp", System.getProperty("java.class.path"), main.getName()));
        Path log = Files.createTempFile("threadline-fork", ".log");
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            boolean exited = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!exited) {
                p.destroyForcibly();
                p.waitFor();
            }
            return new Forked(exited, Files.readString(log).trim());
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
