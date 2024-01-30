package com.github.hypfvieh.java.rtcompiler.resources;

import java.io.IOException;
import java.util.Set;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * JavaFileManager which will try two different other JavaFileManagers.<br>
 * A given resources is first tried to get from the first given JavaFileManager.<br>
 * If no result is found, the second JavaFileManager will be tried.
 *
 * @since 1.0.0 - 2024-01-25
 */
public class FallbackResourceLocatorInputJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final JavaFileManager second;

    public FallbackResourceLocatorInputJavaFileManager(JavaFileManager _first, JavaFileManager _second) {
        super(_first);
        second = _second;
    }

    @Override
    public Iterable<JavaFileObject> list(Location _location, String _packageName, Set<Kind> _kinds,
        boolean _recurse) throws IOException {

        Iterable<JavaFileObject> result = super.list(_location, _packageName, _kinds, _recurse);
        if (result == null) {
            result = second.list(_location, _packageName, _kinds, _recurse);
        }
        return result;
    }

    @Override
    public String inferBinaryName(Location _location, JavaFileObject _file) {
        String result = super.inferBinaryName(_location, _file);
        if (result == null) {
            result = second.inferBinaryName(_location, _file);
        }
        return result;
    }

    @Override
    public boolean hasLocation(Location _location) {
        if (!super.hasLocation(_location)) {
            return second.hasLocation(_location);
        }
        return true;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location _location, String _className, Kind _kind)
        throws IOException {

        JavaFileObject result = super.getJavaFileForInput(_location, _className, _kind);
        if (result == null) {
            result = second.getJavaFileForInput(_location, _className, _kind);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
        second.close();
    }

}
