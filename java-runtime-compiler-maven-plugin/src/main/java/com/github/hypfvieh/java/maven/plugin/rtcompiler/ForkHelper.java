package com.github.hypfvieh.java.maven.plugin.rtcompiler;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class to create a JVM process running the given compiled static main.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
public class ForkHelper {

    /**
     * Regular expression to detect java version.
     */
    private static final Pattern JVM_PRE9_VERSION_MATCHER = Pattern.compile("^(?:java|openjdk) version \"(\\d)\\.[^\"]+\".*");

    private final Log            logger;

    private final File           workingDirectory;

    public ForkHelper(Log _logger, File _workingDirectory) {
        logger = _logger;
        workingDirectory = _workingDirectory;
    }

    /**
     * Create and run the given main class using the given class/module path and arguments.
     *
     * @param _classPath class path to use
     * @param _modulePath module path to use (ignored for java &lt; 9)
     * @param _compiledMain FQCN main class to execute
     * @param _args arguments given to compiled main class
     * @param _systemProperties system properties to set for forked JVM (each will be added as -D option)
     *
     * @return exit code of forked JVM or -1 if execution of fork failed due to exception
     */
    public int createAndRunJvm(List<File> _classPath, List<File> _modulePath, String _compiledMain, String[] _args, Properties _systemProperties) {
        String jvmToUse = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        List<String> cmd = new ArrayList<>();
        cmd.add(jvmToUse);
        if (_classPath != null && !_classPath.isEmpty()) {
            String clzPath = _classPath.stream().map(File::getAbsolutePath).collect(Collectors.joining(":"));
            cmd.add("-cp");
            cmd.add(clzPath);
        }

        if (isJava9OrHigher(jvmToUse) && _modulePath != null && !_modulePath.isEmpty()) {
            String modulePath = _modulePath.stream().map(File::getAbsolutePath).collect(Collectors.joining(":"));
            cmd.add("-p");
            cmd.add(modulePath);
        }

        Optional.ofNullable(_systemProperties).ifPresent(s ->
            s.stringPropertyNames().forEach(p -> cmd.add("-D" + p + "=" + _systemProperties.getProperty(p))));

        cmd.add(_compiledMain);

        List<String> args = Optional.ofNullable(_args).map(Arrays::asList).orElse(List.of());
        cmd.addAll(args);

        if (logger.isDebugEnabled()) {
            logger.debug("Forking process: " + cmd);
        } else if (logger.isInfoEnabled()) {
            logger.info("Forking process " + jvmToUse + " with arguments: '" + String.join(" ", args) + "'");
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(workingDirectory);
         // setup working directory
        pb.environment().putAll(System.getProperties().stringPropertyNames().stream().collect(Collectors.toMap(k -> k, System.getProperties()::getProperty)));
        Map<String, String> sysProps = _systemProperties.stringPropertyNames().stream().collect(Collectors.toMap(k -> k, _systemProperties::getProperty));
        if (!sysProps.isEmpty()) {
            pb.environment().putAll(sysProps);
            if (logger.isDebugEnabled()) {
                logger.debug("System properties: " + sysProps.entrySet().stream().map(e -> e.getKey() + " => " + e.getValue()).collect(Collectors.joining(", ")));
            }
        }
        pb.inheritIO().command(cmd.toArray(new String[0]));

        try {
            Process forked = pb.start();
            Thread.sleep(200L); // give fork time to print to stdout during jvm startup

            logger.info("Forked process id (pid): " + forked.pid());
            forked.waitFor();

            return forked.exitValue();
        } catch (IOException | InterruptedException _ex) {
            logger.error(_ex);
        }

        return -1;
    }

    static boolean isEmpty(List<File> _list) {
        return _list == null || _list.isEmpty();
    }

    private boolean isJava9OrHigher(String _jvmToUse) {
        ProcessBuilder pb = new ProcessBuilder(_jvmToUse, "-version");
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();

            try (BufferedInputStream bis = new BufferedInputStream(p.getInputStream())) {
                String versionStr = new String(bis.readAllBytes(), Charset.defaultCharset());
                Matcher matcher = JVM_PRE9_VERSION_MATCHER.matcher(versionStr);
                return !matcher.matches();
            }
        } catch (IOException _ex) {
            return true;
        }
    }
}
