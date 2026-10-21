package threadline.java.producerconsumer;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Runnable twin of scenarios/java/producer-consumer.yaml. Keep the code in sync with the YAML. */
public class Main {
    static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(2); // room for 2

    static void produce() throws InterruptedException {
        for (String job : List.of("a", "b", "c", "d")) {
            queue.put(job); // waits while the queue is full
        }
    }

    static void consume() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            String job = queue.take(); // waits while the queue is empty
            handle(job);               // slow: takes a while per job
        }
    }

    static void handle(String job) throws InterruptedException {
        Thread.sleep(30);
        System.out.println("handled " + job);
    }

    interface Body {
        void run() throws InterruptedException;
    }

    static Thread start(String name, Body body) {
        Thread t = new Thread(() -> {
            try {
                body.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, name);
        t.start();
        return t;
    }

    public static void main(String[] args) throws Exception {
        Thread consumer = start("consumer", Main::consume);
        Thread producer = start("producer", Main::produce);
        producer.join();
        consumer.join();
    }
}
