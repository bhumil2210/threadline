package threadline.java.latchloaderthrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Facts behind scenarios/java/latch-loader-throws.yaml. Asserts facts, never timings. */
@DisplayName("latch-loader-throws")
class LatchLoaderThrowsTest {

    @Test
    @DisplayName("a loader that throws skips countDown(), so the count sticks at 1 and await() never returns")
    void latchNeverOpens() throws Exception {
        CountDownLatch ready = Main.startLoaders();
        assertFalse(ready.await(2, TimeUnit.SECONDS), "the latch must not open");
        assertEquals(1, ready.getCount());
    }

    @Test
    @DisplayName("counting down in finally opens the latch even when a loader throws")
    void finallyFixesIt() throws Exception {
        CountDownLatch ready = new CountDownLatch(3);
        for (String name : java.util.List.of("db", "cache", "config")) {
            Thread t = new Thread(() -> {
                try {
                    Main.load(name);
                } finally {
                    ready.countDown();
                }
            }, name);
            t.setUncaughtExceptionHandler((th, ex) -> {}); // config's exception is expected here
            t.start();
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
    }
}
