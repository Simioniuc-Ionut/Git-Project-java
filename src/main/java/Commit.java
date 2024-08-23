import java.time.LocalDateTime;

public class Commit {
    private String author;
    private String committer;
    private String tree_hash;
    private String parent_commit_hash;
    private LocalDateTime timestamp;
    private String message;

    // Constructor
    public Commit(String author, String committer, String tree_hash, String parent_commit_hash, LocalDateTime timestamp, String message) {
        this.author = author;
        this.committer = committer;
        this.tree_hash = tree_hash;
        this.parent_commit_hash = parent_commit_hash;
        this.timestamp = timestamp;
        this.message = message;
    }

    // Getters and Setters
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCommitter() {
        return committer;
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    public String getTreeHash() {
        return tree_hash;
    }

    public void setTreeHash(String tree_hash) {
        this.tree_hash = tree_hash;
    }

    public String getParentCommitHash() {
        return parent_commit_hash;
    }

    public void setParentCommitHash(String parent_commit_hash) {
        this.parent_commit_hash = parent_commit_hash;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        // Adjusting format to match desired output
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(tree_hash).append("\n");
        if (parent_commit_hash != null && !parent_commit_hash.isEmpty()) {
            sb.append("parent ").append(parent_commit_hash).append("\n");
        }
        sb.append("author ").append(author).append(" ").append(timestamp.toString()).append("\n");
        sb.append("committer ").append(committer).append(" ").append(timestamp.toString()).append("\n");
        sb.append("\n").append(message).append("\n");
        return sb.toString();
    }
}
