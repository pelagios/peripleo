name := "pelagios-api-v3"

version := "0.0.1"

play.Project.playScalaSettings

libraryDependencies ++= Seq(jdbc, cache)   

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.6.0.1"
)     

