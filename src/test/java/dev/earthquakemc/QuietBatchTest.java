package dev.earthquakemc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuietBatchTest {
    private static void string(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b.length); // under 128: one varint byte
        out.writeBytes(b);
    }

    private static byte[] payload(String mod, String... lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        string(out, mod);
        out.write(lines.length);
        for (String l : lines) string(out, l);
        return out.toByteArray();
    }

    @Test
    void decodesWhatHorizonEncodes() {
        // The layout quiet.rs's `encode` writes: mod id, line count, lines.
        var batch = QuietBatch.decode(payload("orbital", "fill 0 0 0 1 1 1 stone", "say hi"));
        assertEquals(new QuietBatch("orbital", List.of("fill 0 0 0 1 1 1 stone", "say hi")), batch);
    }

    @Test
    void refusesTrailingBytesTruncationAndTooManyLines() {
        byte[] ok = payload("m", "a");
        assertThrows(IllegalArgumentException.class, () -> QuietBatch.decode(Arrays.copyOf(ok, ok.length + 1)));
        assertThrows(RuntimeException.class, () -> QuietBatch.decode(Arrays.copyOf(ok, ok.length - 1)));
        ByteArrayOutputStream many = new ByteArrayOutputStream();
        string(many, "m");
        many.writeBytes(new byte[] {(byte) 0x81, 0x02}); // varint 257
        assertThrows(IllegalArgumentException.class, () -> QuietBatch.decode(many.toByteArray()));
    }
}
