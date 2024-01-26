package com.github.hypfvieh.java.rtcompiler.resources.writer;

import com.github.hypfvieh.java.rtcompiler.util.ByteArrayOutputStreamWithCallback;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A resource based on a Map of byte arrays.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class ByteArrMapResourceWriter implements IResourceWriter {

    private final Map<String, byte[]> contents;

    public ByteArrMapResourceWriter(Map<String, byte[]> _contents) {
        contents = _contents == null ? new HashMap<>() : _contents;
    }

    @Override
    public OutputStream create(String _name) {
        return new ByteArrayOutputStreamWithCallback(buf -> contents.put(_name, buf));
    }
}
