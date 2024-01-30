package com.github.hypfvieh.java.rtcompiler.classloader;

import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResource;
import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResourceLocator;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link ClassLoader} implementation which uses a {@link AbstractResourceLocator} to find classes.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class ResourceLocatorClassLoader extends ClassLoader {

    private final AbstractResourceLocator resourceLocator;

    public ResourceLocatorClassLoader(AbstractResourceLocator _locator, ClassLoader _parent) {
        super(_parent);
        resourceLocator = _locator;
    }

    @Override
    protected Class<?> findClass(String _className) throws ClassNotFoundException {
        Objects.requireNonNull(_className);

        AbstractResource classFileResource = resourceLocator.getResource(_className.replace('.', '/') + ".class");
        if (classFileResource == null) {
            throw new ClassNotFoundException(_className);
        }

        byte[] ba;
        try (var is = classFileResource.open()) {
            ba = is.readAllBytes();
        } catch (IOException _ex) {
            throw new ClassNotFoundException(
                String.format("Cannot read class file from %s: %s",
                    classFileResource.getResourceName(), _ex.getMessage()), _ex);
        }

        Class<?> clazz = super.defineClass(null, ba, 0, ba.length);

        if (!clazz.getName().equals(_className)) {
            throw new ClassNotFoundException(_className);
        }

        return clazz;
    }
}
