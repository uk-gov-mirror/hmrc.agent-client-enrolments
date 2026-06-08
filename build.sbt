import uk.gov.hmrc.DefaultBuildSettings

val appName = "agent-client-enrolments"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"

Test / parallelExecution := false

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(ScoverageSettings())
  .settings(scalafmtOnCompile := true)
  .settings(scalacOptions += "-Wconf:src=routes/.*:s")
  .settings(PlayKeys.playDefaultPort := 9456)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    Compile / packageDoc / publishArtifact := false,
    Compile / doc / sources := Seq.empty
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
