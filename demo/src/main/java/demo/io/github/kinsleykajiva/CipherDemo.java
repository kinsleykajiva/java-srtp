package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import io.github.kinsleykajiva.libsrtp.srtp_h;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class CipherDemo {
    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("Cipher Demo (AES-ICM)");

        if (srtp_h.srtp_crypto_kernel_init() != 0) {
            System.err.println("Error: srtp_crypto_kernel_init failed");
            System.exit(1);
        }

        try (Arena arena = Arena.ofConfined()) {
            testAesIcm(arena);
        }

        srtp_h.srtp_crypto_kernel_shutdown();
        System.out.println("Cipher Demo passed!");
    }

    private static void testAesIcm(Arena arena) {
        System.out.println("Testing AES-ICM 128...");

        MemorySegment cipherPtr = arena.allocate(srtp_h.C_POINTER);
        
        int status = srtp_h.srtp_crypto_kernel_alloc_cipher(srtp_h.SRTP_AES_ICM_128(), cipherPtr, 30, 0);
        if (status != 0) {
            System.err.println("Error: srtp_crypto_kernel_alloc_cipher failed: " + status);
            System.exit(1);
        }
        
        MemorySegment cipher = cipherPtr.get(srtp_h.C_POINTER, 0);

        byte[] key = new byte[30];
        for (int i = 0; i < 30; i++) key[i] = (byte) i;
        MemorySegment keySegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, key);

        if (srtp_h.srtp_cipher_init(cipher, keySegment) != 0) {
            System.err.println("Error: srtp_cipher_init failed");
            System.exit(1);
        }

        byte[] iv = new byte[16];
        Arrays.fill(iv, (byte)0);
        MemorySegment ivSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, iv);
        if (srtp_h.srtp_cipher_set_iv(cipher, ivSegment, srtp_h.srtp_direction_encrypt()) != 0) {
            System.err.println("Error: srtp_cipher_set_iv failed");
            System.exit(1);
        }

        byte[] plaintext = "This is a secret message!!!".getBytes();
        MemorySegment buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, plaintext);
        MemorySegment lenBuffer = arena.allocateFrom(srtp_h.C_LONG_LONG, (long)plaintext.length);
        
        System.out.println("Encrypting...");
        if (srtp_h.srtp_cipher_encrypt(cipher, buffer, (long)plaintext.length, buffer, lenBuffer) != 0) {
            System.err.println("Error: srtp_cipher_encrypt failed");
            System.exit(1);
        }

        System.out.println("Decrypting...");
        if (srtp_h.srtp_cipher_set_iv(cipher, ivSegment, srtp_h.srtp_direction_decrypt()) != 0) {
            System.err.println("Error: srtp_cipher_set_iv failed");
            System.exit(1);
        }
        if (srtp_h.srtp_cipher_decrypt(cipher, buffer, (long)plaintext.length, buffer, lenBuffer) != 0) {
            System.err.println("Error: srtp_cipher_decrypt failed");
            System.exit(1);
        }

        byte[] decrypted = new byte[plaintext.length];
        MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, decrypted, 0, plaintext.length);
        String result = new String(decrypted);
        System.out.println("Result: " + result);

        if (!new String(plaintext).equals(result)) {
            System.err.println("Error: Decryption failed, mismatch!");
            System.exit(1);
        }

        srtp_h.srtp_cipher_dealloc(cipher);
    }
}
