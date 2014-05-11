package controllers.common.io

import java.security.MessageDigest
import java.math.BigInteger

abstract class BaseImporter {
  
  private val MD5 = "MD5"
  
  /** Utility method that produces an MD5 hash from a string **/
  protected def md5(str: String): String = {
    val md = MessageDigest.getInstance(MD5).digest(str.getBytes())
    new BigInteger(1, md).toString(16)
  }

}