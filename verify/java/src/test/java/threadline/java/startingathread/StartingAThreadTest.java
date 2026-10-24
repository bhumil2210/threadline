package threadline.java.startingathread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/starting-a-thread.yaml. Asserts facts, never timings. */
@DisplayName("starting-a-thread")
class StartingAThreadTest {

    @Test
    @DisplayName("a new Thread is NEW until start(), which runs it on its own thread")
    void startRunsOnTheNewThread() throws Exception {
        Thread worker = new Thread(() -> {}, "worker");
        assertEquals(Thread.State.NEW, worker.getState(), "creating a Thread object runs nothing");
        worker.start();
        assertNotEquals(Thread.State.NEW, worker.getState());
        worker.join();
    }

    @Test
    @DisplayName("main finishes first, download() runs on worker, and the JVM waits for it")
    void jvmWaitsForTheNonDaemonWorker() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 20_000);
        assertTrue(run.exited(), run.out());
        List<String> lines = run.lines();
        assertTrue(lines.contains("download on worker"), run.out());
        assertTrue(lines.indexOf("main goes on") < lines.indexOf("worker done"), "main is done before worker: " + lines);
        assertEquals("worker done", lines.getLast(), "the JVM only exits after worker finishes");
    }

    @Test
    @DisplayName("start() can only be called once")
    void startOnlyOnce() throws Exception {
        Thread worker = new Thread(() -> {}, "worker");
        worker.start();
        worker.join();
        assertThrows(IllegalThreadStateException.class, worker::start);
    }
}
