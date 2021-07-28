package zhttp.experiment

import io.netty.channel.{
  ChannelHandler => JChannelHandler,
  ChannelHandlerContext => JChannelHandlerContext,
  ChannelInboundHandlerAdapter => JChannelInboundHandlerAdapter,
  SimpleChannelInboundHandler => JSimpleChannelInboundHandler,
}
import io.netty.handler.codec.http.{HttpContent => JHttpContent, HttpObject => JHttpObject, HttpRequest => JHttpRequest}
import zhttp.http.Http
import zhttp.service.UnsafeChannelExecutor
import zio.{UIO, ZIO}

/**
 * Low level handle to control the flow of messages over a network channel.
 */
final case class Channel[-R, +E, -A, +B](private[zhttp] val channel: Channel.Handle[R, E, A, B]) {
  self =>
  def compile[R1 <: R, A1 <: A](implicit rtm: UnsafeChannelExecutor[R1]): JSimpleChannelInboundHandler[A1] = ???

  def ++[R1 <: R, E1 >: E, A1 <: A, B1 >: B](other: Channel[R1, E1, A1, B1]): Channel[R1, E1, A1, B1] =
    Channel(self.channel.zipRight(other.channel))
}

object Channel {
  def collect[A, B]: MkCollect[A, B] = new MkCollect(())
  final class MkCollect[A, B](val unit: Unit) extends AnyVal {
    def apply[R, E](pf: PartialFunction[(Event[A], Context[B]), ZIO[R, E, Any]]): Channel[R, E, A, B] =
      Channel(
        new Handle[R, E, A, B] {
          override def execute(event: Event[A], ctx: Context[B]): ZIO[R, E, Any] =
            pf((event, ctx)).when(pf.isDefinedAt((event, ctx)))
        },
      )
  }

  trait Handle[-R, +E, -A, +B] { self =>
    def execute(event: Event[A], ctx: Context[B]): ZIO[R, E, Any] =
      event match {
        case Event.Read(data)     => ctx.fireChannelRead(data)
        case Event.Register       => ctx.fireRegistered()
        case Event.Failure(cause) => ctx.fireExceptionCaught(cause)
        case Event.Complete       => ctx.fireChannelReadComplete()
      }

    final def *>[R1 <: R, E1 >: E, A1 <: A, B1 >: B](
      other: Handle[R1, E1, A1, B1],
    ): Handle[R1, E1, A1, B1] =
      self.zipRight(other)

    final def zipRight[R1 <: R, E1 >: E, A1 <: A, B1 >: B](
      other: Handle[R1, E1, A1, B1],
    ): Handle[R1, E1, A1, B1] =
      new Handle[R1, E1, A1, B1] {
        override def execute(event: Event[A1], ctx: Context[B1]): ZIO[R1, E1, Any] =
          self.execute(event, ctx) *> other.execute(event, ctx)
      }
  }

  private[zhttp] def compile[R](
    zExec: UnsafeChannelExecutor[R],
    http: Http[R, Throwable, JHttpRequest, Channel[Any, Nothing, JHttpContent, JHttpObject]],
  ): JChannelHandler =
    new JChannelInboundHandlerAdapter { self =>
      private var channel: Channel[Any, Nothing, JHttpContent, JHttpObject] = _

      override def channelRegistered(ctx: JChannelHandlerContext): Unit = {
        ctx.channel().config().setAutoRead(false)
        ctx.read(): Unit
      }

      override def channelRead(ctx: JChannelHandlerContext, msg: Any): Unit = {
        zExec.unsafeExecute_(ctx) {
          val c = new Context(ctx)
          msg match {
            case req: JHttpRequest     =>
              http
                .executeAsZIO(req)
                .flatMap(channel => UIO(self.channel = channel))
                .zipRight(self.channel.channel.execute(Event.Register, c))
                .catchAll({
                  case None    => ??? // TODO: send a not found response
                  case Some(_) => ??? // TODO: send an internal server error response
                })
            case content: JHttpContent => self.channel.channel.execute(Event.Read(content), c)
            case _                     => UIO(ctx.fireExceptionCaught(new Error("Unknown message")))
          }
        }
      }

      override def exceptionCaught(ctx: JChannelHandlerContext, cause: Throwable): Unit =
        zExec.unsafeExecute_(ctx) {
          channel.channel.execute(Event.Failure(cause), new Context(ctx))
        }
    }
}