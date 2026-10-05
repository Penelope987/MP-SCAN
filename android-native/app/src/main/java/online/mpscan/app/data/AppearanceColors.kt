package online.mpscan.app.data

import kotlin.math.pow
object AppearanceColors {
 fun valid(value:String?)=value!=null&&Regex("#[0-9a-fA-F]{6}").matches(value)
 fun luminance(hex:String):Double {
  require(valid(hex))
  fun channel(start:Int):Double {val value=hex.substring(start,start+2).toInt(16)/255.0;return if(value<=.04045)value/12.92 else ((value+.055)/1.055).pow(2.4)}
  return .2126*channel(1)+.7152*channel(3)+.0722*channel(5)
 }
 fun darkText(hex:String)=luminance(hex)>.179
}
