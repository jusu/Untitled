package com.pinktwins.elephant.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class CryptoUtil {
	final String transform = "AES/CBC/PKCS5Padding";

	public static String encrypt(String password, String input) {
		try {
			// Create encryption key from password + random salt
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[16];
			random.nextBytes(salt);

			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			byte[] key = f.generateSecret(spec).getEncoded();
			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

			// Encrypt
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			byte[] iv = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
			byte[] encrypted = cipher.doFinal(input.getBytes());

			// Encode salt, iv, enc
			String salt64 = Base64.encodeBase64String(salt);
			String iv64 = Base64.encodeBase64String(iv);
			String enc64 = Base64.encodeBase64String(encrypted);

			// Encoded string contains salt, iv + enc data.
			// Salt and IV are randomized for each encryption, and need not be kept secret.

			return "encv0" + ":" + salt64 + ":" + iv64 + ":" + enc64 + "\n";
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public static String decrypt(String password, String encrypted) throws Exception {
		try {
			String[] a = encrypted.split(":");
			if (a.length == 4) {
				String version = a[0];
				String salt = a[1];
				String iv = a[2];
				String enc = a[3];

				if (version.equals("encv0")) {
					byte[] bSalt = Base64.decodeBase64(salt);

					byte[] ivBytes = Base64.decodeBase64(iv);
					IvParameterSpec ivParam = new IvParameterSpec(ivBytes);

					KeySpec spec = new PBEKeySpec(password.toCharArray(), bSalt, 65536, 128);
					SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
					byte[] key = f.generateSecret(spec).getEncoded();

					SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

					Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
					cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParam);

					byte[] original = cipher.doFinal(Base64.decodeBase64(enc));
					return new String(original);
				}
			}
		} catch (Exception e) {
			throw (e);
		}
		return null;
	}

	public String encryptToBase64(String password, String input) throws IOException {
		return encrypt(password, input);
	}

	public String decryptBase64(String password, String input) throws Exception {
		return decrypt(password, input);
	}
}
