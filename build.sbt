ThisBuild / organization := "com.github.simy4.coregex"
ThisBuild / organizationName := "Alex Simkin"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val scala212 = "2.12.14"
lazy val scala213 = "2.13.6"
lazy val scala3 = "3.0.0"
lazy val supportedScalaVersions = List(scala212, scala213, scala3)

ThisBuild / scalaVersion := scala213
ThisBuild / startYear := Some(2021)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

lazy val root = (project in file("."))
  .settings(
    name := "coregex-parent",
    crossScalaVersions := Nil,
    publish / skip := true
  )
  .aggregate(core, junitQuickcheck, scalacheck)

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
    ),
    crossScalaVersions := Nil
  )

lazy val junitQuickcheck = (project in file("junit-quickcheck"))
  .settings(
    name := "junit-quickcheck",
    moduleName := "coregex-junit-quickcheck",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "com.pholser" % "junit-quickcheck-core" % "1.0" % Provided,
      "com.pholser" % "junit-quickcheck-generators" % "1.0" % Test,
      "junit" % "junit" % "4.13.2" % Test
    ),
    crossScalaVersions := Nil
  )
  .dependsOn(core)

lazy val scalacheck = (project in file("scalacheck"))
  .settings(
    name := "scalacheck",
    moduleName := "coregex-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.15.4" % Provided
    ),
    crossScalaVersions := supportedScalaVersions
  )
  .dependsOn(core)