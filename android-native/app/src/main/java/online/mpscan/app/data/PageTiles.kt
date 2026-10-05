package online.mpscan.app.data

/** Vertical regions fit in bounded bitmaps while retaining the page's full height. */
object PageTiles {
 data class Tile(val top:Int,val bottom:Int){val height get()=bottom-top}
 fun plan(width:Int,height:Int):List<Tile>{
  require(width>0&&height>0)
  val step=minOf(1536,maxOf(256,4_000_000/width)).coerceAtLeast(1)
  return (0 until height step step).map{Tile(it,minOf(it+step,height))}
 }
 fun sample(width:Int,height:Int,targetWidth:Int):Int{
  require(width>0&&height>0&&targetWidth>0)
  var sample=1
  while(width/sample>minOf(targetWidth.toLong()*2,3072L)||width.toLong()*height/(sample.toLong()*sample)>4_000_000L)sample*=2
  return sample
 }
 fun sourceIndex(key:Any?):Int?=key?.toString()?.takeIf{it.startsWith("page:")}?.split(':')?.getOrNull(1)?.toIntOrNull()
}
