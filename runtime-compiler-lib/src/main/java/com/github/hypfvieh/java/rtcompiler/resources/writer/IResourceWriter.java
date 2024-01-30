package com.github.hypfvieh.java.rtcompiler.resources.writer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Base interface for all resource writers.<br>
 * ResourceWriter are used to write any sort of resource to an {@link OutputStream}.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
public interface IResourceWriter {
    /**
     * Creates an {@link OutputStream} pointing to the given resource name.<br>
     * The actual target of the {@link OutputStream} is specified by the implementation.<br>
     * The caller has to close the {@link OutputStream} after use.
     *
     * @param _name name of resource
     * @return OutputStream
     * @throws IOException when stream could not be created
     */
    OutputStream create(String _name) throws IOException;
}
