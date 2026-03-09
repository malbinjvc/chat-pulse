val scala3Version = "3.3.4"
val http4sVersion = "0.23.30"
val circeVersion = "0.14.10"

lazy val root = project
  .in(file("."))
  .settings(
    name := "chat-pulse",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion % Test,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    javacOptions ++= Seq("--release", "21"),
    scalacOptions ++= Seq("-release", "21"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  )
