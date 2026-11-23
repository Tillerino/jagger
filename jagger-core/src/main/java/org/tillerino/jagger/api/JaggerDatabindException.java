package org.tillerino.jagger.api;

/** Thrown when a value cannot be read from or written to the databind format. */
public class JaggerDatabindException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JaggerDatabindException(String message) {
        super(message);
    }

    public JaggerDatabindException(String message, Throwable cause) {
        super(message, cause);
    }

    public JaggerDatabindException(Throwable cause) {
        super(cause);
    }
}
