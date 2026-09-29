package app.revanced.extension.youtube.shared;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal editable protobuf message for working with serialized innertube protos
 * without their (obfuscated) generated classes.
 * <p>
 * Fields keep their order and raw values, so a message that is parsed and serialized again
 * without changes produces the same bytes.
 */
public final class ProtoMessage {

    private static final int WIRE_TYPE_VARINT = 0;
    private static final int WIRE_TYPE_FIXED64 = 1;
    private static final int WIRE_TYPE_LENGTH_DELIMITED = 2;
    private static final int WIRE_TYPE_FIXED32 = 5;

    private static final int MAX_SEARCH_DEPTH = 32;

    public static final class Field {
        public final int number;
        public final int wireType;
        /**
         * Payload of a length delimited field, otherwise the encoded value.
         */
        public byte[] value;

        private Field(int number, int wireType, byte[] value) {
            this.number = number;
            this.wireType = wireType;
            this.value = value;
        }

        public static Field ofBytes(int number, byte[] value) {
            return new Field(number, WIRE_TYPE_LENGTH_DELIMITED, value);
        }

        public boolean isLengthDelimited() {
            return wireType == WIRE_TYPE_LENGTH_DELIMITED;
        }
    }

    private final List<Field> fields;

    public ProtoMessage() {
        fields = new ArrayList<>();
    }

    private ProtoMessage(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * @return The parsed message, or null if the bytes are not a valid message.
     */
    @Nullable
    public static ProtoMessage parse(@Nullable byte[] bytes) {
        if (bytes == null) return null;

        List<Field> fields = new ArrayList<>();
        int position = 0;
        while (position < bytes.length) {
            final long tag = readVarint(bytes, position);
            if (tag < 0) return null;
            position += varintSize(tag);

            final int number = (int) (tag >>> 3);
            final int wireType = (int) (tag & 7);
            if (number <= 0) return null;

            final int valueStart;
            final int valueEnd;
            switch (wireType) {
                case WIRE_TYPE_VARINT -> {
                    final long value = readVarint(bytes, position);
                    if (value < 0) return null;
                    valueStart = position;
                    valueEnd = position + varintSize(value);
                }
                case WIRE_TYPE_FIXED64 -> {
                    valueStart = position;
                    valueEnd = position + 8;
                }
                case WIRE_TYPE_LENGTH_DELIMITED -> {
                    final long length = readVarint(bytes, position);
                    if (length < 0) return null;
                    valueStart = position + varintSize(length);
                    final long end = valueStart + length;
                    if (end > bytes.length) return null;
                    valueEnd = (int) end;
                }
                case WIRE_TYPE_FIXED32 -> {
                    valueStart = position;
                    valueEnd = position + 4;
                }
                default -> {
                    return null;
                }
            }
            if (valueEnd > bytes.length) return null;

            byte[] value = new byte[valueEnd - valueStart];
            System.arraycopy(bytes, valueStart, value, 0, value.length);
            fields.add(new Field(number, wireType, value));
            position = valueEnd;
        }

        return new ProtoMessage(fields);
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Field field : fields) {
            writeVarint(out, ((long) field.number << 3) | field.wireType);
            if (field.isLengthDelimited()) {
                writeVarint(out, field.value.length);
            }
            out.write(field.value, 0, field.value.length);
        }
        return out.toByteArray();
    }

    public List<Field> fields() {
        return fields;
    }

    public ProtoMessage copy() {
        List<Field> copy = new ArrayList<>(fields.size());
        for (Field field : fields) {
            copy.add(new Field(field.number, field.wireType, field.value));
        }
        return new ProtoMessage(copy);
    }

    /**
     * @return The payload of the first length delimited field, following nested messages in order.
     */
    @Nullable
    public byte[] getBytes(int... fieldPath) {
        ProtoMessage message = this;
        byte[] value = null;
        for (int i = 0; i < fieldPath.length; i++) {
            if (message == null) return null;
            value = message.firstLengthDelimited(fieldPath[i]);
            if (value == null) return null;
            if (i < fieldPath.length - 1) message = parse(value);
        }
        return value;
    }

    @Nullable
    public ProtoMessage getMessage(int... fieldPath) {
        return parse(getBytes(fieldPath));
    }

    @Nullable
    public String getString(int... fieldPath) {
        byte[] value = getBytes(fieldPath);
        return value == null ? null : new String(value);
    }

    /**
     * @return The first varint field, or the default value if absent.
     */
    public long getVarint(int number, long defaultValue) {
        for (Field field : fields) {
            if (field.number == number && field.wireType == WIRE_TYPE_VARINT) {
                return readVarint(field.value, 0);
            }
        }
        return defaultValue;
    }

    public boolean has(int number) {
        for (Field field : fields) {
            if (field.number == number) return true;
        }
        return false;
    }

    /**
     * Replaces the first field with the number, or adds it if absent.
     */
    public ProtoMessage setBytes(int number, byte[] value) {
        for (Field field : fields) {
            if (field.number == number && field.isLengthDelimited()) {
                field.value = value;
                return this;
            }
        }
        return addBytes(number, value);
    }

    public ProtoMessage setMessage(int number, ProtoMessage message) {
        return setBytes(number, message.toByteArray());
    }

    public ProtoMessage addBytes(int number, byte[] value) {
        fields.add(Field.ofBytes(number, value));
        return this;
    }

    public ProtoMessage addString(int number, String value) {
        return addBytes(number, value.getBytes());
    }

    public ProtoMessage addMessage(int number, ProtoMessage message) {
        return addBytes(number, message.toByteArray());
    }

    public ProtoMessage addVarint(int number, long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, value);
        fields.add(new Field(number, WIRE_TYPE_VARINT, out.toByteArray()));
        return this;
    }

    public ProtoMessage remove(int number) {
        fields.removeIf(field -> field.number == number);
        return this;
    }

    /**
     * @return Every nested message, including the message itself, that has the field at its top level.
     */
    public static List<ProtoMessage> findMessagesWithField(byte[] bytes, int number) {
        List<ProtoMessage> found = new ArrayList<>();
        findMessagesWithField(parse(bytes), number, 0, found);
        return found;
    }

    private static void findMessagesWithField(@Nullable ProtoMessage message, int number,
                                              int depth, List<ProtoMessage> found) {
        if (message == null || depth > MAX_SEARCH_DEPTH) return;
        if (message.has(number)) {
            found.add(message);
            return;
        }
        for (Field field : message.fields) {
            if (field.isLengthDelimited()) {
                findMessagesWithField(parse(field.value), number, depth + 1, found);
            }
        }
    }

    @Nullable
    private byte[] firstLengthDelimited(int number) {
        for (Field field : fields) {
            if (field.number == number && field.isLengthDelimited()) return field.value;
        }
        return null;
    }

    /**
     * @return The varint at the position, or -1 if it is malformed.
     */
    private static long readVarint(byte[] data, int position) {
        long result = 0;
        for (int shift = 0; shift < 64 && position < data.length; shift += 7) {
            final byte b = data[position++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
        }
        return -1;
    }

    private static int varintSize(long value) {
        int size = 1;
        while ((value >>>= 7) != 0) size++;
        return size;
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
    }

    @NonNull
    @Override
    public String toString() {
        return "ProtoMessage{" + fields.size() + " fields}";
    }
}
