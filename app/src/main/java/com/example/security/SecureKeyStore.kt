package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor de almacenamiento seguro para claves de API y secretos de VIERNES.
 *
 * Utiliza Android KeyStore respaldado por hardware (ARM TrustZone / TEE en el Snapdragon 685)
 * con cifrado simétrico AES-256 en modo GCM (Galois/Counter Mode) sin relleno.
 *
 * Los datos cifrados (IV + Texto Cifrado) se almacenan en SharedPreferences privadas
 * excluidas de copias de seguridad de Google Cloud y transferencias entre dispositivos.
 *
 * CRÍTICO: Ninguna función expone ni registra el valor de la clave en Logcat.
 */
class SecureKeyStore(private val context: Context) {

    companion object {
        private const val TAG = "SecureKeyStore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "viernes_secure_vault"
        private const val KEY_ALIAS = "viernes_master_aes_key"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SEPARATOR = "]"

        const val DEFAULT_ALIAS = "ai_api_key"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val isAndroidKeyStoreAvailable by lazy {
        try {
            java.security.Security.getProvider(ANDROID_KEYSTORE) != null
        } catch (_: Exception) {
            false
        }
    }

    private val keyStore: KeyStore? by lazy {
        if (isAndroidKeyStoreAvailable) {
            try {
                KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val ks = keyStore
        if (ks != null) {
            if (!ks.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(parameterSpec)
                return keyGenerator.generateKey()
            }
            val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        } else {
            // Entorno de pruebas JVM sin daemon nativo de AndroidKeyStore
            val rawKey = prefs.getString("__jvm_vault_master_key__", null)
            if (rawKey != null) {
                val keyBytes = Base64.decode(rawKey, Base64.NO_WRAP)
                return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            }
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val newKey = keyGenerator.generateKey()
            val encoded = Base64.encodeToString(newKey.encoded, Base64.NO_WRAP)
            prefs.edit().putString("__jvm_vault_master_key__", encoded).commit()
            return newKey
        }
    }

    /**
     * Guarda una clave secreta cifrada con AES-GCM.
     * @param key Valor secreto a cifrar y guardar.
     * @param alias Identificador de la clave (por defecto 'ai_api_key').
     * @return true si se guardó con éxito, false en caso de fallo.
     */
    fun save(key: String, alias: String = DEFAULT_ALIAS): Boolean {
        if (key.isBlank()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(key.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val payload = "$ivBase64$IV_SEPARATOR$cipherBase64"

            prefs.edit().putString(alias, payload).commit()
            Log.d(TAG, "Clave para alias '$alias' cifrada y persistida exitosamente.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error seguro al cifrar clave para alias '$alias'.")
            false
        }
    }

    /**
     * Lee y descifra la clave secreta guardada.
     * @param alias Identificador de la clave.
     * @return El valor descifrado, o null si no existe o hubo un error.
     */
    fun read(alias: String = DEFAULT_ALIAS): String? {
        val payload = prefs.getString(alias, null) ?: return null
        return try {
            val parts = payload.split(IV_SEPARATOR)
            if (parts.size != 2) {
                Log.w(TAG, "Payload malformado para alias '$alias'.")
                return null
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error seguro al descifrar clave para alias '$alias'.")
            null
        }
    }

    /**
     * Elimina la clave cifrada del almacén.
     * @param alias Identificador de la clave.
     * @return true si se eliminó con éxito.
     */
    fun delete(alias: String = DEFAULT_ALIAS): Boolean {
        return try {
            prefs.edit().remove(alias).commit()
            Log.d(TAG, "Clave eliminada para alias '$alias'.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error seguro al eliminar clave para alias '$alias'.")
            false
        }
    }

    /**
     * Comprueba si existe una clave guardada para el alias dado.
     * @param alias Identificador de la clave.
     */
    fun hasKey(alias: String = DEFAULT_ALIAS): Boolean {
        return prefs.contains(alias)
    }
}
