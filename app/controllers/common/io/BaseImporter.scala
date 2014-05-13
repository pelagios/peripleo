package controllers.common.io

import java.math.BigInteger
import java.security.MessageDigest
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.UnsupportedRDFormatException

abstract class BaseImporter {
  
  private val MD5 = "MD5"
    
  protected def getFormat(filename: String): RDFFormat = filename match {
    case f if f.endsWith("rdf") => RDFFormat.RDFXML
    case f if f.endsWith("ttl") => RDFFormat.TURTLE
    case f if f.endsWith("n3") => RDFFormat.N3
    case _ => throw new UnsupportedRDFormatException("Format not supported")
  }
  
  /** Utility method that produces an MD5 hash from a string **/
  protected def md5(str: String): String = {
    val md = MessageDigest.getInstance(MD5).digest(str.getBytes())
    new BigInteger(1, md).toString(16)
  }

}