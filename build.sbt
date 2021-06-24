ThisBuild / organization := "com.github.simy4.coregex"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .settings(
    name := "coregex-parent",
  )
  .dependsOn(core, junitQuickcheck)
  .aggregate(core, junitQuickcheck)

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    moduleName := "coregex-core",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "com.pholser" % "junit-quickcheck-core" % "1.0" % Test,
      "com.pholser" % "junit-quickcheck-generators" % "1.0" % Test,
      "junit" % "junit" % "4.13.2" % Test
    )
  )

lazy val junitQuickcheck = (project in file("junit-quickcheck"))
  .settings(
    name := "junit-quickcheck",
    moduleName := "coregex-junit-quickcheck",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "com.pholser" % "junit-quickcheck-core" % "1.0" % Provided,
      "junit" % "junit" % "4.13.2" % Test
    )
  )
  .dependsOn(core)