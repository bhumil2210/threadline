package threadline.java.producerconsumerunbounded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/producer-consumer-unbounded.yaml. Asserts facts, never timings. */
@DisplayName("producer-consumer-unbounded")
class ProducerConsumerUnboundedTest {

    @Test
    @DisplayName("with no limit, put() never waits: all 4 jobs pile up before anyone takes one")
    void producerNeverWaits() throws Exception {
        Main.queue.clear();
        Thread producer = Main.start("producer", Main::produce);
        producer.join(5_000);
        assertFalse(producer.isAlive(), "the producer finished with no consumer running");
        assertEquals(List.of("a", "b", "c", "d"), List.copyOf(Main.queue));
        Main.queue.clear();
    }

    @Test
    @DisplayName("the consumer still handles every job, in order")
    void consumerHandlesAll() throws Exception {
        Main.queue.clear();
        String out = Facts.stdout(() -> Main.main(new String[0]));
        assertEquals(List.of("handled a", "handled b", "handled c", "handled d"), out.lines().toList());
    }

    @Test
    @DisplayName("newFixedThreadPool queues tasks in an unbounded LinkedBlockingQueue")
    void fixedPoolQueueIsUnbounded() {
        var pool = (java.util.concurrent.ThreadPoolExecutor) java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            assertTrue(pool.getQueue() instanceof java.util.concurrent.LinkedBlockingQueue);
            assertEquals(Integer.MAX_VALUE, pool.getQueue().remainingCapacity());
        } finally {
            pool.shutdown();
        }
    }
}
