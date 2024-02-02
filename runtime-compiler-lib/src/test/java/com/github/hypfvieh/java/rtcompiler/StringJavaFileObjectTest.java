package com.github.hypfvieh.java.rtcompiler;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class StringJavaFileObjectTest extends AbstractBaseTest {

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

    @Test
    void testCharContent() throws IOException {
        File tmp = createTestFile(getShortTestMethodName() + ".txt", "Content");

        StringJavaFileObject sjfo = new StringJavaFileObject(tmp.getPath());
        CharSequence charContent = sjfo.getCharContent(false);
        assertEquals("Content", charContent);
    }
}
