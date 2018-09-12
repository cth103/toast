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

    public static void putInt16(byte[] data, int offset, int value) {
        assert(value < 65536);
        data[offset] = (byte) (value & 0xff);
        data[offset+1] = (byte) ((value & 0xff00) >> 8);
    }

    public static void putInt16(ByteArrayOutputStream s, int value) {
        assert(value < 65536);
        s.write(value & 0xff);
        s.write((value & 0xff00) >> 8);
    }

    public static void putFloat(byte[] data, int offset, double value) {
        putInt16(data, offset,(int) (value * 16));
    }

    public static void putString(ByteArrayOutputStream s, String t) {
        s.write(t.length());
        try {
            s.write(t.getBytes());
        } catch (IOException e) {

        }
    }

    public static void putFloat(ByteArrayOutputStream data, double value) {
        putInt16(data, (int) (value * 16));
    }
}
