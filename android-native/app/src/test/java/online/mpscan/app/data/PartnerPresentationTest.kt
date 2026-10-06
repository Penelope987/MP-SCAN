package online.mpscan.app.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray
class PartnerPresentationTest {
 private fun work(id:String="w",owner:String="u",hosted:Boolean=true)=Work(id,"Obra","","","","","","",emptyList(),0,0,scanOwnerUid=owner,partnerOnly=hosted)
 @Test fun hostingUsesOwnerAndPartnerOnlyWithoutMixingDonations(){val scan=JSONObject().put("ownerUid","u");assertEquals(listOf("w"),PartnerPresentation.members(scan,false,listOf(work(),work("donated",hosted=false),work("other","x"))).map{it.id})}
 @Test fun donationsUseExplicitIdsAndNeverHostedWorks(){val scan=JSONObject().put("donatedWorkIds",JSONArray(listOf("w","hosted")));assertEquals(listOf("w"),PartnerPresentation.members(scan,true,listOf(work(hosted=false),work("hosted"))).map{it.id})}
 @Test fun pausedDeletedAndUnconfiguredScansAreNotVisible(){val s=JSONObject().put("status","approved").put("configured",true);assertTrue(PartnerPresentation.visible(s,false));s.put("hostingPaused",true);assertFalse(PartnerPresentation.visible(s,false));assertFalse(PartnerPresentation.visible(JSONObject(),false));assertFalse(PartnerPresentation.visible(JSONObject().put("status","approved"),false))}
 @Test fun originCreditAndLogoPersistThroughOfflineMetadata(){val scan=JSONObject().put("ownerUid","u").put("scanName","Equipe").put("photo","https://example/logo.png");val enriched=PartnerPresentation.enrich(work(),listOf(PartnerEntry("u",false,scan)));val saved=OfflineMetadata.decode(OfflineMetadata.encode(enriched));assertEquals(enriched,saved);assertEquals("hosting",saved.originKind);assertEquals("Equipe",saved.originName);assertEquals("https://example/logo.png",saved.originPhoto)}
 @Test fun externalCreditsNeverBecomeMpScanCredits(){val w=work().copy(originKind="external",originId="p",originName="Equipe externa",originPhoto="logo",originUid="author");assertEquals("Equipe externa",PartnerPresentation.origin(w,emptyList())?.name);assertEquals("external",PartnerPresentation.origin(w,emptyList())?.kind)}
 @Test fun everySitePresetHasAValidPalette(){listOf("mpscan","editorial","aurora","velvet","garden","ocean","paper","neon","sunset","minimal").forEach{preset->assertTrue(PartnerPresentation.preset(JSONObject().put("themePreset",preset)).all(AppearanceColors::valid))}}
 @Test fun publicRosterPreservesOtherAdminsAndWorksWithoutProfileRequests(){val s=JSONObject().put("ownerUid","owner").put("scanName","Equipe").put("publicAdmins",JSONObject().put("owner",JSONObject().put("name","Penélope").put("handle","penelope").put("photo","avatar")).put("admin2",JSONObject().put("name","Ana").put("handle","ana").put("photo","photo")));val roster=PartnerPresentation.roster(s,false,"owner");assertEquals(listOf("owner","admin2"),roster.map{it.uid});assertEquals("Penélope",roster.first().name);assertEquals("ana",roster.last().username)}
}
