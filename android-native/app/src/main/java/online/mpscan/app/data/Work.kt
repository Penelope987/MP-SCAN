package online.mpscan.app.data

data class Work(val id:String,val title:String,val synopsis:String,val cover:String,val banner:String,val type:String,val status:String,val author:String,val genres:List<String>,val updatedAt:Long,val reads:Long)
data class Chapter(val id:String,val number:Double?,val title:String,val published:Boolean,val updatedAt:Long){val label:String get()="Capítulo "+(number?.toString()?.removeSuffix(".0")?:"—")}
