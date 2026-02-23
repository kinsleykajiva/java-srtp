package demo.io.github.kinsleykajiva;

import io.github.kinsleykajiva.services.SRTPService;

public class SrtpDemo {
    public static void main(String[] args) {
        System.out.println("Starting SRTP Demo...");
        
        int status = SRTPService.init();
        System.out.println("srtp_init() status: " + SRTPService.getErrorString(status));
        
        if (status == 0) {
            System.out.println("SRTP library initialized successfully!");
            
            // Cleanup
            status = SRTPService.shutdown();
            System.out.println("srtp_shutdown() status: " + SRTPService.getErrorString(status));
        } else {
            System.err.println("Failed to initialize SRTP library.");
        }
    }
}
