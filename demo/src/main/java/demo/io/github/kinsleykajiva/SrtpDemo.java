package demo.io.github.kinsleykajiva;

import java.util.Arrays;

import io.github.kinsleykajiva.services.SRTPService;
import io.github.kinsleykajiva.services.SRTPSession;

public class SrtpDemo {
    public static void main(String[] args) {
        System.out.println("Starting SRTP Demo...");
        
        int status = SRTPService.init();
        System.out.println("srtp_init() status: " + SRTPService.getErrorString(status));
        
        if (status == 0) {
            try {
                testRtp();
                testRtcp();
            } finally {
                status = SRTPService.shutdown();
                System.out.println("srtp_shutdown() status: " + SRTPService.getErrorString(status));
            }
        } else {
            System.err.println("Failed to initialize SRTP library.");
        }
    }

    private static void testRtp() {
        System.out.println("\n--- Testing RTP Protection ---");
        byte[] masterKey = new byte[30]; // 128-bit key + 112-bit salt
        Arrays.fill(masterKey, (byte) 0xab);
        
        int ssrc = 0x12345678;
        try (SRTPSession senderSession = SRTPService.createSession(masterKey, ssrc);
             SRTPSession receiverSession = SRTPService.createSession(masterKey, ssrc)) {
             
            byte[] rtpHeader = {
                (byte) 0x80, 0x08, 0x00, 0x01, // V=2, P=0, X=0, CC=0, M=0, PT=8, Seq=1
                0x00, 0x00, 0x00, 0x00,       // Timestamp
                0x12, 0x34, 0x56, 0x78        // SSRC
            };
            byte[] payload = "Hello SRTP!".getBytes();
            byte[] packet = new byte[rtpHeader.length + payload.length];
            System.arraycopy(rtpHeader, 0, packet, 0, rtpHeader.length);
            System.arraycopy(payload, 0, packet, rtpHeader.length, payload.length);
            
            System.out.println("Original RTP Packet: " + bytesToHex(packet));
            
            byte[] protectedPacket = senderSession.protect(packet, packet.length);
            System.out.println("Protected RTP Packet: " + bytesToHex(protectedPacket));
            
            int unprotectStatus = receiverSession.unprotect(protectedPacket, protectedPacket.length);
            System.out.println("Unprotect Status: " + SRTPService.getErrorString(unprotectStatus));
            
            if (unprotectStatus == 0) {
                System.out.println("Unprotected RTP Packet matches original!");
            } else {
                System.err.println("Unprotect FAILED!");
            }
        }
    }

    private static void testRtcp() {
        System.out.println("\n--- Testing RTCP Protection ---");
        byte[] masterKey = new byte[30];
        Arrays.fill(masterKey, (byte) 0xcd);
        
        int ssrc = 0x87654321;
        try (SRTPSession senderSession = SRTPService.createSession(masterKey, ssrc);
             SRTPSession receiverSession = SRTPService.createSession(masterKey, ssrc)) {
             
            byte[] rtcpHeader = {
                (byte) 0x80, (byte) 0xc8, 0x00, 0x01, // V=2, P=0, PT=200, Len=1
                (byte) 0x87, 0x65, 0x43, 0x21        // SSRC
            };
            byte[] payload = "RTCP Report".getBytes();
            byte[] packet = new byte[rtcpHeader.length + payload.length];
            System.arraycopy(rtcpHeader, 0, packet, 0, rtcpHeader.length);
            System.arraycopy(payload, 0, packet, rtcpHeader.length, payload.length);
            
            System.out.println("Original RTCP Packet: " + bytesToHex(packet));
            
            byte[] protectedPacket = senderSession.protectRtcp(packet, packet.length);
            System.out.println("Protected RTCP Packet: " + bytesToHex(protectedPacket));
            
            int unprotectStatus = receiverSession.unprotectRtcp(protectedPacket, protectedPacket.length);
            System.out.println("Unprotect Status: " + SRTPService.getErrorString(unprotectStatus));
            
            if (unprotectStatus == 0) {
                System.out.println("Unprotected RTCP Packet matches original!");
            } else {
                System.err.println("Unprotect FAILED!");
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 32); i++) {
            sb.append(String.format("%02x ", bytes[i]));
        }
        if (bytes.length > 32) sb.append("...");
        return sb.toString();
    }
}
