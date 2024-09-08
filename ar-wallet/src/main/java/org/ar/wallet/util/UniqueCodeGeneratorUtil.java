package org.ar.wallet.util;

import java.math.BigInteger;
import java.util.UUID;

/**
 * @author Simon
 * @date 2024/05/06
 */
public class UniqueCodeGeneratorUtil {

    private static final char[] BASE_62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 使用 UUID to base62 生成 随机邀请码
     *
     * @return {@link String}
     */
    public static String generateInvitationCode() {
        UUID uuid = UUID.randomUUID();
        return toBase62(uuid).substring(0, 6);
    }

    private static String toBase62(UUID uuid) {
        StringBuilder builder = new StringBuilder();
        BigInteger bi = new BigInteger(uuid.toString().replace("-", ""), 16);

        BigInteger base62 = BigInteger.valueOf(62);
        while (bi.compareTo(BigInteger.ZERO) > 0) {
            BigInteger mod = bi.mod(base62);
            builder.append(BASE_62_CHARS[mod.intValue()]);
            bi = bi.divide(base62);
        }
        while (builder.length() < 6) {
            builder.append('0');
        }
        return builder.reverse().toString();
    }
}
