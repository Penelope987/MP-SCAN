package online.mpscan.app.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class AppearanceCollectionTest {
 @Test fun internalErrorsAreNeverShown(){assertEquals("Tente novamente.",PublicErrors.message(IllegalStateException("Não foi possível conectar ao Firebase https://host"),"Tente novamente."));assertEquals("Sua sessão expirou.",PublicErrors.message(IllegalStateException("Sua sessão expirou."),"Tente novamente."))}
 @Test fun foregroundAdaptsToBrightAndDarkPanels(){assertTrue(AppearanceColors.darkText("#FFFFFF"));assertFalse(AppearanceColors.darkText("#000000"));assertTrue(AppearanceColors.valid("#AABBCC"));assertFalse(AppearanceColors.valid("not-a-color"))}
 @Test fun privateEditRemovesPublicCopyAndPreservesMembers(){val previous=JSONObject().put("obras",JSONObject().put("work",true)).put("data",123L);val payload=CollectionWrites.payload("  Favoritas  ","Minha lista",false,"cover",previous,456L);val changes=CollectionWrites.updates("user","list",payload);assertTrue(changes.isNull("colecoesPublicas/user/list"));assertTrue(payload.getJSONObject("obras").getBoolean("work"));assertEquals(123L,payload.getLong("data"));assertEquals("Favoritas",payload.getString("nome"))}
 @Test fun publicSaveAndDeletionUseBothPaths(){val payload=CollectionWrites.payload("Lista","",true,"",now=1L);assertEquals(payload,CollectionWrites.updates("u","c",payload).getJSONObject("colecoesPublicas/u/c"));val deletion=CollectionWrites.updates("u","c",null);assertTrue(deletion.isNull("colecoes/u/c"));assertTrue(deletion.isNull("colecoesPublicas/u/c"))}
}
