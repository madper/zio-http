package zhttp.experiment

import zhttp.experiment.Router.{OnlyString, RouteSet}
import zhttp.http._
import zio._

sealed trait Router[A] { self =>
  def /(name: String)                                                                = RouteSet(OnlyString(name), self)
  def /[B, C](other: Router[B])(implicit ev: Router.Combine.Aux[A, B, C]): Router[C] = RouteSet(self, other)
}

object Router {
  case class OnlyMethod(v: Method) extends Router[Unit]
  case class OnlyString(v: String) extends Router[Unit]
  // Extract
  trait RouteParam[A] {
    def extract(data: String): Option[A]
  }
  object RouteParam   {
    implicit object IntExtract extends RouteParam[Int] {
      override def extract(data: String): Option[Int] = data.toIntOption
    }

    implicit object StringExtract extends RouteParam[String] {
      override def extract(data: String): Option[String] = Option(data)
    }
  }

  case class Arg[A]() extends Router[A]
  object Arg {
    def apply[A](implicit ev: RouteParam[A]): Router[A] = new Arg()
  }

  class RouteSet[A, B, C](val head: Router[A], val tail: Router[B]) extends Router[C]
  object RouteSet {
    def apply[A, B, C](head: Router[A], tail: Router[B])(implicit ev: Router.Combine.Aux[A, B, C]): Router[C] =
      new RouteSet(head, tail)
  }

  implicit final class MethodRouterSyntax(val method: Method) extends AnyVal {
    def /(name: String): Router[Unit]                                              = OnlyMethod(method) / name
    def /[B, C](other: Router[B])(implicit ev: Combine.Aux[Unit, B, C]): Router[C] =
      OnlyMethod(method) / other
  }

  val route: Router[(Int, String)] = Method.GET / Arg[Int] / Arg[String] / "c"

  trait Request {
    def is[A](router: Router[A]): Boolean
  }

  implicit class HttpEndpointSyntax(http: Http.type) {
    def endpoint[A, B](router: Router[A])(f: A => B): Http[Any, Nothing, Request, B]           = ???
    def endpointM[R, E, A, B](router: Router[A])(f: A => ZIO[R, E, B]): Http[R, E, Request, B] = ???
  }

  // Combine Logic for Router
  sealed trait Combine[A, B] {
    type Out
  }

  object Combine {
    type Aux[A, B, C] = Combine[A, B] {
      type Out = C
    }

    // scalafmt: { maxColumn = 1200 }

    implicit def combine0[A, B](implicit ev: A =:= Unit): Combine.Aux[A, B, B]                                                                                = null
    implicit def combine1[A, B](implicit evA: RouteParam[A], evB: RouteParam[B]): Combine.Aux[A, B, (A, B)]                                                   = null
    implicit def combine2[A, B, T1, T2](implicit evA: A =:= (T1, T2), evB: RouteParam[B]): Combine.Aux[A, B, (T1, T2, B)]                                     = null
    implicit def combine3[A, B, T1, T2, T3](implicit evA: A =:= (T1, T2, T3), evB: RouteParam[B]): Combine.Aux[A, B, (T1, T2, T3, B)]                         = null
    implicit def combine4[A, B, T1, T2, T3, T4](implicit evA: A =:= (T1, T2, T3, T4), evB: RouteParam[B]): Combine.Aux[A, B, (T1, T2, T3, T4, B)]             = null
    implicit def combine5[A, B, T1, T2, T3, T4, T5](implicit evA: A =:= (T1, T2, T3, T4, T5), evB: RouteParam[B]): Combine.Aux[A, B, (T1, T2, T3, T4, T5, B)] = null

    // TODO: Add combine6 -> combine 22

    // scalafmt: { maxColumn = 120 }

  }

}
