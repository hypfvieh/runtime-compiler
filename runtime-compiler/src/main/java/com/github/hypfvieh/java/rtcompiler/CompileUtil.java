package com.github.hypfvieh.java.rtcompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for compilation tasks.
 *
 * @since 1.0.0 - 2024-01-25
 */
public final class CompileUtil {
    private static final Pattern REGEX_COMMENT_SINGLELINE    = Pattern.compile("^\\s*//.*$");

    private static final Pattern REGEX_COMMENT_MULTILINE_BGN = Pattern.compile("^\\s*/\\*.*$");

    private static final Pattern REGEX_COMMENT_MULTILINE_END = Pattern.compile(".*\\*/.*$");

    private static final Pattern REGEX_PACKAGE               = Pattern.compile("^(?:\\s*)package(?:\\s+)([a-zA-Z][a-zA-Z0-9_\\$\\.]*)", Pattern.MULTILINE);
    private static final Pattern REGEX_TYPE                  = Pattern.compile("(?:class|interface|enum)(?:\\s+)([a-zA-Z][a-zA-Z0-9_\\$]*)", Pattern.MULTILINE);
    private static final Pattern REGEX_MISSING_SYMBOL        = Pattern.compile("symbol: +(?:class|interface|enum) ([a-zA-Z][a-zA-Z0-9_\\$]*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern REGEX_MISSING_VARIABLE      = Pattern.compile("symbol: +(?:variable) ([a-zA-Z][a-zA-Z0-9_\\$]*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private CompileUtil() {
    }

    public static String getClassNameFromSourceFile(File _sourceFile) throws IOException {
        Objects.requireNonNull(_sourceFile, "Source file required");
        return getClassNameFromSource(Files.readString(_sourceFile.toPath()));
    }

    static String getClassNameFromSource(String _fileContent) {
        Objects.requireNonNull(_fileContent, "File content required");
        String className = getSimpleClassNameFromSource(_fileContent);
        if (className == null) {
            return null;
        }
        String packageName = getPackageNameFromSource(_fileContent);
        return packageName == null ? className : packageName + "." + className;
    }

    static String getPackageNameFromSource(String _fileContent) {
        return findWithRegex(_fileContent, REGEX_PACKAGE);
    }

    static String getSimpleClassNameFromSource(String _fileContent) {
        return findWithRegex(_fileContent, REGEX_TYPE);
    }

    /**
     * Determines the source folder root from a source file.
     *
     * @param _sourceFile source file
     * @return source path
     * @throws IOException if file cannot be read
     */
    static File findSourcePath(File _sourceFile) throws IOException {
        String packageName = getPackageNameFromSource(Files.readString(_sourceFile.toPath()));
        String packagePath = packageName.replace('.', File.separatorChar);
        File path;
        if (_sourceFile.getPath().contains(packagePath)) {
            path = new File(_sourceFile.getPath().replaceFirst(packagePath + ".*" + _sourceFile.getName(), ""));
        } else {
            path = _sourceFile.getParentFile();
        }
        return path;
    }

    /**
     * Attempts to find a missing symbol/type in compiler output.
     * @param _output compiler output
     * @return type name or null
     */
    public static String findMissingSymbolName(String _output) {
        return findWithRegex(_output, REGEX_MISSING_SYMBOL);
    }

    /**
     * Attempts to find a missing variable in compiler output.
     * @param _output compiler output
     * @return variable name or null
     */
    public static String findMissingVariable(String _output) {
        return findWithRegex(_output, REGEX_MISSING_VARIABLE);
    }

    /**
     * Attempts to locate missing symbols i.e. types, member and static variables in a set of source files.
     * @param _missingSymbols missing symbols
     * @param _sourceFiles source files to scan
     * @return source files that contain definitions for at least one of the symbols
     * @throws IOException if source file cannot be read
     */
    static Set<File> findMissingSources(Set<String> _missingSymbols, Set<File> _sourceFiles) throws IOException {
        Set<File> foundMissingSources = new LinkedHashSet<>();

        for (File newSourceFile : _sourceFiles) {
            // file name check
            if (_missingSymbols.stream().anyMatch(s -> newSourceFile.getName().equals(s + ".java"))) {
                foundMissingSources.add(newSourceFile);
            } else {

                List<String> typesInSourceFile = new ArrayList<>();
                for (String sourceLine : Files.readAllLines(newSourceFile.toPath())) {
                    if (_missingSymbols.stream().anyMatch(sym -> sourceLine.matches(" " + sym + "(\\s*;|\\s*=\\s*)"))) {
                        foundMissingSources.add(newSourceFile);
                        break;
                    }
                    Matcher matcher = REGEX_TYPE.matcher(sourceLine);
                    while (matcher.find()) {
                        typesInSourceFile.add(matcher.group(1));
                    }
                }

                if (_missingSymbols.stream().anyMatch(typesInSourceFile::contains)) {
                    foundMissingSources.add(newSourceFile);
                }

            }
        }

        return foundMissingSources;
    }

    private static String findWithRegex(String _str, Pattern _regex) {
        String content = _str;
        if (_str.contains("\n")) {
            content = "";
            boolean inCommentBlock = false;
            for (String line : _str.split("\r?\n")) {

                if (REGEX_COMMENT_SINGLELINE.matcher(line).matches()) {
                    continue;
                } else if (REGEX_COMMENT_MULTILINE_BGN.matcher(line).matches()) {
                    inCommentBlock = true;
                    continue;
                }

                // check line for both, block begin and end
                boolean blockBgn = REGEX_COMMENT_MULTILINE_BGN.matcher(line).matches();
                boolean blockEnd = REGEX_COMMENT_MULTILINE_END.matcher(line).matches();

                if (blockBgn && blockEnd) { // begin and end in same line
                    inCommentBlock = false;
                    continue;
                } else if (blockBgn) { // begin of comment block
                    inCommentBlock = true;
                    continue;
                } else if (blockEnd) { // end of comment block
                    inCommentBlock = false;
                    continue;
                }

                if (inCommentBlock) { // still in comment block
                    continue;
                }

                content += line + System.lineSeparator();
            }
        }
        Matcher matcher = _regex.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    static boolean isJavaFile(File _file) {
        return _file != null && _file.isFile() && _file.getName().toLowerCase().endsWith(".java");
    }

    static String removeExtension(String _fileName) {
        if (_fileName == null) {
            return _fileName;
        }
        int lastDot = _fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return _fileName;
        }
        return _fileName.substring(0, lastDot);
    }

}
