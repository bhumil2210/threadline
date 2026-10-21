package threadline.java.cfpipeline;

import java.util.concurrent.CompletableFuture;

/** Runnable twin of scenarios/java/cf-pipeline.yaml. Keep the code in sync with the YAML. */
public class Main {
    record User(int id, String name) {}

    public static void main(String[] args) {
        CompletableFuture
            .supplyAsync(() -> fetchUser(42))              // starts on a pool thread
            .thenApply(user -> user.name())                // after fetchUser
            .thenAccept(name -> System.out.println(name))  // after thenApply
            .join();                                       // main waits for the chain
    }

    static User fetchUser(int id) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new User(id, "Ada");
    }
}
