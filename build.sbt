
name := """budget"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
  "org.apache.httpcomponents" % "httpcore" % "4.4.5",
  "oauth.signpost" % "signpost-core" % "1.2.1.2",
  "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2",
  "joda-time" % "joda-time" % "2.9.4",
  "org.scalatestplus.play"   %%  "scalatestplus-play"   % "1.5.1" % Test,
  "org.scalatest" %% "scalatest" % "2.2.5" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Signpost releases" at "https://oss.sonatype.org/content/repositories/signpost-releases/"
resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

fork in run := false
