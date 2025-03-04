import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

val defaultVersions = Map(
  "chisel3" -> "3.6.0",
  "chisel3-plugin" -> "3.6.0"
)

def getVersion(dep: String, org: String = "edu.berkeley.cs", cross: Boolean = false) = {
  val version = sys.env.getOrElse(dep + "Version", defaultVersions(dep))
  if (cross)
    ivy"$org:::$dep:$version"
  else
    ivy"$org::$dep:$version"
}

/** The reason to split build.sc to two file is
  * [[CommonRocketChip]] doesn't need to import `$file.chisel3` and `$file.firrtl`.
  *
  * You can extends from [[CommonRocketChip]] to use rocket-chip as build-from-source dependency.
  * When doing this, you may like to override `chisel3Module`, `hardfloatModule`, `cdeModule`,
  * setting them to your favorite commit of those packages.
  *
  * If you don't override `chisel3Module`, which will default to be `None`,
  * and mill will automatically use chisel3 from ivy.
  */
trait CommonRocketChip extends SbtModule with PublishModule {
  m =>

  object macros extends SbtModule with PublishModule {
    override def scalaVersion = T {
      m.scalaVersion()
    }

    override def ivyDeps = T {
      m.ivyDeps()
    }

    override def pomSettings = T {
      m.pomSettings()
    }

    override def publishVersion = T {
      m.publishVersion()
    }

    override def repositories = super.repositories ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
      MavenRepository("https://oss.sonatype.org/content/repositories/releases")
    )
  }

  object test extends Tests {
    override def scalacPluginClasspath = m.scalacPluginClasspath

    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest:3.2.0"
    )

    def testFrameworks = T {
      Seq("org.scalatest.tools.Framework")
    }
  }

  override def millSourcePath = super.millSourcePath / os.up

  def chisel3Module: Option[PublishModule] = None

  def hardfloatModule: PublishModule

  def cdeModule: PublishModule

  def chisel3IvyDeps = if (chisel3Module.isEmpty) Agg(
    getVersion("chisel3")
  ) else Agg.empty[Dep]

  override def mainClass = T {
    Some("freechips.rocketchip.diplomacy.Main")
  }

  override def moduleDeps = Seq(macros) ++ chisel3Module :+ hardfloatModule :+ cdeModule

  override def scalacOptions = T {
    Seq("-deprecation", "-unchecked")
  }

  override def ivyDeps = T {
    Agg(
      ivy"${scalaOrganization()}:scala-reflect:${scalaVersion()}",
      ivy"org.json4s::json4s-jackson:4.0.5",
      ivy"org.scalatest::scalatest:3.2.0",
      ivy"com.lihaoyi::mainargs:0.5.0"
    ) ++ chisel3IvyDeps
  }

  private val chisel3Plugin = getVersion("chisel3-plugin", cross = true)

  override def repositories = super.repositories ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("https://oss.sonatype.org/content/repositories/releases")
  )

  override def scalacPluginIvyDeps = if(chisel3Module.isEmpty) Agg(chisel3Plugin) else Agg.empty[Dep]

  def publishVersion = T {
    "1.6.0"
  }

  def pomSettings = T {
    PomSettings(
      description = artifactName(),
      organization = "edu.berkeley.cs",
      url = "https://github.com/chipsalliance/rocket-chip",
      licenses = Seq(License.`Apache-2.0`, License.`BSD-3-Clause`),
      versionControl = VersionControl.github("chipsalliance", "rocket-chip"),
      developers = Seq.empty
    )
  }
  override def artifactName = "rocketchip"
}
