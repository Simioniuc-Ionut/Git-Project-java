package pack;

public record PackObjectHeader(git.pack.PackObjectType type, int size) {}