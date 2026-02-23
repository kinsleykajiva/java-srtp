package io.github.kinsleykajiva.services;

import io.github.kinsleykajiva.libsrtp.*;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class SRTPService {
    static {
        NativeLibraryLoader.load();
    }

    public static int init() {
        return srtp_h.srtp_init();
    }

    public static int shutdown() {
        return srtp_h.srtp_shutdown();
    }

    public static String getErrorString(int status) {
        // Simple mapping for now
        return switch (status) {
            case 0 -> "srtp_err_status_ok";
            case 1 -> "srtp_err_status_fail";
            case 2 -> "srtp_err_status_bad_param";
            case 3 -> "srtp_err_status_alloc_fail";
            case 5 -> "srtp_err_status_init_fail";
            default -> "Unknown error: " + status;
        };
    }
}
