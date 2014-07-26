import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "census"
  val appVersion      = "0.2.0"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.github.nscala-time" %% "nscala-time" % "1.0.0"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
