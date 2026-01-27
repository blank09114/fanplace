package kr.co.fanplace.setting.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class TokenUtil
{
    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private TokenUtil() {}

    public static String generateToken(int length)
    {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) out[i] = ALPHABET[RND.nextInt(ALPHABET.length)];
        return new String(out);
    }

    public static String sha256Hex(String raw)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException("sha256 failed", e); }
    }
}