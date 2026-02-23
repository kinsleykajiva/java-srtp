package io.github.kinsleykajiva.services;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import io.github.kinsleykajiva.libsrtp.srtp_h;
import io.github.kinsleykajiva.libsrtp.srtp_policy_t;
import io.github.kinsleykajiva.libsrtp.srtp_ssrc_t;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

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

    public static SRTPSession createSession(byte[] masterKey, int ssrcValue) {
        Arena sessionArena = Arena.ofShared();
        try {
            MemorySegment policy = srtp_policy_t.allocate(sessionArena);
            
            // Set up RTP policy
            srtp_h.srtp_crypto_policy_set_rtp_default(srtp_policy_t.rtp(policy));
            
            // Set up RTCP policy
            srtp_h.srtp_crypto_policy_set_rtcp_default(srtp_policy_t.rtcp(policy));
            
            // Set SSRC
            MemorySegment ssrc = srtp_policy_t.ssrc(policy);
            srtp_ssrc_t.type(ssrc, srtp_h.ssrc_specific());
            srtp_ssrc_t.value(ssrc, ssrcValue);
            
            // Set master key
            MemorySegment keySegment = sessionArena.allocate(masterKey.length);
            MemorySegment.copy(MemorySegment.ofArray(masterKey), 0, keySegment, 0, masterKey.length);
            srtp_policy_t.key(policy, keySegment);
            
            // Other settings
            srtp_policy_t.window_size(policy, 128);
            srtp_policy_t.next(policy, MemorySegment.NULL);
            
            MemorySegment sessionPtr = sessionArena.allocate(srtp_h.C_POINTER);
            int status = srtp_h.srtp_create(sessionPtr, policy);
            if (status != 0) {
                sessionArena.close();
                throw new RuntimeException("srtp_create failed with status: " + status);
            }
            
            MemorySegment session = sessionPtr.get(srtp_h.C_POINTER, 0);
            return new SRTPSession(session, sessionArena);
        } catch (Exception e) {
            sessionArena.close();
            throw e;
        }
    }

    public static String getErrorString(int status) {
        // Simple mapping for now
        return switch (status) {
            case 0 -> "srtp_err_status_ok";
            case 1 -> "srtp_err_status_fail";
            case 2 -> "srtp_err_status_bad_param";
            case 3 -> "srtp_err_status_alloc_fail";
            case 4 -> "srtp_err_status_dealloc_fail";
            case 5 -> "srtp_err_status_init_fail";
            case 6 -> "srtp_err_status_terminus";
            case 7 -> "srtp_err_status_auth_fail";
            case 8 -> "srtp_err_status_cipher_fail";
            case 9 -> "srtp_err_status_replay_fail";
            case 10 -> "srtp_err_status_replay_old";
            case 11 -> "srtp_err_status_algo_fail";
            case 28 -> "srtp_err_status_buffer_small";
            default -> "Unknown error: " + status;
        };
    }
}
