name := "wordbots-parser"
version := "0.10.0-alpha-SNAPSHOT" // wordbots-parser versions should correspond to the current wordbots-core release.

val http4sVersion = "0.15.3"
val circeVersion = "0.6.1"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.log4s" %% "log4s" % "1.3.3",
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion
)

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

mainClass in Compile := Some("wordbots.WordbotsServer")

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "wordbots"
