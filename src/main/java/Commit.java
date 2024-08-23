import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
@AllArgsConstructor
public class Commit {
    @Getter @Setter private String author;
    @Getter @Setter private String committer;
    @Getter @Setter private String tree_hash;
    @Getter @Setter private String parent_commit_hash;
    @Getter @Setter private LocalDateTime timestamp;
    @Getter @Setter private String message;
    @Override
    public String toString() {
        return String.format(
                "tree %s\nparent %s\nauthor %s %s\ncommitter %s %s\n\n%s\n", tree_hash,
                parent_commit_hash, author, timestamp.toString(), author,
                timestamp.toString(), message);
    }
}