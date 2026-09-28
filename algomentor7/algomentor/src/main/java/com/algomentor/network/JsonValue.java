package com.algomentor.network;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A minimal tagged-union representation of a parsed JSON value, produced by
 * {@link JsonParser}. No external JSON library is used anywhere in this
 * project - this class plus JsonParser is a complete, from-scratch reader
 * for the subset of JSON this app needs (objects, arrays, strings, numbers,
 * booleans, null).
 */
public class JsonValue {
    public enum Kind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    private final Kind kind;
    private final Map<String, JsonValue> objectValue;
    private final List<JsonValue> arrayValue;
    private final String stringValue;
    private final double numberValue;
    private final boolean booleanValue;

    private JsonValue(Kind kind, Map<String, JsonValue> o, List<JsonValue> a,
                      String s, double n, boolean b) {
        this.kind = kind; this.objectValue = o; this.arrayValue = a;
        this.stringValue = s; this.numberValue = n; this.booleanValue = b;
    }

    static JsonValue ofObject(Map<String, JsonValue> map) { return new JsonValue(Kind.OBJECT, map, null, null, 0, false); }
    static JsonValue ofArray(List<JsonValue> list) { return new JsonValue(Kind.ARRAY, null, list, null, 0, false); }
    static JsonValue ofString(String s) { return new JsonValue(Kind.STRING, null, null, s, 0, false); }
    static JsonValue ofNumber(double n) { return new JsonValue(Kind.NUMBER, null, null, null, n, false); }
    static JsonValue ofBoolean(boolean b) { return new JsonValue(Kind.BOOLEAN, null, null, null, 0, b); }
    static JsonValue ofNull() { return new JsonValue(Kind.NULL, null, null, null, 0, false); }

    public static JsonValue newObject() { return ofObject(new LinkedHashMap<>()); }
    public static JsonValue newArray() { return ofArray(new ArrayList<>()); }

    public Kind kind() { return kind; }
    public boolean isObject() { return kind == Kind.OBJECT; }
    public boolean isArray() { return kind == Kind.ARRAY; }
    public boolean isNull() { return kind == Kind.NULL; }

    public JsonValue get(String key) {
        if (!isObject()) throw new IllegalStateException("Not an object");
        return objectValue.get(key);
    }

    public void put(String key, JsonValue value) {
        if (!isObject()) throw new IllegalStateException("Not an object");
        objectValue.put(key, value);
    }

    public List<JsonValue> asArray() {
        if (!isArray()) throw new IllegalStateException("Not an array");
        return arrayValue;
    }

    public String asString() {
        if (kind != Kind.STRING) throw new IllegalStateException("Not a string");
        return stringValue;
    }

    public double asNumber() {
        if (kind != Kind.NUMBER) throw new IllegalStateException("Not a number");
        return numberValue;
    }

    public int asInt() { return (int) asNumber(); }

    public boolean asBoolean() {
        if (kind != Kind.BOOLEAN) throw new IllegalStateException("Not a boolean");
        return booleanValue;
    }

    /** Serializes this value back to compact JSON text. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        write(sb);
        return sb.toString();
    }

    private void write(StringBuilder sb) {
        switch (kind) {
            case OBJECT -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<String, JsonValue> e : objectValue.entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    writeEscapedString(sb, e.getKey());
                    sb.append(':');
                    e.getValue().write(sb);
                }
                sb.append('}');
            }
            case ARRAY -> {
                sb.append('[');
                for (int i = 0; i < arrayValue.size(); i++) {
                    if (i > 0) sb.append(',');
                    arrayValue.get(i).write(sb);
                }
                sb.append(']');
            }
            case STRING -> writeEscapedString(sb, stringValue);
            case NUMBER -> {
                if (numberValue == Math.floor(numberValue) && !Double.isInfinite(numberValue)) {
                    sb.append((long) numberValue);
                } else {
                    sb.append(numberValue);
                }
            }
            case BOOLEAN -> sb.append(booleanValue);
            case NULL -> sb.append("null");
        }
    }

    private static void writeEscapedString(StringBuilder sb, String s) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }
}