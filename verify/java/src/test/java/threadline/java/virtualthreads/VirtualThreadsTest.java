package threadline.java.virtualthreads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/**
 * Facts behind scenarios/java/virtual-threads.yaml. Asserts facts, never timings.
 * The test JVM runs with jdk.virtualThreadScheduler.parallelism=2 (see build.gradle.kts): two carriers.
 */
@DisplayName("virtual-threads")
class VirtualThreadsTest {

    @Test
    @DisplayName("1,000 sleeping virtual threads share at most 2 carrier threads")
    void manyVirtualThreadsFewCarriers() {
        Set<String> carriers = ConcurrentHashMap.newKeySet();
        AtomicInteger virtualCount = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1_000; i++) {
                executor.submit(() -> {
                    if (Thread.currentThread().isVirtual()) virtualCount.incrementAndGet();
                    carriers.add(carrier());
                    Thread.sleep(50);
                    carriers.add(carrier());
                    return null;
                });
            }
        }
        assertEquals(1_000, virtualCount.get());
        assertTrue(carriers.size() <= 2, "carriers: " + carriers);
        carriers.forEach(c -> assertTrue(c.startsWith("ForkJoinPool-"), c));
    }

    @Test
    @DisplayName("after sleep(), a virtual thread can resume on a different carrier")
    void virtualThreadsHopCarriers() {
        AtomicInteger hops = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 200; i++) {
                executor.submit(() -> {
                    String before = carrier();
                    Thread.sleep(20);
                    if (!before.equals(carrier())) hops.incrementAndGet();
                    return null;
                });
            }
        }
        assertTrue(hops.get() > 0, "no virtual thread changed carrier across 200 sleeps");
    }

    @Test
    @DisplayName("sleeping inside synchronized pins the carrier before JDK 24, not from JDK 24 on")
    void synchronizedPinsBeforeJdk24() throws Exception {
        Object lock = new Object();
        Path file = Files.createTempFile("pinned", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("jdk.VirtualThreadPinned").withThreshold(Duration.ZERO);
            recording.start();
            Thread vt = Thread.ofVirtual().start(() -> {
                synchronized (lock) {
                    Facts.sleep(50);
                }
            });
            vt.join();
            recording.stop();
            recording.dump(file);
        }
        long pinned = RecordingFile.readAllEvents(file).stream()
                .filter(e -> e.getEventType().getName().equals("jdk.VirtualThreadPinned"))
                .count();
        Files.deleteIfExists(file);

        if (Runtime.version().feature() < 24) {
            assertTrue(pinned > 0, "JDK " + Runtime.version().feature() + ": expected a pinned event");
        } else {
            assertEquals(0, pinned, "JDK " + Runtime.version().feature() + ": synchronized no longer pins");
        }
    }

    @Test
    @DisplayName("main runs vt-1, vt-2, vt-3, each computing before and after its sleep")
    void mainRunsThreeNamedVirtualThreads() throws Exception {
        var lines = Facts.stdout(() -> Main.main(new String[0])).lines().toList();
        assertEquals("done", lines.get(lines.size() - 1));
        var names = lines.stream().filter(l -> l.startsWith("vt-")).map(l -> l.substring(0, 4)).toList();
        assertEquals(6, names.size(), lines.toString());
        assertEquals(Set.of("vt-1", "vt-2", "vt-3"), Set.copyOf(names));
    }

    /** The carrier is only visible through VirtualThread.toString(), e.g. "...@ForkJoinPool-1-worker-2". */
    private static String carrier() {
        String s = Thread.currentThread().toString();
        return s.substring(s.lastIndexOf('@') + 1);
    }
}
