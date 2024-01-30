package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resource based on a {@link Path}.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class PathResource extends AbstractResource {

    private final Path pathObj;

    public PathResource(Path _path) {
        super(Objects.requireNonNull(_path, "Path required").toString());
        pathObj = _path;
    }

    @Override
    public long getLastMod() {
        return pathObj != null ? pathObj.toFile().lastModified() : 0L;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(pathObj.toFile());
    }

}
