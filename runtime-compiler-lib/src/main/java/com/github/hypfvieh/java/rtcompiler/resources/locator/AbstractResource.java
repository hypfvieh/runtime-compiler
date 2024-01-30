package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Base of any resource found by {@link AbstractResourceLocator}.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public abstract class AbstractResource {

    private final String resourceName;

    /**
     * Create a resource with the given name.
     * @param _resourceName name
     */
    public AbstractResource(String _resourceName) {
        resourceName = Objects.requireNonNull(_resourceName, "Resource name required");
    }

    /**
     * Returns the name of the resource.<br>
     * This might be a single name or a name with a path.
     *
     * @return String
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Returns the last modified date in milliseconds since the epoch.<br>
     * Defaults to 0 when no lastMod can be provided.
     *
     * @return long
     */
    public long getLastMod() {
        return 0L;
    }

    /**
     * Creates an {@link InputStream} for the underlying resource.<br>
     * The call must take care of closing this stream!
     *
     * @return InputStream
     * @throws IOException when opening stream fails
     */
    public abstract InputStream open() throws IOException;

}
