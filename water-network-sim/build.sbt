name := """water-network-sim"""

version := "1.0"

scalaVersion := "2.11.1"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

seq(bintrayResolverSettings:_*)

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.0.2"

libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.2"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.squants" %% "squants" % "0.4.2"
