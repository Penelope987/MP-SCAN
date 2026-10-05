package online.mpscan.app.data

import org.json.JSONObject
object CollectionWrites {
 fun payload(name:String,description:String,isPublic:Boolean,cover:String,previous:JSONObject=JSONObject(),now:Long=System.currentTimeMillis()):JSONObject {
  val clean=name.trim();require(clean.isNotBlank()&&clean.length<=80){"Dê um nome de até 80 caracteres à coleção."}
  require(description.length<=1200){"A descrição pode ter até 1200 caracteres."}
  return JSONObject(previous.toString()).put("nome",clean).put("descricao",description.trim()).put("publico",isPublic).put("capa",cover).put("obras",previous.optJSONObject("obras")?:JSONObject()).put("data",previous.optLong("data",now)).put("atualizadoEm",now)
 }
 fun updates(uid:String,id:String,payload:JSONObject?):JSONObject {
  require(uid.isNotBlank()&&id.isNotBlank()&&listOf(uid,id).none{Regex("[.#$\\[\\]/]").containsMatchIn(it)})
  return JSONObject().put("colecoes/$uid/$id",payload?:JSONObject.NULL).put("colecoesPublicas/$uid/$id",if(payload?.optBoolean("publico")==true)payload else JSONObject.NULL)
 }
}
