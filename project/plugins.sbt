ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

val artefactsUrl: String = "https://open.artefacts.tax.service.gov.uk"

resolvers += "HMRC-open-artefacts-maven" at (artefactsUrl + "/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url(artefactsUrl + "/ivy2"))(Resolver.ivyStylePatterns)
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "3.15.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.4.0")

addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")
addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")
addSbtPlugin("net.ground5hark.sbt" % "sbt-concat" % "0.2.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "2.0.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
