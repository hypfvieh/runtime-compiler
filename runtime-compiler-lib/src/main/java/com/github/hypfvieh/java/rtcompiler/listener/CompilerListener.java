package com.github.hypfvieh.java.rtcompiler.listener;

import com.github.hypfvieh.java.rtcompiler.CompileUtil;
import com.github.hypfvieh.java.rtcompiler.exception.CompileException;
import com.github.hypfvieh.java.rtcompiler.exception.DiagnosticLocation;
import com.github.hypfvieh.java.rtcompiler.resources.ResourceBasedJavaFileObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * This compiler listener collects compilation errors and compiler logs
 * into the data structures provided to it during construction.
 *
 * @since 1.0.0 - 2024-01-25
 */
public class CompilerListener implements DiagnosticListener<JavaFileObject> {

    /** Compiler warning codes to ignore (not logged). */
    private final List<String>             ignoreCompilerCodes = List.of("compiler.warn.pkg-info.already.seen");
    /** List of compile errors. */
    private final List<CompileException>   compileErrors;
    /** Map of compiler codes and print stream for later output. */
    private final Map<String, PrintStream> logs;
    /** List of missing symbols (in compilation failures). */
    private final Map<Type, Set<String>>   missingSymbols;
    /** Set of classes which were unresolveable (compiler failure) because of a different package. */
    private final Set<File>                unresolvableSymbols;

    public CompilerListener(Set<File> _unresolvableSymbols, List<CompileException> _compileErrors, Map<String, PrintStream> _logs, Map<Type, Set<String>> _missingSymbols) {
        unresolvableSymbols = Objects.requireNonNull(_unresolvableSymbols);
        compileErrors = Objects.requireNonNull(_compileErrors);
        logs = Objects.requireNonNull(_logs);
        missingSymbols = Objects.requireNonNull(_missingSymbols);
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> _diagnostic) {
        DiagnosticLocation loc = new DiagnosticLocation(Objects.toString(_diagnostic.getSource(), "{unknown source}"),
            (int) _diagnostic.getLineNumber(), (int) _diagnostic.getColumnNumber());
        String msg = _diagnostic.getMessage(null) + " (" + _diagnostic.getCode() + ")";

        String logMsg = loc + ": " + msg;

        if (_diagnostic.getCode() != null) {
            if (_diagnostic.getCode().startsWith("compiler.err.cant.resolve")) {
                String missingClass = Optional.ofNullable(extractMissing(_diagnostic))
                    .orElse(CompileUtil.findMissingSymbolName(_diagnostic.getMessage(Locale.ENGLISH)));
                if (missingClass != null) {
                    putToMap(Type.CLASS, missingClass);
                } else {
                    String missingVar = Optional.ofNullable(extractMissing(_diagnostic))
                        .orElse(CompileUtil.findMissingVariable(_diagnostic.getMessage(Locale.ENGLISH)));
                    if (missingVar != null) {
                        putToMap(Type.VARIABLE, missingVar);
                    }
                }
            } else if (_diagnostic.getCode().startsWith("compiler.err.doesnt.exist")) {
                handleErrDoesNotExist(_diagnostic, loc);
            }
        }

        boolean isError = Diagnostic.Kind.ERROR == _diagnostic.getKind();
        if (isError) { // we have an error
            logs.put(logMsg, System.err);
        } else if (!ignoreCompilerCodes.contains(_diagnostic.getCode())) {
            logs.put(logMsg, System.out);
        }

        // do not throw exception if this diagnostic object is of kind != 'ERROR'
        if (isError) {
            compileErrors.add(new CompileException(msg, loc));
        }
    }

    /**
     * Extracts missing class names or variable names from a diagnostic message.
     *
     * @param _diagnostic message to process
     * @return extracted error part or null
     */
    private String extractMissing(Diagnostic<? extends JavaFileObject> _diagnostic) {
        if (_diagnostic == null) {
            return null;
        }

        try {
            String str = String.valueOf(_diagnostic.getSource().getCharContent(true));
            return str.substring((int) _diagnostic.getStartPosition(), (int) _diagnostic.getEndPosition());
        } catch (IOException _ex) {
        }
        return null;
    }

    private void putToMap(Type _type, String _content) {
        Set<String> list = missingSymbols.get(_type);
        if (list == null) {
            list = new LinkedHashSet<>();
            missingSymbols.put(_type, list);
        }
        list.add(_content);
    }

    /**
     * Takes care of 'package a.b does not exist' compiler errors.
     *
     * @param _diagnostic compiler diagnostic object
     * @param _location location of error in source code
     */
    private void handleErrDoesNotExist(Diagnostic<? extends JavaFileObject> _diagnostic, DiagnosticLocation _location) {
        String pkgName = extractMissing(_diagnostic); // the missing package name

        if (pkgName != null) {
            pkgName = pkgName.substring(0, pkgName.lastIndexOf('.'));
        } else {
            pkgName = _diagnostic.getMessage(Locale.US).replaceFirst("package ([^\\s]+) does not exist", "$1");
        }

        String fn = _diagnostic.getSource().getName();

        if (_diagnostic.getSource() instanceof ResourceBasedJavaFileObject) {
            fn = ((ResourceBasedJavaFileObject) _diagnostic.getSource()).getResource().getResourceName();
        }
        File currentSourceFile = new File(fn); // the file which caused the failure

        pkgName = pkgName.replace(".", File.separator);

        try {
            String curSrcAbsolutePath = currentSourceFile.getAbsolutePath(); // absolute path of failed source

            String srcFqcnWhichFailed = CompileUtil.getClassNameFromSourceFile(currentSourceFile); // FQCN of failed source file
            if (srcFqcnWhichFailed == null) {
                logs.put(_location + ": " + "Could not find FQCN for source file: " + currentSourceFile, System.err);
                return;
            }

            srcFqcnWhichFailed = srcFqcnWhichFailed.replace(".", File.separator) + ".java";
            curSrcAbsolutePath = curSrcAbsolutePath.replace(srcFqcnWhichFailed, ""); // create the guessed 'root path' of all classes

            File searchPath = new File(curSrcAbsolutePath, pkgName); // search in path consisting of guessed-root-path plus packagename as path of failure
            try (Stream<Path> walk = Files.walk(searchPath.toPath())) {
                walk
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> f.getName().toLowerCase().endsWith(".java"))
                    .forEach(unresolvableSymbols::add); // add all matches to unresolvedSymbols set
            } catch (IOException _ex) {
                logs.put(_location + ": " + "Could not retrieve additional required classes from " + searchPath  + " - " + _ex.getMessage(), System.err);
            }
        } catch (IOException _ex) {
            logs.put(_location + ": " + "Could not retrieve additional required classes: " + _ex.getMessage(), System.err);
        }

    }

    public enum Type {
        CLASS,
        VARIABLE;
    }

}
