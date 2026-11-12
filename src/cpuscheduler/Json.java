package cpuscheduler;

import java.util.Map;

/** Minimal JSON writer (no external libraries needed). */
public final class Json {

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {

        if (v == null) {
            sb.append("null");

        } else if (v instanceof String s) {
            string(sb, s);

        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            sb.append(v);

        } else if (v instanceof Number n) {
            double d = n.doubleValue();
            // JSON has no NaN / Infinity
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : n.toString());

        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                string(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');

        } else if (v instanceof Iterable<?> items) {
            sb.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item);
            }
            sb.append(']');

        } else {
            string(sb, v.toString());
        }
    }

    private static void string(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
