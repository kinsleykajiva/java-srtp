package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import io.github.kinsleykajiva.libsrtp.srtp_h;
import io.github.kinsleykajiva.libsrtp.srtp_policy_t;
import io.github.kinsleykajiva.libsrtp.srtp_ssrc_t;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class SrtpDemo {
    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("SRTP Protection Demo");

        int status = srtp_h.srtp_init();
        if (status != 0) {
            System.err.println("Error: srtp_init failed: " + status);
            System.exit(1);
        }

        try (Arena arena = Arena.ofConfined()) {
            testSrtp(arena);
        }

        srtp_h.srtp_shutdown();
        System.out.println("SRTP Demo passed!");
    }

    private static void testSrtp(Arena arena) {
        // 1. Setup Sender policy
        MemorySegment sendPolicy = srtp_policy_t.allocate(arena);
        MemorySegment ssrc = srtp_policy_t.ssrc(sendPolicy);
        srtp_ssrc_t.type(ssrc, srtp_h.ssrc_specific());
        srtp_ssrc_t.value(ssrc, 1234);

        srtp_h.srtp_crypto_policy_set_rtp_default(srtp_policy_t.rtp(sendPolicy));
        srtp_h.srtp_crypto_policy_set_rtcp_default(srtp_policy_t.rtcp(sendPolicy));

        byte[] masterKey = new byte[30];
        Arrays.fill(masterKey, (byte) 0xab);
        MemorySegment keySegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, masterKey);
        srtp_policy_t.key(sendPolicy, keySegment);
        srtp_policy_t.window_size(sendPolicy, 128);
        srtp_policy_t.allow_repeat_tx(sendPolicy, false);
        srtp_policy_t.next(sendPolicy, MemorySegment.NULL);

        // 2. Create sender session
        MemorySegment senderPtr = arena.allocate(srtp_h.C_POINTER);
        int status = srtp_h.srtp_create(senderPtr, sendPolicy);
        if (status != 0) {
            System.err.println("Error: srtp_create sender failed: " + status);
            System.exit(1);
        }
        MemorySegment senderSession = senderPtr.get(srtp_h.C_POINTER, 0);

        // 3. Setup Receiver policy (mirrors sender except SSRC type)
        MemorySegment recvPolicy = srtp_policy_t.allocate(arena);
        // Copy everything from sendPolicy
        MemorySegment.copy(sendPolicy, 0, recvPolicy, 0, srtp_policy_t.sizeof());
        // Set to inbound
        srtp_ssrc_t.type(srtp_policy_t.ssrc(recvPolicy), srtp_h.ssrc_any_inbound());

        MemorySegment receiverPtr = arena.allocate(srtp_h.C_POINTER);
        status = srtp_h.srtp_create(receiverPtr, recvPolicy);
        if (status != 0) {
            System.err.println("Error: srtp_create receiver failed: " + status);
            System.exit(1);
        }
        MemorySegment receiverSession = receiverPtr.get(srtp_h.C_POINTER, 0);

        // 4. Create dummy RTP packet
        byte[] rtpPacket = new byte[12 + 10];
        rtpPacket[0] = (byte) 0x80; // V=2
        rtpPacket[1] = (byte) 0x08; // PT=8 (PCMA)
        rtpPacket[2] = (byte) 0x00; // Seq high
        rtpPacket[3] = (byte) 0x01; // Seq low
        rtpPacket[8] = (byte) 0x00; // SSRC 1234
        rtpPacket[9] = (byte) 0x00;
        rtpPacket[10] = (byte) 0x04;
        rtpPacket[11] = (byte) 0xD2;
        System.arraycopy("HelloWorld".getBytes(), 0, rtpPacket, 12, 10);

        MemorySegment inBuffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, rtpPacket);
        long outSize = 1024L;
        MemorySegment outBuffer = arena.allocate(outSize);
        MemorySegment srtpLenPtr = arena.allocateFrom(srtp_h.C_LONG_LONG, outSize);

        // 5. Protect
        System.out.println("Protecting packet...");
        status = srtp_h.srtp_protect(senderSession, inBuffer, 22, outBuffer, srtpLenPtr, 0);
        if (status != 0) {
            System.err.println("Error: srtp_protect failed: " + status);
            System.exit(1);
        }
        long protectedLen = srtpLenPtr.get(srtp_h.C_LONG_LONG, 0);
        System.out.println("Protected packet length: " + protectedLen);

        // 6. Unprotect
        System.out.println("Unprotecting packet...");
        MemorySegment rtpLenPtr = arena.allocateFrom(srtp_h.C_LONG_LONG, outSize);
        status = srtp_h.srtp_unprotect(receiverSession, outBuffer, protectedLen, outBuffer, rtpLenPtr);
        if (status != 0) {
            System.err.println("Error: srtp_unprotect failed: " + status);
            System.exit(1);
        }
        long unprotectedLen = rtpLenPtr.get(srtp_h.C_LONG_LONG, 0);
        System.out.println("Unprotected packet length: " + unprotectedLen);

        byte[] result = new byte[(int)unprotectedLen];
        MemorySegment.copy(outBuffer, ValueLayout.JAVA_BYTE, 0, result, 0, (int)unprotectedLen);
        
        String payload = new String(result, 12, (int)unprotectedLen - 12);
        System.out.println("Decrypted payload: " + payload);

        if (!"HelloWorld".equals(payload)) {
            System.err.println("Error: Decrypted payload mismatch!");
            System.exit(1);
        }

        srtp_h.srtp_dealloc(senderSession);
        srtp_h.srtp_dealloc(receiverSession);
    }
}
