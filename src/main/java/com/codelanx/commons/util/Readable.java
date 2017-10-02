package com.codelanx.commons.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for string inputs and outputs, as well as number formatting / conversion
 *
 * @since 0.3.3
 * @author 1Rogue
 * @version 0.3.3
 */
public final class Readable {

    private Readable() {
    }

    public static UUID parseUUID(String uuid) {
        Validate.isTrue(uuid.length() == 32 || uuid.length() == 36, "Invalid UUID format supplied");
        if (uuid.length() == 36) {
            return UUID.fromString(uuid);
        } else {
            return UUID.fromString(uuid.substring(0, 8)
                    + "-" + uuid.substring(8, 12)
                    + "-" + uuid.substring(12, 16)
                    + "-" + uuid.substring(16, 20)
                    + "-" + uuid.substring(20, 32));
        }
    }

    public static String objectString(Object in) {
        if (in == null) {
            return "null";
        }
        return in.getClass().getName() + "@" + Integer.toHexString(in.hashCode());
    }

    //guaranteed safety, so no class cast if wrong (just converts)
    //CCE occurs for bad Class<T> parameter
    public static <T extends Number> T convertNumber(Number in, Class<T> out) {
        if (Primitives.isWrapperType(out)) {
            out = Primitives.unwrap(out);
        }
        if (out == int.class) {
            return (T) (Number) in.intValue();
        } else if (out == byte.class) {
            return (T) (Number) in.byteValue();
        } else if (out == short.class) {
            return (T) (Number) in.shortValue();
        } else if (out == long.class) {
            return (T) (Number) in.longValue();
        } else if (out == float.class) {
            return (T) (Number) in.floatValue();
        } else if (out == double.class) {
            return (T) (Number) in.doubleValue();
        } else {
            return (T) in; //CCE
        }
    }

    public static Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDouble(String s) {
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Float> parseFloat(String s) {
        try {
            return Optional.of(Float.parseFloat(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Short> parseShort(String s) {
        try {
            return Optional.of(Short.parseShort(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Byte> parseByte(String s) {
        try {
            return Optional.of(Byte.parseByte(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static String properEnumName(Enum<?> val) {
        String s = val.name().toLowerCase();
        char[] ch = s.toCharArray();
        boolean skip = false;
        for (int i = 0; i < ch.length; i++) {
            if (skip) {
                skip = false;
                continue;
            }
            if (i == 0) {
                ch[i] = Character.toUpperCase(ch[i]);
                continue;
            }
            if (ch[i] == '_') {
                ch[i] = ' ';
                if (i < ch.length - 1) {
                    ch[i + 1] = Character.toUpperCase(ch[i + 1]);
                }
                skip = true;
            }
        }
        return new String(ch).intern();
    }

    /**
     * Converts a stack trace into a String for ease-of-use in network-level debugging
     *
     * @since 0.3.0
     * @version 0.3.0
     *
     * @param t The {@link Throwable}
     * @return A String form of the exception
     */
    public static String stackTraceToString(Throwable t) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t.toString(); //not the best, but better than null
    }
}
