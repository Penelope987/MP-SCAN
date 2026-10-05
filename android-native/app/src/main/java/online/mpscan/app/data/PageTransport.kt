package online.mpscan.app.data

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A transfer is not published until the response is complete and nonempty. */
class PageTransport(private val connectTimeout:Int=20000,private val readTimeout:Int=90000){
 fun download(source:String,target:File){
  var url=URL(source)
  repeat(6){
   val c=url.openConnection() as HttpURLConnection
   c.connectTimeout=connectTimeout;c.readTimeout=readTimeout;c.instanceFollowRedirects=false
   c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
   c.setRequestProperty("Referer","https://www.mpscan.online/")
   c.setRequestProperty("Accept","image/avif,image/webp,image/png,image/jpeg,image/*;q=0.9,*/*;q=0.5")
   try{
    val status=c.responseCode
    if(status in listOf(301,302,303,307,308)){
     val next=URL(url,c.getHeaderField("Location")?:throw IOException("Redirecionamento sem destino."))
     if(next.protocol !in listOf("http","https")||(url.protocol=="https"&&next.protocol!="https"))throw IOException("Redirecionamento inválido.")
     url=next
    }else{
     if(status !in 200..299)throw IOException("Não foi possível carregar a imagem (HTTP $status).")
     val expected=c.contentLengthLong
     target.outputStream().use{out->c.inputStream.use{it.copyTo(out)}}
     if(target.length()==0L||(expected>0&&target.length()!=expected))throw IOException("A transferência da página ficou incompleta.")
     return
    }
   }finally{c.disconnect()}
  }
  throw IOException("Não foi possível concluir o redirecionamento da imagem.")
 }
}
