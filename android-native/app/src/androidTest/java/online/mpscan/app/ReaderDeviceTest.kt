package online.mpscan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.runBlocking
import online.mpscan.app.data.*
import online.mpscan.app.ui.ReaderImages
import online.mpscan.app.ui.theme.MpScanTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class ReaderDeviceTest {
 @get:Rule val compose=createComposeRule()
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private fun image(width:Int,height:Int):File{
  val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565)
  Canvas(bitmap).apply{drawColor(android.graphics.Color.BLUE);drawRect(0f,(height-1536).coerceAtLeast(0).toFloat(),width.toFloat(),height.toFloat(),Paint().apply{color=android.graphics.Color.RED})}
  val file=File(context.cacheDir,"device-${java.util.UUID.randomUUID()}.jpg")
  file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle();return file
 }
 @Test fun actualAndroidDecoderAndReaderDisplayBothEndsOfVeryLongOfflinePage(){
  val file=image(1440,28000);val asset=PageFiles.inspect(file);val tiles=PageTiles.plan(asset.width,asset.height)
  runBlocking{tiles.forEach{tile->val bitmap=PageFiles.bitmap(asset,tile,1080);assertTrue(bitmap.width<=3072);assertTrue(bitmap.allocationByteCount<=8_000_000);bitmap.recycle()}}
  compose.setContent{MpScanTheme{ReaderImages(listOf(file.toURI().toString()),rememberLazyListState(),1f,1f,false,Modifier.testTag("reader-list")){null}}}
  compose.waitUntil(20000){compose.onAllNodesWithTag("reader-tile-0").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("reader-list").performScrollToIndex(tiles.lastIndex)
  val tag="reader-tile-${tiles.last().top}"
  compose.waitUntil(20000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag(tag).assertIsDisplayed()
 }
 @Test fun offlineDownloadSurvivesFailedReplacementAndRestoresTruncatedPage()=runBlocking{
  val file=image(720,15000);val inline="data:image/jpeg;base64,"+Base64.encodeToString(file.readBytes(),Base64.NO_WRAP)
  val work=Work("device_${java.util.UUID.randomUUID()}","Leitura offline","","","","","","",emptyList(),0,0)
  val chapter=Chapter("chapter",1.0,"",true,1);val store=OfflineStore(context)
  try{
   val saved=store.download(work,chapter,listOf(inline),{});assertEquals(1,saved.size)
   PageFiles.fetch(context,saved.single())
   try{store.download(work,chapter,listOf("data:image/jpeg;base64,SGVsbG8="),{},true);fail("Invalid image must not replace the working chapter")}catch(expected:java.io.IOException){}
   assertEquals(saved,store.localPages(work.id,chapter.id))
   RandomAccessFile(File(java.net.URI(saved.single())),"rw").use{it.setLength(16)}
   assertTrue(store.localPages(work.id,chapter.id).isEmpty())
   store.restorePage(work.id,chapter.id,0,inline)
   val restored=store.localPages(work.id,chapter.id);assertEquals(1,restored.size);assertEquals(15000,PageFiles.fetch(context,restored.single()).height)
  }finally{store.delete(work.id,chapter.id);file.delete()}
 }
}
