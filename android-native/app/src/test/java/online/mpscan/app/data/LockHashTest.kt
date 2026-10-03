package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
class LockHashTest {
 @Test fun storedPasswordsRemainCompatible(){assertEquals("ead21ce9ef6b221492b5d1caf0c5e225fc1849d7c8906f4d0804ced133c2459a",LockHash.encode("abc123","test-salt"))}
 @Test fun incorrectPasswordAndDifferentSaltAreRejected(){val expected=LockHash.encode("1234","salt-a");assertNotEquals(expected,LockHash.encode("1235","salt-a"));assertNotEquals(expected,LockHash.encode("1234","salt-b"))}
}
