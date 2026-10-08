package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureKeyStoreTest {

    private lateinit var context: Context
    private lateinit var secureKeyStore: SecureKeyStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureKeyStore = SecureKeyStore(context)
        // Limpiar cualquier estado previo
        secureKeyStore.delete()
    }

    @Test
    fun testSaveAndReadKey() {
        val testKey = "AIzaSy_fake_test_key_1234567890"

        assertFalse(secureKeyStore.hasKey())
        val saved = secureKeyStore.save(testKey)
        assertTrue("La clave debe guardarse con éxito", saved)
        assertTrue(secureKeyStore.hasKey())

        val retrieved = secureKeyStore.read()
        assertNotNull(retrieved)
        assertEquals(testKey, retrieved)
    }

    @Test
    fun testKeyIsNotStoredInPlainTextInSharedPreferences() {
        val testSecret = "super_secret_api_token_xyz"
        secureKeyStore.save(testSecret)

        val rawPrefs = context.getSharedPreferences("viernes_secure_vault", Context.MODE_PRIVATE)
        val storedPayload = rawPrefs.getString(SecureKeyStore.DEFAULT_ALIAS, null)

        assertNotNull(storedPayload)
        // La clave en claro NUNCA debe estar en el XML de SharedPreferences
        assertFalse(
            "El archivo no debe contener la clave en texto claro",
            storedPayload!!.contains(testSecret)
        )
        // El payload debe contener el separador IV ] Ciphertext
        assertTrue("El payload debe estar cifrado en Base64 con IV", storedPayload.contains("]"))
    }

    @Test
    fun testDeleteKey() {
        val testKey = "token_to_be_deleted"
        secureKeyStore.save(testKey)
        assertTrue(secureKeyStore.hasKey())

        val deleted = secureKeyStore.delete()
        assertTrue(deleted)
        assertFalse(secureKeyStore.hasKey())
        assertNull(secureKeyStore.read())
    }

    @Test
    fun testBlankKeyRejected() {
        val blankKey = "   "
        val saved = secureKeyStore.save(blankKey)
        assertFalse("Una clave en blanco no debe guardarse", saved)
        assertFalse(secureKeyStore.hasKey())
    }

    @Test
    fun testCustomAlias() {
        val customAlias = "custom_openai_key"
        val secret = "sk-custom-12345"

        secureKeyStore.save(secret, alias = customAlias)
        assertTrue(secureKeyStore.hasKey(customAlias))
        assertEquals(secret, secureKeyStore.read(customAlias))

        // La clave por defecto sigue vacía
        assertFalse(secureKeyStore.hasKey(SecureKeyStore.DEFAULT_ALIAS))

        secureKeyStore.delete(customAlias)
        assertFalse(secureKeyStore.hasKey(customAlias))
    }
}
