name := """water-network-sim"""

version := "1.0"

scalaVersion := "2.11.1"

resolvers += Resolver.sonatypeRepo("public")

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

seq(bintrayResolverSettings:_*)

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.0.2"

libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.2"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.squants" %% "squants" % "0.4.2"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0"

libraryDependencies += "com.norbitltd" % "spoiwo" % "1.0.3"


