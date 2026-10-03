package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
class OfflineCatalogTest {
 @Test fun onlyDownloadedWorksAreListedOnceWithSearchMetadata(){val saved=listOf(OfflineChapter("one","Romance","https://cover","1","Capítulo 1",3,true,"Manhwa","ongoing","Autora",listOf("Romance")),OfflineChapter("one","Romance","file:/local-cover","2","Capítulo 2",4,true),OfflineChapter("two","Comédia","file:/other-cover","1","Capítulo 1",2));val works=OfflineCatalog.works(saved);assertEquals(2,works.size);val romance=works.single{it.id=="one"};assertEquals("file:/local-cover",romance.cover);assertTrue(romance.adult);assertEquals(listOf("Romance"),romance.genres);assertEquals("Autora",romance.author)}
 @Test fun noDownloadsMeansNoOfflineCatalog(){assertTrue(OfflineCatalog.works(emptyList()).isEmpty())}
}
