package protocol;

import lombok.RequiredArgsConstructor;
import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import domain.Reference;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@RequiredArgsConstructor
public class GitClient {
    public static final int HASH_BYTES_LENGTH = 20;
    public static final int HASH_STRING_LENGTH = 40;
    public static final HexFormat HEX = HexFormat.of();
    public static final MediaType X_GIT_UPLOAD_PACK_REQUEST =
            MediaType.parse("application/x-git-upload-pack-request");

    private final OkHttpClient httpClient = new OkHttpClient();
    private final URI baseUri;

    public List<Reference> fetchReferences() throws IOException {
        final var references = new ArrayList<Reference>();

        final var request =
                new Request.Builder()
                        .url(HttpUrl.get(baseUri)
                                .newBuilder()
                                .addPathSegment("info")
                                .addPathSegment("refs")
                                .addQueryParameter("service", "git-upload-pack")
                                .build())
                        .get()
                        .build();

        try(final var response = httpClient.newCall(request).execute();
            final var responseBody = response.body();
            final var inputStream = responseBody.byteStream();) {

            if (!response.isSuccessful()) {
                throw new IOException("Request failed with code: " + response.code());
            }

            final var lines = parsePacketLines(inputStream);
            for (final var line : lines) {
                if (!(line instanceof PacketLine.Data data) || data.isComment()) {
                    continue;
                }
                final var bytes = data.content();
                final var hash = new String(bytes, 0, HASH_STRING_LENGTH);
                var startIndex = HASH_STRING_LENGTH + 1;
                var endIndex = startIndex + 1;
                while (endIndex != bytes.length &&
                        (bytes[endIndex] != '\0' && bytes[endIndex] != '\n')) {
                    ++endIndex;
                }

                final var name = new String(bytes, startIndex, endIndex - startIndex);

                references.add(new Reference(name, hash));
            }
        }
     return references;
    }
    public byte[] getPack(Reference reference) throws IOException {
        final var requestLines =
                List.of(PacketLine.data("want %s\n".formatted(reference.hash())),
                        PacketLine.flush(), PacketLine.data("done\n"));
        final var outputStream = new ByteArrayOutputStream();
        for (final var line : requestLines) {
            line.serialize(outputStream);
        }
        final var request = new Request.Builder()
                .url(HttpUrl.get(baseUri)
                        .newBuilder()
                        .addPathSegment("git-upload-pack")
                        .build())
                .post(RequestBody.create(outputStream.toByteArray(),
                        X_GIT_UPLOAD_PACK_REQUEST))
                .build();
        try (final var response = httpClient.newCall(request).execute();
             final var responseBody = response.body();
             final var inputStream = responseBody.byteStream();) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("response is not successful: " +
                        response.code());
            }
            final var first = parsePacketLine(inputStream);
            if (!(first instanceof PacketLine.Data data)) {
                throw new IllegalStateException("first packet line must be data: " +
                        first);
            }
            final var nak = new String(data.content(), 0, 3);
            if (!"NAK".equals(nak)) {
                throw new IllegalStateException("first packet line must be nak: " +
                        nak);
            }
            return inputStream.readAllBytes();
        }
    }
    public List<PacketLine> parsePacketLines(Response response)
            throws IOException {
        //Check if the response is successful
        if (!response.isSuccessful()) {
            throw new IllegalStateException("response is not successful: " +
                    response.code());
        }
        try (final var responseBody = response.body();
             final var inputStream = responseBody.byteStream();) {
            return parsePacketLines(inputStream);
        }
    }

    public List<PacketLine> parsePacketLines(InputStream inputStream)
            throws IOException {
        final var lines = new ArrayList<PacketLine>();
        PacketLine line;
        while ((line = parsePacketLine(inputStream)) != null) {
            lines.add(line);
        }
        return lines;
    }
    public PacketLine parsePacketLine(InputStream inputStream)
            throws IOException {
        final var sizeBuffer = new byte[4];
        if (inputStream.read(sizeBuffer) != sizeBuffer.length) {
            return null;
        }
        final var size =
                Integer.parseInt(new String(sizeBuffer, StandardCharsets.US_ASCII), 16);
        if (size == 0) {
            return PacketLine.flush();
        }
        final var content = inputStream.readNBytes(size - sizeBuffer.length);
        return PacketLine.data(content);
    }
}
