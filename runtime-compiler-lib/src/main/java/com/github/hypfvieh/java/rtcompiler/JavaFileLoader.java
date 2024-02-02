package com.github.hypfvieh.java.rtcompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

/**
 * Utility class that loads a java source from file, compiles it and returns the class object.
 *
 * @author hypfvieh / spannm
 * @since 1.0.0 - 2024-01-25
 */
public final class JavaFileLoader {

    static final String SYSPROP_COMPILER_TARGET = "compilerTarget";

    private JavaFileLoader() {
    }

    public static Class<?> createClassFromFile(File _file) throws Exception {
        Objects.requireNonNull(_file, "Source file to create class required");
        return createClassFromFile(_file.getAbsolutePath());
    }

    public static Class<?> createClassFromFile(String _fileName) throws Exception {
        Objects.requireNonNull(_fileName, "Source file name to create class required");
        File sourceFile = new File(_fileName).getAbsoluteFile();
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Source file does not exist: " + sourceFile);
        } else if (!sourceFile.canRead()) {
            throw new FileNotFoundException("Source file not readable: " + sourceFile);
        }

        RtCompiler compiler = new RtCompiler();
        compiler.compile(new File(System.getProperty(SYSPROP_COMPILER_TARGET, "target/")), sourceFile);

        String className = CompileUtil.getClassNameFromSourceFile(sourceFile);
        return compiler.getClassLoader().loadClass(className);

    }

}
