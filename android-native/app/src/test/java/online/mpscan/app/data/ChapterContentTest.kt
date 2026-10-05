package online.mpscan.app.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ChapterContentTest {
 @Test fun novelFromTheSitePublisherRemainsReadable(){
  val parts=ChapterText.native(JSONObject().put("formatoCapitulo","text").put("textoNovel","Primeiro parágrafo.\n\nSegundo parágrafo."))
  assertEquals("Primeiro parágrafo.\n\nSegundo parágrafo.",ChapterText.decode(parts.single()))
 }
 @Test fun mixedReaderPreservesTextImagesAndTheirOrder(){
  val parts=ExternalSourceParser.pages("<div class='reading-content'><p>Antes &amp; agora</p><div class='page-break'><img src='/1.jpg'></div><p>Depois</p><img src='/1.jpg'></div>","https://scan.example/chapter/")
  assertEquals(4,parts.size);assertEquals("Antes & agora",ChapterText.decode(parts[0]));assertEquals("https://scan.example/1.jpg",parts[1]);assertEquals("Depois",ChapterText.decode(parts[2]));assertEquals(parts[1],parts[3])
 }
 @Test fun scriptsAndAdvertisingAreNotChapterContent(){
  val parts=ChapterText.blocks("<p>Uma história.</p><script>password()</script><div class='ads'><img src='/ad.jpg'></div><iframe src='https://other.example/'></iframe>","https://scan.example/")
  assertEquals(listOf("Uma história."),parts.map(ChapterText::decode))
 }
 @Test fun textOnlyPartnerChapterIsSupported(){
  val parts=ExternalSourceParser.pages("<div class='reading-content'><div class='text-left'><h2>Um encontro</h2><p>Era uma vez.</p></div></div>","https://scan.example/chapter/")
  assertEquals(listOf("Um encontro","Era uma vez."),parts.map(ChapterText::decode))
 }
 @Test fun privateDraftStateAndResponsibleIdentitySurviveStorage(){
  val p=ScanPartnership("scan","Minha scan","https://scan.example/tag/team/",ownerUid="owner",responsibleUid="person",draft=true)
  assertEquals(p,ScanPartnership.parse(p.id,p.json()));assertEquals("draft",p.json().getString("publicationMode"))
 }
 @Test fun atSearchDoesNotConfuseDisplayNameWithHandle(){
  val person=ProfilePerson("uid","Ana Maria","penelope","photo")
  assertTrue(UserDirectory.matches(person,"@pene"));assertFalse(UserDirectory.matches(person,"@ana"));assertTrue(UserDirectory.matches(person,"maria"));assertFalse(UserDirectory.matches(person,"@"))
 }
}
