package org.ar.pay.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;

import javax.crypto.spec.SecretKeySpec;



public class AESUtil {

//    private static final String ALGORITHM = "AES";
//    private static final String CHARSET = "UTF-8";
//
//
//
//    /**
//     * 实现数据的AES加密
//     *
//     * @param content
//     * @param key
//     * @return
//     * @throws Exception
//     */
//    public static String encrypt(String content,String key) throws Exception {
//
//        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
//
//        Cipher cipher = Cipher.getInstance(ALGORITHM);
//
//        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
//
//        byte[] encryptedBytes = cipher.doFinal(content.getBytes(CHARSET));
//
//
//        return Base64.getEncoder().encodeToString(encryptedBytes);
//    }
//
//
//    /**
//     * 实现AES解密
//     *
//     * @param content
//     * @param key
//     * @return
//     * @throws Exception
//     */
//    public static String decrypt(String content,String key) throws Exception {
//
//        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
//
//        Cipher cipher = Cipher.getInstance(ALGORITHM);
//
//        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
//
//        byte[] encryptedBytes = Base64.getDecoder().decode(content);
//
//        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
//
//        return new String(decryptedBytes, CHARSET);
//    }



    // 加密
    public static String Encrypt128(String sSrc, String sKey) throws Exception {
        if (sKey == null) {
            System.out.print("Key为空null");
            return null;
        }
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            System.out.print("Key长度不是16位");
            return null;
        }
        byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));

        return new Base64().encodeToString(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
    }

    // 解密
    public static String Decrypt128(String sSrc, String sKey) throws Exception {
        try {
            // 判断Key是否正确
            if (sKey == null) {
                System.out.print("Key为空null");
                return null;
            }
            // 判断Key是否为16位
            if (sKey.length() != 16) {
                System.out.print("Key长度不是16位");
                return null;
            }
            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = new Base64().decode(sSrc);//先用base64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original,"utf-8");
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }
}
