/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object BamBuild extends Build {
  
  val Organization = "org.evactor"
  val Version      = "0.2-SNAPSHOT"
  val ScalaVersion = "2.9.2"

  lazy val evactor = Project(
    id = "evactor",
    base = file(".")
  ) aggregate (core, storageCassandra, api, example)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.core
    )
  )
                         
  lazy val storageCassandra = Project(
    id = "storage-cassandra",
    base = file("storage-cassandra"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.storageCassandra
    )
  ) dependsOn (core)
                           
  lazy val api = Project(
    id = "api",
    base = file("api"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.api
    )
  ) dependsOn (core)
	
  lazy val monitoringOstrich = Project(
    id = "monitoring-ostrich",
    base = file("monitoring-ostrich"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.monitoringOstrich
    )
  ) dependsOn (core)
                     
  lazy val example = Project(
    id = "example",
    base = file("example"),
    settings = defaultSettings ++ exampleAssemblySettings ++ Seq(
      libraryDependencies ++= Dependencies.example
    )
  ) dependsOn (core, storageCassandra, api, monitoringOstrich)
	
  override lazy val settings = super.settings ++ Seq(
        resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        resolvers += "Typesafe Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
        resolvers += "Twitter Repository" at "http://maven.twttr.com/",
        resolvers += "Scala Tools" at "http://www.scala-tools.org/repo-releases/"
  )
  
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version      := Version,
    scalaVersion := ScalaVersion,
    crossPaths   := false,
    publishTo	   := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
  ) 

  lazy val defaultSettings = buildSettings ++ Seq(
	  scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
	  javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
  )  

  lazy val exampleAssemblySettings = assemblySettings ++ Seq(
    test in assembly := {},
	excludedJars in assembly <<= (fullClasspath in assembly) map { cp => 
	  cp filter { x => x.data.getName == "uuid-3.2.0.jar" ||  x.data.getName == "slf4j-log4j12-1.6.1.jar" || x.data.getName == "log4j-1.2.16.jar" }},
	excludedFiles in assembly := { (bases: Seq[File]) =>
	  bases flatMap { base =>
        (base / "META-INF" * "*").get collect {
          case f if f.getName.toLowerCase == "license" => f
          case f if f.getName.toLowerCase == "license.txt" => f
          case f if f.getName.toLowerCase == "manifest.mf" => f
          case f if f.getName.toLowerCase == "notice.txt" => f
          case f if f.getName.toLowerCase == "notice" => f
	    }
	  }
	},
	mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
	  {
	    case "application.conf" => MergeStrategy.concat
	    case x => old(x)
	  }
	},
	mainClass in assembly := Some("org.evactor.ExampleKernel"),
	jarName in assembly := "evactorExample.jar"
  )
}


object Dependencies {
  import Dependency._

  val core = Seq(akkaActor, akkaCamel, camelCore, jacksonScala, activemq, camelJms, jacksonCore, jacksonMapper, mvel2, Test.scalatest, Test.junit, Test.mockito, Test.akkaTestkit)
  val api = Seq (grizzled, jacksonCore, jacksonMapper, jacksonScala, unfilteredFilter, unfilteredNetty, unfilteredNettyServer)
  val example = Seq (akkaKernel, akkaSlf4j, httpClient, logback, Test.scalatest, Test.junit, Test.akkaTestkit)
  val storageCassandra = Seq(akkaActor, cassandraThrift, grizzled, guava, hectorCore, jodaConvert, jodaTime, thrift, Test.scalatest, Test.junit)
  val monitoringOstrich = Seq(ostrich)

}

object Dependency {

  // Versions
  object V {
    val Akka = "2.1-SNAPSHOT"
    val Camel = "2.6.0"
    val Cassandra = "1.0.6"
    val Hector = "1.0-2"
    val Jackson = "1.9.0"
    val Scalatest = "1.6.1"
    val Slf4j = "1.6.4"
    val TwitterUtil = "1.12.13"
    val Unfiltered = "0.5.3"
  }

  val activemq = "org.apache.activemq" % "activemq-core" % "5.1.0"
  val camelJms = "org.apache.camel" % "camel-jms" % V.Camel

  val akkaActor = "com.typesafe.akka" % "akka-actor" % V.Akka
  val akkaKernel = "com.typesafe.akka" % "akka-kernel" % V.Akka
  val akkaRemote = "com.typesafe.akka" % "akka-remote" % V.Akka
  val akkaSlf4j = "com.typesafe.akka" % "akka-slf4j" % V.Akka
  val akkaCamel = "com.typesafe.akka" % "akka-camel" % V.Akka
  val camelAtom = "org.apache.camel" % "camel-atom" % V.Camel
  val camelCore = "org.apache.camel" % "camel-core" % V.Camel
  val camelIrc = "org.apache.camel" % "camel-irc" % V.Camel
  val cassandraAll = "org.apache.cassandra" % "cassandra-all" % V.Cassandra
  val cassandraThrift = "org.apache.cassandra" % "cassandra-thrift" % V.Cassandra
  val grizzled = "org.clapper" % "grizzled-slf4j_2.9.1" % "0.6.6"
  val groovy = "org.codehaus.groovy" % "groovy" % "1.8.6" % "runtime"
  val guava = "com.google.guava" % "guava" % "r09"
  val hector = "me.prettyprint" % "hector" % V.Hector
  val hectorCore = "me.prettyprint" % "hector-core" % V.Hector
  val highScaleLib = "org.cliffc.high_scale_lib" % "high-scale-lib" % "1.0"
  val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.1"
  val jacksonMapper = "org.codehaus.jackson" % "jackson-mapper-asl" % V.Jackson
  val jacksonCore = "org.codehaus.jackson" % "jackson-core-asl" % V.Jackson
  val jacksonScala = "com.fasterxml.jackson.module" % "jackson-module-scala" % "2.0.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.1"
  val jodaTime = "joda-time" % "joda-time" % "2.0"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime"
  val mvel2 = "org.mvel" % "mvel2" % "2.0.9"
  val netty = "io.netty" % "netty" % "3.3.0.Final"
  val ostrich = "com.twitter" % "ostrich_2.9.1" % "4.10.6"
  val perf4j = "org.perf4j" % "perf4j" % "0.9.14"
  val protobuf = "com.google.protobuf" % "protobuf-java" % "2.4.1"
  val slf4jApi = "org.slf4j" % "slf4j-api" % V.Slf4j
  val thrift = "org.apache.thrift" % "libthrift" % "0.6.1"
  val twitterJson = "com.twitter" % "json_2.9.1" % "2.1.7"
  val twitterUtilCore = "com.twitter" % "util-core_2.9.1" % V.TwitterUtil
  val twitterUtilEval = "com.twitter" % "util-eval_2.9.1" % V.TwitterUtil
  val twitterUtilLogging = "com.twitter" % "util-logging_2.9.1" % V.TwitterUtil
  val unfilteredFilter = "net.databinder" % "unfiltered-filter_2.9.1" % V.Unfiltered
  val unfilteredNetty = "net.databinder" % "unfiltered-netty_2.9.1" % V.Unfiltered
  val unfilteredNettyServer = "net.databinder" % "unfiltered-netty-server_2.9.1" % V.Unfiltered

  object Test {
    val junit = "junit" % "junit" % "4.4" % "test"
    val scalatest = "org.scalatest" % "scalatest_2.9.1" % V.Scalatest % "test"
    val mockito = "org.mockito" % "mockito-core" % "1.8.1" % "test"
    val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0" % "test"
  }
  
}

  
