package threadline.java.producerconsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/producer-consumer.yaml. Asserts facts, never timings. */
@DisplayName("producer-consumer")
class ProducerConsumerTest {

    @Test
    @DisplayName("take() on an empty queue leaves the consumer WAITING until something is put")
    void takeWaitsWhenEmpty() throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
        List<String> got = new CopyOnWriteArrayList<>();
        Thread consumer = new Thread(() -> {
            try {
                got.add(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");
        consumer.start();
        Facts.awaitState(consumer, Thread.State.WAITING);
        queue.put("a");
        consumer.join(5_000);
        assertEquals(List.of("a"), got);
    }

    @Test
    @DisplayName("put() on a full queue leaves the producer WAITING until a take() frees a slot")
    void putWaitsWhenFull() throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
        Thread producer = new Thread(() -> {
            try {
                for (String job : List.of("b", "c", "d")) queue.put(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");
        producer.start();
        Facts.awaitState(producer, Thread.State.WAITING);
        assertEquals(List.of("b", "c"), List.copyOf(queue), "full at 2; d is not in yet");

        assertEquals("b", queue.take());
        producer.join(5_000);
        assertEquals(List.of("c", "d"), List.copyOf(queue), "the freed slot let d in");
    }

    @Test
    @DisplayName("jobs come out in the order they went in")
    void fifoOrder() throws Exception {
        String out = Facts.stdout(() -> Main.main(new String[0]));
        assertEquals(List.of("handled a", "handled b", "handled c", "handled d"), out.lines().toList());
    }
}
