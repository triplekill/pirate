package pirate.example

import pirate._, Pirate._
import scalaz._, Scalaz._, effect.IO

case class Git(
  cwd: String,
  conf: String, // FIX support a read like thing for x=y
  exec: String,
  cmd: GitCommand
)
trait GitCommand
object GitVersion extends GitCommand
object GitHtmlPath extends GitCommand
object GitManPath extends GitCommand
object GitInfoPath extends GitCommand
case class GitHelp(command: Option[String]) extends GitCommand

object GitMain extends PirateMainIO[Git] {
  val version: Parse[GitCommand] =
    terminator(long("version"), GitVersion)

  val help: Parse[GitCommand] =
    terminatorx(long("help"), GitHelp.apply)

  val cwd: Parse[String] =
    flag(short('C') |+| description("<path>"))

  val conf: Parse[String] =
    flag(short('c') |+| description("<name>=<value>"))

  val exec: Parse[String] =
    flag(long("exec-path") |+| description("<path>")) // FIX fork on arg terminator vs option

  val html: Parse[GitCommand] =
    terminator(long("html-path"), GitHtmlPath)

  val man: Parse[GitCommand] =
    terminator(long("man-path"), GitManPath)

  val info: Parse[GitCommand] =
    terminator(long("info-path"), GitInfoPath)

  def git(cmd: Parse[GitCommand]): Parse[Git] =
    Git |*| (cwd, conf, exec, cmd)

  val command: Command[Git] =
    (git { version } |||
     git { help } |||
     git { html } |||
     git { man } |||
     git { info }) ~ "git" ~~
      "This is a demo of the git command line"

  def run(a: Git) = a.cmd match {
    case GitVersion => IO.putStrLn("git the pirate version")
    case GitHtmlPath => IO.putStrLn("html-path")
    case GitManPath => IO.putStrLn("man-path")
    case GitInfoPath => IO.putStrLn("info-path")
    case GitHelp(x) => x match {
      case None => IO.putStrLn(Usage.print(command))
      case Some(c) => sys.error("Implement sub-command usage printing.")
    }
  }
}

class GitExample extends spec.Spec { def is = s2"""

  Git Examples
  ============

  git --version                            $version
  git --help                               $help
  git --help status                        $helpAt

  Git Checks
  ==========

  Name is set                              ${GitMain.command.name == "git"}
  Description is set                       ${GitMain.command.description == Some("This is a demo of the git command line")}

"""

  def run(args: String*): ParseError \/ Git =
    Interpretter.run(GitMain.command.parse, args.toList)

  // FIX make these work

  def version = {
    run("--version") must_==
      GitVersion.right
  }.pendingUntilFixed

  def help = {
    run("--help") must_==
      GitHelp(None).right
  }.pendingUntilFixed

  def helpAt = {
    run("--help", "status") must_==
      GitHelp(Some("status")).right
  }.pendingUntilFixed
}
