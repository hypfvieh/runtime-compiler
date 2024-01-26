package com.github.hypfvieh.java.rtcompiler.util;

import com.github.hypfvieh.java.rtcompiler.resources.writer.IResourceWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;

import javax.tools.*;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;

public final class JavaFileManagerUtil {

    private JavaFileManagerUtil() {

    }

    public static <M extends JavaFileManager> ForwardingJavaFileManager<M> fromResourceFactory(
        M _delegate,
        final Location _location,
        final Kind _kind,
        final IResourceWriter _resourceCreator,
        final Charset _charset) {

        return new ForwardingJavaFileManager<M>(_delegate) {

            @Override
            public JavaFileObject getJavaFileForOutput(
                Location _innerLocation,
                final String _className,
                Kind _innerKind,
                FileObject _sibling) throws IOException {

                if (_innerKind == _kind && _innerLocation == _location) {
                    final String resourceName = _className.replace('.', '/') + ".class";
                    return fromResourceWriter(_resourceCreator, resourceName, _kind, _charset);
                } else {
                    return super.getJavaFileForOutput(_innerLocation, _className, _innerKind, _sibling);
                }
            }
        };
    }

    public static JavaFileObject fromResourceWriter(IResourceWriter _resourceCreator, String _resourceName, Kind _kind, Charset _charset) {
        return new SimpleJavaFileObject(URI.create("bytearray:///" + _resourceName), _kind) {

            @Override
            public OutputStream openOutputStream() throws IOException {
                return _resourceCreator.create(_resourceName);
            }

            @Override
            public Writer openWriter() throws IOException {
                return new OutputStreamWriter(this.openOutputStream(), _charset);
            }
        };
    }

}
