// TODO: Put all dependencies in a Dependencies.scala file with versions

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.github.walfie",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint"),
  Scalariform.settings
)

lazy val stream = (project in file("stream"))
  .settings(commonSettings: _*)
  .settings(
    name := "gbf-raidfinder-stream",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-agent" % Versions.Akka,
      "io.monix" %% "monix" % Versions.Monix,
      "org.twitter4j" % "twitter4j-stream" % Versions.Twitter4j,
      "org.scalatest" %% "scalatest" % Versions.ScalaTest % "test",
      "org.mockito" % "mockito-all" % Versions.Mockito % "test"
    )
  )

lazy val protocol = (crossProject.crossType(CrossType.Pure) in file("protocol"))
  .settings(name := "gbf-raidfinder-protocol")
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(ScalaPB.settings: _*)
lazy val protocolJVM = protocol.jvm
lazy val protocolJS = protocol.js

lazy val server = (project in file("server"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(
    name := "gbf-raidfinder-server",
    herokuSkipSubProjects in Compile := false,
    herokuAppName in Compile := "gbf-raidfinder",
    herokuProcessTypes in Compile := Map(
      "web" -> s"target/universal/stage/bin/${name.value} -Dhttp.port=$$PORT"
    ),
    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %% "scalapb-json4s" % Versions.ScalaPB_json4s,
      "com.typesafe.play" %% "play-netty-server" % Versions.Play
    )
  )
  .dependsOn(stream, protocolJVM)

val jsPath = settingKey[File]("Output directory for scala.js compiled files")
lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .settings(commonSettings: _*)
  .settings(
    name := "gbf-raidfinder-client",

    // Put output JS files in `target/scala_2.11/classes/public/js`
    jsPath := crossTarget.value / "classes" / "public" / "js",
    crossTarget in (Compile, fullOptJS) := jsPath.value,
    crossTarget in (Compile, fastOptJS) := jsPath.value,
    crossTarget in (Compile, packageJSDependencies) := jsPath.value,
    crossTarget in (Compile, packageScalaJSLauncher) := jsPath.value,
    crossTarget in (Compile, packageMinifiedJSDependencies) := jsPath.value,

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.thoughtworks.binding" %%% "dom" % "9.0.0",
      "org.webjars.npm" % "moment" % Versions.MomentJS,
      "org.webjars.bower" % "dialog-polyfill" % Versions.DialogPolyfillJS,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    ),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "moment" % Versions.MomentJS
        / s"${Versions.MomentJS}/moment.js"
        minified "min/moment.min.js",

      "org.webjars.bower" % "dialog-polyfill" % Versions.DialogPolyfillJS
        / s"${Versions.DialogPolyfillJS}/dialog-polyfill.js"
    )
  )
  .dependsOn(protocolJS)

lazy val root = (project in file("."))
  .enablePlugins(SbtWeb)
  .dependsOn(server, client)
  .settings(commonSettings: _*)
  .settings(
    name := "gbf-raidfinder",
    scalaJSProjects := Seq(client),
    mainClass in Compile := Some("walfie.gbf.raidfinder.server.Application"),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline.map(f => f(Seq.empty))
  )

