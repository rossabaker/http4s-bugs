scalaVersion := "2.12.8"

libraryDependencies ++= List(
  "org.http4s" %% "http4s-blaze-client" % "0.21.0-M3",
  "org.http4s" %% "http4s-async-http-client" % "0.21.0-M3",
  "org.http4s" %% "http4s-circe" % "0.21.0-M3",
  "io.monix" %% "monix-eval" % "3.0.0-RC3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)

fork in run := true
