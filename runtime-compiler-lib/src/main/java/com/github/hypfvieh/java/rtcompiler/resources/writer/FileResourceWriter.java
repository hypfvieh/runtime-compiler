package com.github.hypfvieh.java.rtcompiler.resources.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * A resource writer which writes the resource content to file.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class FileResourceWriter implements IResourceWriter {

    private File baseDir;

    public FileResourceWriter(File _baseDir) {
        baseDir = Objects.requireNonNull(_baseDir);
    }

    @Override
    public OutputStream create(String _name) throws IOException {

        File file = new File(baseDir, _name);
        if (file.exists() && file.isDirectory()) {
            throw new IOException("File " + _name + " is a directory");
        }
        Files.createDirectories(file.getParentFile().toPath());
        file.createNewFile();
        return new FileOutputStream(file);
    }

}
