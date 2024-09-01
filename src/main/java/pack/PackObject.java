package pack;
import domain.ObjectType;
import pack.PackObject.Deltified;
import pack.PackObject.Undeltified;
import java.util.List;
@SuppressWarnings("rawtypes")
public sealed interface PackObject permits Undeltified, Deltified {
    public record Undeltified(ObjectType type, byte[] content)
            implements PackObject {}
    public record Deltified(String baseHash, int size,
                            List<DeltaInstruction> instructions)
            implements PackObject {}
    public static Undeltified undeltified(ObjectType type, byte[] content) {
        return new Undeltified(type, content);
    }
    public static Deltified deltified(String bashHash, int size,
                                      List<DeltaInstruction> instructions) {
        return new Deltified(bashHash, size, instructions);
    }
}