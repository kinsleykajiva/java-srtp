package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_rdb_add_index;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_rdb_check;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_rdb_init;
import io.github.kinsleykajiva.libsrtp.srtp_rdb_t;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class ReplayDemo {
    private static final int NUM_TRIALS = 1000;

    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("Anti-Replay Database Demo");

        try (Arena arena = Arena.ofConfined()) {
            testRdb(arena);
        }

        System.out.println("Replay Demo passed!");
    }

    private static void testRdb(Arena arena) {
        MemorySegment rdb = srtp_rdb_t.allocate(arena);
        
        if (srtp_rdb_init(rdb) != 0) {
            System.err.println("Error: srtp_rdb_init failed");
            System.exit(1);
        }

        System.out.println("Testing sequential insertion...");
        for (int i = 0; i < NUM_TRIALS; i++) {
            if (srtp_rdb_check(rdb, i) != 0) {
                System.err.println("Error: srtp_rdb_check failed at index " + i);
                System.exit(1);
            }
            if (srtp_rdb_add_index(rdb, i) != 0) {
                System.err.println("Error: srtp_rdb_add_index failed at index " + i);
                System.exit(1);
            }
        }

        System.out.println("Testing for false positives (replaying packets)...");
        for (int i = 0; i < NUM_TRIALS; i++) {
            int err = srtp_rdb_check(rdb, i);
            if (err == 0) {
                System.err.println("Error: srtp_rdb_check failed to detect replay at index " + i);
                System.exit(1);
            }
        }

        System.out.println("Testing non-sequential insertion...");
        srtp_rdb_init(rdb);
        UnreliableTransportSimulator sim = new UnreliableTransportSimulator();
        for (int i = 0; i < NUM_TRIALS; i++) {
            int idx = sim.nextIndex();
            int rstat = srtp_rdb_check(rdb, idx);
            if (rstat == 0) {
                srtp_rdb_add_index(rdb, idx);
            } else if (rstat != 9 /* srtp_err_status_replay_old */ && rstat != 10 /* srtp_err_status_replay_fail */) {
                 // Note: I should probably find the actual enum values for srtp_err_status_t
                 // Based on err.h:
                 // srtp_err_status_ok = 0
                 // srtp_err_status_replay_old = 9
                 // srtp_err_status_replay_fail = 10
                 System.err.println("Error: srtp_rdb_check returned unexpected status " + rstat + " at index " + idx);
                 System.exit(1);
            }
        }
        System.out.println("Non-sequential tests passed.");
    }
}
