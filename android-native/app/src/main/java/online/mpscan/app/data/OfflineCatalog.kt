package online.mpscan.app.data
object OfflineCatalog {
 fun works(chapters:List<OfflineChapter>):List<Work> = chapters.groupBy{it.workId}.map{(id,saved)->val first=saved.first();Work(id,first.workTitle,"",saved.firstOrNull{it.workCover.startsWith("file:")}?.workCover?:first.workCover,"",first.type,first.status,first.author,first.genres,0,0,adult=saved.any{it.adult})}.sortedBy{it.title.lowercase()}
}
