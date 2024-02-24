inThisBuild(
  Seq(
    organization     := "com.github.simy4.coregex",
    organizationName := "Alex Simkin",
    homepage         := Some(url("https://github.com/SimY4/coregex")),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/SimY4/coregex"),
        "scm:git@github.com:SimY4/coregex.git"
      )
    ),
    developers := List(
      Developer(
        id = "SimY4",
        name = "Alex Simkin",
        email = null,
        url = url("https://github.com/SimY4")
      )
    ),
    releaseNotesURL := Some(url("https://github.com/SimY4/coregex/releases")),
    versionScheme   := Some("early-semver"),
    startYear       := Some(2021)
  )
)

lazy val scala213               = "2.13.13"
lazy val scala3                 = "3.3.1"
lazy val supportedScalaVersions = List(scala213, scala3)

def javaLibSettings(release: Int) = Seq(
  crossPaths       := false,
  autoScalaLibrary := false,
  Compile / compile / javacOptions ++= Seq("-Xlint:all", "-Werror") ++
    (if (9 <= release || scala.util.Properties.isJavaAtLeast("9")) Seq("--release", release.toString)
     else Seq("-source", "1.8", "-target", "1.8")),
  Compile / doc / javacOptions ++= Seq("-Xdoclint:all,-missing") ++
    (if (9 <= release || scala.util.Properties.isJavaAtLeast("9")) Seq("--release", release.toString, "-html5")
     else Seq("-source", "1.8", "-target", "1.8"))
)
lazy val jacocoSettings = Test / jacocoReportSettings := JacocoReportSettings(
  "Jacoco Coverage Report",
  None,
  JacocoThresholds(
    line = 50
  ),
  Seq(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML), // note XML formatter
  "utf-8"
)

ThisBuild / scalaVersion := scala213

releaseTagComment        := s"[sbt release] - releasing ${(ThisBuild / version).value}"
releaseCommitMessage     := s"[sbt release] - setting version to ${(ThisBuild / version).value}"
releaseNextCommitMessage := s"[skip ci][sbt release] - new version commit: ${(ThisBuild / version).value}"
sonatypeProfileName      := "com.github.simy4"

lazy val root = (project in file("."))
  .settings(
    name           := "coregex-parent",
    publish / skip := true
  )
  .aggregate(core, jqwik, junitQuickcheck, kotest, scalacheck, vavrTest)

lazy val core = (project in file("core"))
  .settings(
    name          := "core",
    moduleName    := "coregex-core",
    description   := "A handy utility for generating strings that match given regular expression criteria.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"            % "0.7.29" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test
    )
  )
  .settings(javaLibSettings(8))
  .settings(jacocoSettings)

lazy val jqwik = (project in file("jqwik"))
  .settings(
    name          := "jqwik",
    moduleName    := "coregex-jqwik",
    description   := "JQwik bindings for coregex library.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "net.jqwik"   % "jqwik-api"         % "1.8.3"  % Provided,
      "net.jqwik"   % "jqwik-engine"      % "1.8.3"  % Test,
      "net.jqwik"   % "jqwik-testing"     % "1.8.3"  % Test,
      "net.aichler" % "jupiter-interface" % "0.11.1" % Test
    ),
    Test / parallelExecution := false,
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v")
  )
  .settings(javaLibSettings(8))
  .settings(jacocoSettings)
  .dependsOn(core)

lazy val junitQuickcheck = (project in file("junit-quickcheck"))
  .settings(
    name          := "junit-quickcheck",
    moduleName    := "coregex-junit-quickcheck",
    description   := "JUnit Quickcheck bindings for coregex library.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "com.pholser"    % "junit-quickcheck-core"       % "1.0"    % Provided,
      "com.pholser"    % "junit-quickcheck-generators" % "1.0"    % Test,
      "junit"          % "junit"                       % "4.13.2" % Test,
      "org.slf4j"      % "slf4j-simple"                % "1.7.25" % Test,
      "com.github.sbt" % "junit-interface"             % "0.13.3" % Test
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
  )
  .settings(javaLibSettings(8))
  .settings(jacocoSettings)
  .dependsOn(core)

lazy val kotest = (project in file("kotest"))
  .settings(
    name          := "kotest",
    moduleName    := "coregex-kotest",
    description   := "Kotest bindings for coregex library.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "io.kotest"   % "kotest-property-jvm" % "5.8.0"  % Provided,
      "net.aichler" % "jupiter-interface"   % "0.11.1" % Test
    ),
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v")
  )
  .settings(javaLibSettings(8))
  .settings(jacocoSettings)
  .dependsOn(core)

lazy val scalacheck = (project in file("scalacheck"))
  .settings(
    name          := "scalacheck",
    moduleName    := "coregex-scalacheck",
    description   := "ScalaCheck bindings for coregex library.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Provided
    ),
    crossScalaVersions := supportedScalaVersions,
    Test / tpolecatExcludeOptions += org.typelevel.scalacoptions.ScalacOptions.warnNonUnitStatement
  )
  .settings(jacocoSettings)
  .dependsOn(core)

lazy val vavrTest = (project in file("vavr-test"))
  .settings(
    name          := "vavr-test",
    moduleName    := "coregex-vavr-test",
    description   := "VAVR Test bindings for coregex library.",
    headerEndYear := Some(2023),
    libraryDependencies ++= Seq(
      "io.vavr"     % "vavr-test"         % "0.10.4" % Provided,
      "net.aichler" % "jupiter-interface" % "0.11.1" % Test
    ),
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v")
  )
  .settings(javaLibSettings(8))
  .settings(jacocoSettings)
  .dependsOn(core)

addCommandAlias("build", ";javafmtCheckAll;scalafmtCheckAll;headerCheck;jacoco")
addCommandAlias("fmt", ";javafmtAll;scalafmtAll;scalafmtSbt;headerCreate")
