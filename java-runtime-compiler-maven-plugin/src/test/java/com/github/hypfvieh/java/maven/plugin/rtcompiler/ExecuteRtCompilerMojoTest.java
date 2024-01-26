package com.github.hypfvieh.java.maven.plugin.rtcompiler;

import static org.mockito.Mockito.*;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

class ExecuteRtCompilerMojoTest extends Assertions {

    private static final File TARGET_DIR      = new File("target/java-runtime-compiler-maven-plugin/");
    private static final String TEST_PKG_NAME = "com.github.hypfvieh.java.maven.plugin.rtcompiler";
    private static final String TEST_SRC_PATH = TEST_PKG_NAME.replace(".", "/");

    /**
     * Attempt to execute the plugin without source files. This should fail.
     * @throws Exception on test failure
     */
    @Test
    void testExecuteMojoNoSourceFiles() throws Exception {
        ExecuteRtCompilerMojo mojoMock = spy(ExecuteRtCompilerMojo.class);
        when(mojoMock.getSourceLocations()).thenReturn(Arrays.asList());
        when(mojoMock.getClassOutputDir()).thenReturn(TARGET_DIR);

        // without source files an exception occurs
        Assertions.assertThrows(MojoFailureException.class, mojoMock::execute);
    }

    /**
     * The configured main class does not have a main method. This should fail.
     * @throws Exception on test failure
     */
    @Test
    void testExecuteMojoNoMainMethod() throws Exception {
        ExecuteRtCompilerMojo mojoMock = spy(ExecuteRtCompilerMojo.class);
        when(mojoMock.getSourceLocations()).thenReturn(Arrays.asList(new File("src/test/java/" + TEST_SRC_PATH + "/TestClazzWithoutMain.java")));
        when(mojoMock.getClassOutputDir()).thenReturn(TARGET_DIR);
        when(mojoMock.getMainClass()).thenReturn(TEST_PKG_NAME + ".TestClazzWithoutMain");
        when(mojoMock.getMainArgs()).thenReturn(null);

        // as no main method is found, an exception occurs
        Assertions.assertThrows(MojoFailureException.class, mojoMock::execute);
    }

    /**
     * Successfully execute the plugin.
     * @throws Exception on unexpected test failure
     */
    @Test
    void testExecuteMojoSuccess() throws Exception {
        ExecuteRtCompilerMojo mojoMock = spy(ExecuteRtCompilerMojo.class);
        when(mojoMock.getSourceLocations()).thenReturn(Arrays.asList(new File("src/test/java/", TEST_SRC_PATH + "/TestClazz.java")));
        when(mojoMock.getClassOutputDir()).thenReturn(TARGET_DIR);
        when(mojoMock.getMainClass()).thenReturn(TEST_PKG_NAME + ".TestClazz");
        when(mojoMock.getMainArgs()).thenReturn(null);

        // main should not yet have been called
        assertEquals(0, TestClazz.MAIN_INVOCATION_COUNT);

        // this should instantiate the class and call its main method
        mojoMock.execute();

        // main should not yet have been called
        assertEquals(1, TestClazz.MAIN_INVOCATION_COUNT);

        verify(mojoMock, times(1)).execute();
    }
}
