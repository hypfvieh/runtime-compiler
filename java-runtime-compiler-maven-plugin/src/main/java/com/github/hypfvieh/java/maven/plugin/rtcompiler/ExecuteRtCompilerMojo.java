package com.github.hypfvieh.java.maven.plugin.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.RtCompilerUtil;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The execute goal of the Maven plugin compiles the configured list of java sources using the,
 * runtime-compiler and executes the static main method in an
 * optionally configured main class.<br>
 *
 * The run-time environment must be a JDK (Java Development Kit), so source files may be compiled.<br>
 *
 * @author spannm / hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
//requiresDependencyResolution must be set to see the classpath
@Mojo(name = "execute", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
      configurator = "include-project-compile-dependencies")
public class ExecuteRtCompilerMojo extends AbstractMojo {

    private static final String METHOD_SET_MAVEN_PROJECT = "setMavenProject";
    private static final String METHOD_MAIN              = "main";

    /**
     * Constant for the plugin name.
     */
    private static final String PLUGIN_NAME              = "java-runtime-compiler-maven-plugin";

    /**
     * Reference to maven project itself.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject        mavenProject;

    /**
     * List of source directories or source files. Directories are scanned recursively for Java files
     * (having a *.java file extension). All other files are ignored.
     * <br>
     * At least one source file must be found or the plugin will abort your build process with an error.
     */
    @Parameter(property = "sourceLocations", required = true)
    private List<File>          sourceLocations;

    /**
     * List of paths/files which should be used as classpath instead of the compile time classpath.<br>
     * The list can either contain files or directories.<br>
     * The latter will be searched recursively to find all class/jar files.
     */
    @Parameter(property = "classPathElements", required = false)
    private List<File>          classpathElements;

    /**
     * If set to true, all elements of {@link #classpathElements} will be added to the compile time classpath.<br>
     * If false, {@link #classpathElements} will replace the compile time classpath (default).
     */
    @Parameter(defaultValue = "false", property = "addClassPathElements", required = false)
    private boolean             addClassPathElements;

    /**
     * Adds test classpath to compile/execution classpath.
     */
    @Parameter(defaultValue = "false", property = "addClassPathPath", required = false)
    private boolean             addTestClassPath;

    /**
     * Print the used class path when executing code. Defaults to false.<br>
     */
    @Parameter(defaultValue = "false", property = "printClassPath", required = false)
    private boolean             printClassPath;

    /**
     * The output directory to write Java class files to that have been compiled by this plugin.<br>
     * The directory is created if it not yet exists.<br>
     * If not configured the directory defaults to the Maven project build directory and the plugin name as subdirectory.<br>
     */
    @Parameter(defaultValue = "${project.build.directory}/" + PLUGIN_NAME + "/", property = "classOutputDir", required = false)
    private File                classOutputDir;

    /**
     * The fully qualified name of an optional main class to execute after successful compilation.<br>
     * The specified class must contain the standard Java static void main() method
     * that takes a String array as its only argument.
     */
    @Parameter(property = "mainClass", required = false)
    private String              mainClass;

    /**
     * Optional array of String arguments to pass to the main method.
     */
    @Parameter(property = "mainArgs", required = false)
    private String[]            mainArgs;

    /**
     * Optional array of String arguments to set as system properties.
     */
    @Parameter(property = "systemProperties", required = false)
    private Properties          systemProperties;

    /**
     * Optional parameter to disable forking of JVM.
     * This flag is ignored when called script/program implements a 'setMavenProject' method.
     * When this method is used, the process will never be forked because the MavenProject object
     * would not be available in the forked process. */
    @Parameter(property = "noFork", required = false)
    private boolean             noFork;

    /**
     * Optional parameter to disable failing of plugin execution when execution of java code fails.
     */
    @Parameter(property = "failOnError", required = false)
    @SuppressWarnings("PMD.ImmutableField")
    private boolean             failOnError              = true;

    /**
     * Optional parameter to allow changing the working directory of executed scripts/programs.
     */
    @Parameter(property = "workingDirectory", required = false)
    private File                workingDirectory;

