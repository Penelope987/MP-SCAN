package online.mpscan.app.data

import org.junit.Assert.*
import org.junit.Test

class PageManifestTest {
 @Test fun acceptsObjectsInArrayWithoutDroppingPages(){assertEquals(listOf("https://image/1","https://image/2"),PageManifest.parse("""[{"url":"https://image/1"},{"dataUrl":"https://image/2"}]"""))}
 @Test fun sortsNumericKeysAsNumbers(){assertEquals(listOf("https://image/2","https://image/10"),PageManifest.parse("""{"10":"https://image/10","2":"https://image/2"}"""))}
 @Test fun sortsPushIdsByExplicitPageOrder(){assertEquals(listOf("https://image/1","https://image/2"),PageManifest.parse("""{"-abc":{"ordem":2,"url":"https://image/2"},"-xyz":{"ordem":1,"url":"https://image/1"}}"""))}
 @Test fun scheduledChapterCannotBeReadBeforeRelease(){assertFalse(Chapter("c",20.0,"",true,0,0,"scheduled",System.currentTimeMillis()+60000).available)}
 @Test fun scheduledChapterCanBeReadAfterRelease(){assertTrue(Chapter("c",20.0,"",true,0,0,"scheduled",System.currentTimeMillis()-60000).available)}
 @Test fun draftsCannotBeRead(){assertFalse(Chapter("c",20.0,"",true,0,0,"draft").available)}
}
