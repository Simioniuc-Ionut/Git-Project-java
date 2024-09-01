package domain.serial;

import domain.GitObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
public interface ObjectContentSerializer<T extends GitObject> {
    void serialize(T object, DataOutputStream dataOutputStream)
            throws IOException;
    T deserialize(DataInputStream dataInputStream) throws IOException;
}