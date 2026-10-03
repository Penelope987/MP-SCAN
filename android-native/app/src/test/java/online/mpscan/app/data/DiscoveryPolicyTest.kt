package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
class DiscoveryPolicyTest {
 @Test fun adultDisplayDoesNotHideOrdinaryWorks(){assertFalse(DiscoveryPolicy.visible(true,"hide"));assertTrue(DiscoveryPolicy.visible(false,"hide"));assertTrue(DiscoveryPolicy.visible(true,"blur"));assertTrue(DiscoveryPolicy.visible(true,"show"))}
 @Test fun genresAllowMultipleSelectionsAndCaseInsensitiveAnyOrAll(){val genres=listOf("Romance","Drama");assertTrue(DiscoveryPolicy.matchesGenres(genres,setOf("romance","Comédia")));assertFalse(DiscoveryPolicy.matchesGenres(genres,setOf("romance","Comédia"),true));assertTrue(DiscoveryPolicy.matchesGenres(genres,setOf("romance","drama"),true));assertTrue(DiscoveryPolicy.matchesGenres(genres,emptySet(),true));assertFalse(DiscoveryPolicy.matchesGenres(genres,setOf("Ação")))}
}
