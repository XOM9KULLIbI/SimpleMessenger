package com.example.simplemessenger

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import android.util.Base64

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val SECRET_KEY = "12345678901234567890123456789012" // 32 символа = AES-256

    private val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray())
        val ivAndEncrypted = iv + encrypted
        return Base64.encodeToString(ivAndEncrypted, Base64.NO_WRAP)
    }

    fun decrypt(base64Input: String): String {
        val ivAndEncrypted = Base64.decode(base64Input, Base64.NO_WRAP)
        val iv = ivAndEncrypted.sliceArray(0..15)
        val encrypted = ivAndEncrypted.sliceArray(16 until ivAndEncrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted)
    }
}
