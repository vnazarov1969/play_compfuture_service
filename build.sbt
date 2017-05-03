
name := """just-play-java"""

version := "1.0-SNAPSHOT"


lazy val root = (project in file(".")).enablePlugins(PlayJava)


scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaWs
)

//fork in run := true