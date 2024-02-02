package com.github.hypfvieh.java.rtcompiler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;

class JavaFileLoaderTest extends AbstractBaseTest {

    @BeforeAll
    static void setCompileOutputDirectory() {
        System.setProperty(JavaFileLoader.SYSPROP_COMPILER_TARGET, "target/" + JavaFileLoaderTest.class.getSimpleName() + "/");
    }

    @Test
    void testCreateClassFromFileNull() {
        assertThrows(NullPointerException.class, () -> JavaFileLoader.createClassFromFile((String) null));
    }

    @Test
    void testCreateClassFromFileMissing() {
        assertThrows(FileNotFoundException.class, () -> JavaFileLoader.createClassFromFile("dontexist"));
    }

    @Test
    void testCreateClassFromFileInheritance() throws Exception {
        Class<?> child = JavaFileLoader.createClassFromFile(new File(SAMPLES, "SampleSourceInheritChild.java"));
        assertNotNull(child);
        child.getDeclaredConstructor().newInstance();
    }

}
