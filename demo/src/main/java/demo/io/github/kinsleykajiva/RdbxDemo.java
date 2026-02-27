package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import io.github.kinsleykajiva.libsrtp.srtp_h;
import io.github.kinsleykajiva.libsrtp.srtp_rdbx_t;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class RdbxDemo {
    private static final int NUM_TRIALS = 1000;

    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("Extended Replay Database (RDBX) Demo");

        try (Arena arena = Arena.ofConfined()) {
            testRdbx(arena);
        }

        System.out.println("Rdbx Demo passed!");
    }

    private static void testRdbx(Arena arena) {
        MemorySegment rdbx = srtp_rdbx_t.allocate(arena);
        
        if (srtp_h.srtp_rdbx_init(rdbx, 128) != 0) {
            System.err.println("Error: srtp_rdbx_init failed");
            System.exit(1);
        }

        System.out.println("Testing sequential insertion...");
        for (int i = 1; i <= NUM_TRIALS; i++) {
            // Initial index is 0. To add index i, delta is always 1 from i-1.
            int status = srtp_h.srtp_rdbx_check(rdbx, 1);
            if (status != 0) {
                System.err.println("Error: srtp_rdbx_check failed at index " + i + " status " + status);
                System.exit(1);
            }
            status = srtp_h.srtp_rdbx_add_index(rdbx, 1);
            if (status != 0) {
                System.err.println("Error: srtp_rdbx_add_index failed at index " + i + " status " + status);
                System.exit(1);
            }
        }

        System.out.println("Testing for false positives (replaying packets)...");
        for (int i = 1; i <= NUM_TRIALS; i++) {
            // Current index is NUM_TRIALS. To check index i, delta is i - NUM_TRIALS.
            long delta = (long)i - NUM_TRIALS;
            int status = srtp_h.srtp_rdbx_check(rdbx, delta);
            if (status == 0) {
                System.err.println("Error: srtp_rdbx_check failed to detect replay at index " + i + " (delta " + delta + ")");
                System.exit(1);
            }
        }
        System.out.println("Sequential tests passed.");
    }
}
