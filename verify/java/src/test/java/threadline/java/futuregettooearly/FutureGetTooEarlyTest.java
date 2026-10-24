package threadline.java.futuregettooearly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/future-get-too-early.yaml. Asserts facts, never timings. */
@DisplayName("future-get-too-early")
class FutureGetTooEarlyTest {

    @Test
    @DisplayName("get() right after submit() means doOtherWork only starts once slowCall is done")
    void nothingOverlaps() throws Exception {
        Main.log.clear();
        assertEquals("data", Facts.stdout(() -> Main.main(new String[0])));
        assertEquals(List.of("slowCall done", "doOtherWork start"), Main.log);
    }
}
