import sbt._

object AppDependencies {
  import play.core.PlayVersion
  private val bootstrapVersion = "7.19.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.14.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % "1.3.0",
    "org.julienrf"      %% "play-json-derived-codecs"      % "10.1.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "com.openhtmltopdf"  % "openhtmltopdf-pdfbox"          % "1.0.10"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion,
    "org.scalatest"          %% "scalatest"               % "3.2.16",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             % "pegdown"                 % "1.6.0",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
    "org.mockito"            %% "mockito-scala"           % "1.16.42",
    "org.scalacheck"         %% "scalacheck"              % "1.17.0",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "1.3.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8",
    "com.softwaremill.diffx" %% "diffx-scalatest-should"  % "0.8.3",
    "org.jsoup"               % "jsoup"                   % "1.16.1"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
