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
 @Test fun newBadgeExpiresAtExactly48Hours(){
  val published=1_800_000_000_000L
  val chapter=Chapter("c",1.0,"",true,published)
  assertTrue(ChapterMetadata.isNew(chapter,published))
  assertTrue(ChapterMetadata.isNew(chapter,published+ChapterMetadata.NEW_WINDOW_MILLIS-1))
  assertFalse(ChapterMetadata.isNew(chapter,published+ChapterMetadata.NEW_WINDOW_MILLIS))
  assertFalse(ChapterMetadata.isNew(chapter,published+30L*24*60*60*1000))
  assertFalse(ChapterMetadata.isNew(chapter,published-1))
 }
 @Test fun secondBasedTimestampsUseTheSameExpiry(){
  val chapter=Chapter("c",1.0,"",true,1_800_000_000L)
  assertTrue(ChapterMetadata.isNew(chapter,1_800_000_000_000L+47L*60*60*1000))
  assertFalse(ChapterMetadata.isNew(chapter,1_800_000_000_000L+48L*60*60*1000))
 }
 @Test fun editingAnOldChapterDoesNotRestartTheNewBadge(){
  val now=1_800_000_000_000L
  val chapter=Chapter("c",1.0,"",true,now,now-30L*24*60*60*1000)
  assertFalse(ChapterMetadata.isNew(chapter,now))
 }

 @Test fun publishingAnOldDraftUsesTheExplicitReleaseDate(){
  val now=1_800_000_000_000L
  val chapter=ChapterMetadata.parse("c",JSONObject().put("publicadoEm",now).put("criadoEm",now-30L*24*60*60*1000))
  assertTrue(ChapterMetadata.isNew(chapter,now))
 }
 @Test fun editingAReleasedChapterKeepsTheExplicitReleaseDate(){
  val now=1_800_000_000_000L
  val chapter=ChapterMetadata.parse("c",JSONObject().put("publishedAt",now-3L*24*60*60*1000).put("atualizadoEm",now))
  assertFalse(ChapterMetadata.isNew(chapter,now))
  assertEquals(chapter,OfflineMetadata.chapter(OfflineMetadata.encode(chapter)))
 }

}
