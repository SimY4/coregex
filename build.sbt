ThisBuild / name := "coregex-parent"
ThisBuild / organization := "com.github.simy4.coregex"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.5"

val core = (project in file("core"))
  .settings(
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "com.pholser" % "junit-quickcheck-core" % "1.0" % Test,
      "com.pholser" % "junit-quickcheck-generators" % "1.0" % Test,
      "junit" % "junit" % "4.13.2" % Test
    )
  )