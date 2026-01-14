import sbt._

object AppDependencies {
  private val playVersion = "play-30"
  private val bootstrapVersion = "10.5.0"
  private val hmrcMongoVersion = "2.11.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"            %% s"play-frontend-hmrc-$playVersion"            % "12.25.0",
    "uk.gov.hmrc"            %% s"play-conditional-form-mapping-$playVersion" % "3.4.0",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion"                    % hmrcMongoVersion,
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox"                        % "1.1.24",
    "org.typelevel"          %% "cats-core"                                   % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.scalacheck"         %% "scalacheck"                    % "1.18.0",
    "org.scalatestplus"      %% "scalacheck-1-17"               % "3.2.18.0",
    "org.scalatestplus"      %% "mockito-4-11"                  % "3.2.18.0",
    "org.jsoup"               % "jsoup"                         % "1.18.1",
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.64.8",
    "com.softwaremill.diffx" %% "diffx-scalatest-should"        % "0.9.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
