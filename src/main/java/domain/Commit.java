package domain;
public record Commit(String treeHash, String parentHash, AuthorSignature author,
                     AuthorSignature committer, String message)
        implements GitObject {}