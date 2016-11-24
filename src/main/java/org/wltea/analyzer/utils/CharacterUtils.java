package org.wltea.analyzer.utils;

/**
 * Created by Lanxiaowei
 * Craated on 2016/11/24 19:07
 * 字符操作工具类
 */
public class CharacterUtils {
    private CharacterUtils() {
    }

    public static int codePointCount(CharSequence seq) {
        return Character.codePointCount(seq, 0, seq.length());
    }

    public static int offsetByCodePoints(char[] buf, int start, int count, int index, int offset) {
        return Character.offsetByCodePoints(buf, start, count, index, offset);
    }

    public static int codePointAt(final CharSequence seq, final int offset) {
        return Character.codePointAt(seq, offset);
    }

    public static int codePointAt(final char[] chars, final int offset, final int limit) {
        return Character.codePointAt(chars, offset, limit);
    }

    public final static int toCodePoints(char[] src, int srcOff, int srcLen, int[] dest, int destOff) {
        if (srcLen < 0) {
            throw new IllegalArgumentException("srcLen must be >= 0");
        }
        int codePointCount = 0;
        for (int i = 0; i < srcLen; ) {
            final int cp = codePointAt(src, srcOff + i, srcOff + srcLen);
            final int charCount = Character.charCount(cp);
            dest[destOff + codePointCount++] = cp;
            i += charCount;
        }
        return codePointCount;
    }

    public final static int toChars(int[] src, int srcOff, int srcLen, char[] dest, int destOff) {
        if (srcLen < 0) {
            throw new IllegalArgumentException("srcLen must be >= 0");
        }
        int written = 0;
        for (int i = 0; i < srcLen; ++i) {
            written += Character.toChars(src[srcOff + i], dest, destOff + written);
        }
        return written;
    }
}
