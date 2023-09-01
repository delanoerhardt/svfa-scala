scalaVersion := "2.12.17"

name := "svfa-scala"
organization := "br.unb.cic"

version := "0.1.0-SNAPSHOT"

githubOwner := "delanoerhardt"
githubRepository := "svfa-scala"
githubTokenSource := TokenSource.GitConfig("github.token")

parallelExecution in Test := false

resolvers += "soot snapshots" at "https://soot-build.cs.uni-paderborn.de/nexus/repository/soot-snapshot/"

resolvers += "soot releases" at "https://soot-build.cs.uni-paderborn.de/nexus/repository/soot-release/"

resolvers += "Local maven repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

resolvers += "Google repository" at "https://maven.google.com/"

resolvers += Classpaths.typesafeReleases

libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.0"

libraryDependencies += "com.android.tools" % "r8" % "8.0.40" % "runtime"

libraryDependencies += "org.soot-oss" % "sootup.core" % "1.1.0"
libraryDependencies += "org.soot-oss" % "sootup.java.bytecode" % "1.1.2"
libraryDependencies += "org.soot-oss" % "sootup.java.core" % "1.1.0"
libraryDependencies += "org.soot-oss" % "sootup.java.sourcecode" % "1.1.0"
libraryDependencies += "org.soot-oss" % "sootup.jimple.parser" % "1.1.0"
libraryDependencies += "org.soot-oss" % "sootup.callgraph" % "1.1.0"
libraryDependencies += "org.soot-oss" % "sootup.analysis" % "1.1.0"

libraryDependencies += "com.google.guava" % "guava" % "27.1-jre"
libraryDependencies += "org.scala-graph" %% "graph-core" % "1.13.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "2.0.7" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

parallelExecution in Test := false
