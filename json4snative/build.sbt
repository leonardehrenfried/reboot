name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

Seq(lsSettings :_*)

val json4sVersion = "3.4.2"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % json4sVersion,
  "org.json4s" %% "json4s-native" % json4sVersion,
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.9.0-beta2" % "test"
)
