package online.mpscan.app.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class ChapterMetadataTest {
 @Test fun explicitPublishedModeMatchesSiteDespiteLegacyFalseFlag(){
  val chapter=ChapterMetadata.parse("c",JSONObject("""{"modoPublicacao":"published","publicado":false}"""))
  assertTrue(chapter.available)
 }
 @Test fun releasedScheduledChapterIsNewAndUsesReleaseDate(){
  val now=System.currentTimeMillis()
  val chapter=ChapterMetadata.parse("c",JSONObject().put("publicationMode","scheduled").put("scheduledAt",now-1000).put("createdAt",now-30L*24*60*60*1000))
  assertTrue(chapter.available);assertTrue(ChapterMetadata.isNew(chapter,now));assertEquals(now-1000,ChapterMetadata.latest(chapter))
 }
 @Test fun futureDraftAndMissingDatesNeverGetNewBadge(){
  val now=System.currentTimeMillis()
  val future=ChapterMetadata.parse("c",JSONObject().put("agendadoPara",now+60000))
  assertFalse(future.available);assertFalse(ChapterMetadata.isNew(future,now))
  assertFalse(ChapterMetadata.isNew(ChapterMetadata.parse("c",JSONObject().put("rascunho",true).put("criadoEm",now)),now))
  assertFalse(ChapterMetadata.isNew(ChapterMetadata.parse("c",JSONObject()),now))
 }
 @Test fun timestampsInSecondsAreNormalized(){assertEquals(1_800_000_000_000L,ChapterMetadata.parse("c",JSONObject().put("criadoEm",1_800_000_000L)).createdAt)}
}
