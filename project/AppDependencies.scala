import sbt._

object AppDependencies {
  private val playVersion = "play-30"
  private val bootstrapVersion = "8.2.0"
  private val hmrcMongoVersion = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVersion"            % "8.5.0",
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"                    % hmrcMongoVersion,
    "com.openhtmltopdf"  % "openhtmltopdf-pdfbox"                        % "1.0.10",
    "org.julienrf"      %% "play-json-derived-codecs"                    % "10.1.0",
    "org.typelevel"     %% "cats-core"                                   % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.scalacheck"         %% "scalacheck"                    % "1.17.0",
    "org.scalatestplus"      %% "scalacheck-1-17"               % "3.2.17.0",
    "org.mockito"            %% "mockito-scala"                 % "1.17.30",
    "org.scalatestplus"      %% "mockito-4-11"                  % "3.2.17.0",
    "org.jsoup"               % "jsoup"                         % "1.17.1",
    "org.pegdown"             % "pegdown"                       % "1.6.0",
    "com.softwaremill.diffx" %% "diffx-scalatest-should"        % "0.9.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
