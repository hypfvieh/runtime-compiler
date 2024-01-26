package com.github.hypfvieh.java.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.RtCompilerRunner.ReturnCode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RtCompilerRunnerTest extends Assertions {

    static final String SAMPLES = "src/test/java/com/github/hypfvieh/java/rtcompiler/samples/";

    @Test
    void testNullArgs() {
        assertEquals(ReturnCode.ERR_NO_ARGS, run((String[]) null));
    }

    @Test
    void testEmptyArgs() {
        assertEquals(ReturnCode.ERR_NO_ARGS, run());
    }

    @Test()
    public void testNoSourceFile() {
        assertEquals(ReturnCode.ERR_NO_FILE, run(""));
    }

    @Test()
    public void testMissingSourceFile() {
        assertEquals(ReturnCode.ERR_NO_FILE, run("ABC.java"));
    }

    @Test()
    public void testNoStaticMain() {
        assertEquals(ReturnCode.ERR_NO_MAIN, new RtCompilerRunner().run(SAMPLES + "SampleSourceEmpty.java"));
    }

    @Test()
    public void testNonPublicStaticMain() {
        assertEquals(ReturnCode.ERR_NO_MAIN, run(SAMPLES + "SampleSourceNonPublicStaticMain.java"));
    }

    @Test()
    public void testStaticMainThrows() {
        assertEquals(ReturnCode.ERR_NO_MAIN, run(SAMPLES + "SampleSourceStaticMainThrows.java"));
    }

    @Test()
    public void testCompileError() {
        assertEquals(ReturnCode.ERR_COMPILE, run(SAMPLES + "SampleSourceCompileError.txt"));
    }

    @Test()
    public void testRunTimeException() {
        assertEquals(ReturnCode.ERR_RUNTIME, run(SAMPLES + "SampleSourceExceptionAtRunTime.java"));
    }

    @Test()
    public void testRunSuccess() {
        assertEquals(ReturnCode.SUCCESS, run(SAMPLES + "SampleSourceSuccess.java"));
    }

    @Test()
    public void testRunSuccessNonPublicClass() {
        assertEquals(ReturnCode.SUCCESS, run(SAMPLES + "SampleSourceSuccessNonPublicClass.java"));
    }

    @Test
    void testRunSuccessInheritance() throws Exception {
        assertEquals(ReturnCode.SUCCESS, run(SAMPLES + "SampleSourceInheritChild.java"));
    }

    @Test()
    public void testRunSuccessAbstractClass() {
        assertEquals(ReturnCode.SUCCESS, new RtCompilerRunner().run(SAMPLES + "SampleSourceAbstract.java"));
    }

    static ReturnCode run(String... _args) {
        return new RtCompilerRunner().run(_args);
    }

}
