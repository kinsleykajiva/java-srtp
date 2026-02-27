package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_sha1_final;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_sha1_init;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_sha1_update;
import io.github.kinsleykajiva.libsrtp.srtp_sha1_ctx_t;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class AuthDemo {
    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("Auth (SHA-1) Demo");

        try (Arena arena = Arena.ofConfined()) {
            validateSha1(arena);
        }

        System.out.println("Auth Demo passed!");
    }

    private static void validateSha1(Arena arena) {
        // Test case from NIST: "abc"
        // hex: 616263
        // expected sha1: a9993e364706816aba3e25717850c26c9cd0d89d
        
        byte[] data = "abc".getBytes();
        MemorySegment dataSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
        
        MemorySegment ctx = srtp_sha1_ctx_t.allocate(arena);
        srtp_sha1_init(ctx);
        srtp_sha1_update(ctx, dataSegment, data.length);
        
        MemorySegment output = arena.allocate(ValueLayout.JAVA_INT, 5);
        srtp_sha1_final(ctx, output);
        
        byte[] hash = new byte[20];
        MemorySegment.copy(output, ValueLayout.JAVA_BYTE, 0, hash, 0, 20);
        
        String expected = "a9993e364706816aba3e25717850c26c9cd0d89d";
        String actual = bytesToHex(hash);
        
        System.out.println("Data: abc");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        
        if (!expected.equalsIgnoreCase(actual)) {
            System.err.println("SHA-1 validation failed!");
            System.exit(1);
        }
        System.out.println("SHA-1 validation passed!");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
