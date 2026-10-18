package com.intuthuko.smartreminder.data
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
object Passwords {
    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val result = derive(password, salt)
        return "pbkdf2:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(result)
    }
    private fun derive(password: String, salt: ByteArray) = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 120000, 256)).encoded
    fun verify(password: String, stored: String): Boolean = try {
        val parts = stored.split(":")
        parts.size == 3 && parts[0] == "pbkdf2" && MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[1])), Base64.getDecoder().decode(parts[2]))
    } catch (_: Exception) { false }
}
