import sbt._
import Keys._

object BamBuild extends Build {
  
  lazy val bam = Project(
    id = "bam",
    base = file(".")
  ) aggregate (core, storageCassandra, api, example, benchmark)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = Defaults.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.core
    )
  )
                         
  lazy val storageCassandra = Project(
    id = "storage-cassandra",
    base = file("storage-cassandra"),
    settings = Defaults.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.storageCassandra
    )
  ) dependsOn (core)
                           
  lazy val api = Project(
    id = "api",
    base = file("api"),
    settings = Defaults.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.api
    )
  ) dependsOn (core, storageCassandra)
                         
  lazy val example = Project(
    id = "example",
    base = file("example"),
    settings = Defaults.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.example
    )
  ) dependsOn (core, storageCassandra, api)
                           
  lazy val benchmark = Project(
    id = "benchmark",
    base = file("benchmark"),
    settings = Defaults.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.benchmark
    )
  ) dependsOn (core)
                            
  override lazy val settings = super.settings ++ Seq(
        resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        resolvers += "Twitter Repository" at "http://maven.twttr.com/",
        resolvers += "Scala Tools" at "http://www.scala-tools.org/repo-releases/"
  )
  
}


object Dependencies {
  import Dependency._
  
  val core = Seq(akkaActor, grizzled, jacksonCore, jacksonMapper, mvel2, 
      ostrich, twitterJson, twitterUtilCore, twitterUtilEval, twitterUtilLogging,
      Test.scalatest, Test.junit, Test.mockito, Test.akkaTestkit)
  val api = Seq (jerkson, unfilteredFilter, unfilteredNetty, unfilteredNettyServer)
  val example = Seq (akkaKernel, camelCore, camelIrc, camelAtom, unfilteredNettyServer)
  val benchmark = Seq(akkaKernel, akkaRemote, netty, protobuf)
  val storageCassandra = Seq(cassandraAll, cassandraThrift, guava, hector, hectorCore, jodaConvert, jodaTime, perf4j, thrift, uuid) //highScaleLib
  
}

object Dependency {

  // Versions
  object V {
    val Akka = "2.0"
    val Camel = "2.6.0"
    val Cassandra = "1.0.6"
    val Hector = "1.0-2"
    val Jackson = "1.9.0"
    val Scalatest = "1.6.1"
    val Slf4j = "1.6.4"
    val TwitterUtil = "1.12.13"
    val Unfiltered = "0.5.3"
  }

  val akkaActor = "com.typesafe.akka" % "akka-actor" % V.Akka
  val akkaKernel = "com.typesafe.akka" % "akka-kernel" % V.Akka
  val akkaRemote = "com.typesafe.akka" % "akka-remote" % V.Akka
  val camelAtom = "org.apache.camel" % "camel-atom" % V.Camel
  val camelCore = "org.apache.camel" % "camel-core" % V.Camel
  val camelIrc = "org.apache.camel" % "camel-irc" % V.Camel
  val cassandraAll = "org.apache.cassandra" % "cassandra-all" % V.Cassandra
  val cassandraThrift = "org.apache.cassandra" % "cassandra-thrift" % V.Cassandra
  val grizzled = "org.clapper" % "grizzled-slf4j_2.9.1" % "0.6.6"
  val guava = "com.google.guava" % "guava" % "r09"
  val hector = "me.prettyprint" % "hector" % V.Hector
  val hectorCore = "me.prettyprint" % "hector-core" % V.Hector
  val highScaleLib = "org.cliffc.high_scale_lib" % "high-scale-lib" % "1.0"
  val jacksonMapper = "org.codehaus.jackson" % "jackson-mapper-asl" % V.Jackson
  val jacksonCore = "org.codehaus.jackson" % "jackson-core-asl" % V.Jackson
  val jerkson = "com.codahale" % "jerkson_2.9.1" % "0.5.0"
  val jodaConvert = "org.joda" % "joda-convert" % "1.1"
  val jodaTime = "joda-time" % "joda-time" % "2.0"
  val log4j = "log4j" % "log4j" % "1.2.14"
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
  val uuid = "com.eaio.uuid" % "uuid" % "3.2"
  
  object Test {
    val junit = "junit" % "junit" % "4.4" % "test"
    val scalatest = "org.scalatest" % "scalatest_2.9.1" % V.Scalatest % "test"
    val mockito = "org.mockito" % "mockito-core" % "1.8.1" % "test"
    val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0" % "test"
  }
  
}

  