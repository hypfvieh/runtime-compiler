package com.github.hypfvieh.java.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.exception.CompileException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

class RtCompilerTest extends AbstractBaseTest {

    /**
     * Test to check if compiler will stop when a import is missing.
     */
    @Test
    void testCompileMissingImports() {
        String source = "package test;\n"
                + "\n"
                + "public class TmpCompileErrorMissingImport {\n"
                + "    public static void main(String[] args) {\n"
                + "        // should fail to compile because java.util.List is not imported\n"
                + "        System.out.println(\"List: \" + List.of(\"1\",\"2\",\"3\"));\n"
                + "    }\n"
                + "}";

        File tmpSourceFile = createTestFile("TmpCompileErrorMissingImport.java", source);

        RtCompiler compiler = createCompiler();

        assertThrows(CompileException.class, () -> compiler.compile(getTestcaseOutputDir(), tmpSourceFile));
    }

    @Test
    void testCompileAndCreateFile() {
        RtCompiler compiler = createCompiler();

        String jpackage = "test.gourmet.garry";
        String jcode = "package " + jpackage + ";\n"
                + "public class TmpTestRtCompilerProcessor {\n "
                    + "public TmpTestRtCompilerProcessor() {\n"
                        + "System.out.println(\"TmpTestRtCompilerProcessor successfully loaded\");"
                    + "}\n"
                    + "public void helloWorld() {\n"
                        + "System.out.println(\"Hello World, says TmpTestRtCompilerProcessor!\");\n"
                    + "}\n"
                + "}\n";
        File tmpSourceFile = createTestFile("TmpTestRtCompilerProcessor.java", jcode);

        assertDoesNotThrow(() -> {
            compiler.compile(getTestcaseOutputDir(), tmpSourceFile);
            Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".TmpTestRtCompilerProcessor");

            fileCreator.getDeclaredConstructor().newInstance();
        }, () -> "Failed to compile java source file " + tmpSourceFile);

