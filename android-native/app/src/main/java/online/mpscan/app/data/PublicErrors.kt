package online.mpscan.app.data

/** Keeps implementation details out of messages shown to readers. */
object PublicErrors {
 fun message(error:Throwable,fallback:String):String {
  val text=error.message.orEmpty()
  if(error is ExternalSourceException)return text
  val internal=Regex("firebase|firestore|googleapis|https?://|exception|java\\.|auth/|api[_ -]?key|token[_ -]?expired",RegexOption.IGNORE_CASE)
  val portuguese=listOf("Não ","Sua ","Seu ","Informe ","Use ","Este ","Esta ","A senha","E-mail ","Escolha ","A imagem","O arquivo","Entre ")
  return text.takeIf{it.length<=500&&!internal.containsMatchIn(it)&&portuguese.any(it::startsWith)}?:fallback
 }
}
