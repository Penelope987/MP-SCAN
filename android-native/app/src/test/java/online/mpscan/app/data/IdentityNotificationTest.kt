package online.mpscan.app.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class IdentityNotificationTest {
 @Test fun identityUsesActualNameAndPhotoAcrossPublicAndCommentRecords(){val p=ProfileIdentity.person("u",JSONObject().put("nome",""),JSONObject().put("nome","Pam").put("foto","avatar.jpg").put("nomeUsuario","mpscan"));assertEquals("Pam",p.name);assertEquals("avatar.jpg",p.photo);assertEquals("mpscan",p.username)}
 @Test fun missingIdentityDoesNotInventReaderName(){assertEquals("Perfil indisponível",ProfileIdentity.person("u",JSONObject()).name)}
 @Test fun falseSubscriptionIsExcluded(){assertEquals(setOf("b"),NotificationRules.subscriptions(JSONObject("""{"a":false,"b":{"data":1},"c":{"enabled":false}}""")))}
 @Test fun scheduledLockedChaptersAndAlreadyKnownChaptersDoNotNotify(){val now=System.currentTimeMillis();val known=Chapter("a",1.0,"",true,now-100);val fresh=Chapter("b",2.0,"",true,now-50);val future=Chapter("c",3.0,"",true,0,0,"scheduled",now+60000);assertEquals(listOf("b"),NotificationRules.newChapters(listOf(known,fresh,future),setOf("a"),now-1000,now).map{it.id})}
 @Test fun alreadyReadOrDeliveredNotificationsAreNotRepeated(){assertFalse(NotificationRules.pendingRemote("a",JSONObject().put("lida",true),emptySet()));assertFalse(NotificationRules.pendingRemote("a",JSONObject(),setOf("a")));assertTrue(NotificationRules.pendingRemote("b",JSONObject().put("lida",false),setOf("a")))}
 @Test fun oldChaptersBeforeSubscriptionDoNotNotify(){val now=System.currentTimeMillis();assertTrue(NotificationRules.newChapters(listOf(Chapter("a",1.0,"",true,now-10000)),emptySet(),now-1000,now).isEmpty())}
}
