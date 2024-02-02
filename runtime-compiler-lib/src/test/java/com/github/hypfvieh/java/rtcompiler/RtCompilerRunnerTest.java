package com.github.hypfvieh.java.rtcompiler;

import static org.mockito.ArgumentMatchers.any;

import com.github.hypfvieh.java.rtcompiler.RtCompilerRunner.ReturnCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

class RtCompilerRunnerTest extends AbstractBaseTest {

    static List<Entry<ReturnCode, Supplier<ReturnCode>>> getData() {
        List<Entry<ReturnCode, Supplier<ReturnCode>>> results = new ArrayList<>();
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_ARGS, () -> run((String[]) null)));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_ARGS, () -> run()));

        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_FILE, () -> run("")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_FILE, () -> run("ABC.java")));

        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_MAIN, () -> new RtCompilerRunner().run(SAMPLES + "SampleSourceEmpty.java")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_MAIN, () -> run(SAMPLES + "SampleSourceNonPublicStaticMain.java")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_NO_MAIN, () -> run(SAMPLES + "SampleSourceStaticMainThrows.java")));

        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_COMPILE, () -> run(SAMPLES + "SampleSourceCompileError.txt")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.ERR_RUNTIME, () -> run(SAMPLES + "SampleSourceExceptionAtRunTime.java")));

        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.SUCCESS, () -> run(SAMPLES + "SampleSourceSuccess.java")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.SUCCESS, () -> run(SAMPLES + "SampleSourceSuccessNonPublicClass.java")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.SUCCESS, () -> run(SAMPLES + "SampleSourceInheritChild.java")));
        results.add(new AbstractMap.SimpleEntry<>(ReturnCode.SUCCESS, () -> new RtCompilerRunner().run(SAMPLES + "SampleSourceAbstract.java")));

        return results;
    }

    @ParameterizedTest
    @MethodSource("getData")
    void testReturns(Entry<ReturnCode, Supplier<ReturnCode>> _entry) {
        assertEquals(_entry.getKey(), _entry.getValue().get());
    }

    @Test
    void testMain() {
        try (MockedStatic<RtCompilerRunner> runner = Mockito.mockStatic(RtCompilerRunner.class)) {
            AtomicBoolean called = new AtomicBoolean();
            runner.when(() -> RtCompilerRunner.exit(any()))
            .then(new Answer<>() {
                @Override
                public Object answer(InvocationOnMock _invocation) throws Throwable {
                    called.set(true);
                    return null;
                }
            });

            runner.when(() -> RtCompilerRunner.main(any())).thenCallRealMethod();
            RtCompilerRunner.main(new String[] {SAMPLES + "SampleSourceSuccess.java"});
            runner.verify(() -> RtCompilerRunner.exit(any()));
            assertTrue(called.get());
        }
    }

    static ReturnCode run(String... _args) {
        return new RtCompilerRunner().run(_args);
    }

}
