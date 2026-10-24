package threadline.java.threadrunbymistake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/thread-run-by-mistake.yaml. Asserts facts, never timings. */
@DisplayName("thread-run-by-mistake")
class ThreadRunByMistakeTest {

    @Test
    @DisplayName("run() executes download() on main, before main goes on; worker stays NEW")
    void runIsAPlainMethodCall() throws Exception {
        Facts.Forked run = Facts.fork(Main.class, 20_000);
        assertEquals(List.of("download on main", "download done", "main goes on", "NEW"), run.lines());
    }
}