    /**
     * Implementation of the plugin's 'execute' goal.<br>
     *
     * @throws MojoExecutionException if an unexpected problem occurs.
     *         Throwing this exception causes a "BUILD ERROR" message to be displayed.
     * @throws MojoFailureException if an expected problem (such as a compilation failure) occurs.
     *         Throwing this exception causes a "BUILD FAILURE" message to be displayed.
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // setup classpath for the compiler
        List<File> classPathFiles = new ArrayList<>();

        List<File> clzPathElements = getClasspathElements();

        // always add the output directory of newly compiled classes first in classpath
        classPathFiles.add(getClassOutputDir());

        if (clzPathElements == null || clzPathElements.isEmpty()) {
            getLog().debug("No custom classpath given, using depenencies from maven project as classpath");
            classPathFiles.addAll(getMavenProjectClassPath());
        } else {
            if (isAddClassPathElements()) {
                getLog().debug("Adding depenencies from maven project path to classpath");
                classPathFiles.addAll(getMavenProjectClassPath());
            }
            clzPathElements.stream().map(this::resolveDir).forEach(classPathFiles::addAll);
        }

        if (addTestClassPath) {
            try {
                mavenProject.getTestClasspathElements().stream().map(File::new).forEach(classPathFiles::add);
            } catch (DependencyResolutionRequiredException _ex) {
                getLog().error("Unable to resolve test dependencies", _ex);
            }
        }

        if (workingDirectory == null) {
            workingDirectory = new File(System.getProperty("user.dir", "."));
        }

        getLog().info(PLUGIN_NAME + " configuration:");
        getLog().info("  project           : " + getMavenProject());
        getLog().info("  sourceLocations   : " + getSourceLocations());
        getLog().info("  classOutputFolder : " + getClassOutputDir());
        if (printClassPath) {
            getLog().info("  classPath         : " + (classPathFiles.isEmpty()
                    ? "<empty classpath>"
                    : classPathFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator))));
        }

        if (getSystemProperties() != null && !getSystemProperties().isEmpty()) {
            getLog().info("  systemProperties  : ");
            for (String p : getSystemProperties().stringPropertyNames()) {
                getLog().info("    " + p + " => " + getSystemProperties().getProperty(p));
            }
        }

        getLog().info("  workingDirectory  : " + workingDirectory.getAbsolutePath());
        getLog().info("  mainClass         : " + getMainClass());
        getLog().info("  mainArgs          : " + Arrays.toString(getMainArgs()));

        Set<File> sourceFiles = new TreeSet<>();
        for (File location : getSourceLocations()) {
            try (Stream<Path> walk = Files.walk(location.toPath())) {
                sourceFiles.addAll(
                    walk.map(Path::toFile)
                        .filter(File::isFile)
                        .filter(File::canRead)
                        .filter(f -> f.getName().toLowerCase().endsWith("java"))
                        .collect(Collectors.toList())
                );
            } catch (IOException _ex) {
                throw new MojoExecutionException("Failed to build list of source files in location " + location, _ex);
            }
        }

        if (sourceFiles.isEmpty()) {
            throw new MojoFailureException("No readable java source files found");
        }

        // all nice and dandy on the eastern front

        Set<String> compileSourceFiles = compileSourceFiles(sourceFiles, classPathFiles);
        addClassesToCurrentClassLoader(classPathFiles, compileSourceFiles);

        boolean canFork = false;

        if (getMainClass() == null || getMainClass().isBlank()) {
            getLog().info("No main class configured");
        } else {
            Class<?> mainClazz;
            try {
                mainClazz = Class.forName(getMainClass());
            } catch (ClassNotFoundException _ex) {
                throw new MojoFailureException("Main class " + getMainClass() + " not found", _ex);
            }

            if (getMavenProject() != null) {
                // look for an optional static setMavenProject method and invoke it
                // to pass the reference to the execution project
                try {
                    getLog().debug("Looking for method " + METHOD_SET_MAVEN_PROJECT + "(" + MavenProject.class.getSimpleName() + ") in class " + getMainClass());
                    Method setProjMethod = findMethod(mainClazz, METHOD_SET_MAVEN_PROJECT, MavenProject.class);
                    try {
                        setProjMethod.invoke(null, getMavenProject()); // call static method using reflection
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException _ex) {
                        getLog().warn("Failed to execute method " + METHOD_SET_MAVEN_PROJECT + " in class " + getMainClass(), _ex);
                    }
                } catch (NoSuchMethodException _ex) {
                    getLog().debug("No method " + METHOD_SET_MAVEN_PROJECT + " in class " + getMainClass());
                    canFork = true;
                }
            }

            // fork JVM if possible
            if (canFork && !isNoFork()) {
                ForkHelper forkHelper = new ForkHelper(getLog(), workingDirectory);
                int exitCode = forkHelper.createAndRunJvm(classPathFiles, null, getMainClass(), getMainArgs(), getSystemProperties());

                if (exitCode == 0) {
                    getLog().info("Fork ended successfully (exit code: 0)");
                } else if (exitCode > 0) {
                    getLog().warn("Fork exit code: " + exitCode);
                    if (failOnError) {
                        throw new MojoFailureException("Failed to execute main method in class " + getMainClass() + ", JVM exitCode = " + exitCode);
                    }
                } else {
                    getLog().error("Failed to fork JVM");
                    if (failOnError) {
                        throw new MojoFailureException("Failed to execute main method in class " + getMainClass() + ", ProcessBuilder failed");
                    }
                }

            } else {

                System.setProperty("user.dir", workingDirectory.getAbsolutePath());

                if (getSystemProperties() != null) {
                    System.getProperties().putAll(getSystemProperties());
                }

                boolean hasMainArgs = getMainArgs() != null && getMainArgs().length > 0;
                getLog().info("Invoking main method in class " + getMainClass() + (hasMainArgs ? " with arguments: " + Arrays.toString(getMainArgs()) : " without arguments"));
                Method mainMethod;
                try {
                    mainMethod = findMethod(mainClazz, METHOD_MAIN, String[].class);
                } catch (IllegalArgumentException | NoSuchMethodException | SecurityException _ex) {
                    throw new MojoFailureException("No main method found in class " + getMainClass(), _ex);
                }

                try {
                    String stars = "*".repeat(15);
                    getLog().info(stars + " " + PLUGIN_NAME + ": BGN Invocation " + stars);
                    mainMethod.invoke((Object) null, (Object) getMainArgs());
                    getLog().info(stars + " " + PLUGIN_NAME + ": END Invocation " + stars);
                } catch (Exception _ex) {
                    if (failOnError) {
                        throw new MojoFailureException("Failed to execute main method in class " + getMainClass(), _ex);
                    } else {
                        getLog().debug("Siliently ignoring execution failure (failOnError disabled)", _ex);
                    }
                }

            }

        }

    }

    /**
     * Returns a list of files found in the maven projects class path.
     *
     * @return List maybe empty but never null
     * @throws MojoFailureException if failed to read classpath
     */
    List<File> getMavenProjectClassPath() throws MojoFailureException {
        List<File> result = new ArrayList<>();

        if (getMavenProject() != null) {
            try {
                getMavenProject().getRuntimeClasspathElements().stream()
                    .map(File::new)
                    .forEach(result::add);
                getLog().debug("Maven project runtime classpath: " + result);
            } catch (DependencyResolutionRequiredException _ex) {
                throw new MojoFailureException("Failed to read classpath", _ex);
            }
        }

        return result;
    }

