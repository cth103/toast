package net.carlh.toast;

public class Binary {

    public static int getInt16(byte[] data, int offset) {
        return data[offset] | data[offset+1] << 8;
    }

    public static String getString(byte[] data, int offset) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < data[offset]; ++i) {
            b.append(data[offset+i+1]);
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

    public static void putFloat(byte[] data, int offset, double value) {
        putInt16(data, offset,(int) (value * 16));
    }
}
