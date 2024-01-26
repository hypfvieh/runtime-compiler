package com.github.hypfvieh.java.rtcompiler.exception;

/**
 * RuntimeException used when compiler fails.
 *
 * @since 1.0.0 - 2024-01-25
 */
public class RuntimeCompilerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RuntimeCompilerException() {
    }

    public RuntimeCompilerException(String _message) {
        super(_message);
    }

    public RuntimeCompilerException(Throwable _cause) {
        super(_cause);
    }

    public RuntimeCompilerException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

}
