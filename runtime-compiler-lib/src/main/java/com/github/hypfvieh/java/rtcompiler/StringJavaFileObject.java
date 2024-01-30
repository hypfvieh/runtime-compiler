package com.github.hypfvieh.java.rtcompiler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * {@link JavaFileObject} implementation that reads a source file into a UTF-8 String.
 *
 * @since 1.0.0 - 2024-01-25
 */
final class StringJavaFileObject extends SimpleJavaFileObject {

    private final File file;
    private String     content;

    StringJavaFileObject(String _fileName) {
        this(new File(_fileName), Kind.SOURCE);
    }

    StringJavaFileObject(File _file, Kind _kind) {
        super(_file.toURI(), _kind == null ? Kind.SOURCE : _kind);

        if (!_file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + _file);
        } else if (!_file.canRead()) {
            throw new IllegalArgumentException("File not readable: " + _file);
        }

        file = _file;
    }

    @Override
    public boolean isNameCompatible(String _simpleName, Kind _kind) {
        return true;
    }

    @Override
    public synchronized CharSequence getCharContent(boolean _ignoreEncodingErrors) {
        if (content == null) {
            try {
                content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException _ex) {
                throw new UncheckedIOException("Failed to read file: " + file, _ex);
            }
        }
        return content;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + file.getPath() + ", lastMod=" + new Date(file.lastModified()) + "]";
    }

}
