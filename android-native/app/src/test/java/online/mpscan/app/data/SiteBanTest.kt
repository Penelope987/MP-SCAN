package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class SiteBanTest {
 @Test fun permanentBanDoesNotExpire(){assertTrue(SiteBan(true,true,0).blocks(9999))}
 @Test fun temporaryBanEndsAtItsDeadline(){val ban=SiteBan(true,false,100);assertTrue(ban.blocks(99));assertFalse(ban.blocks(100))}
 @Test fun revokedBanDoesNotBlock(){assertFalse(SiteBan(false,true,Long.MAX_VALUE).blocks())}
 @Test fun acceptsWebsiteRecord(){val x=JSONObject("""{"active":true,"permanent":false,"until":200,"reason":"Teste"}""");val ban=SiteBan.parse(x);assertTrue(ban.blocks(100));assertEquals("Teste",ban.reason)}
}
