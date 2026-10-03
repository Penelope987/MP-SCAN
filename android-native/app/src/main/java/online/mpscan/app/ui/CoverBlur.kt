package online.mpscan.app.ui
import android.graphics.Bitmap
import coil3.transform.Transformation
import coil3.size.Size
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAdultDisplay=staticCompositionLocalOf{"show"}
val LocalAdultCovers=staticCompositionLocalOf<Set<String>>{emptySet()}
/** Pixel transformation works on every supported Android version, including API 24. */
class CoverBlur:Transformation(){
 override val cacheKey="mp-adult-cover-blur-v1"
 override suspend fun transform(input:Bitmap,size:Size):Bitmap {
  val width=96;val height=(input.height.toDouble()*width/input.width).toInt().coerceIn(1,192)
  val scaled=Bitmap.createScaledBitmap(input,width,height,true)
  var pixels=IntArray(width*height);scaled.getPixels(pixels,0,width,0,0,width,height)
  // Three separable box passes approximate a Gaussian without native API dependencies.
  repeat(3){
   val horizontal=IntArray(pixels.size);val output=IntArray(pixels.size)
   for(y in 0 until height)for(x in 0 until width){var a=0;var r=0;var g=0;var b=0;for(k in -8..8){val p=pixels[y*width+(x+k).coerceIn(0,width-1)];a+=p ushr 24;r+=(p ushr 16)and 255;g+=(p ushr 8)and 255;b+=p and 255};horizontal[y*width+x]=((a/17)shl 24)or((r/17)shl 16)or((g/17)shl 8)or(b/17)}
   for(y in 0 until height)for(x in 0 until width){var a=0;var r=0;var g=0;var b=0;for(k in -8..8){val p=horizontal[(y+k).coerceIn(0,height-1)*width+x];a+=p ushr 24;r+=(p ushr 16)and 255;g+=(p ushr 8)and 255;b+=p and 255};output[y*width+x]=((a/17)shl 24)or((r/17)shl 16)or((g/17)shl 8)or(b/17)}
   pixels=output
  }
  return Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888)
 }
}
