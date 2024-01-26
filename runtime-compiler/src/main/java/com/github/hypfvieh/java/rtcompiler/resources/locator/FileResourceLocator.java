package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A resource locator used for regular files.<br>
 * <br>
 * This resource locator is capable of recursively traverse directories when
 * the {@link #locate(String, boolean)} method is used.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class FileResourceLocator extends AbstractResourceLocator {

    private final String startDir;

    public FileResourceLocator(String _startDir) {
        startDir = _startDir == null || _startDir.isEmpty() ? "." : _startDir;
    }

    public FileResourceLocator(File _startDir) {
        startDir = _startDir == null || !_startDir.exists() ? "." : _startDir.getAbsolutePath();
    }

    @Override
    public List<AbstractResource> locate(String _filterPrefix, boolean _recursive) throws IOException {
        int depth = _recursive ? Integer.MAX_VALUE : 1;

        int idx = _filterPrefix.lastIndexOf('/');
        String searchDir = idx == -1 ? null : _filterPrefix.substring(0, idx);
        String filterFileName = _filterPrefix.substring(idx + 1);

        Path path = Path.of(startDir, searchDir);
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path, depth)) {
                return walk
                    .filter(p -> filterFileName.isEmpty() || p.getFileName().toString().startsWith(filterFileName))
                    .map(p -> new PathResource(p))
                    .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    @Override
    public AbstractResource getResource(String _resourceName) {

        if (_resourceName == null) {
            return null;
        }

        File file = new File(startDir, _resourceName.replace('/', File.separatorChar));
        if (file.exists()) {
            return new PathResource(file.toPath());
        }

        return null;
    }
}
