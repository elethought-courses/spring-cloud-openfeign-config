package tech.elethoughts.courses.cloud.feign.domain;

public record ApiError(String error, String message, String path) {
}
