package com.github.hypfvieh.java.rtcompiler.resources.locator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MapResourceLocator extends AbstractResourceLocator {

    private final Map<String, ByteArrResource> mappedValues = new HashMap<>();

    public MapResourceLocator(Map<String, byte[]> _values) {
        if (_values != null) {
            for (Entry<String, byte[]> e : _values.entrySet()) {
                mappedValues.put(e.getKey(), new ByteArrResource(e.getKey(), e.getValue()));
            }
        }
    }

    @Override
    public List<AbstractResource> locate(String _prefixFilter, boolean _recursive) throws IOException {
        String filter = _prefixFilter == null ? "" : _prefixFilter;

        return mappedValues.entrySet()
            .stream()
            .filter(f -> filter.isEmpty() || f.getKey().startsWith(filter))
            .map(Entry<String, ByteArrResource>::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public ByteArrResource getResource(String _resourceName) {
        return mappedValues.get(_resourceName);
    }

}
