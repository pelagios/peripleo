package ingest

import java.math.BigInteger
import java.security.MessageDigest
import org.pelagios.Scalagios

abstract class AbstractImporter {
  
  private val SHA256 = "SHA-256"
    
  private val UTF8 = "UTF-8"
   
  /** Utility method that returns the RDF format corresponding to a particular file extension **/
  protected def getFormat(filename: String): String = filename match {
    case f if f.endsWith("rdf") => Scalagios.RDFXML
    case f if f.endsWith("ttl") => Scalagios.TURTLE
    case f if f.endsWith("n3") => Scalagios.N3
    case _ => throw new IllegalArgumentException("Format not supported")
  }
  
  /** Utility method that produces a SHA256 hash from a string **/
  protected def sha256(str: String): String = {
    val md = MessageDigest.getInstance(SHA256).digest(str.getBytes(UTF8))
    (new BigInteger(1, md)).toString(16)
  }

}