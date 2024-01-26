package com.github.hypfvieh.java.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.exception.CompileException;
import com.github.hypfvieh.java.rtcompiler.exception.RuntimeCompilerException;
import com.github.hypfvieh.java.rtcompiler.filemanager.ResourceLocatorInputJavaFileManager;
import com.github.hypfvieh.java.rtcompiler.listener.CompilerListener;
import com.github.hypfvieh.java.rtcompiler.listener.CompilerListener.Type;
import com.github.hypfvieh.java.rtcompiler.resources.FallbackResourceLocatorInputJavaFileManager;
import com.github.hypfvieh.java.rtcompiler.resources.ResourceBasedJavaFileObject;
import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResource;
import com.github.hypfvieh.java.rtcompiler.resources.locator.AbstractResourceLocator;
import com.github.hypfvieh.java.rtcompiler.resources.locator.FileResourceLocator;
import com.github.hypfvieh.java.rtcompiler.resources.locator.PathResource;
import com.github.hypfvieh.java.rtcompiler.resources.writer.ByteArrMapResourceWriter;
import com.github.hypfvieh.java.rtcompiler.resources.writer.IResourceWriter;
import com.github.hypfvieh.java.rtcompiler.util.JavaFileManagerUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

/**
 * Compiler implementation.
 * <br>
 * This version of Compiler has an enhanced {@link #compile(AbstractResource[])} method.
 * These enhancements will allow compiling multiple class and handle missing
 * sources in case of inheritance.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
public class BaseCompiler {

    private JavaFileManager                fileManagerEnn;

    private Collection<String>             compilerOptions = new ArrayList<>();

    private final List<CompileException>   compileErrors;

    private final Map<String, PrintStream> compilerLogs;

    private final Set<File>                unresolvableSymbols;

    private final Map<Type, Set<String>>   missingSymbols;

    private File[] classPath = Arrays.stream(System.getProperty("java.class.path", "").split(String.valueOf(File.pathSeparatorChar))).map(e -> new File(e)).toArray(File[]::new);

    private AbstractResourceLocator sourceLocator;
    private AbstractResourceLocator classFileLocator;
    private IResourceWriter classFileFactory;

    private boolean debugLines;
    private boolean debugVars;
    private boolean debugSource;
    private Charset sourceCharset;

    private StandardJavaFileManager        standardFileManager;

    public BaseCompiler() {
        compileErrors = new ArrayList<>();
        compilerLogs = new HashMap<>();
        missingSymbols = new HashMap<>();
        unresolvableSymbols = new LinkedHashSet<>();
        sourceCharset = Charset.defaultCharset();
        sourceLocator = new FileResourceLocator(".");
        classFileLocator = new FileResourceLocator(".");
        classFileFactory = new ByteArrMapResourceWriter(new HashMap<>());
    }

    public boolean isDebugLines() {
        return debugLines;
    }

    public void setDebugLines(boolean _debugLines) {
        debugLines = _debugLines;
    }

    public boolean isDebugVars() {
        return debugVars;
    }

    public void setDebugVars(boolean _debugVars) {
        debugVars = _debugVars;
    }

    public boolean isDebugSource() {
        return debugSource;
    }

    public void setDebugSource(boolean _debugSource) {
        debugSource = _debugSource;
    }

    public File[] getClassPath() {
        return classPath;
    }

    public void setClassPath(File[] _classPath) {
        classPath = _classPath;
    }

    public AbstractResourceLocator getSourceLocator() {
        return sourceLocator;
    }

    public void setSourceLocator(AbstractResourceLocator _sourceLocator) {
        sourceLocator = _sourceLocator;
    }

    public AbstractResourceLocator getClassFileLocator() {
        return classFileLocator;
    }

    public void setClassFileLocator(AbstractResourceLocator _classFileLocator) {
        classFileLocator = _classFileLocator;
    }

    public void setClassFileCreator(IResourceWriter _classFileCreator) {
        classFileFactory = _classFileCreator;
    }

    /**
     * Contains all compiler exceptions fetched during last call to {@link #compile(AbstractResource[])}.
     * @return List, maybe empty - never null
     */
    public List<CompileException> getCompileErrors() {
        return compileErrors;
    }

    /**
     * Contains all compiler messages fetched during last call to {@link #compile(AbstractResource[])}.
     * @return Set, maybe empty - never null
     */
    public Set<String> getCompilerLogs() {
        return compilerLogs.keySet();
    }

    /**
     * Contains all unresolvable symbols reported by the compiler during last call to {@link #compile(AbstractResource[])}.
     * @return Set, maybe empty - never null
     */
    public Set<File> getUnresolvableSymbols() {
        return unresolvableSymbols;
    }

    /**
     * Contains all missing symbols reported by the compiler during last call to {@link #compile(AbstractResource[])}.
     * @return Map, maybe empty - never null
     */
    public Map<Type, Set<String>> getMissingSymbols() {
        return missingSymbols;
    }

    /**
     * Add additional command line options to the compiler.
     * <br>
     * These options should be any option supported by 'javac' except the '-g' options which
     * are set by {@link #setDebugLines(boolean)},
     * {@link #setDebugVars(boolean)} and {@link #setDebugSource(boolean)}.
     *
     * @param _compilerOpts command line options supported javac
     */
    public void setCompilerOptions(String[] _compilerOpts) {
        compilerOptions = Arrays
            .stream(_compilerOpts)
            .filter(opt -> opt.equals("-g") || opt.startsWith("-g:"))
            .collect(Collectors.toList());
    }

    private List<String> getOptions() {
        String optionStr = Map.of(
            "lines", debugLines,
            "vars", debugVars,
            "source", debugSource)
            .entrySet()
            .stream()
            .filter(Entry::getValue)
            .map(Entry::getKey)
            .collect(Collectors.joining(","));

        List<String> options = new ArrayList<>(compilerOptions);
        if (optionStr != null && !optionStr.isEmpty()) {
            options.add("-g:" + optionStr);
        }

        return options;
    }

    public void compile(AbstractResource[] _sourceResources) throws CompileException, IOException {

        compileErrors.clear();
        compilerLogs.clear();
        missingSymbols.clear();
        unresolvableSymbols.clear();

        List<String> sourceFileNames = Arrays.stream(_sourceResources)
            .map(AbstractResource::getResourceName)
            .distinct()
            .collect(Collectors.toList());

        Collection<JavaFileObject> sourceFileObjects = new ArrayList<>();
        for (AbstractResource sourceResource : _sourceResources) {
            String fn = sourceResource.getResourceName();
            String className = fn.substring(fn.lastIndexOf(File.separatorChar) + 1, fn.length() - 5).replace('/', '.');
            sourceFileObjects.add(new ResourceBasedJavaFileObject(
                sourceResource,
                className,
                Kind.SOURCE,
                sourceCharset));
        }

        // take care of closing fileManager after compiling!
        try (JavaFileManager fileManager = getJavaFileManager()) {
            if (classPath != null && classPath.length > 0) {
                standardFileManager.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(classPath));
            }

            // Run the compiler
            getSystemJavaCompiler()
            .getTask(
                null, // out
                fileManager, // fileManager
                new CompilerListener(unresolvableSymbols, compileErrors, compilerLogs, missingSymbols),
                getOptions(), // options
                null, // classes
                sourceFileObjects // compilationUnits
            ).call();
        } catch (RuntimeException _ex) {

            // Unwrap the compilation exception and throw it
            Throwable cause = _ex.getCause();
            if (cause != null) {
                cause = cause.getCause();
                if (cause instanceof CompileException) {
                    throw (CompileException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
            }
            throw new RuntimeCompilerException(_ex);
        }

        // if we have unresolved errors during compilation, try to compile the found additional sources
        if (!unresolvableSymbols.isEmpty()) {
            compile(unresolvableSymbols.toArray(File[]::new));
        }

        if (!missingSymbols.isEmpty()) {

            // Look for missing symbols, if any found invoke compilation again

            // NOTE: the look-up of missing source files, based on message in compiler output,
            // is limited to checking source file contents in sourceFileNames in a very
            // limited and naive way, nothing in comparison to the mechanism that exists in the Java compiler itself.

            Set<File> missingSources = new LinkedHashSet<>();
            for (Set<String> missing : missingSymbols.values()) {
                missingSources.addAll(findMissingSources(sourceFileNames, missing));
            }

            if (!missingSources.isEmpty()) {
                compile(Stream.concat(missingSources.stream(), sourceFileNames.stream().map(File::new))
                    .toArray(File[]::new));
                return;
            }

        }

        compilerLogs.entrySet().forEach(e -> e.getValue().println(e.getKey()));

        if (compileErrors.size() == 1) {
            throw compileErrors.get(0);
        } else if (compileErrors.size() > 1) {
            String msg = compileErrors.size() + " errors:" + System.lineSeparator()
                + compileErrors.stream()
                    .map(ex -> ex.getLocation() + ": " + ex.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new CompileException(msg, compileErrors.get(0).getLocation());
        }
    }

    public final boolean compile(File[] _sourceFiles) throws CompileException, IOException {
        PathResource[] sourceFileResources = Arrays.stream(_sourceFiles)
            .map(e -> new PathResource(e.toPath()))
            .toArray(PathResource[]::new);

        this.compile(sourceFileResources);

        return true;
    }

    /**
     * Attempts to locate missing symbols in additional source files that are not yet part of the list of files to be compiled.
     *
     * @param _sourceFileNamesToCompile known source file names that are part of the current compile job
     * @param _missingSymbols one or more missing symbols
     * @return set of additional source files
     * @throws IOException if a source cannot be read
     */
    Set<File> findMissingSources(List<String> _sourceFileNamesToCompile, Set<String> _missingSymbols) throws IOException {
        if (_missingSymbols.isEmpty()) {
            return Set.of();
        }

        List<File> sourceDirs = _sourceFileNamesToCompile.stream()
            .map(File::new)
            .map(File::getParentFile)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Set<File> newSourceFiles = sourceDirs.stream().flatMap(d -> Stream.of(d.list())
            .filter(f -> !_sourceFileNamesToCompile.contains(f))
            .map(f -> new File(d, f))
            .filter(CompileUtil::isJavaFile)).collect(Collectors.toSet());

        return CompileUtil.findMissingSources(_missingSymbols, newSourceFiles);
    }

    private static JavaCompiler getSystemJavaCompiler() {
        return Optional.ofNullable(ToolProvider.getSystemJavaCompiler())
            .orElseThrow(() -> new UnsupportedOperationException(
                "JDK Java compiler not available. A JDK (not JRE) is required"));
    }

    /**
     * Creates the underlying {@link JavaFileManager} lazily, because {@link #setSourcePath(File[])} and consorts are
     * called <em>after</em> initialization.
     */
    private JavaFileManager getJavaFileManager() {
        if (fileManagerEnn == null) {
            fileManagerEnn = createJavaFileManager();
        }
        return fileManagerEnn;
    }

    private JavaFileManager createJavaFileManager() {

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and CLASSPATH.
        JavaFileManager jfm = getSystemJavaCompiler().getStandardFileManager(null, null, null);
        standardFileManager = (StandardJavaFileManager) jfm;

        // Store .class file via the classFileFactory
        jfm = JavaFileManagerUtil.fromResourceFactory(
            jfm,
            StandardLocation.CLASS_OUTPUT,
            Kind.CLASS,
            classFileFactory,
            Charset.defaultCharset());

        jfm = new FallbackResourceLocatorInputJavaFileManager(jfm, new ResourceLocatorInputJavaFileManager<>(
            classFileLocator,
            jfm,
            StandardLocation.CLASS_PATH,
            Kind.CLASS,
            Charset.defaultCharset()
        ));

        // Wrap it in a file manager that finds source using sourceLocator
        jfm = new ResourceLocatorInputJavaFileManager<>(sourceLocator, jfm,
            StandardLocation.SOURCE_PATH, Kind.SOURCE, sourceCharset);

        fileManagerEnn = jfm;
        return fileManagerEnn;
    }

}
