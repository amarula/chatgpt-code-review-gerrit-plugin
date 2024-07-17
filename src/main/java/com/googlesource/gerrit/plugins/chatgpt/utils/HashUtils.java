package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
public class HashUtils {

    public static String hashData(List<String> dataItems) {
        StringBuilder concatenatedData = new StringBuilder();
        for (String item : dataItems) {
            concatenatedData.append(item);
        }
        log.debug("Concatenated data for hashing: {}", concatenatedData);
        String hash = sha1(concatenatedData.toString());
        log.debug("Computed SHA-1 hash: {}", hash);
        return hash;
    }

    private static String sha1(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String hexResult = bytesToHex(hashBytes);
            log.debug("SHA-1 hash in hex: {}", hexResult);
            return hexResult;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to find SHA-1 hashing algorithm", e);
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
