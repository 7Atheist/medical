package com.geriatric.assessment.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具。
 *
 * <p>用于对身份证号、健康答案等敏感字段做加密存储（方案"安全与存储模块"要求），
 * 密钥由配置注入并支持环境变量覆盖。采用 AES-128/GCM（随机 12 字节 IV），
 * 每次加密生成独立 IV 并拼接到密文头部，兼顾安全性与可解密性。</p>
 */
public final class AesUtil {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private AesUtil() {
    }

    /** 由配置字符串派生 AES 密钥（取前16字节作为128位密钥） */
    private static SecretKeySpec buildKey(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 16));
        return new SecretKeySpec(key, "AES");
    }

    /**
     * AES-GCM 加密
     *
     * @param plainText 明文
     * @param secret    密钥
     * @return Base64(IV + 密文 + tag)
     */
    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(secret), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, result, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, result, IV_BYTES, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("AES加密失败", e);
        }
    }

    /**
     * AES-GCM 解密（兼容传入 null，返回 null）
     */
    public static String decrypt(String cipherTextB64, String secret) {
        if (cipherTextB64 == null || cipherTextB64.isBlank()) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherTextB64);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            byte[] body = new byte[all.length - IV_BYTES];
            System.arraycopy(all, IV_BYTES, body, 0, body.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(secret), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败不抛致命异常，返回占位，避免历史脏数据阻断系统
            return "[解密失败]";
        }
    }
}
