//> using platform "jvm"
//> using scala "2.13.8"
//> using lib "dev.zio::zio:1.0.15"
//> using lib "com.monovore::decline:2.2.0"
//> using mainClass "DeclinedApp"

import cats.implicits._
import com.monovore.decline._
import zio._
import zio.console._

object DeclinedApp extends zio.App {
  val VERSION = "0.1.0"

  val nameOpt = Opts.option[String]("user", help = "User name", "u")

  val alphaOpt = Opts
    .option[Double]("alpha", help = "Alpha Filtering", "a")
    .withDefault(1.23)

  val forceOpt = Opts.flag("fail", help = "Manually trigger a failure").orFalse

  val versionOpt: Opts[RIO[Console, Unit]] = Opts
    .flag(
      "version",
      "Show version and Exit",
      "v",
      visibility = Visibility.Partial
    )
    .orFalse
    .map(_ => putStrLn(VERSION))

  val mainOpt: Opts[RIO[Console, Unit]] =
    (nameOpt, alphaOpt, forceOpt).mapN[RIO[Console, Unit]] {
      (name, alpha, force) =>
        if (force)
          ZIO.fail(
            new Exception(s"Manually FAIL triggered by $name! alpha=$alpha")
          )
        else
          putStrLn(s"Hello $name. Running with alpha=$alpha")
    }

  val runOpt = versionOpt orElse mainOpt

  val command: Command[RIO[Console, Unit]] = Command(
    name = "DeclinedApp",
    header = "Testing decline + zio"
  )(runOpt)

  def effect(args: List[String]): ZIO[Console, Throwable, Unit] =
    command.parse(args) match {
      case Left(help) => // a bit odd that --help returns here
        if (help.errors.isEmpty) putStrLn(help.show)
        else IO.fail(new Exception(s"${help.errors}"))
      case Right(value) =>
        value.map(_ => ())
    }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    effect(args)
      .map(_ => ExitCode.success)
      .catchAll { ex =>
        for {
          _ <- putStrLn(s"Error $ex").orDie
        } yield ExitCode.failure
      }
}
