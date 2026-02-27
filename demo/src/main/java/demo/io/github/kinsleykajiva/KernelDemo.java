package demo.io.github.kinsleykajiva;

import io.github.kinsleykajiva.utils.NativeLibraryLoader;
import static io.github.kinsleykajiva.libsrtp.srtp_h.*;

public class KernelDemo {
    public static void main(String[] args) {
        NativeLibraryLoader.load();
        System.out.println("Crypto Kernel Demo");

        // Initialize kernel
        int status = srtp_crypto_kernel_init();
        if (status != 0) {
            System.err.println("Error: srtp_crypto_kernel_init failed with status " + status);
            System.exit(1);
        }
        System.out.println("srtp_crypto_kernel successfully initialized");

        // Check kernel status (runs self-tests)
        System.out.println("Checking srtp_crypto_kernel status...");
        status = srtp_crypto_kernel_status();
        if (status != 0) {
            System.err.println("Error: srtp_crypto_kernel_status failed with status " + status);
            System.exit(1);
        }
        System.out.println("srtp_crypto_kernel passed self-tests");

        // Shutdown kernel
        status = srtp_crypto_kernel_shutdown();
        if (status != 0) {
            System.err.println("Error: srtp_crypto_kernel_shutdown failed with status " + status);
            System.exit(1);
        }
        System.out.println("srtp_crypto_kernel successfully shut down");

        System.out.println("Kernel Demo passed!");
    }
}
