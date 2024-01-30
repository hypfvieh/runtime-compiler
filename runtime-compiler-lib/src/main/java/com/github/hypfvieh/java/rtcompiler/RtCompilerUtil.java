package com.github.hypfvieh.java.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.classloader.ResourceLocatorClassLoader;
import com.github.hypfvieh.java.rtcompiler.exception.CompileException;
import com.github.hypfvieh.java.rtcompiler.resources.locator.FileResourceLocator;
import com.github.hypfvieh.java.rtcompiler.resources.locator.MapResourceLocator;
import com.github.hypfvieh.java.rtcompiler.resources.locator.PathResource;
import com.github.hypfvieh.java.rtcompiler.resources.writer.ByteArrMapResourceWriter;
import com.github.hypfvieh.java.rtcompiler.resources.writer.FileResourceWriter;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to compile java sources. <br>
 * <br>
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
public class RtCompilerUtil {

    /** Do not write class files to the file system, but keep them in memory only. */
    private boolean     compileToMemory;

    private boolean     debugLines  = true;
    private boolean     debugVars   = false;
    private boolean     debugSource = true;

    private ClassLoader clazzLoader;

    private File[]      classPath;

    /**
     * Creates a new compiler.
     */
    public RtCompilerUtil() {
        compileToMemory = !findDebugEnabledFromEnv();
    }

    /**
     * Determines what kind of debugging information is included in the generates classes.
     *
     * @param _debugSource source name
     * @param _debugLines line information
     * @param _debugVars variable information
     */
    public void setDebuggingInformation(boolean _debugSource, boolean _debugLines, boolean _debugVars) {
        debugSource = _debugSource;
        debugLines = _debugLines;
        debugVars = _debugVars;
    }

    /**
     * Toggle compiling to memory. Per default, compiling to memory is true as long as the process is not running in a
     * debugger.
     *
     * @param _compileToMemory true to enable compile to memory, false to disable
     */
    public void setCompileToMemory(boolean _compileToMemory) {
        compileToMemory = _compileToMemory;
    }

    /**
     * Set the classpath. Setting null resets the classpath to its default.
     *
     * @param _classPath new class path
     */
    public void setClasspath(File[] _classPath) {
        classPath = _classPath;
    }

    /**
     * Compiles the given source files and writes compilation result to target directory if {@link #compileToMemory} is
     * false.
     *
     * @param _outputClassDir output directory
     * @param _sourceFiles source files to read
     *
     * @return Set of FQCN created by the compiler
     *
     * @throws CompileException when compilation fails
     * @throws IOException when reading source file fails
     */
    public Set<String> compile(File _outputClassDir, File... _sourceFiles) throws CompileException, IOException {
        if (_sourceFiles == null || _sourceFiles.length == 0) {
            throw new IllegalArgumentException("Source files required");
        }
        Objects.requireNonNull(_outputClassDir, "Output directory required");

        if (!compileToMemory && !_outputClassDir.exists() && !_outputClassDir.mkdirs()) {
            throw new IllegalStateException("Could not create output directory " + _outputClassDir);
        }

        BaseCompiler compiler = new BaseCompiler();
        compiler.setDebugLines(debugLines);
        compiler.setDebugSource(debugSource);
        compiler.setDebugVars(debugVars);

        compiler.setClassPath(Optional.ofNullable(classPath).orElse(parseJavaClassPath(System.getProperty("java.class.path"))));

        Map<String, byte[]> classes = new HashMap<>();

        if (compileToMemory) {
            // store generated .class files in a map:
            compiler.setClassFileCreator(new ByteArrMapResourceWriter(classes));
        } else {
            compiler.setClassFileCreator(new FileResourceWriter(_outputClassDir));
        }

        // compile all source files
        compiler.compile(Arrays.stream(_sourceFiles)
            .map(f -> f.toPath())
            .map(PathResource::new).toArray(PathResource[]::new));

        clazzLoader = new ResourceLocatorClassLoader(
            compileToMemory ? new MapResourceLocator(classes) : new FileResourceLocator(_outputClassDir),
            ClassLoader.getSystemClassLoader() // parent
        );

        Set<String> classNames = Arrays.stream(_sourceFiles)
            .map(s -> {
                try {
                    return CompileUtil.getClassNameFromSourceFile(s);
                } catch (IOException _ex) {
                    LoggerFactory.getLogger(getClass().getName()).warn("Could not extract class name from source file {}", s);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return classNames;
    }

    Set<File> findMissingSources(File _sourceDir, Set<String> _missingSymbols) {
        try (Stream<String> stream = Stream.of(_sourceDir.list())) {
            Set<File> allSources = stream
                .map(s -> new File(_sourceDir, s))
                .filter(CompileUtil::isJavaFile)
                .collect(Collectors.toSet());
            Set<File> missingSources = new LinkedHashSet<>();
            for (String symbol : _missingSymbols) {
                allSources.stream()
                    .filter(f -> f.getName().equals(symbol + ".java"))
                    .forEach(missingSources::add);
            }
            return missingSources;
        }
    }

    /**
     * Returns the classloader containing the results of the last {@link #compile(File, File...)} call.
     *
     * @return {@link ClassLoader}, maybe null
     */
    public ClassLoader getClassLoader() {
        return clazzLoader;
    }

    /**
     * Examines some system properties to determine whether the process is likely being debugged in an IDE or remotely.
     *
     * @return true if process is being debugged
     */
    static boolean findDebugEnabledFromEnv() {
        if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0) {
            return true;
        } else if (ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xdebug")) {
            return true;
        } else if (System.getProperty("debug", "").equals("true")) {
            return true;
        }
        return false;
    }

    static File[] parseJavaClassPath(String _path) {
        return Arrays.stream(_path.split(File.pathSeparator))
            .filter(Objects::nonNull)
            .filter(String::isBlank)
            .map(File::new)
            .filter(File::exists)
            .toArray(File[]::new);
    }
}
