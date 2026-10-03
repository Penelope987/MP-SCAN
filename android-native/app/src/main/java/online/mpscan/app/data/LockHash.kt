package online.mpscan.app.data
object LockHash {
 fun encode(value:String,salt:String):String {
  val spec=javax.crypto.spec.PBEKeySpec(value.toCharArray(),salt.toByteArray(Charsets.UTF_8),120000,256)
  return try{javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.joinToString(""){"%02x".format(it)}}finally{spec.clearPassword()}
 }
}
