package online.mpscan.app.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class DiscoveryTest {
 private fun work(id:String,title:String="História Ágil",synopsis:String="Uma aventura mágica",author:String="João")=Work(id,title,synopsis,"","","Manhwa","Em andamento",author,listOf("Fantasia"),0,0)
 @Test fun rankingUsesVoteCountThenAverageAndIgnoresInvalidVotes(){
  val ratings=JSONObject("""{"a":{"u1":{"nota":5}},"b":{"u1":{"nota":3},"u2":{"nota":4}},"c":{"u1":{"nota":4},"u2":{"nota":5},"bad":{"nota":9}},"d":{"bad":{"nota":0}}}""")
  val ranks=Discovery.ranking(listOf(work("a"),work("b"),work("c"),work("d")),ratings)
  assertEquals(listOf("c","b","a"),ranks.map{it.work.id})
  assertEquals(2,ranks.first().votes);assertEquals(4.5,ranks.first().average,0.001)
 }
 @Test fun searchIncludesSynopsisAuthorAndAccents(){
  val work=work("a")
  assertTrue(Discovery.matches(work,"magica joao"))
  assertTrue(Discovery.matches(work,"historia fantasia"))
  assertTrue(Discovery.matches(work,""))
  assertFalse(Discovery.matches(work,"joao inexistente"))
 }
 @Test fun unratedWorksAreNotInventedIntoRanking(){assertTrue(Discovery.ranking(listOf(work("a")),JSONObject()).isEmpty())}
}
