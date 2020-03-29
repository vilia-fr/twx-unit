package fr.vilia.twxunit;

public class AssertionFailedError extends RuntimeException {

    public AssertionFailedError() {
    }

    public AssertionFailedError(String message) {
        super(message);
    }

    public AssertionFailedError(String message, Throwable cause) {
        super(message, cause);
    }

    public AssertionFailedError(Throwable cause) {
        super(cause);
    }

    public AssertionFailedError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
