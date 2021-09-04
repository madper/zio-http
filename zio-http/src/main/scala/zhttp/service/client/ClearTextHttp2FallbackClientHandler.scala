package zhttp.service.client

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpResponse}

final case class ClearTextHttp2FallbackClientHandler(httpH: ChannelHandler)
  extends SimpleChannelInboundHandler[HttpResponse]() {
  @throws[Exception]
  override protected def channelRead0(ctx: ChannelHandlerContext, msg: HttpResponse): Unit = { // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
    val pipeline = ctx.pipeline
    println("Fallback! the attempt for http2 failed. now trying to resolve the promise by the response from the server for the upgrade request")
    println("handlers before fallback")
    println(ctx.pipeline().names())
pipeline.addAfter(ctx.name(), "object",new HttpObjectAggregator(Int.MaxValue))
  .addAfter("object","http",httpH)
    ctx.fireChannelRead(msg)
    println("handlers after fallback")
    println(ctx.pipeline().names())
    ()
  }
}