    /**
     * Finds all jar and class files in the given directory recursively.<br>
     * If {@code _dir} is not a directory, the object is returned in a list.
     *
     * @param _dir directory to traverse
     * @return List of files, maybe empty, never null
     */
    Collection<? extends File> resolveDir(File _dir) {
        if (_dir == null) {
            return List.of();
        }

        List<File> files = new ArrayList<>();
        files.add(_dir); // add this folder to class path to allow access to resources in the same path (e.g. xml configs)

        if (_dir.isDirectory()) {
            try (Stream<Path> walk = Files.walk(_dir.toPath(), FileVisitOption.FOLLOW_LINKS)) {
                // find and add *.jar/*.class files to class path by directly referencing them
                files.addAll(walk.map(Path::toFile)
                    .filter(f -> {
                        String name = f.getName().toLowerCase();
                        return name.endsWith(".jar") || name.endsWith(".class");
                    })
                    .collect(Collectors.toList()));
            } catch (IOException _ex) {
                getLog().error("Error walking directory " + _dir, _ex);
            }
        }

        return files;
    }

    /**
     * Add all project runtime classes/jars/folders to the current plexus {@link ClassRealm} to allow execution of the newly compiled source.
     */
    void addClassesToCurrentClassLoader(List<File> _classPathFiles, Set<String> _compiledClassesToLoad) throws MojoExecutionException {
        try {

            ClassLoader classLoader = getClass().getClassLoader();

            if (classLoader instanceof ClassRealm) {
                // add classpaths to class loader
                for (File cpElem : _classPathFiles) {
                    ((ClassRealm) classLoader).addURL(cpElem.toURI().toURL());
                }

                // force class loader to load the newly compiled classes, so they are available for the JVM
                for (String clz : _compiledClassesToLoad) {
                    classLoader.loadClass(clz);
                }

            }
        } catch (SecurityException | IllegalArgumentException | MalformedURLException _ex) {
            throw new MojoExecutionException("Failed to add *.class output dir " + getClassOutputDir() + " to system class loader.", _ex);
        } catch (ClassNotFoundException _ex) {
            throw new MojoExecutionException("Could not load additional class.", _ex);
        }
    }

