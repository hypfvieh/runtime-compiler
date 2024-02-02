package com.github.hypfvieh.java.rtcompiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class CompileUtilTest extends AbstractBaseTest {

    @Test
    void testGetClassNameFromSourceComplex() throws IOException {
        String source = Files.readString(Path.of("src/test/resources/compileUtilTest.txt"));
        String className = CompileUtil.getClassNameFromSource(source);
        assertEquals("HocusPocus", className);
    }

    @Test
    void testGetClassNameFromSourceStrNull() {
        assertThrows(NullPointerException.class, () -> CompileUtil.getClassNameFromSource(null));
    }

    @Test
    void testGetClassNameFromSourceStrBogus() {
        assertNull(CompileUtil.getClassNameFromSource("bogus"));
    }

    @Test
    void testGetClassNameFromSourceStrPackageAndClass() {
        assertEquals("crap.noc.whisper.Fedelice", CompileUtil.getClassNameFromSource("package crap.noc.whisper;\n\nclass Fedelice"));
    }

    @Test
    void testGetClassNameFromSourceStrDefaultPackage() {
        assertEquals("Gargamel", CompileUtil.getClassNameFromSource("\n\n\ninterface Gargamel"));
    }

    @Test
    void testFindSourcePath() throws IOException {
        File sourcePath = CompileUtil.findSourcePath(new File("src/test/java/com/github/hypfvieh/java/rtcompiler/samples", "SampleSourceEmpty.java"));
        assertEquals(new File("src/test/java/"), sourcePath);
    }

    @Test
    void testIsJavaFile() throws IOException {
        assertFalse(CompileUtil.isJavaFile(null));
        assertFalse(CompileUtil.isJavaFile(new File(System.getProperty("java.io.tmpdir"))));
        assertFalse(CompileUtil.isJavaFile(new File("readme.txt")));
        File dummyFile = File.createTempFile("DummySource", ".java");
        dummyFile.deleteOnExit();
        assertTrue(CompileUtil.isJavaFile(dummyFile));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @CsvSource({",", "txt,txt", "readme.txt,readme"})
    void testRemoveExtension(String _input, String _expected) throws IOException {
        assertEquals(_expected, CompileUtil.removeExtension(_input));
    }

}
