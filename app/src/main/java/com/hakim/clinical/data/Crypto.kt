package com.hakim.clinical.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * تشفير بيانات المرضى.
 * - على الجهاز: مفتاح AES محفوظ بـ Android Keystore (ما بيطلع من الجهاز).
 * - النسخ الاحتياطية: مفتاح مشتق من كلمة سر بيختارها الطبيب.
 */
object Crypto {

    private const val ALIAS = "drali_data_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private val BACKUP_MAGIC = "DRALI1".toByteArray(Charsets.US_ASCII)

    private fun deviceKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, deviceKey())
        val iv = c.iv
        val ct = c.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + ct
    }

    fun decrypt(data: ByteArray): ByteArray {
        val ivLen = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivLen)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, iv))
        return c.doFinal(data, 1 + ivLen, data.size - 1 - ivLen)
    }

    private fun passwordKey(password: CharArray, salt: ByteArray): SecretKey {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, 120_000, 256)
        return SecretKeySpec(f.generateSecret(spec).encoded, "AES")
    }

    fun encryptWithPassword(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, passwordKey(password, salt), GCMParameterSpec(128, iv))
        return BACKUP_MAGIC + salt + iv + c.doFinal(plain)
    }

    /** يرمي استثناء إذا كلمة السر غلط أو الملف مو نسخة DrAli. */
    fun decryptWithPassword(data: ByteArray, password: CharArray): ByteArray {
        val m = BACKUP_MAGIC.size
        require(data.size > m + 28 && data.copyOfRange(0, m).contentEquals(BACKUP_MAGIC)) { "not a DrAli backup" }
        val salt = data.copyOfRange(m, m + 16)
        val iv = data.copyOfRange(m + 16, m + 28)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, passwordKey(password, salt), GCMParameterSpec(128, iv))
        return c.doFinal(data, m + 28, data.size - m - 28)
    }
}
