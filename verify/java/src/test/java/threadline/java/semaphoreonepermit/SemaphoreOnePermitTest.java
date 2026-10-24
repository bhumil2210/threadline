package threadline.java.semaphoreonepermit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import threadline.java.Facts;

/** Facts behind scenarios/java/semaphore-one-permit.yaml. Asserts facts, never timings. */
@DisplayName("semaphore-one-permit")
class SemaphoreOnePermitTest {

    @Test
    @DisplayName("with 1 permit, only one worker is ever inside callApi")
    void oneAtATime() throws Exception {
        assertEquals("max inside callApi: 1", Facts.stdout(() -> Main.main(new String[0])));
    }

    @Test
    @DisplayName("a permit has no owner: another thread can release it")
    void anyThreadCanRelease() throws Exception {
        var permits = new java.util.concurrent.Semaphore(1);
        permits.acquire();
        Thread other = new Thread(permits::release, "other");
        other.start();
        other.join();
        assertEquals(1, permits.availablePermits());
    }
}
