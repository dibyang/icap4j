package net.xdob.icap4j;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.xdob.icap4j.codec.FullResponse;

public class IcapClientHandler extends SimpleChannelInboundHandler<FullResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullResponse response) {
    release(ctx);
    ctx.close();
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.completed(response);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    release(ctx);
    super.channelInactive(ctx);
  }

  private void release(ChannelHandlerContext ctx) {
    synchronized (ctx) {
      IcapClientContext context = ctx.channel().attr(IcapConstants.CONTEXT).get();
      if (context != null) {
        String channelId = ctx.channel().id().asLongText();
        ReqSem semaphore = context.removeReqSem(channelId);
        if(semaphore!=null) {
          semaphore.getSemaphore().release();
        }
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    release(ctx);
    ctx.close();
    // 处理异常情况
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.failed(cause);
    }
  }
}