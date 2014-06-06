package controllers.common.io

import java.math.BigInteger
import java.security.MessageDigest
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.UnsupportedRDFormatException

abstract class AbstractImporter {
  
  private val MD5 = "MD5"
   
  /** Utility method that returns the RDF format corresponding to a particular file extension **/
  protected def getFormat(filename: String): RDFFormat = filename match {
    case f if f.endsWith("rdf") => RDFFormat.RDFXML
    case f if f.endsWith("ttl") => RDFFormat.TURTLE
    case f if f.endsWith("n3") => RDFFormat.N3
    case _ => throw new UnsupportedRDFormatException("Format not supported")
  }
  
  /** Utility method that produces a SHA256 hash from a string **/
  protected def sha256(str: String): String = {
    val md = MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8"))
    new BigInteger(1, md).toString(16)
  }

}