name := "pelagios-api-v3"

version := "0.0.1"

play.Project.playScalaSettings

libraryDependencies ++= Seq(jdbc, cache)   

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.8.1",
  "org.apache.lucene" % "lucene-queryparser" % "4.8.1",
  "org.apache.lucene" % "lucene-facet" % "4.8.1",
  "org.apache.lucene" % "lucene-spatial" % "4.8.1",
  "com.vividsolutions" % "jts" % "1.13",
  "org.geotools" % "gt-geojson" % "10.0",
  "org.xerial" % "sqlite-jdbc" % "3.7.2"
)  

/** Transient dependencies required by Scalagios
  *
  * TODO: remove once Scalagios is included as managed dependency!
  */
libraryDependencies ++= Seq(
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.7"
)   

