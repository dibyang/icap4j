package net.xdob.icap4j;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.xdob.icap4j.IcapConstants;
import net.xdob.icap4j.IcapFuture;
import net.xdob.icap4j.codec.FullResponse;

import java.util.concurrent.Semaphore;

public class IcapClientHandler extends SimpleChannelInboundHandler<FullResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullResponse response) {
    ctx.close();
    release(ctx);
    // 处理ICAP响应
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.completed(response);
    }
  }

  private void release(ChannelHandlerContext ctx) {
    Semaphore semaphore = ctx.channel().attr(IcapConstants.SEMAPHORE).get();
    if(semaphore!=null){
      ctx.channel().attr(IcapConstants.SEMAPHORE).set(null);
      semaphore.release();
//      String id = ctx.channel().id().asShortText();
//      System.out.println("Semaphore release id = " + id);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
    release(ctx);
    // 处理异常情况
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.failed(cause);
    }
  }
}