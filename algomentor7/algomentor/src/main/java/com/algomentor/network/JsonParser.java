package com.algomentor.network;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A small hand-written recursive-descent JSON parser. No Gson/Jackson/org.json
 * dependency is used anywhere in this project - this class walks the raw
 * character stream itself and builds a {@link JsonValue} tree, which is
 * exactly what the assignment brief asks to be demonstrated ("show how JSON
 * data is parsed").
 *
 * Supports: objects, arrays, strings (with standard escapes), numbers
 * (including negative/decimal/exponent), true/false/null. Not meant to be a
 * fully spec-compliant parser for arbitrary input - it is deliberately kept
 * readable over exhaustive, since the point is to demonstrate the parsing
 * technique, not to replace a production JSON library.
 */
public class JsonParser {
    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static JsonValue parse(String json) {
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        return value;
    }

    private JsonValue parseValue() {
        skipWhitespace();
        char c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> JsonValue.ofString(parseString());
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private JsonValue parseObject() {
        expect('{');
        Map<String, JsonValue> map = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') { pos++; return JsonValue.ofObject(map); }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            JsonValue value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; break; }
            throw error("Expected ',' or '}' in object");
        }
        return JsonValue.ofObject(map);
    }

    private JsonValue parseArray() {
        expect('[');
        List<JsonValue> list = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') { pos++; return JsonValue.ofArray(list); }

        while (true) {
            JsonValue value = parseValue();
            list.add(value);
            skipWhitespace();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; break; }
            throw error("Expected ',' or ']' in array");
        }
        return JsonValue.ofArray(list);
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = src.charAt(pos++);
            if (c == '"') break;
            if (c == '\\') {
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw error("Unknown escape \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            pos++;
            if (src.charAt(pos) == '+' || src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String numStr = src.substring(start, pos);
        if (numStr.isEmpty() || numStr.equals("-")) throw error("Invalid number");
        return JsonValue.ofNumber(Double.parseDouble(numStr));
    }

    private JsonValue parseBoolean() {
        if (src.startsWith("true", pos)) { pos += 4; return JsonValue.ofBoolean(true); }
        if (src.startsWith("false", pos)) { pos += 5; return JsonValue.ofBoolean(false); }
        throw error("Invalid literal");
    }

    private JsonValue parseNull() {
        if (src.startsWith("null", pos)) { pos += 4; return JsonValue.ofNull(); }
        throw error("Invalid literal");
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw error("Unexpected end of input");
        return src.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) throw error("Expected '" + c + "'");
        pos++;
    }

    private RuntimeException error(String msg) {
        return new IllegalArgumentException("JSON parse error at " + pos + ": " + msg);
    }
}