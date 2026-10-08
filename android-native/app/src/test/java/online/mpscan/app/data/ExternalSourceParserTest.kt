package online.mpscan.app.data

import org.junit.Assert.*
import org.junit.Test

class ExternalSourceParserTest {
 private val scope="https://nocfsb.com/manga-tag/kenji-traducoes/"
 @Test fun followsOnlyPaginationInsideTheRegisteredCategory(){
  assertTrue(ExternalSourceParser.paginationAllowed(scope,scope+"page/2/"))
  assertFalse(ExternalSourceParser.paginationAllowed(scope,"https://nocfsb.com/manga-tag/other/page/2/"))
  assertFalse(ExternalSourceParser.paginationAllowed(scope,"https://nocfsb.com/"))
  assertFalse(ExternalSourceParser.paginationAllowed(scope,"https://other.example/manga-tag/kenji-traducoes/page/2/"))
  assertFalse(ExternalSourceParser.paginationAllowed(scope,scope+"?manga-tag=other"))
 }
 @Test fun ignoresRecommendationsAndLinksOutsideListing(){
  val html="""<a href='/manga/not-a-work/'>Menu</a><div class='sidebar'><div class='page-item-detail'><h3 class='post-title'><a href='/manga/recommendation/'>Recommendation</a></h3></div></div><div class='page-content-listing'><div class='page-item-detail'><div class='item-thumb'><img data-src='/cover.jpg'></div><h3 class='post-title'><a href='/manga/allowed/'>Allowed</a></h3></div><div class='page-item-detail'><h3 class='post-title'><a href='https://elsewhere.example/manga/other/'>Outside</a></h3></div></div><a class='next page-numbers' href='/manga-tag/kenji-traducoes/page/2/'>Next</a>"""
  val result=ExternalSourceParser.listing(html,scope,scope,"Kenji","kenji")
  assertEquals(listOf("Allowed"),result.works.map{it.first.title});assertEquals("https://nocfsb.com/cover.jpg",result.works.single().first.cover);assertEquals(scope+"page/2/",result.next)
 }
 @Test fun unavailablePageCannotLookLikeAnEmptySuccessfulCatalog(){
  try{ExternalSourceParser.listing("<title>Site Unavailable</title><p>Unable to access this site.</p>",scope,scope,"Kenji","kenji");fail()}catch(expected:ExternalSourceException){assertTrue(expected.message!!.contains("indisponível"))}
 }
 @Test fun protectionPagesHaveAnActionableMessage(){
  try{ExternalSourceParser.document("<title>Just a moment</title><p>Checking your browser</p>",scope);fail()}catch(expected:ExternalSourceException){assertTrue(expected.message!!.contains("verificação"))}
 }
 @Test fun chaptersStayWithinTheOriginalHostAndKeepStableIdentifiers(){
  val html="""<ul><li class='wp-manga-chapter'><a href='/manga/allowed/capitulo-02/'>Capítulo 02</a></li><li class='wp-manga-chapter'><a href='https://other.example/chapter/'>Capítulo 99</a></li></ul>"""
  val list=ExternalSourceParser.chapters(html,"https://nocfsb.com/manga/allowed/")
  assertEquals(1,list.size);assertEquals(2.0,list.single().first.number!!,0.0);assertEquals(list,ExternalSourceParser.chapters(html,"https://nocfsb.com/manga/allowed/"))
 }
 @Test fun readerPreservesOrderAndDoesNotPickAvatarsOrAds(){
  val html="""<img src='/advert.jpg'><div class='reading-content'><div class='page-break'><img data-src='/1.jpg'></div><div class='page-break'><img src='/2.jpg'></div></div>"""
  assertEquals(listOf("https://nocfsb.com/1.jpg","https://nocfsb.com/2.jpg"),ExternalSourceParser.pages(html,scope))
 }
 @Test fun missingImageCannotProduceAnIncompleteDownload(){
  try{ExternalSourceParser.pages("<div class='reading-content'><div class='page-break'><img src='/1.jpg'></div><div class='page-break'><img></div></div>",scope);fail()}catch(expected:ExternalSourceException){}
 }
 @Test fun refusesCredentialsLocalEndpointsAndInsecureSchemes(){
  for(u in listOf("http://example.com/","https://localhost/","https://127.0.0.1/","https://user:password@example.com/","https://example.com:9443/")){try{ExternalSourceParser.url(u);fail(u)}catch(expected:ExternalSourceException){}}
 }
}
