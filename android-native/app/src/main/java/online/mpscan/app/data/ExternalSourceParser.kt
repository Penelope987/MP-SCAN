package online.mpscan.app.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.security.MessageDigest
import java.io.IOException

class ExternalSourceException(message:String):IOException(message)
data class ExternalListing(val works:List<Pair<Work,String>>,val next:String?)
object ExternalSourceParser {
 fun id(prefix:String,url:String)=prefix+MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString(""){"%02x".format(it)}
 fun url(value:String):String {
  val u=runCatching{URI(value.trim()).normalize()}.getOrNull()?:throw ExternalSourceException("Informe um endereço válido da página da scan.")
  val host=u.host?.lowercase().orEmpty()
  if(u.scheme!="https"||u.userInfo!=null||u.fragment!=null||u.port !in listOf(-1,443)||host.isBlank()||host=="localhost"||host.endsWith(".local")||host.endsWith(".localhost")||host.contains(':')||host.matches(Regex("[0-9.]+")))throw ExternalSourceException("Use um endereço HTTPS público, sem senha ou dados de acesso.")
  return u.toASCIIString()
 }
 fun sameOrigin(a:String,b:String):Boolean=runCatching{val x=URI(url(a));val y=URI(url(b));x.host.equals(y.host,true)}.getOrDefault(false)
 fun paginationAllowed(scope:String,candidate:String):Boolean=runCatching{
  if(!sameOrigin(scope,candidate))return@runCatching false
  val a=URI(scope);val b=URI(candidate);val path=a.path.trimEnd('/')
  val queryA=a.rawQuery.orEmpty().split('&').filter{it.isNotBlank()&&!it.startsWith("paged=")&&!it.startsWith("page=")}.sorted()
  val queryB=b.rawQuery.orEmpty().split('&').filter{it.isNotBlank()&&!it.startsWith("paged=")&&!it.startsWith("page=")}.sorted()
  queryA==queryB&&(b.path.trimEnd('/')==path||Regex(Regex.escape(path)+"/page/[1-9][0-9]*").matches(b.path.trimEnd('/')))
 }.getOrDefault(false)
 fun document(html:String,address:String):Document {
  val doc=Jsoup.parse(html,address);val text=doc.text().lowercase()
  if(doc.title().contains("Site Unavailable",true)||text.contains("unable to access this site"))throw ExternalSourceException("A página parceira está indisponível nesta conexão. Tente novamente mais tarde.")
  if(doc.title().contains("Just a moment",true)||text.contains("verify you are human")||text.contains("checking your browser")||doc.select("#cf-challenge-running, .g-recaptcha, #challenge-form").isNotEmpty())throw ExternalSourceException("O site parceiro está pedindo verificação no navegador. A equipe precisa liberar a integração para leitura no aplicativo.")
  return doc
 }
 private fun image(el:Element?):String {
  if(el==null)return ""
  for(key in listOf("data-src","data-lazy-src","data-original","src")){val v=el.absUrl(key).trim();if(v.startsWith("https://")&&!v.contains("data:image"))return v}
  return ""
 }
 fun listing(html:String,address:String,scope:String,name:String,partnerId:String):ExternalListing {
  require(paginationAllowed(scope,address)){"Página fora do filtro da parceria."}
  val doc=document(html,address)
  val containers=doc.select(".page-content-listing, main .c-tabs-item__content, .main-col .c-tabs-item__content")
  if(containers.isEmpty())throw ExternalSourceException("Esta página ainda não usa um formato de catálogo compatível. Nenhuma obra de outra página foi importada.")
  val cards=containers.select(".page-item-detail, .manga-item")
  val works=cards.mapNotNull{card->
   if(card.parents().any{it.hasClass("widget")||it.hasClass("related-manga")||it.hasClass("sidebar")})return@mapNotNull null
   val link=card.selectFirst(".post-title a[href], .item-summary h3 a[href], .item-summary h4 a[href]")?:return@mapNotNull null
   val target=runCatching{url(link.absUrl("href"))}.getOrNull()?:return@mapNotNull null
   if(!sameOrigin(scope,target)||link.text().isBlank())return@mapNotNull null
   Work(id("ext_",partnerId+"|"+target),link.text().trim(),"",image(card.selectFirst(".item-thumb img, img")),"","Obra parceira","",card.selectFirst(".item-author")?.text().orEmpty(),emptyList(),0,0,scan=name,hosting=URI(scope).host,scanOwnerUid="external:$partnerId") to target
  }.distinctBy{it.first.id}
  val next=doc.select(".pagination a.next[href], a.next.page-numbers[href], .nav-previous a[href]").map{it.absUrl("href")}.firstOrNull{it!=address&&paginationAllowed(scope,it)}
  if(next==null&&doc.select(".navigation-ajax, .load-more").any{it.text().contains("more",true)||it.text().contains("mais",true)})throw ExternalSourceException("A listagem usa carregamento dinâmico ainda não compatível. O aplicativo não vai importar só uma parte sem avisar.")
  return ExternalListing(works,next)
 }
 fun details(html:String,address:String,work:Work):Work {
  val doc=document(html,address)
  fun field(label:String)=doc.select(".post-content_item").firstOrNull{it.selectFirst(".summary-heading")?.text()?.contains(label,true)==true}?.selectFirst(".summary-content")?.text().orEmpty()
  return work.copy(synopsis=doc.selectFirst(".summary__content, .description-summary .summary__content")?.text().orEmpty(),author=field("Autor"),artist=field("Artista"),status=field("Estado").ifBlank{field("Status")},type=field("Tipo").ifBlank{work.type},year=field("Lançamento"),genres=doc.select(".genres-content a").map{it.text()}.distinct())
 }
 fun chapters(html:String,address:String):List<Pair<Chapter,String>> {
  val doc=document(html,address)
  return doc.select("li.wp-manga-chapter a[href], .listing-chapters_wrap .chapter-item a[href]").mapNotNull{link->
   val target=runCatching{url(link.absUrl("href"))}.getOrNull()?:return@mapNotNull null
   if(!sameOrigin(address,target))return@mapNotNull null
   val title=link.text().trim();if(title.isBlank())return@mapNotNull null
   val number=Regex("(?i)(?:cap[ií]tulo|chapter|cap\\.?)[ \\t]*([0-9]+(?:[.,][0-9]+)?)").find(title)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull()
   Chapter(id("extc_",target),number,title,true,0) to target
  }.distinctBy{it.first.id}
 }
 fun pages(html:String,address:String):List<String> {
  val doc=document(html,address)
  if(doc.select(".reading-content").isEmpty())throw ExternalSourceException("O leitor desta origem ainda não é compatível com imagens offline.")
  val pages=doc.select(".reading-content .page-break img, .reading-content img.wp-manga-chapter-img").map{image(it)}
  if(pages.isEmpty()||pages.any{it.isBlank()})throw ExternalSourceException("Não foi possível obter todas as imagens do capítulo na origem.")
  return pages.distinct()
 }
}
