package threadline.java.cfexceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/cf-exceptions.yaml. Asserts facts, never timings. */
@DisplayName("cf-exceptions")
class CfExceptionsTest {

    @Test
    @DisplayName("after a failure, thenApply is skipped and exceptionally recovers with -1")
    void failureSkipsThenApplyAndRecovers() {
        AtomicBoolean thenApplyRan = new AtomicBoolean();
        AtomicReference<Throwable> seen = new AtomicReference<>();
        int price = CompletableFuture.supplyAsync(() -> Main.priceAt("A"), Main.io)
                .thenApply(p -> {
                    thenApplyRan.set(true);
                    return p * 2;
                })
                .exceptionally(ex -> {
                    seen.set(ex);
                    return -1;
                })
                .join();
        assertEquals(-1, price);
        assertFalse(thenApplyRan.get(), "thenApply never ran: there was no value");
        assertInstanceOf(CompletionException.class, seen.get(), "the error arrives wrapped");
        assertInstanceOf(Main.ShopDown.class, seen.get().getCause(), "getCause() is the real one");
    }

    @Test
    @DisplayName("exceptionally does not run on success")
    void exceptionallySkippedOnSuccess() {
        AtomicBoolean ran = new AtomicBoolean();
        int price = CompletableFuture.supplyAsync(() -> Main.priceAt("B"), Main.io)
                .exceptionally(ex -> {
                    ran.set(true);
                    return -1;
                })
                .join();
        assertEquals(12, price);
        assertFalse(ran.get());
    }

    @Test
    @DisplayName("handle always runs: (value, null) on success, (null, error) on failure")
    void handleAlwaysRuns() {
        String ok = CompletableFuture.supplyAsync(() -> Main.priceAt("B"), Main.io)
                .handle((p, ex) -> p + "/" + ex)
                .join();
        assertEquals("12/null", ok);

        AtomicReference<Integer> value = new AtomicReference<>(0);
        Throwable error = CompletableFuture.supplyAsync(() -> Main.priceAt("A"), Main.io)
                .handle((p, ex) -> {
                    value.set(p);
                    return ex;
                })
                .join();
        assertNull(value.get());
        assertInstanceOf(Main.ShopDown.class, error.getCause());
    }

    @Test
    @DisplayName("main prints -1 €12")
    void mainPrints() throws Exception {
        assertEquals("-1 €12", Facts.stdout(() -> Main.main(new String[0])));
    }
}
