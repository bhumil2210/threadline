package threadline.java.commonpool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/common-pool.yaml. Asserts facts, never timings. */
@DisplayName("common-pool")
class CommonPoolTest {
    static final String PARALLELISM_2 = "-Djava.util.concurrent.ForkJoinPool.common.parallelism=2";

    /** "<ms> <thread> <what>" → "<thread> <what>", in print order. */
    static List<String> events(Facts.Forked run) {
        return run.lines().stream().map(l -> l.substring(l.indexOf(' ') + 1)).toList();
    }

    static int indexOf(List<String> events, String suffix) {
        for (int i = 0; i < events.size(); i++) if (events.get(i).endsWith(suffix)) return i;
        return -1;
    }

    @Test
    @DisplayName("the stream runs on main plus the 2 common-pool workers")
    void streamUsesMainAndWorkers() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 30_000, PARALLELISM_2);
        assertTrue(run.exited(), run.out());
        Set<String> threads = events(run).stream().filter(e -> e.contains(" report ") && e.endsWith(" start"))
                .map(e -> e.substring(0, e.indexOf(' '))).collect(Collectors.toSet());
        assertTrue(threads.contains("main"), "the calling thread takes part: " + threads);
        assertEquals(3, threads.size(), "main + 2 workers: " + threads);
        threads.stream().filter(t -> !t.equals("main"))
                .forEach(t -> assertTrue(t.startsWith("ForkJoinPool.commonPool-worker-"), t));
    }

    @Test
    @DisplayName("quickTask runs on a common-pool worker, and only after a report has finished")
    void quickTaskWaitsForAFreeWorker() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 30_000, PARALLELISM_2);
        assertTrue(run.exited(), run.out());
        List<String> events = events(run);
        int quick = indexOf(events, " quickTask");
        assertTrue(events.get(quick).startsWith("ForkJoinPool.commonPool-worker-"), events.get(quick));
        int firstEnd = -1;
        for (int i = 0; i < events.size(); i++) if (events.get(i).matches(".* report \\d end")) { firstEnd = i; break; }
        assertTrue(firstEnd >= 0 && quick > firstEnd, "quickTask ran while every worker was busy:\n" + run.out());
    }

    /** Prints the common pool's size; forked with a CPU count. */
    public static class PoolSize {
        public static void main(String[] args) {
            System.out.println(java.util.concurrent.ForkJoinPool.getCommonPoolParallelism());
        }
    }

    @Test
    @DisplayName("by default the common pool has one thread fewer than there are CPU cores")
    void defaultSizeIsCoresMinusOne() throws Exception {
        assertEquals("3", Facts.fork(PoolSize.class, 20_000, "-XX:ActiveProcessorCount=4").out());
        assertEquals("1", Facts.fork(PoolSize.class, 20_000, "-XX:ActiveProcessorCount=2").out());
    }
}
