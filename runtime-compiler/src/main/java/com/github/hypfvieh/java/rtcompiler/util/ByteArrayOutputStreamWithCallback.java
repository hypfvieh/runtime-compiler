package com.github.hypfvieh.java.rtcompiler.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Specialized version of a {@link ByteArrayOutputStream} which will provide the created byte array to a callback when stream is closed.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class ByteArrayOutputStreamWithCallback extends ByteArrayOutputStream {

    private Consumer<byte[]> callback;

    public ByteArrayOutputStreamWithCallback(Consumer<byte[]> _callback) {
        super();
        callback = _callback;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (callback != null) {
            callback.accept(toByteArray());
        }
    }
}
