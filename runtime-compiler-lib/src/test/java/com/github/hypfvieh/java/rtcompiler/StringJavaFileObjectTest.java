package com.github.hypfvieh.java.rtcompiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class StringJavaFileObjectTest extends Assertions {

    @Test
    void testConstructorFileNull() {
        assertThrows(NullPointerException.class, () -> new StringJavaFileObject(null));
    }

    @Test
    void testConstructorFileNotExists() {
        assertThrows(IllegalArgumentException.class, () -> new StringJavaFileObject(new File(".", "dontexist").getPath()));
    }

    @Test
    void testToString() throws IOException {
        File tempFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
        tempFile.deleteOnExit();

        String toStr = new StringJavaFileObject(tempFile.getPath()).toString();
        assertTrue(toStr.contains(tempFile.getPath()));
        assertTrue(toStr.contains("lastMod="));
    }

}
