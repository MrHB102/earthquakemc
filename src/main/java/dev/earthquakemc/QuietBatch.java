package dev.earthquakemc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** A {@code horizon:quiet_commands} payload: a mod id, then command lines without their slash. */
record QuietBatch(String modId, List<String> lines) {
    static final String CHANNEL = "horizon:quiet_commands";
    // Caps from Horizon's own decoder (crates/horizon-protocol/src/quiet.rs); a payload that breaks one is refused whole.
    static final int MAX_LINES = 256, MAX_LINE = 256, MAX_MOD_ID = 64, MAX_PAYLOAD = 32767;

    static QuietBatch decode(byte[] data) {
        if (data.length > MAX_PAYLOAD) throw new IllegalArgumentException("payload too big");
        ByteBuffer in = ByteBuffer.wrap(data);
        String modId = string(in, MAX_MOD_ID);
        int count = varint(in);
        if (count < 0 || count > MAX_LINES) throw new IllegalArgumentException(count + " lines");
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) lines.add(string(in, MAX_LINE));
        if (in.hasRemaining()) throw new IllegalArgumentException("trailing bytes");
        return new QuietBatch(modId, List.copyOf(lines));
    }

    private static int varint(ByteBuffer in) {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            byte b = in.get();
            value |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return value;
        }
        throw new IllegalArgumentException("varint too long");
    }

    private static String string(ByteBuffer in, int maxChars) {
        int len = varint(in);
        if (len < 0 || len > in.remaining()) throw new IllegalArgumentException("string overruns payload");
        byte[] bytes = new byte[len];
        in.get(bytes);
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (s.codePointCount(0, s.length()) > maxChars) throw new IllegalArgumentException("string too long");
        return s;
    }
}
