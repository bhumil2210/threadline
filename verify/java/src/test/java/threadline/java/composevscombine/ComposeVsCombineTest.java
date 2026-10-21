package threadline.java.composevscombine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/compose-vs-combine.yaml. Asserts facts, never timings. */
@DisplayName("compose-vs-combine")
class ComposeVsCombineTest {

    @Test
    @DisplayName("thenCompose: the second call starts only after the first finishes, and gets its result")
    void composeRunsOneAfterAnother() {
        List<String> log = new CopyOnWriteArrayList<>();
        Main.Order order = CompletableFuture
                .supplyAsync(() -> {
                    log.add("user:start");
                    Facts.sleep(20);
                    log.add("user:done");
                    return new Main.User(42, "Ada");
                }, Main.io)
                .thenCompose(user -> CompletableFuture.supplyAsync(() -> {
                    log.add("order:start for " + user.name());
                    return new Main.Order(7);
                }, Main.io))
                .join();
        assertEquals(new Main.Order(7), order);
        assertEquals(List.of("user:start", "user:done", "order:start for Ada"), log);
    }

    @Test
    @DisplayName("thenCombine: both calls run at the same time, then the merge sees both results")
    void combineRunsSideBySide() {
        // Each call waits at the barrier until the other one is running too. If they were run
        // one after another, the barrier would time out and the call would fail.
        CyclicBarrier bothRunning = new CyclicBarrier(2);
        List<String> merged = new CopyOnWriteArrayList<>();
        String page = CompletableFuture
                .supplyAsync(() -> meet(bothRunning, new Main.User(42, "Ada")), Main.io)
                .thenCombine(
                        CompletableFuture.supplyAsync(() -> meet(bothRunning, "sunny"), Main.io),
                        (user, weather) -> {
                            merged.add(user.name() + "+" + weather);
                            return Main.render(user, weather);
                        })
                .join();
        assertEquals("Ada, it's sunny", page);
        assertEquals(List.of("Ada+sunny"), merged, "the merge runs once, with both results");
    }

    @Test
    @DisplayName("thenApply with a future-returning function gives a future inside a future")
    void thenApplyNestsFutures() {
        Object result = Main.fetchUser(42).thenApply(user -> Main.fetchLastOrder(user)).join();
        assertInstanceOf(CompletableFuture.class, result);
    }

    @Test
    @DisplayName("main prints the order and the page")
    void mainPrints() throws Exception {
        assertEquals("Order[id=7] Ada, it's sunny", Facts.stdout(() -> Main.main(new String[0])));
    }

    private static <T> T meet(CyclicBarrier barrier, T value) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("the other call was not running at the same time", e);
        }
        return value;
    }
}