        File classFile = new File(getTestcaseOutputDir(), jpackage.replace('.', File.separatorChar) + File.separatorChar + "TmpTestRtCompilerProcessor.class");
        assertTrue(classFile.exists());
    }

    private RtCompiler createCompiler() {
        RtCompiler compiler = new RtCompiler();
        compiler.setDebuggingInformation(true, true, true);
        compiler.setCompileToMemory(false);
        return compiler;
    }

    @Test
    void testIgnoreCompilerCodeKindNote() {

        // to test ignore error codes, try to compile code which produces the compiler code you want to ignore
        // in this case we want to test "deprecation warnings",

        String jpackage = "test.bang.bag";
        String jcode = "package " + jpackage + ";\n"
                + "public class TmpTestDeprecatedRtCompilerProcessor {\n"
                    + "public TmpTestDeprecatedRtCompilerProcessor() {\n"
                        + "java.io.StringBufferInputStream is; //deprecated\n"
                    + "}\n"
                + "}\n";
        File tmpSourceFile = createTestFile("TmpTestDeprecatedRtCompilerProcessor.java", jcode);

        assertDoesNotThrow(() -> {
            RtCompiler compiler = new RtCompiler();
            compiler.compile(getTestcaseOutputDir(), tmpSourceFile);

            Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".TmpTestDeprecatedRtCompilerProcessor");
            fileCreator.getDeclaredConstructor().newInstance();
        }, () -> "Failed to compile java source file " + tmpSourceFile);

    }

    /**
     * Test to check if package-already-seen warning is suppressed.
     */
    @Test
    void testIgnoreCompilerCodePackageInfoAlreadySeen() throws UnsupportedEncodingException {

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;

        ByteArrayOutputStream stdOutBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErrBaos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdOutBaos, true, Charset.defaultCharset().name()));
        System.setErr(new PrintStream(stdErrBaos, true, Charset.defaultCharset().name()));

        RtCompiler compiler = new RtCompiler();
        compiler.setDebuggingInformation(true, true, true);

        String jpackage = "test.holiday.geneva.cern";
        String jcode = "package " + jpackage + ";\n"
                + "public class PackageInfoSeen1 {\n"
                    + "public PackageInfoSeen1() {\n"
                    + "}\n"
                + "}\n";
        File tmpSourceFile1 = createTestFile("PackageInfoSeen1.java", jcode);
        File tmpSourceFile2 = createTestFile("PackageInfoSeen2.java", jcode.replace("PackageInfoSeen1", "PackageInfoSeen2"));

        try {
            compiler.compile(getTestcaseOutputDir(), tmpSourceFile1, tmpSourceFile2);

            Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".PackageInfoSeen1");
            fileCreator.getDeclaredConstructor().newInstance();

            assertTrue(!stdOutBaos.toString().contains("pkg-info.already.seen"));
            assertTrue(!stdErrBaos.toString().contains("pkg-info.already.seen"));

        } catch (Exception _ex) {
            fail("Failed to compile temp java source file: " + _ex.getMessage(), _ex);
        } finally {
            // reset stdin/stdout
            System.setOut(oldOut);
            System.setErr(oldErr);
        }

    }

    @Test
    void testCompileInheritance() throws Exception {
        RtCompiler compiler = new RtCompiler();

        String jpackage = "test.foo.bar";
        File fileChild = createTestFile("InheritanceChild.java", "package " + jpackage + "; public class InheritanceChild extends InheritanceParent {}");
        createTestFile("InheritanceParent.java", "package " + jpackage + "; public class InheritanceParent {}");

        compiler.compile(getTestcaseOutputDir(), fileChild);
        Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".InheritanceChild");
        fileCreator.getDeclaredConstructor().newInstance();
    }

    @Test
    void testCompileUseOtherNonCompiled() throws Exception {
        RtCompiler compiler = new RtCompiler();

        String jpackage = "test.usage.test";
        File fileChild = createTestFile("UsageTest.java", "package " + jpackage + "; public class UsageTest { public static void main(String[] args) { UsageMember.main(null); } }");
        createTestFile("UsageMember.java", "package " + jpackage + "; public class UsageMember {public static void main(String[] args) { System.out.println(\"Usage works\"); }}");

        compiler.compile(getTestcaseOutputDir(), fileChild);
        Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".UsageTest");
        fileCreator.getDeclaredConstructor().newInstance();
    }

    @Test
    void testCompileUseOtherNonCompiledStaticMember() throws Exception {
        RtCompiler compiler = new RtCompiler();

        String jpackage = "test.usage.noncompiled";
        File fileChild = createTestFile("FirstCompiled.java",
                "package " + jpackage + "; public class FirstCompiled { public void method() { System.out.println(\" + LastCompiled.SOME_VALUE + \"); } }");
        createTestFile("LastCompiled.java", "package " + jpackage + "; public class LastCompiled {public static final String SOME_VALUE = \"whoohoo\"; }");

        compiler.compile(getTestcaseOutputDir(), fileChild);
        Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".FirstCompiled");
        fileCreator.getDeclaredConstructor().newInstance();
    }

    @Test
    void testCompileOverrideInherited() throws Exception {
        RtCompiler compiler = new RtCompiler();

        File toCompile = new File(getMavenTestResources("uncompiled"), "SubClass.java");
        compiler.compile(getTestcaseOutputDir(), toCompile);
        Class<?> fileCreator = compiler.getClassLoader().loadClass("uncompiled.SubClass");
        Object newInstance = fileCreator.getDeclaredConstructor().newInstance();
        Method declaredMethod = fileCreator.getDeclaredMethod("testMethod");
        declaredMethod.setAccessible(true);
        declaredMethod.invoke(newInstance);
    }

    @Test
    void testCompileSubClassUseConstantFromAbstractClass() throws Exception {
        RtCompiler compiler = new RtCompiler();

        String jpackage = "test.usage.tooabstract";
        createTestFile("AbstractFirst.java",
                "package " + jpackage + "; public class AbstractFirst { public static final int FIRST = 1; protected void myMethod() { System.out.println(\"2\"); } }");
        File sub = createTestFile("ConcreteSub.java",
                "package " + jpackage + "; public class ConcreteSub extends AbstractFirst { @Override protected void myMethod() { System.out.println(AbstractFirst.FIRST); } }");

        compiler.compile(getTestcaseOutputDir(), sub);
        Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".ConcreteSub");
        fileCreator.getDeclaredConstructor().newInstance();
    }

    /**
     * Test for 'compiler.err.cant.resolve.location'.
     * In this case a Class which should be compiled and run (e.g. through RtCompilerRunner)
     * uses (not inherits from) another (uncompiled) class found as source in same package.
     *
     * @throws Exception if something unexpected happens
     */
    @Test
    void testCompileCantResolveLocation() throws Exception {
        RtCompiler compiler = new RtCompiler();

        String jpackage = "test.fun.bird";
        File filetoCompile = createTestFile("Fun.java", "package " + jpackage + "; public class Fun { private Bird bird = new Bird(); }");
        createTestFile("Bird.java", "package " + jpackage + "; public class Bird { public Bird() { System.out.println(\"Bird is flying\"); }}");

        compiler.compile(getTestcaseOutputDir(), filetoCompile);
        Class<?> fileCreator = compiler.getClassLoader().loadClass(jpackage + ".Fun");
        fileCreator.getDeclaredConstructor().newInstance();
    }

}
