package com.github.hypfvieh.java.rtcompiler.exception;

/**
 * Exception thrown when compilation fails.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-26
 */
public class CompileException extends Exception {
    private static final long serialVersionUID = 1L;

    private final DiagnosticLocation location;

    public CompileException(String _message, DiagnosticLocation _location) {
        this(_message, _location, null);
    }

    public CompileException(String _message, DiagnosticLocation _location, Throwable _cause) {
        super(_message, _cause);
        location = _location;
    }

    @Override
    public String getMessage() {
        return (location != null ? location.toString() + ": " + super.getMessage() : super.getMessage());
    }

    public DiagnosticLocation getLocation() {
        return location;
    }
}
