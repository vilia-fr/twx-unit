package fr.vilia.twxunit;

public class TestingException extends Exception {

    public TestingException() {
    }

    public TestingException(String message) {
        super(message);
    }

    public TestingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestingException(Throwable cause) {
        super(cause);
    }

    public TestingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
