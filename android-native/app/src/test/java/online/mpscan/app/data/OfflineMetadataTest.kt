package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class OfflineMetadataTest {
 private val work=Work("obra_1","História","Uma sinopse completa","https://capa","https://banner","Manhwa","ongoing","Autora",listOf("Romance","Fantasia","Drama"),1234,99,"Outro nome","Artista","2026","Scan","Hospedagem","Português","Sexta-feira","scan-uid")
 @Test fun preservesCompleteWorkAcrossDiskRoundTrip(){assertEquals(work,OfflineMetadata.decode(JSONObject(OfflineMetadata.encode(work).toString())))}
 @Test fun preservesDecimalNumberTitleAndDates(){val chapter=Chapter("especial",12.5,"Extra de inverno",true,1234,1122);assertEquals(chapter,OfflineMetadata.chapter(JSONObject(OfflineMetadata.encode(chapter).toString())))}
 @Test fun preservesUnnumberedChapter(){val chapter=Chapter("extra",null,"Prólogo",true,0);assertEquals(chapter,OfflineMetadata.chapter(OfflineMetadata.encode(chapter)))}
 @Test fun allowsAnyNumberOfSelectedGenres(){assertTrue(Discovery.genresMatch(work,setOf("romance","FANTASIA","Drama")));assertFalse(Discovery.genresMatch(work,setOf("Romance","Terror")));assertTrue(Discovery.genresMatch(work,emptySet()))}
 @Test fun rankingArtDoesNotGrantFreeFrames(){assertEquals(1,BuiltinAvatarFrames.rankingArt("mp_rank_avatar_1_v1")!!.getInt("rank"));assertNull(BuiltinAvatarFrames.rankingArt("mp_rank_avatar_11_v1"));assertFalse(BuiltinAvatarFrames.contains("mp_rank_avatar_1_v1"))}
 @Test fun includesAllTwentyOneWebsiteBuiltins(){assertEquals(21,BuiltinAvatarFrames.definitions().length());assertTrue(BuiltinAvatarFrames.contains("mp_cachorrinho_v1"));assertFalse(BuiltinAvatarFrames.contains("ranking_1"))}
}
