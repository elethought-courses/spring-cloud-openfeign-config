package tech.elethoughts.courses.cloud.feign.infrastructure;

public class CustomHttpException extends RuntimeException {

    private final int status;
    private final String body;

    public CustomHttpException(int status, String message, String body) {
        super(message);
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
