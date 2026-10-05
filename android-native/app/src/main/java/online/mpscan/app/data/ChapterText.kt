package online.mpscan.app.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.json.JSONObject

/** Ordered native text blocks. No scripts, remote frames or web rendering. */
object ChapterText {
 const val PREFIX="mpscan-text:"
 const val HEADER="MPSCAN-TEXT-1\n"
 fun encode(text:String)=PREFIX+JSONObject().put("text",text).toString()
 fun decode(source:String)=JSONObject(source.removePrefix(PREFIX)).getString("text").also{require(it.isNotBlank()&&it.length<=200000)}
 fun blocks(html:String,address:String=""):List<String>{
  val body=Jsoup.parseBodyFragment(html,address).body()
  body.select("script,style,iframe,form,button,nav,.ads,.advertisement,.ad-container").remove()
  val result=mutableListOf<String>();val buffer=StringBuilder()
  fun flush(){val text=buffer.toString().trim();if(text.isNotBlank())text.chunked(4000).forEach{result+=encode(it)};buffer.setLength(0)}
  fun walk(node:org.jsoup.nodes.Node){
   when(node){
    is TextNode->buffer.append(node.wholeText)
    is Element->{
     if(node.tagName()=="img"){
      flush();val source=listOf("data-src","data-lazy-src","data-original","src").firstNotNullOfOrNull{key->node.attr(key).trim().takeIf{it.isNotBlank()&&!it.startsWith("data:image/gif")}}
       ?:throw ExternalSourceException("Uma imagem do capítulo está sem endereço. O download não será salvo incompleto.")
      val image=PageManifest.normalize(if(source.startsWith("data:"))source else node.absUrl(listOf("data-src","data-lazy-src","data-original","src").first{node.attr(it).trim()==source}))
       ?:throw ExternalSourceException("O capítulo contém uma imagem em formato não suportado.")
      result+=image
     }else{
      if(node.tagName()=="br")buffer.append('\n')
      node.childNodes().forEach{walk(it)}
      if(node.tagName() in listOf("p","div","li","blockquote","h1","h2","h3","h4","section","article")){buffer.append('\n');flush()}
     }
    }
   }
  }
  walk(body);flush();return result
 }
 fun native(raw:JSONObject):List<String>{
  val field=listOf("texto","text","conteudo","content","html","novelText","textoNovel").firstOrNull{raw.opt(it) is String&&raw.optString(it).isNotBlank()}?:return emptyList()
  val text=raw.getString(field)
  return if(text.contains(Regex("<[^>]+>")))blocks(text)else text.chunked(4000).filter{it.isNotBlank()}.map(::encode)
 }
}
