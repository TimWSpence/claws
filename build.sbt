ThisBuild / baseVersion := "0.11"

ThisBuild / organization := "io.github.timwspence"
ThisBuild / organizationName := "TimWSpence"
ThisBuild / startYear := Some(2021)
ThisBuild / endYear := Some(2021)
publishGithubUser in ThisBuild := "TimWSpence"
publishFullName in ThisBuild := "Tim Spence"

ThisBuild / developers := List(
  Developer("TimWSpence", "Tim Spence", "@TimWSpence", url("https://github.com/TimWSpence"))
)

val PrimaryOS = "ubuntu-latest"

val Scala3 = "3.0.2"

ThisBuild / crossScalaVersions := Seq(Scala3)

val LTSJava = "adopt@1.11"
val LatestJava = "adopt@1.15"
val GraalVM8 = "graalvm-ce-java8@20.2.0"

ThisBuild / githubWorkflowJavaVersions := Seq(LTSJava, LatestJava, GraalVM8)
ThisBuild / githubWorkflowOSes := Seq(PrimaryOS)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("${{ matrix.ci }}")),

  // WorkflowStep.Sbt(
  //   List("docs/mdoc"),
  //   cond = Some(s"matrix.scala == '$Scala3' && matrix.ci == 'ciJVM'")),
)

ThisBuild / githubWorkflowBuildMatrixAdditions += "ci" -> List("ciJVM")

ThisBuild / homepage := Some(url("https://github.com/TimWSpence/claws"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/TimWSpence/claws"),
    "git@github.com:TimWSpence/claws.git"))

addCommandAlias("ciJVM", "; project claws; headerCheck; scalafmtCheck; clean; test; core/mimaReportBinaryIssues")

addCommandAlias("prePR", "; project `claws`; clean; scalafmtAll; headerCreate")

val CatsVersion = "2.6.1"
val CatsEffectVersion = "3.2.9"
val DisciplineVersion = "1.0.9"
val ScalaCheckVersion = "1.15.4"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.5"
val ScalacheckEffectVersion = "1.0.2"

lazy val `claws` = project.in(file("."))
  .settings(commonSettings)
  .aggregate(
    core
  )
  .settings(noPublishSettings)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "claws",
  )
  .settings(initialCommands in console := """
    import cats._
    import cats.implicits._
    import cats.effect._
    import cats.effect.implicits._
    import cats.effect.unsafe.implicits.global
    """
  )

lazy val commonSettings = Seq(
  organizationHomepage := Some(url("https://github.com/TimWSpence")),
  libraryDependencies ++= Seq(
    "org.typelevel"              %% "cats-effect"               % CatsEffectVersion,
    "org.typelevel"              %% "cats-core"                 % CatsVersion,
    "org.scalacheck"             %% "scalacheck"                % ScalaCheckVersion % Test,
    "org.scalameta"              %% "munit"                     % MunitVersion % Test,
    "org.scalameta"              %% "munit-scalacheck"          % MunitVersion % Test,
    "org.typelevel"              %% "scalacheck-effect-munit"   % ScalacheckEffectVersion % Test,
    "org.typelevel"              %% "munit-cats-effect-3"       % MunitCatsEffectVersion % Test
  ),
  scalacOptions := scalacOptions.value.filterNot(_ == "-source:3.0-migration") :+ "-source:future",
  scalacOptions ++= Seq("-Ykind-projector"),
)

lazy val skipOnPublishSettings = Seq(
  skip in publish := true,
  publish := (()),
  publishLocal := (()),
  publishArtifact := false,
  publishTo := None
)