    /**
     * Compiles all given source files.
     *
     * @param _sourceFiles source files
     * @param _classPathFiles class path components
     * @return set of class names (FQCN) created by the compiler
     * @throws MojoFailureException on compilation failure
     */
    Set<String> compileSourceFiles(Collection<File> _sourceFiles, List<File> _classPathFiles) throws MojoFailureException {
        RtCompilerUtil rtCompiler = new RtCompilerUtil();
        rtCompiler.setDebuggingInformation(true, true, true);
        rtCompiler.setCompileToMemory(false);

        if (!_classPathFiles.isEmpty()) {
            rtCompiler.setClasspath(_classPathFiles.toArray(new File[0]));
        }

        Set<String> compiledClasses = new LinkedHashSet<>();
        List<File> failedFiles = new ArrayList<>();
        Optional<Exception> firstFailure = Optional.empty();
        getLog().info("Compiling " + _sourceFiles.size() + " source files");
        for (File sourceFile : _sourceFiles) {
            getLog().debug("Compiling source file: " + sourceFile);
            try {

                Set<String> compiled = rtCompiler.compile(getClassOutputDir(), sourceFile);
                compiledClasses.addAll(compiled);

            } catch (Exception _ex) {
                if (firstFailure.isEmpty()) {
                    firstFailure = Optional.ofNullable(_ex);
                }
                failedFiles.add(sourceFile);
                getLog().error("Failed to compile and write source file " + sourceFile + ": " + _ex);
            }
        }
        if (!failedFiles.isEmpty()) {
            String fileList = failedFiles.stream().map(f -> f.getParentFile().getName() + File.separator +  f.getName()).collect(Collectors.joining(", "));
            throw new MojoFailureException(failedFiles.size() + " file(s) failed to compile: " + fileList, firstFailure.get());
        }

        return compiledClasses;
    }

    /**
     * Tries to find the given method name in the given class using reflection.
     */
    static Method findMethod(Class<?> _class, String _name, Class<?>... _parameterTypes) throws NoSuchMethodException {
        Objects.requireNonNull(_class, "Class required");
        Objects.requireNonNull(_name, "Method name required");

        Method method;
        try {
            method = _class.getMethod(_name, _parameterTypes);
        } catch (NoSuchMethodException | SecurityException _ex) {
            method =
            Arrays.stream(_class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getName().equals(_name))
                .findFirst().orElse(null);
        }
        if (method == null) {
            throw new NoSuchMethodException("Method " + _name + " not found in " + _class);
        }
        return method;
    }

    /**
     * Returns true if class path should be printed on stdout when executing plugin.
     * @return boolean
     */
    boolean isPrintClassPath() {
        return printClassPath;
    }

    /**
     * Returns the configured classpath element.
     * @return list, maybe empty or null
     */
    List<File> getClasspathElements() {
        return classpathElements;
    }

    /**
     * Returns true if {@link #classpathElements} should be added to the compile-time classpath.<br>
     * False means {@link #classpathElements} will replace the classpath entries.
     * @return true to add, false to replace
     */
    boolean isAddClassPathElements() {
        return addClassPathElements;
    }

    /**
     * Returns the Maven project in which this plugin executes.
     * @return Maven project
     */
    MavenProject getMavenProject() {
        return mavenProject;
    }

    /**
     * Returns a list of source locations (files or directories).
     * @return list of File objects
     */
    List<File> getSourceLocations() {
        return sourceLocations;
    }

    /**
     * Returns the output directory to write Java class files to that have been compiled by this plugin.
     * @return output directory
     */
    File getClassOutputDir() {
        return classOutputDir;
    }

    /**
     * Returns the fully qualified name of an optional main class to execute after successful compilation.
     * @return main class name
     */
    String getMainClass() {
        return mainClass;
    }

    /**
     * Returns an optional array of String arguments to pass to the main method.
     * @return argument array
     */
    String[] getMainArgs() {
        return mainArgs;
    }

    /**
     * Returns an optional array of String arguments to set as system properties.
     * @return argument array
     */
    Properties getSystemProperties() {
        return systemProperties;
    }

    /**
     * Adds test classpath to compile/execution classpath.
     */
    boolean isAddTestClassPath() {
        return addTestClassPath;
    }

    /**
     * Returns an boolean indicating if processes should be forked.
     * This flag is ignored when called script/program implements a 'setMavenProject' method.
     * When this method is used, the process will never be forked because the MavenProject object
     * would not be available in forked process.
     *
     * @return boolean
     */
    boolean isNoFork() {
        return noFork;
    }

}
