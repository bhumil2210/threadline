package threadline.java.racecondition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Facts behind scenarios/java/race-condition.yaml. Asserts facts, never timings. */
@DisplayName("race-condition")
class RaceConditionTest {

    @Test
    @DisplayName("count++ compiles to separate read, add and write steps")
    void countPlusPlusIsThreeSteps() {
        ToolProvider javap = ToolProvider.findFirst("javap").orElseThrow();
        StringWriter out = new StringWriter();
        String classPath = System.getProperty("java.class.path");
        int exit = javap.run(new PrintWriter(out), new PrintWriter(out),
                "-c", "-p", "-cp", classPath, Main.class.getName());
        assertEquals(0, exit, out.toString());

        String body = out.toString().split("static void increment\\(\\);")[1].split("\n\n")[0];
        List<String> ops = Stream.of(body.split("\n"))
                .map(String::trim)
                .filter(l -> l.matches("\\d+: .*"))
                .map(l -> l.replaceFirst("^\\d+: ", "").split("\\s+")[0])
                .toList();
        assertEquals(List.of("getstatic", "iconst_1", "iadd", "putstatic", "return"), ops,
                "read (getstatic), add (iadd), write (putstatic) are separate instructions");
    }

    @Test
    @DisplayName("increment runs on T1 and T2, not on main")
    void incrementRunsOnTheStartedThreads() throws Exception {
        Set<String> ranOn = ConcurrentHashMap.newKeySet();
        Runnable task = () -> {
            ranOn.add(Thread.currentThread().getName());
            Main.increment();
        };
        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(Set.of("T1", "T2"), ranOn);
    }

    @Test
    @DisplayName("two threads running count++ lose updates")
    void concurrentIncrementsLoseUpdates() throws Exception {
        int perThread = 1_000_000;
        int trials = 50;
        for (int trial = 1; trial <= trials; trial++) {
            int result = runTrial(perThread);
            if (result < 2 * perThread) {
                System.out.printf("trial %d: expected %d, got %d (%d lost)%n",
                        trial, 2 * perThread, result, 2 * perThread - result);
                return;
            }
        }
        fail("no lost update in " + trials + " trials; count++ behaved atomically");
    }

    private static int runTrial(int perThread) throws InterruptedException {
        Main.count = 0;
        CountDownLatch go = new CountDownLatch(1);
        Runnable task = () -> {
            try {
                go.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < perThread; i++) {
                Main.increment();
            }
        };
        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        go.countDown();
        t1.join();
        t2.join();
        return Main.count;
    }
}
