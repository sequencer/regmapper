import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

trait MyPublishModule extends ScalaModule with PublishModule {m =>
  def publishVersion = de.tobiasroeser.mill.vcs.version.VcsVersion.vcsState().format()
  def pomSettings = T {
    PomSettings(
      description = artifactName(),
      organization = "me.jiuyang",
      url = "https://github.com/sequencer/regmapper",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("sequencer", "regmapper"),
      developers = Seq(
        Developer("sequencer", "Jiuyang Liu", "https://github.com/sequencer")
      )
    )
  }
}

trait RegMapperModule extends MyPublishModule {
  // override to build from source, see the usage of chipsalliance/playground
  def chisel3Module: Option[PublishModule] = None
  // override to build from source, see the usage of chipsalliance/playground
  def chisel3PluginJar: T[Option[PathRef]] = T { None }
  // Use SNAPSHOT chisel by default, downstream users should override this for their own project versions.
  def chisel3IvyDep: T[Option[Dep]] = None
  def chisel3PluginIvyDep: T[Option[Dep]] = None

  def upickleIvyModule: T[Dep]

  // User should not override lines below
  override def moduleDeps = Seq() ++ chisel3Module
  override def scalacPluginClasspath = T {
    super.scalacPluginClasspath() ++ chisel3PluginJar()
  }
  override def scalacOptions = T {
    super.scalacOptions() ++ chisel3PluginJar().map(path => s"-Xplugin:${path.path}")
  }
  override def scalacPluginIvyDeps = T {
    Agg() ++ chisel3PluginIvyDep()
  }
  override def ivyDeps = T {
    Agg() ++ chisel3IvyDep() ++ Seq(upickleIvyModule())
  }
}
