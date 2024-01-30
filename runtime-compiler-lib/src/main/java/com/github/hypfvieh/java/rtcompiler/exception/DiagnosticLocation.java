package com.github.hypfvieh.java.rtcompiler.exception;

/**
 * Class which contains Location information for listeners.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public final class DiagnosticLocation {
    private final String fileName;
    private final int    lineNumber;
    private final int    columnNumber;

    public DiagnosticLocation(String _fileName, int _lineNumber, int _columnNumber) {
        super();
        fileName = _fileName;
        lineNumber = _lineNumber;
        columnNumber = _columnNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.fileName != null) {
            sb.append("File '").append(this.fileName).append("', ");
        }
        sb.append("Line ").append(this.lineNumber).append(", ");
        sb.append("Column ").append(this.columnNumber);
        return sb.toString();
    }
}
