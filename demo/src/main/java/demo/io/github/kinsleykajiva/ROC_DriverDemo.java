package demo.io.github.kinsleykajiva;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static io.github.kinsleykajiva.libsrtp.srtp_h$shared.C_LONG_LONG;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_index_advance;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_index_guess;
import static io.github.kinsleykajiva.libsrtp.srtp_h.srtp_index_init;
import io.github.kinsleykajiva.utils.NativeLibraryLoader;

public class ROC_DriverDemo {
    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("ROC Driver Demo (Port of roc_driver.c)");
        
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment local = arena.allocate(C_LONG_LONG);
            MemorySegment est = arena.allocate(C_LONG_LONG);
            MemorySegment ref = arena.allocate(C_LONG_LONG);
            
            srtp_index_init(local);
            srtp_index_init(ref);
            srtp_index_init(est);
            
            int numBadEst = 0;
            int numTrials = 2048;
            
            System.out.println("Testing sequential insertion...");
            for (int i = 0; i < numTrials; i++) {
                // ref is used as the current sequence number (low 16 bits)
                long refVal = ref.get(C_LONG_LONG, 0);
                short seq = (short)(refVal & 0xFFFF);
                
                srtp_index_guess(local, est, seq);
                
                long estVal = est.get(C_LONG_LONG, 0);
                if (refVal != estVal) {
                    numBadEst++;
                }
                srtp_index_advance(ref, (short)1);
            }
            
            double failureRate = (double) numBadEst / numTrials;
            System.out.println("Sequential tests done. Bad estimates: " + numBadEst + "/" + numTrials + " (Rate: " + failureRate + ")");
            
            if (failureRate > 0.01) {
                System.err.println("Error: failure rate too high!");
                System.exit(1);
            }
            
            System.out.println("ROC Driver Demo passed!");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
