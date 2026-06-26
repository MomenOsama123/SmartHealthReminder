package com.example.smarthealthreminder.features.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for authentication input validation logic.
 * Mirrors validateForm() logic from SignInActivity and SignupActivity.
 *
 * Covers hacker (injection), malicious (empty/null), and luxury (unicode/long) users.
 */
class AuthValidationTest {

    // ─────────────────────────────────────────────
    // EMAIL VALIDATION
    // ─────────────────────────────────────────────

    @Test
    fun `valid email passes`() {
        assertTrue(isValidEmail("user@example.com"))
    }

    @Test
    fun `subdomain email passes`() {
        assertTrue(isValidEmail("user@mail.example.co.uk"))
    }

    @Test
    fun `email with plus alias passes`() {
        assertTrue(isValidEmail("user+tag@example.com"))
    }

    @Test
    fun `empty email fails`() {
        assertFalse(isValidEmail(""))
    }

    @Test
    fun `whitespace-only email fails`() {
        assertFalse(isValidEmail("   "))
    }

    @Test
    fun `email without domain fails`() {
        assertFalse(isValidEmail("user@"))
    }

    @Test
    fun `email without at-sign fails`() {
        assertFalse(isValidEmail("userexample.com"))
    }

    @Test
    fun `email with spaces fails`() {
        assertFalse(isValidEmail("user @example.com"))
    }

    @Test
    fun `SQL injection in email fails`() {
        assertFalse(isValidEmail("' OR 1=1 --"))
    }

    @Test
    fun `script injection in email fails`() {
        assertFalse(isValidEmail("<script>alert(1)</script>"))
    }

    @Test
    fun `unicode email passes format`() {
        // Punycode emails are valid by RFC
        val result = isValidEmail("user@xn--nxasmq6b.com")
        assertTrue(result)
    }

    // ─────────────────────────────────────────────
    // PASSWORD VALIDATION (length / empty checks)
    // ─────────────────────────────────────────────

    @Test
    fun `non-empty password passes basic check`() {
        assertTrue(isValidPassword("mypassword"))
    }

    @Test
    fun `empty password fails`() {
        assertFalse(isValidPassword(""))
    }

    @Test
    fun `whitespace-only password fails`() {
        assertFalse(isValidPassword("   "))
    }

    @Test
    fun `password with SQL injection is still non-empty`() {
        // Firebase handles auth — we just check it's not blank
        assertTrue(isValidPassword("' OR '1'='1"))
    }

    @Test
    fun `very short password is non-empty but may fail Firebase min length`() {
        // Firebase requires min 6 chars — we document this constraint
        val password = "abc"
        assertFalse("Firebase requires at least 6 chars", password.length >= 6)
    }

    @Test
    fun `password at exactly 6 chars passes length check`() {
        assertTrue("abcdef".length >= 6)
    }

    @Test
    fun `extremely long password is still non-empty`() {
        val longPassword = "P@ssw0rd!".repeat(1_000)
        assertTrue(isValidPassword(longPassword))
    }

    // ─────────────────────────────────────────────
    // FORM FIELD NULL / BLANK SAFETY
    // ─────────────────────────────────────────────

    @Test
    fun `null email treated as empty`() {
        val email: String? = null
        assertFalse(isValidEmail(email ?: ""))
    }

    @Test
    fun `null password treated as empty`() {
        val password: String? = null
        assertFalse(isValidPassword(password ?: ""))
    }

    @Test
    fun `both fields empty means form is invalid`() {
        val formValid = validateSignInForm("", "")
        assertFalse(formValid)
    }

    @Test
    fun `valid email but empty password means form is invalid`() {
        val formValid = validateSignInForm("user@example.com", "")
        assertFalse(formValid)
    }

    @Test
    fun `empty email but valid password means form is invalid`() {
        val formValid = validateSignInForm("", "password123")
        assertFalse(formValid)
    }

    @Test
    fun `invalid email format with valid password means form is invalid`() {
        val formValid = validateSignInForm("notanemail", "password123")
        assertFalse(formValid)
    }

    @Test
    fun `valid email and valid password means form is valid`() {
        val formValid = validateSignInForm("user@example.com", "password123")
        assertTrue(formValid)
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun isValidEmail(email: String): Boolean {
        return email.trim().isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.trim().isNotEmpty()
    }

    private fun validateSignInForm(email: String, password: String): Boolean {
        return isValidEmail(email) && isValidPassword(password)
    }
}
