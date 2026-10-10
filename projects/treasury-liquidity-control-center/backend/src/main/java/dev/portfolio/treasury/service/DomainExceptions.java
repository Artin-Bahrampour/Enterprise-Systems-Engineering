package dev.portfolio.treasury.service;

public final class DomainExceptions {
    private DomainExceptions() {}
    public static class ResourceNotFound extends RuntimeException {
        public ResourceNotFound(String message) { super(message); }
    }
    public static class IdempotencyConflict extends RuntimeException {
        public IdempotencyConflict(String message) { super(message); }
    }
    public static class InsufficientFunds extends RuntimeException {
        public InsufficientFunds(String message) { super(message); }
    }
}
