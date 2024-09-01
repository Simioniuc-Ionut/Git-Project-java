package domain;
public record Blob(byte[] data) implements GitObject {}