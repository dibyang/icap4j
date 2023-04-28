package net.xdob.icap4j.example.simple;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.xdob.icap4j.IcapConstants;
import net.xdob.icap4j.IcapFuture;
import net.xdob.icap4j.codec.FullResponse;

public class IcapClientHandler extends SimpleChannelInboundHandler<FullResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullResponse response) {
    ctx.close();
    // 处理ICAP响应
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.completed(response);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
    // 处理异常情况
    IcapFuture<FullResponse> future = ctx.channel().attr(IcapConstants.FUTURE).get();
    if (future != null) {
      future.failed(cause);
    }
  }
}