package com.github.hypfvieh.java.rtcompiler;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Compile and execute a Java source file at run-time.<br>
 * Requires a JDK to execute.<br>
 * Returns 0 using System.exit() on success. Positive return codes greater than 0 indicate an error.<br>
 * See {@link ReturnCode} for possible error conditions.
 *
 * @author spannm
 * @since 1.0.0 - 2024-01-25
 */
public final class RtCompilerRunner {

    private final Logger  logger;
    private final boolean logException;

    RtCompilerRunner() {
        this(false);
    }

    RtCompilerRunner(boolean _logException) {
        logger = System.getLogger(getClass().getName());
        logException = _logException;
    }

    ReturnCode run(final String... _args) {
        if (_args == null || _args.length == 0) {
            return ReturnCode.ERR_NO_ARGS;
        }

        String javaFile = _args[0];
        if (javaFile == null || javaFile.isBlank() || !Files.exists(Paths.get(javaFile))) {
            return ReturnCode.ERR_NO_FILE;
        }

        Class<?> clazz;
        try {
            clazz = JavaFileLoader.createClassFromFile(javaFile);
        } catch (Exception _ex) {
            if (logException && _ex != null) {
                logger.log(Level.WARNING, "Failed to compile java source file {0}: ", javaFile, _ex);
            } else {
                logger.log(Level.WARNING, "Failed to compile java source file {0}", javaFile);
            }
            return ReturnCode.ERR_COMPILE;
        }

        Method mainMethod = findMainMethod(clazz);
        if (mainMethod == null) {
            logger.log(Level.WARNING, "No valid main method in java source file {0} (public static void main(String[]))", javaFile);
            return ReturnCode.ERR_NO_MAIN;
        }

        String[] remainingArgs = IntStream.range(1, _args.length)
            .boxed()
            .map(i -> _args[i])
            .toArray(String[]::new);

        Throwable ex;
        try {
            mainMethod.invoke(null, (Object) remainingArgs);
            return ReturnCode.SUCCESS;
        } catch (Exception _ex) {
            ex = _ex.getCause() != null ? _ex.getCause() : _ex; // unwrap root cause
        }

        if (logException && ex != null) {
            logger.log(Level.WARNING, "Failed to execute Java class {0} with arguments {1}: ", clazz.getName(), Arrays.toString(_args), ex);
        } else {
            logger.log(Level.WARNING, "Failed to execute Java class {0} with arguments {1}", clazz.getName(), Arrays.toString(_args));
        }

        return ex instanceof RuntimeException ? ReturnCode.ERR_RUNTIME : ReturnCode.ERR_EXCEPTN;
    }

    Method findMainMethod(Class<?> _clazz) {
        try {

            Method mm = _clazz.getMethod("main", String[].class);
            if (!Modifier.isPublic(mm.getModifiers())) {
                return null;
            } else if (!Modifier.isStatic(mm.getModifiers())) {
                return null;
            } else if (mm.getReturnType() != Void.TYPE) {
                return null;
            } else if (mm.getExceptionTypes().length > 0) {
                return null;
            }
            try {
                mm.setAccessible(true);
                return mm;
            } catch (Exception _ex) {
                return mm;
            }

        } catch (NoSuchMethodException | SecurityException _ex) {
            return null;
        }
    }

    public static void main(String[] _args) {
        RtCompilerRunner runner = new RtCompilerRunner(true);

        String progName = runner.getClass().getName();

        runner.logger.log(Level.INFO, "Starting {0} with arguments {1}", progName, Arrays.toString(_args));

        ReturnCode rc = runner.run(_args);

        if (rc != ReturnCode.SUCCESS) {
            runner.logger.log(Level.ERROR, "{0} ended with error return code={1} ({2})", progName, rc.getCode(), rc);
            runner.logger.log(Level.INFO, "Usage: {0} java_source_file [ argument ... ]", progName);
        }
        exit(rc);
    }

    static void exit(ReturnCode _rc) {
        System.exit(_rc.getCode());
    }

    /**
     * Return codes of {@link RtCompilerRunner}.
     */
    public enum ReturnCode {
        SUCCESS(0, ""),
        ERR_NO_ARGS(200, "No or empty arguments"),
        ERR_NO_FILE(201, "No Java source file name given or file does not exist."),
        ERR_COMPILE(203, "Compilation error in Java source file."),
        ERR_NO_MAIN(204, "No valid main method in source file."),
        ERR_RUNTIME(205, "Run-time exception encountered in execution of source file."),
        ERR_EXCEPTN(206, "Checked exception encountered in execution of source file.");

        private final int    code;
        private final String text;

        ReturnCode(int _code, String _text) {
            code = _code;
            text = _text;
        }

        public int getCode() {
            return code;
        }

        public String getText() {
            return text;
        }

    }

}
