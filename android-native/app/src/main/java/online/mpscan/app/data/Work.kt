package online.mpscan.app.data

data class Work(val id:String,val title:String,val synopsis:String,val cover:String,val banner:String,val type:String,val status:String,val author:String,val genres:List<String>,val updatedAt:Long,val reads:Long,val alternateTitle:String="",val artist:String="",val year:String="",val scan:String="",val hosting:String="",val language:String="Português",val schedule:String="Sem dia fixo")
data class Chapter(val id:String,val number:Double?,val title:String,val published:Boolean,val updatedAt:Long,val createdAt:Long=0){val label:String get()="Capítulo "+(number?.toString()?.removeSuffix(".0")?:"—")}

data class WorkRating(
    val average: Double = 0.0,
    val total: Int = 0,
    val counts: Map<Int, Int> = (1..5).associateWith { 0 },
    val mine: Int = 0
)
data class WorkReaction(val id:String,val label:String,val image:String,val source:String,val votes:Int,val selected:Boolean)
data class NewBadgeStyle(
 val enabled:Boolean=true,val text:String="NOVO",val imageUrl:String="",val backgroundMode:String="both",
 val bgColor:String="#ff3f79",val bgColor2:String="#8d2bff",val textColor:String="#ffffff",
 val glowColor:String="#ff4fa3",val effect:String="pulse",val textPosition:String="center",
 val fontSize:Int=100,val fontWeight:Int=900,val letterSpacing:Int=7,val radius:Int=999,val size:Int=100
)

data class RecentUpdate(val work:Work,val chapter:Chapter,val updatedAt:Long)
