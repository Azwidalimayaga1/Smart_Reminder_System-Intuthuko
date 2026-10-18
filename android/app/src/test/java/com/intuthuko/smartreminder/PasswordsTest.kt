package com.intuthuko.smartreminder
import com.intuthuko.smartreminder.data.Passwords
import org.junit.Assert.*
import org.junit.Test
class PasswordsTest {
    @Test fun correctPasswordVerifiesAndWrongPasswordFails() {
        val stored = Passwords.hash("Password123!")
        assertTrue(Passwords.verify("Password123!", stored))
        assertFalse(Passwords.verify("incorrect", stored))
        assertFalse(stored.contains("Password123!"))
    }
    @Test fun saltsAreUnique() {
        val a = Passwords.hash("same-password")
        val b = Passwords.hash("same-password")
        assertNotEquals(a, b)
        assertTrue(Passwords.verify("same-password", a))
        assertTrue(Passwords.verify("same-password", b))
    }
    @Test fun corruptAndPlaintextValuesAreRejected() {
        assertFalse(Passwords.verify("pass123", "pass123"))
        assertFalse(Passwords.verify("pass123", "pbkdf2:broken:broken"))
        assertFalse(Passwords.verify("pass123", ""))
    }
}
