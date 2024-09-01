//package pack;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//
//import java.nio.ByteBuffer;
//
//import static java.lang.StringUTF16.inflate;
//
//@RequiredArgsConstructor
//public class PackParser {
//    public static final int TYPE_MASK = 0b01110000;
//    public static final int SIZE_4_MASK = 0b00001111;
//    public static final int SIZE_7_MASK = 0b0111_1111;
//    public static final int SIZE_CONTINUE_MASK = 0b1000_0000;
//    private final ByteBuffer buffer;
//
//    @SneakyThrows
//    public List<PackObject> parse() throws IOException, DataFormatException {
//
//        final PackObject object =
//                switch (type) {
//                    case COMMIT:
//                    case TREE:
//                    case BLOB: {
//                        final var content = inflate(header.size());
//                        yield PackObject.undeltified(type.nativeType(), content);
//                    }
//                    case TAG: {
//                        throw new UnsupportedOperationException();
//                    }
//                    case OFS_DELTA: {
//                        throw new UnsupportedOperationException();
//                    }
//                    case REF_DELTA: {
//                        final var hashBytes = new byte[Git.HASH_BYTES_LENGTH];
//                        buffer.get(hashBytes);
//                        final var baseHash = Git.HEX.formatHex(hashBytes);
//                        final var content = inflate(header.size());
//                        final var contentBuffer = ByteBuffer.wrap(content);
//                        @SuppressWarnings("unused") final var baseObjectSize =
//                                parseVariableLengthIntegerLittleEndian(contentBuffer);
//                        final var newObjectSize =
//                                parseVariableLengthIntegerLittleEndian(contentBuffer);
//                        final var instructions = parseDeltaInstructions(contentBuffer);
//                        yield PackObject.deltified(baseHash, newObjectSize, instructions);
//                    }
//                }
//    }
//};
