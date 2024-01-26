package com.github.hypfvieh.java.rtcompiler.resources;

import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResource;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;

import javax.tools.SimpleJavaFileObject;

/**
 * Implementation of a JavaFileObject.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
public class ResourceBasedJavaFileObject extends SimpleJavaFileObject {

    private final AbstractResource resource;
    private final Charset  charset;

    public ResourceBasedJavaFileObject(AbstractResource _resource, String _className, Kind _kind, Charset _charset) {
        super(URI.create("bytearray:///" + _className.replace('.', '/') + _kind.extension), _kind);
        resource = _resource;
        charset = _charset;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return resource.open();
    }

    @Override
    public boolean isNameCompatible(String _simpleName, Kind _kind) {
        return !"module-info".equals(_simpleName);
    }

    @Override
    public Reader openReader(boolean _ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(resource.open(), charset);
    }

    @Override
    public CharSequence getCharContent(boolean _ignoreEncodingErrors) throws IOException {

        try (Reader r = openReader(true);
            StringWriter out = new StringWriter()) {
            char[] buffer = new char[4096];

            int n = 0;
            while ((n = r.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            return out.toString();
        }
    }

    @Override
    public long getLastModified() {
        return resource.getLastMod();
    }

    public AbstractResource getResource() {
        return resource;
    }

    @Override
    public String toString() {
        if (resource != null) {
            return resource.getResourceName();
        }
        return super.toString();
    }

}
