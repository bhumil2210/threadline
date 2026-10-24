package threadline.java.cfexceptionunhandled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/cf-exception-unhandled.yaml. Asserts facts, never timings. */
@DisplayName("cf-exception-unhandled")
class CfExceptionUnhandledTest {

    @Test
    @DisplayName("the exception is stored in the future: thenAccept is skipped and nothing is printed or logged")
    void failureIsSilent() throws Exception {
        // Its own JVM, so stdout and stderr are both captured: no stack trace appears anywhere.
        Facts.Forked run = Facts.fork(Main.class, 20_000);
        assertTrue(run.exited(), run.out());
        assertEquals("main moves on", run.out());
    }

    @Test
    @DisplayName("the future did fail, with the exception wrapped")
    void futureFailed() throws Exception {
        Facts.stdout(() -> Main.main(new String[0]));
        Main.last.handle((v, ex) -> null).join(); // wait for the chain, whatever the outcome
        assertTrue(Main.last.isCompletedExceptionally());
        Throwable cause = Main.last.handle((v, ex) -> ex).join();
        assertTrue(cause.getCause() instanceof Main.ShopDown, String.valueOf(cause));
    }
}
