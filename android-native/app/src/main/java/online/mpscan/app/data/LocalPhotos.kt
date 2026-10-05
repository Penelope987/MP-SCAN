package online.mpscan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object LocalPhotos {
 private fun read(context:Context,uri:Uri,maxSize:Int):Bitmap {
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
  context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
  require(bounds.outWidth>0&&bounds.outHeight>0){"Escolha uma imagem válida."}
  var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>maxSize*2)sample*=2
  val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Não foi possível abrir a foto.")
  val orientation=runCatching{context.contentResolver.openInputStream(uri)?.use{ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)}}.getOrNull()
  val matrix=Matrix().apply{when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(270f);postScale(-1f,1f)};8->setRotate(270f)}}
  val oriented=if(matrix.isIdentity)bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
  if(oriented!==bitmap)bitmap.recycle()
  val ratio=minOf(1f,maxSize.toFloat()/maxOf(oriented.width,oriented.height))
  val result=if(ratio<1f)Bitmap.createScaledBitmap(oriented,(oriented.width*ratio).toInt().coerceAtLeast(1),(oriented.height*ratio).toInt().coerceAtLeast(1),true)else oriented
  if(result!==oriented)oriented.recycle();return result
 }
 suspend fun wallpaper(context:Context,uri:Uri):String=withContext(Dispatchers.IO){
  val bitmap=read(context,uri,1600);val file=File(context.filesDir,"appearance-${java.util.UUID.randomUUID()}.jpg")
  try{file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,88,it))};file.absolutePath}finally{bitmap.recycle()}
 }
 suspend fun collectionCover(context:Context,uri:Uri):String=withContext(Dispatchers.IO){
  val bitmap=read(context,uri,1000)
  try{val output=ByteArrayOutputStream();check(bitmap.compress(Bitmap.CompressFormat.JPEG,82,output));"data:image/jpeg;base64,"+Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP)}finally{bitmap.recycle()}
 }
}
