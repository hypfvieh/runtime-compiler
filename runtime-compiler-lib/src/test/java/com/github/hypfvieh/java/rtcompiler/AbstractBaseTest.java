package com.github.hypfvieh.java.rtcompiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class AbstractBaseTest extends Assertions {
    protected static final String SAMPLES = "src/test/java/com/github/hypfvieh/java/rtcompiler/samples/";
    protected static final String OUTPUT_DIR = "target/";

    /** Holds information about the current test. */
    private TestInfo            lastTestInfo;

    @BeforeEach
    protected void setTestMethodName(TestInfo _testInfo) {
        lastTestInfo = _testInfo;
    }

    protected String getShortTestMethodName() {
        return lastTestInfo.getTestMethod().map(Method::getName).orElse(null);
    }

    protected File getTestcaseOutputDir() {
        return new File(OUTPUT_DIR, getShortTestMethodName());
    }

    /**
     * Creates a Java source file in the unit test's subdirectory under {@value #OUTPUT_DIR} ({@link #OUTPUT_DIR});
     *
     * @param _fileName file name
     * @param _fileContent file contents
     * @return file object
     */
    protected File createTestFile(String _fileName, String _fileContent) {
        File tmpInputFile = new File(getTestcaseOutputDir(), _fileName);
        tmpInputFile.getParentFile().mkdirs();
        tmpInputFile.deleteOnExit();

        assertDoesNotThrow(() -> Files.write(tmpInputFile.toPath(), _fileContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
            () -> "Failed to create temp java source file");
        return tmpInputFile;
    }

    protected static File getMavenTestResources(String... _subDirs) {
        File file = new File("src/test/resources/");
        if (_subDirs != null) {
            for (String subDir : _subDirs) {
                if (subDir == null) {
                    break;
                }
                file = new File(file, subDir);
            }
        }
        return file;
    }
}
