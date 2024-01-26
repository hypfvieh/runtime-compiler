package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.IOException;
import java.util.List;

/**
 * Base class to lookup resources somewhere, somehow.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public abstract class AbstractResourceLocator {
    /**
     * Locate all resources using the given parameter.<br>
     * Where and how the resources are located are implementation detail.
     *
     * @param _prefixFilter filter for e.g. file names or pathes
     * @param _recursive list everything recursively
     *
     * @return List, maybe empty - never null
     *
     * @throws IOException when locating resource fails
     */
    public abstract List<AbstractResource> locate(String _prefixFilter, boolean _recursive) throws IOException;

    /**
     * Retrieve a resource by name.
     *
     * @param _resourceName name of resource
     * @return resource or null
     */
    public abstract AbstractResource getResource(String _resourceName);
}
