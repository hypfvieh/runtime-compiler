package com.github.hypfvieh.java.rtcompiler;

import com.github.hypfvieh.java.rtcompiler.exception.CompileException;
import com.github.hypfvieh.java.rtcompiler.resources.locator.PathResource;
import com.github.hypfvieh.java.rtcompiler.resources.writer.FileResourceWriter;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

class BaseCompilerTest extends AbstractBaseTest {

    /**
     * Test to check if compiler will stop when a import is missing.
     */
    @Test
    void testCompileMissingPackage() {
        String source = "package test;\n"
                + "import test.other.Bar;\n"
                + "\n"
                + "public class TmpCompileErrorMissingPackage {\n"
                + "    public static void main(String[] args) {\n"
                + "        // should fail to compile because java.util.List is not imported\n"
                + "        System.out.println(\"List: \" + List.of(\"1\",\"2\",\"3\"));\n"
                + "    }\n"
                + "}";

        File tmpSourceFile = createTestFile("test/TmpCompileErrorMissingPackage.java", source);

        BaseCompiler compiler = createCompiler();

        assertThrows(CompileException.class, () -> compiler.compile(new PathResource(tmpSourceFile.toPath())));
    }

    /**
     * Test setter and getters and defaults.
     */
    @Test
    void testGetterSetterDefaults() {
        BaseCompiler baseCompiler = new BaseCompiler();

        assertEquals(false, baseCompiler.isDebugVars());
        baseCompiler.setDebugVars(true);
        assertEquals(true, baseCompiler.isDebugVars());

        assertEquals(false, baseCompiler.isDebugSource());
        baseCompiler.setDebugSource(true);
        assertEquals(true, baseCompiler.isDebugSource());

        assertEquals(false, baseCompiler.isDebugLines());
        baseCompiler.setDebugLines(true);
        assertEquals(true, baseCompiler.isDebugLines());

        assertEquals(Map.of(), baseCompiler.getMissingSymbols());

        assertNotNull(baseCompiler.getSourceLocator());
        baseCompiler.setSourceLocator(null);
        assertNull(baseCompiler.getSourceLocator());

        assertNotNull(baseCompiler.getClassFileLocator());
        baseCompiler.setClassFileLocator(null);
        assertNull(baseCompiler.getClassFileLocator());

        assertNotNull(baseCompiler.getClassPath());
        baseCompiler.setClassPath(null);
        assertNull(baseCompiler.getClassPath());

        assertIterableEquals(List.of(), baseCompiler.getCompilerOptions());
        baseCompiler.setCompilerOptions("-X", "-g", "-g:FOO");
        assertIterableEquals(List.of("-X"), baseCompiler.getCompilerOptions());

        assertEquals(Set.of(), baseCompiler.getCompilerLogs());
        assertEquals(List.of(), baseCompiler.getCompileErrors());
    }

    private BaseCompiler createCompiler() {
        BaseCompiler compiler = new BaseCompiler();
        compiler.setDebugLines(true);
        compiler.setDebugVars(true);
        compiler.setDebugSource(true);
        compiler.setClassFileCreator(new FileResourceWriter(getTestcaseOutputDir()));
        return compiler;
    }

}
