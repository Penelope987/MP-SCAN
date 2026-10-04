package online.mpscan.app.data
import org.json.JSONObject
object NotificationRules {
 fun subscriptions(root:JSONObject)=root.keys().asSequence().filter{id->val v=root.opt(id);v!=false&&v!=JSONObject.NULL&&(v !is JSONObject||v.optBoolean("enabled",true))}.toSet()
 fun newChapters(chapters:List<Chapter>,known:Set<String>,subscribedAt:Long,now:Long=System.currentTimeMillis())=chapters.filter{it.available&&it.id !in known&&ChapterMetadata.latest(it)>0&&ChapterMetadata.latest(it)>=subscribedAt&&ChapterMetadata.latest(it)<=now}.sortedBy{ChapterMetadata.latest(it)}
}
