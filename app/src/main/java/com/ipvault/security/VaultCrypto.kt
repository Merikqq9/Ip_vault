package com.ipvault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local AES-256-GCM file encryption backed by a non-exportable Android Keystore key.
 * Encrypted files contain a 12-byte IV followed by the authenticated ciphertext.
 */
class VaultCrypto(private val context: Context) {
    private val ioBufferSize = 262144
    private val alias = "ip_vault_file_key_v1"
    private val vaultDir = File(context.filesDir, "vault_encrypted").apply { mkdirs() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypts a content stream into app-private storage. Returns the encrypted file. */
    fun encrypt(input: java.io.InputStream, safeName: String): File {
        val target = File(vaultDir, safeName)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        BufferedOutputStream(FileOutputStream(target), ioBufferSize).use { fileOut ->
            fileOut.write(cipher.iv)
            CipherOutputStream(fileOut, cipher).use { encryptedOut ->
                BufferedInputStream(input, ioBufferSize).use { source -> source.copyTo(encryptedOut, ioBufferSize) }
            }
        }
        return target
    }

    /** Decrypts an encrypted vault file into the caller-provided output stream. */
    fun decrypt(encryptedFile: File, output: java.io.OutputStream) {
        BufferedInputStream(FileInputStream(encryptedFile), ioBufferSize).use { fileIn ->
            val iv = ByteArray(12)
            var offset = 0
            while (offset < iv.size) {
                val count = fileIn.read(iv, offset, iv.size - offset)
                require(count > 0) { "Invalid encrypted vault file" }
                offset += count
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            CipherInputStream(fileIn, cipher).use { decryptedIn ->
                BufferedOutputStream(output, ioBufferSize).use { bufferedOut -> decryptedIn.copyTo(bufferedOut, ioBufferSize); bufferedOut.flush() }
            }
        }
    }

    fun listEncryptedFiles(): List<File> = vaultDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    fun deleteEncryptedFile(fileName: String): Boolean {
        val candidate = File(vaultDir, fileName).canonicalFile
        require(candidate.parentFile == vaultDir.canonicalFile) { "Invalid vault filename" }
        return candidate.delete()
    }
}
