package io.github.kinsleykajiva.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.foreign.SymbolLookup;

public class NativeLibraryLoader {
    private static final String LIB_NAME = "libsrtp3.dll";
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;

        try {
            Path tempLib = extractLibrary();
            System.load(tempLib.toAbsolutePath().toString());
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + LIB_NAME, e);
        }
    }

    private static Path extractLibrary() throws IOException {
        String resourcePath = "/" + LIB_NAME;
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Library not found in resources: " + resourcePath);
            }
            Path tempDir = Files.createTempDirectory("libsrtp_native");
            Path tempFile = tempDir.resolve(LIB_NAME);
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            return tempFile;
        }
    }

    public static SymbolLookup getLookup() {
        load();
        return SymbolLookup.loaderLookup();
    }
}
