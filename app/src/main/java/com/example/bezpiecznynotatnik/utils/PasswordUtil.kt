package com.example.bezpiecznynotatnik.utils

import com.example.bezpiecznynotatnik.R

import android.widget.Toast
import android.content.SharedPreferences
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.ContextCompat.getString

import java.security.MessageDigest
import java.security.SecureRandom

private lateinit var sharedPrefs: SharedPreferences

object SaltUtil {
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16) // 16 bytes = 128-bit salt
        SecureRandom().nextBytes(salt)
        return salt
    }
}
object HashUtil {
    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-512")
        val saltedPassword = salt + password.toByteArray()
        return digest.digest(saltedPassword)
    }
}

fun changePassword(context: Context, newPassword: String) {
    sharedPrefs = context.getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)
    try {
        val salt = SaltUtil.generateSalt()
        val passwordHash = HashUtil.hashPassword(newPassword, salt)

        sharedPrefs.edit()
            .putString("passwordHash", ByteArrayUtil.toBase64(passwordHash))
            .putString("password_salt", ByteArrayUtil.toBase64(salt))
            .apply()

        Toast.makeText(context, getString(context, R.string.password_changed), Toast.LENGTH_SHORT)
            .show()
    } catch (e: Exception) {
        Toast.makeText(context, getString(context, R.string.set_password_failure),
            Toast.LENGTH_SHORT
        ).show()
        e.printStackTrace()
    }
}