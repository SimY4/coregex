ThisBuild / organization     := "com.github.simy4.coregex"
ThisBuild / organizationName := "Alex Simkin"
ThisBuild / homepage         := Some(url("https://github.com/SimY4/coregex"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/SimY4/coregex"),
    "scm:git@github.com:SimY4/coregex.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "SimY4",
    name = "Alex Simkin",
    email = null,
    url = new URL("https://github.com/SimY4")
  )
)

lazy val scala213               = "2.13.9"
lazy val scala3                 = "3.2.0"
lazy val supportedScalaVersions = List(scala213, scala3)

ThisBuild / scalaVersion := scala213
ThisBuild / startYear    := Some(2021)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / versionScheme := Some("early-semver")

releaseTagComment        := s"[sbt release] - releasing ${(ThisBuild / version).value}"
releaseCommitMessage     := s"[sbt release] - setting version to ${(ThisBuild / version).value}"
releaseNextCommitMessage := s"[skip ci][sbt release] - new version commit: ${(ThisBuild / version).value}"
sonatypeProfileName      := "com.github.simy4"

lazy val root = (project in file("."))
  .settings(
    name               := "coregex-parent",
    crossScalaVersions := Nil,
    publish / skip     := true
  )
  .aggregate(core, jqwik, junitQuickcheck, scalacheck)

lazy val core = (project in file("core"))
  .settings(
    name             := "core",
    moduleName       := "coregex-core",
    crossPaths       := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq("org.scalacheck" %% "scalacheck" % "1.17.0" % Test),
    crossScalaVersions := supportedScalaVersions,
    Compile / compile / javacOptions ++= Seq("-Xlint:all", "-Werror") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8")
       else Seq("-source", "1.8", "-target", "1.8")),
    Compile / doc / javacOptions ++= Seq("-Xdoclint:all,-missing") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8", "-html5")
       else Seq("-source", "1.8", "-target", "1.8"))
  )

lazy val jqwik = (project in file("jqwik"))
  .settings(
    name             := "jqwik",
    moduleName       := "coregex-jqwik",
    crossPaths       := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "net.jqwik"   % "jqwik-api"         % "1.7.0"  % Provided,
      "net.jqwik"   % "jqwik-engine"      % "1.7.0"  % Test,
      "net.aichler" % "jupiter-interface" % "0.11.1" % Test
    ),
    crossScalaVersions := supportedScalaVersions,
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v"),
    Compile / compile / javacOptions ++= Seq("-Xlint:all", "-Werror") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8")
       else Seq("-source", "1.8", "-target", "1.8")),
    Compile / doc / javacOptions ++= Seq("-Xdoclint:all,-missing") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8", "-html5")
       else Seq("-source", "1.8", "-target", "1.8"))
  )
  .dependsOn(core)

lazy val junitQuickcheck = (project in file("junit-quickcheck"))
  .settings(
    name             := "junit-quickcheck",
    moduleName       := "coregex-junit-quickcheck",
    crossPaths       := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "com.pholser"    % "junit-quickcheck-core"       % "1.0"    % Provided,
      "com.pholser"    % "junit-quickcheck-generators" % "1.0"    % Test,
      "junit"          % "junit"                       % "4.13.2" % Test,
      "org.slf4j"      % "slf4j-simple"                % "1.7.25" % Test,
      "com.github.sbt" % "junit-interface"             % "0.13.3" % Test
    ),
    crossScalaVersions := supportedScalaVersions,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v"),
    Compile / compile / javacOptions ++= Seq("-Xlint:all", "-Werror") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8")
       else Seq("-source", "1.8", "-target", "1.8")),
    Compile / doc / javacOptions ++= Seq("-Xdoclint:all,-missing") ++
      (if (scala.util.Properties.isJavaAtLeast("9")) Seq("--release", "8", "-html5")
       else Seq("-source", "1.8", "-target", "1.8"))
  )
  .dependsOn(core)

lazy val scalacheck = (project in file("scalacheck"))
  .settings(
    name       := "scalacheck",
    moduleName := "coregex-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Provided
    ),
    crossScalaVersions := supportedScalaVersions
  )
  .dependsOn(core)

addCommandAlias("build", ";javafmtCheckAll;scalafmtCheckAll;headerCheck;test")
