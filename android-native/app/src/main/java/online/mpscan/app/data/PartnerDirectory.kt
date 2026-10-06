package online.mpscan.app.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

data class PartnerEntry(val id:String,val donation:Boolean,val scan:JSONObject)
data class WorkOrigin(val kind:String,val id:String,val name:String,val photo:String,val uid:String="")
object PartnerPresentation {
 fun members(scan:JSONObject,donation:Boolean,works:List<Work>):List<Work>{
  val ids=scan.optJSONArray("donatedWorkIds");val owner=scan.optString("ownerUid")
  return works.filter{if(donation) !it.partnerOnly&&ids!=null&&(0 until ids.length()).any{n->ids.optString(n)==it.id} else owner.isNotBlank()&&it.scanOwnerUid==owner&&it.partnerOnly}
 }
 fun origin(work:Work,entries:List<PartnerEntry>):WorkOrigin?{
  if(work.originKind=="external")return WorkOrigin("external",work.originId,work.originName,work.originPhoto,work.originUid)
  val entry=entries.firstOrNull{members(it.scan,it.donation,listOf(work)).isNotEmpty()}
  if(entry!=null)return WorkOrigin(if(entry.donation)"donation"else"hosting",entry.id,entry.scan.optString("scanName"),entry.scan.optString("photo"),entry.scan.optString(if(entry.donation)"donorUid"else"ownerUid"))
  return if(work.originKind.isNotBlank())WorkOrigin(work.originKind,work.originId,work.originName,work.originPhoto,work.originUid)else null
 }
 fun visible(scan:JSONObject,donation:Boolean)=scan.optString("status")=="approved"&&(donation||(!scan.optBoolean("hostingPaused")&&scan.optBoolean("configured",false)))
 fun enrich(work:Work,entries:List<PartnerEntry>):Work=origin(work,entries)?.let{work.copy(originKind=it.kind,originId=it.id,originName=it.name,originPhoto=it.photo,originUid=it.uid)}?:work
 fun preset(scan:JSONObject):List<String> = when(scan.optString("themePreset")){
  "editorial"->listOf("#9861d9","#62339e","#1b102a");"aurora"->listOf("#9d72ec","#f6a6cb","#201a31");"velvet"->listOf("#d5a5e3","#8c72c5","#1b1524");"garden"->listOf("#72bc9a","#d9bcd4","#14251f");"ocean"->listOf("#62b8e2","#8a8ee9","#102332");"paper"->listOf("#ad786b","#dcb6a1","#eee4d7");"neon"->listOf("#c6ff6b","#a775ff","#131527");"sunset"->listOf("#f6a36a","#d576ab","#30202b");"minimal"->listOf("#9a91b6","#d4c8dc","#211e29");else->listOf("#8d5cff","#ff64af","#15121f")
 }
}
object PartnerDirectory {
 val entries=MutableStateFlow<List<PartnerEntry>>(emptyList())
 val externalWorks=MutableStateFlow<List<Work>>(emptyList())
 val externalErrors=MutableStateFlow<List<String>>(emptyList())
 private var initialized=false
 fun init(context:Context){if(initialized)return;initialized=true;val prefs=context.applicationContext.getSharedPreferences("mp_partner_directory",0);entries.value=runCatching{parse(JSONObject(prefs.getString("entries","{}")?:"{}"))}.getOrDefault(emptyList())}
 private fun parse(root:JSONObject):List<PartnerEntry> = listOf("partnerScans","donationScans").flatMap{path->val node=root.optJSONObject(path)?:JSONObject();node.keys().asSequence().mapNotNull{id->node.optJSONObject(id)?.takeIf{PartnerPresentation.visible(it,path=="donationScans")}?.let{PartnerEntry(id,path=="donationScans",it)}}.toList()}
 suspend fun refresh(context:Context){
  val root=coroutineScope{val hosted=async{SiteAccess.json("partnerScans")};val donated=async{SiteAccess.json("donationScans")};JSONObject().put("partnerScans",hosted.await()).put("donationScans",donated.await())}
  entries.value=parse(root);val published=ExternalCatalog.partners(context).filter{!it.draft&&it.enabled}.map{it.id}.toSet();externalWorks.value=externalWorks.value.filter{(it.originId.ifBlank{it.scanOwnerUid.removePrefix("external:")}) in published};context.getSharedPreferences("mp_partner_directory",0).edit().putString("entries",root.toString()).apply()
 }
 suspend fun searchCatalog(context:Context,refresh:Boolean){
  val partners=ExternalCatalog.partners(context).filter{!it.draft&&it.enabled};val gate=Semaphore(2)
  val errors=java.util.Collections.synchronizedList(mutableListOf<String>())
  externalWorks.value=partners.flatMap{ExternalCatalog.cachedCatalog(it.id).map{work->work.copy(originKind="external",originId=it.id,originName=it.name,originPhoto=it.photo,originUid=it.responsibleUid,hosting=it.url)}}.distinctBy{it.id}
  if(refresh)externalWorks.value=coroutineScope{partners.map{partner->async{gate.withPermit{try{ExternalCatalog.catalog(partner)}catch(e:CancellationException){throw e}catch(e:Exception){errors+=partner.name;ExternalCatalog.cachedCatalog(partner.id).map{work->work.copy(originKind="external",originId=partner.id,originName=partner.name,originPhoto=partner.photo,originUid=partner.responsibleUid,hosting=partner.url)}}}}}.awaitAll().flatten().distinctBy{it.id}}
  externalErrors.value=errors.toList()
 }
}
