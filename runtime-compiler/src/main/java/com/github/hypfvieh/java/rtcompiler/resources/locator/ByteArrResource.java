package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Resource stored in a byte array.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class ByteArrResource extends AbstractResource {

    private final byte[] content;

    /**
     * Create a new resource with given name containing the given byte array.
     *
     * @param _resourceName name
     * @param _content content
     */
    public ByteArrResource(String _resourceName, byte[] _content) {
        super(_resourceName);
        content = Objects.requireNonNull(_content, "Content required");
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(content);
    }

}
