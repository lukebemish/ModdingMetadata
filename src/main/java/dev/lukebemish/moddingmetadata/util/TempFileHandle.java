package dev.lukebemish.moddingmetadata.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFileHandle implements Closeable {
    private final Path path;

    private TempFileHandle(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public static TempFileHandle createTempFile(String prefix, String suffix) throws IOException {
        return new TempFileHandle(Files.createTempFile(prefix, suffix));
    }

    public static TempFileHandle createTempDirectory(String prefix) throws IOException {
        return new TempFileHandle(Files.createTempDirectory(prefix));
    }

    private static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            var exceptions = Files.list(path).map(file -> {
                try {
                    delete(file);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }).toList();
            if (!exceptions.isEmpty()) {
                var exception = new IOException("Failed to delete directory " + path);
                exceptions.forEach(exception::addSuppressed);
                throw exception;
            }
        } else {
            Files.delete(path);
        }
    }

    @Override
    public void close() throws IOException {
        delete(path);
    }
}
