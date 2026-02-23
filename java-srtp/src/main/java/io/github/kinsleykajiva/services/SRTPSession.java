package io.github.kinsleykajiva.services;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import io.github.kinsleykajiva.libsrtp.srtp_h;

public class SRTPSession implements AutoCloseable {
    private final MemorySegment session;
    private final Arena sessionArena;

    public SRTPSession(MemorySegment session, Arena sessionArena) {
        this.session = session;
        this.sessionArena = sessionArena;
    }

    public byte[] protect(byte[] rtpPacket, int length) {
        try (Arena arena = Arena.ofConfined()) {
            long capacity = length + srtp_h.SRTP_MAX_TRAILER_LEN();
            MemorySegment packetSegment = arena.allocate(capacity);
            MemorySegment.copy(MemorySegment.ofArray(rtpPacket), 0, packetSegment, 0, length);
            
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            lenPtr.set(ValueLayout.JAVA_LONG, 0, capacity);
            
            int status = srtp_h.srtp_protect(session, packetSegment, length, packetSegment, lenPtr, 0);
            if (status != 0) {
                throw new RuntimeException("srtp_protect failed with status: " + status + " (" + SRTPService.getErrorString(status) + ")");
            }
            
            long newLen = lenPtr.get(ValueLayout.JAVA_LONG, 0);
            byte[] protectedPacket = new byte[(int) newLen];
            MemorySegment.copy(packetSegment, ValueLayout.JAVA_BYTE, 0, protectedPacket, 0, (int) newLen);
            return protectedPacket;
        }
    }

    public int unprotect(byte[] srtpPacket, int length) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment packetSegment = arena.allocate(length);
            MemorySegment.copy(MemorySegment.ofArray(srtpPacket), 0, packetSegment, 0, length);
            
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            lenPtr.set(ValueLayout.JAVA_LONG, 0, (long)length);
            
            int status = srtp_h.srtp_unprotect(session, packetSegment, (long)length, packetSegment, lenPtr);
            if (status != 0) {
                return status;
            }
            
            long newLen = lenPtr.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment.copy(packetSegment, ValueLayout.JAVA_BYTE, 0, srtpPacket, 0, (int) newLen);
            return 0;
        }
    }

    public byte[] protectRtcp(byte[] rtcpPacket, int length) {
        try (Arena arena = Arena.ofConfined()) {
            long capacity = length + srtp_h.SRTP_MAX_SRTCP_TRAILER_LEN();
            MemorySegment packetSegment = arena.allocate(capacity);
            MemorySegment.copy(MemorySegment.ofArray(rtcpPacket), 0, packetSegment, 0, length);
            
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            lenPtr.set(ValueLayout.JAVA_LONG, 0, capacity);
            
            int status = srtp_h.srtp_protect_rtcp(session, packetSegment, length, packetSegment, lenPtr, 0);
            if (status != 0) {
                throw new RuntimeException("srtp_protect_rtcp failed with status: " + status + " (" + SRTPService.getErrorString(status) + ")");
            }
            
            long newLen = lenPtr.get(ValueLayout.JAVA_LONG, 0);
            byte[] protectedPacket = new byte[(int) newLen];
            MemorySegment.copy(packetSegment, ValueLayout.JAVA_BYTE, 0, protectedPacket, 0, (int) newLen);
            return protectedPacket;
        }
    }

    public int unprotectRtcp(byte[] srtcpPacket, int length) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment packetSegment = arena.allocate(length);
            MemorySegment.copy(MemorySegment.ofArray(srtcpPacket), 0, packetSegment, 0, length);
            
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            lenPtr.set(ValueLayout.JAVA_LONG, 0, (long)length);
            
            int status = srtp_h.srtp_unprotect_rtcp(session, packetSegment, (long)length, packetSegment, lenPtr);
            if (status != 0) {
                return status;
            }
            
            long newLen = lenPtr.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment.copy(packetSegment, ValueLayout.JAVA_BYTE, 0, srtcpPacket, 0, (int) newLen);
            return 0;
        }
    }

    @Override
    public void close() {
        srtp_h.srtp_dealloc(session);
        sessionArena.close();
    }
}
