package net.carlh.toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Util {
    static public byte[] getData(Socket socket, int length) throws IOException {
        byte[] d = new byte[length];
        int offset = 0;
        while (offset < length) {
            int t = socket.getInputStream().read(d, offset, length - offset);
            if (t == -1) {
                break;
            }
            offset += t;
        }

        if (offset != length) {
            throw new IOException("short read");
        }

        return java.util.Arrays.copyOf(d, offset);
    }

    public static int getInt16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset+1] & 0xff) << 8);
    }

    public static long getInt64(byte[] data, int offset) {
        return (data[offset] & 0xff) |
              ((data[offset+1] & 0xff) << 8) |
              ((data[offset+2] & 0xff) << 16) |
              ((data[offset+3] & 0xff) << 24) |
              ((data[offset+4] & 0xff) << 32) |
              ((data[offset+5] & 0xff) << 40) |
              ((data[offset+6] & 0xff) << 48) |
              ((data[offset+7] & 0xff) << 56);
    }

    public static String getString(byte[] data, int offset) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < data[offset]; ++i) {
            b.append((char) data[offset+i+1]);
        }
        return b.toString();
    }

    public static double getFloat(byte[] data, int offset) {
        return getInt16(data, offset) / 16.0;
    }

    public static int putInt16(byte[] data, int offset, int value) {
        assert(value < 65536);
        data[offset] = (byte) (value & 0xff);
        data[offset+1] = (byte) ((value & 0xff00) >> 8);
        return offset + 2;
    }

    public static void putInt16(ByteArrayOutputStream s, int value) {
        assert(value < 65536);
        s.write(value & 0xff);
        s.write((value & 0xff00) >> 8);
    }

    public static int putInt64(byte[] data, int offset, long value) {
        data[offset] = (byte) (value & 0xff);
        data[offset+1] = (byte) ((value & 0xff00) >> 8);
        data[offset+2] = (byte) ((value & 0xff0000) >> 16);
        data[offset+3] = (byte) ((value & 0xff000000) >> 24);
        data[offset+4] = (byte) ((value & 0xff00000000L) >> 32);
        data[offset+5] = (byte) ((value & 0xff0000000000L) >> 40);
        data[offset+6] = (byte) ((value & 0xff000000000000L) >> 48);
        data[offset+7] = (byte) ((value & 0xff00000000000000L) >> 56);
        return offset + 8;
    }

    public static int putFloat(byte[] data, int offset, double value) {
        return putInt16(data, offset,(int) (value * 16));
    }

    public static int putString(byte[] data, int off, String s) {
        data[off++] = (byte) s.length();
        for (int i = 0; i < s.length(); ++i) {
            data[off++] = (byte) s.charAt(i);
        }
        return off;
    }

    public static void putFloat(ByteArrayOutputStream data, double value) {
        putInt16(data, (int) (value * 16));
    }
}
