package dev.clawdboard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteTest {
    private val blob = "SmzbXpoxEQDA8Y/sL/Z/lup8gVAawDA4tk7Tc+8sirzmBJ40sTfI/7+/g/V1/HksIMqjNdl9hYTasoy5kIusohqBnL/TWqefcWrBxF0CvMNdBDOjRXGGi4dsvptZL++QacK9lr/Dmlah+Qh5xaBToVMiL4ALmPxVp0vPcGPBjjONI++CHpQEHmZYX3xeS5Jk"
    private val master = "55a4c64dda9420e81019d1207d475c2b753ed3cb5d04b1373658033e234f389c"

    @Test
    fun readsTheBoxLinkFromTheConnectPage() {
        val s = Remote.parse("banditboard://box?w=https%3A%2F%2Fbanditboard-api.example.dev&b=0123456789abcdef0123456789abcdef&r=abcdef&e=$master")!!
        assertEquals(listOf("https://banditboard-api.example.dev/v1/box/0123456789abcdef0123456789abcdef"), s.urls)
        assertEquals("abcdef", s.key)
        assertEquals(master, s.seal)
    }

    @Test
    fun readsTheLanLinkFromTheDesktopDashboard() {
        val s = Remote.parse("banditboard://pair?k=chave&u=http%3A%2F%2FDES18.local%3A47830%2Chttp%3A%2F%2F10.0.0.141%3A47830")!!
        assertEquals(listOf("http://DES18.local:47830", "http://10.0.0.141:47830"), s.urls)
        assertEquals("chave", s.key)
        assertNull(s.seal)
    }

    @Test
    fun refusesLinksThatAreNotFromBanditboard() {
        assertNull(Remote.parse("https://banditboard.pages.dev/conectar"))
        assertNull(Remote.parse("banditboard://box?w=http%3A%2F%2Finseguro&b=1&r=2&e=$master"))
        assertNull(Remote.parse("banditboard://box?w=https%3A%2F%2Fa.dev&b=1&r=2&e=curta"))
        assertNull(Remote.parse("banditboard://pair?k=chave"))
    }

    @Test
    fun opensWhatThePowerShellHookSealed() {
        val usage = JSONObject(Remote.open(blob, master)!!)
        assertEquals(42.0, usage.getJSONObject("five_hour").getDouble("used_percentage"), 0.0)
    }

    @Test
    fun refusesAWrongKeyOrATamperedBlob() {
        assertNull(Remote.open(blob, "ab".repeat(32)))
        val bytes = java.util.Base64.getDecoder().decode(blob)
        bytes[20] = (bytes[20].toInt() xor 1).toByte()
        assertNull(Remote.open(java.util.Base64.getEncoder().encodeToString(bytes), master))
    }
}
