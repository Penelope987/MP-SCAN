package online.mpscan.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import online.mpscan.app.data.SiteAccess

class MpScanApplication:Application(),SingletonImageLoader.Factory {
 override fun onCreate(){super.onCreate();SiteAccess.init(this)}
 override fun newImageLoader(context:Context):ImageLoader {
  val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).callTimeout(120,TimeUnit.SECONDS).retryOnConnectionFailure(true)
   .addInterceptor{chain->chain.proceed(chain.request().newBuilder().header("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36").header("Referer","https://www.mpscan.online/").build())}.build()
  return ImageLoader.Builder(context).memoryCache{MemoryCache.Builder().maxSizePercent(context,.12).build()}.components{add(OkHttpNetworkFetcherFactory(callFactory={client}))}.build()
 }
}
