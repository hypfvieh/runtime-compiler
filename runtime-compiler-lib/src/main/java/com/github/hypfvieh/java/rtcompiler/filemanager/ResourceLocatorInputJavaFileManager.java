package com.github.hypfvieh.java.rtcompiler.filemanager;

import com.github.hypfvieh.java.rtcompiler.resources.ResourceBasedJavaFileObject;
import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResource;
import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResourceLocator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * JavaFileManager implementation which uses a {@link AbstractResourceLocator} to find classes and resources.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 *
 * @param <M> JavaFileManager compatible type
 */
public class ResourceLocatorInputJavaFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {
    private final Location location;
    private final Kind kind;
    private final AbstractResourceLocator locator;
    private final Charset charset;

    public ResourceLocatorInputJavaFileManager(AbstractResourceLocator _locator, M _delegate, Location _location, Kind _kind, Charset _charset) {
        super(_delegate);
        locator = Objects.requireNonNull(_locator);
        location = _location;
        kind = _kind;
        charset = _charset;
    }

    @Override
    public String inferBinaryName(Location _location, JavaFileObject _jfo) {

        if (!ResourceBasedJavaFileObject.class.isInstance(_jfo)) {
            return super.inferBinaryName(_location, _jfo);
        }

        // "name" like: "/com/github/bar/foo/Baz.java".
        // "binary name" like: "java.nio.channels.Channel".

        String bn = _jfo.getName();
        if (bn.startsWith("/")) {
            bn = bn.substring(1);
        }

        if (!bn.endsWith(_jfo.getKind().extension)) {
            throw new AssertionError(
                "Name \"" + _jfo.getName() + "\" does not match kind \"" + _jfo.getKind() + "\"");
        }
        bn = bn.substring(0, bn.length() - _jfo.getKind().extension.length());

        bn = bn.replace('/', '.');

        return bn;
    }

    @Override
    public boolean hasLocation(Location _innerLocation) {
        return _innerLocation == location || super.hasLocation(_innerLocation);
    }

    @Override
    public Iterable<JavaFileObject> list(Location _location, String _packageName, Set<Kind> _kinds, boolean _recurse) throws IOException {

        Iterable<JavaFileObject> delegatesJfos = super.list(_location, _packageName, _kinds, _recurse);

        if (_location == location && _kinds.contains(kind)) {

            List<AbstractResource> resources = locator.locate(_packageName.replace('.', '/') + "/", _recurse);
            List<JavaFileObject> result = resources.stream()
                .filter(r -> r.getResourceName().endsWith(kind.extension))
                .map(r -> {
                    String className = r.getResourceName();
                    className = className.substring(0, className.length() - kind.extension.length());

                    className = className.replace(File.separatorChar, '.').replace('/', '.');

                    int idx = className.lastIndexOf(_packageName + ".");
                    assert idx != -1 : className + "//" + _packageName;
                    className = className.substring(idx);

                    try {
                        return getJavaFileForInput(_location, className, kind);
                    } catch (IOException _ex) {
                        throw new UncheckedIOException(_ex);
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            return result;
        }

        return delegatesJfos;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location _location, String _className, Kind _kind) throws IOException {

        Objects.requireNonNull(_location);
        Objects.requireNonNull(_className);
        Objects.requireNonNull(_kind);

        if (_location == location && _kind == kind) {
            return Optional.ofNullable(locator.getResource(_className.replace('.', '/') + _kind.extension))
                .map(r -> new ResourceBasedJavaFileObject(r, _className, kind, charset))
                .orElse(null);
        }

        return super.getJavaFileForInput(_location, _className, _kind);
    }

    @Override
    public boolean isSameFile(FileObject _a, FileObject _b) {

        if (_a instanceof ResourceBasedJavaFileObject && _b instanceof ResourceBasedJavaFileObject) {
            return _a.getName().contentEquals(_b.getName());
        }

        return super.isSameFile(_a, _b);
    }

}
