import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = Seq(
    scalaVersion := "2.12.0",
    organization := "com.github.angiolep",
    version := "0.13.0",
    description := "WAMP - Web Application Messaging Protocol implementation written in Scala/Java8 with Akka HTTP"
  )

  val examples: Seq[Setting[_]] = settings ++ Seq(
    // scalacOptions += "-Ymacro-debug-lite",
    scalaSource in Compile := baseDirectory.value,
    resourceDirectory in Compile := baseDirectory.value,
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )
}