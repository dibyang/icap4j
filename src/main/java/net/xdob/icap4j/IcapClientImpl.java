package net.xdob.icap4j;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import net.xdob.icap4j.codec.*;

import java.io.File;
import java.nio.file.Files;

public class IcapClientImpl implements IcapClient{
  private final String host;
  private final int port;
  private final IcapClientContext context;
  private final Bootstrap bootstrap;

  public IcapClientImpl(IcapClientContext context, String host, int port) {
    this.host = host;
    this.port = port;
    this.context = context;
    this.bootstrap = context.newBootstrap();
  }

  IcapFuture<FullResponse> sendRequest(FullIcapRequest request, IcapCallback<FullResponse> callback) {
    IcapFuture future = new IcapFuture(callback,null);

    try {
      context.getSemaphore().acquire();
      File file = request.getFile();
      if(file !=null&&file.exists()&&file.isFile()){
        long fileLength = file.length();
        FullHttpMessage httpMessage = request.getFullHttpMessage();
        httpMessage.headers().set(HttpHeaderNames.CONTENT_LENGTH,
            fileLength);
        byte[] bytes = Files.readAllBytes(file.toPath());
        httpMessage.content().writeBytes(bytes);
      }
      bootstrap.connect(host, port).addListener((ChannelFutureListener) f -> {
        if (f.isSuccess()) {
          Channel channel = f.channel();
          channel.attr(IcapConstants.FUTURE).set(future);
          channel.writeAndFlush(request);
        } else {
          future.failed(f.cause());
        }
      });

    } catch (Exception e) {
      future.failed(e);
    }finally {
      context.getSemaphore().release();
    }
    return future;
  }

  IcapFuture<FullResponse> sendRequest(FullIcapRequest request, ByteBuf context, IcapCallback<FullResponse> callback) {
    if(context !=null&&context.readableBytes()>0){
      long fileLength = context.readableBytes();
      FullHttpMessage httpMessage = request.getFullHttpMessage();
      httpMessage.headers().set(HttpHeaderNames.CONTENT_LENGTH,
          fileLength);
      httpMessage.content().writeBytes(context);
    }
    return sendRequest(request, callback);
  }

  IcapFuture<FullResponse> sendRequest(FullIcapRequest request, byte[] context, IcapCallback<FullResponse> callback) {
    if(context !=null&&context.length>0){
      long fileLength = context.length;
      FullHttpMessage httpMessage = request.getFullHttpMessage();
      httpMessage.headers().set(HttpHeaderNames.CONTENT_LENGTH,
          fileLength);
      httpMessage.content().writeBytes(context);
    }
    return sendRequest(request, callback);
  }

  @Override
  public IcapFuture<FullResponse> options(String service, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = DefaultFullIcapRequest.options("", service);
    return sendRequest(request,callback);
  }

  @Override
  public IcapFuture<FullResponse> reqmod(String service, ByteBuf context, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = getFullIcapRequest4Req();
    return sendRequest(request, context, callback);
  }

  @Override
  public IcapFuture<FullResponse> respmod(String service, ByteBuf context, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = getFullIcapRequest4Resp();
    return sendRequest(request, context, callback);
  }



  @Override
  public IcapFuture<FullResponse> reqmod(String service, byte[] context, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = getFullIcapRequest4Req();
    return sendRequest(request, context, callback);
  }

  private FullIcapRequest getFullIcapRequest4Req() {
    FullIcapRequest request = DefaultFullIcapRequest.reqmod(host, "srv_clamav");
    FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.POST, "");
    httpRequest.headers().set(HttpHeaderNames.HOST, host);
    request.setFullHttpMessage(httpRequest);
    return request;
  }

  @Override
  public IcapFuture<FullResponse> respmod(String service, byte[] context, IcapCallback<FullResponse> callback)  {
    FullIcapRequest request = getFullIcapRequest4Resp();
    return sendRequest(request, context, callback);
  }

  private FullIcapRequest getFullIcapRequest4Resp() {
    FullIcapRequest request = DefaultFullIcapRequest.respmod(host, "srv_clamav");
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
    httpResponse.headers().set(HttpHeaderNames.HOST, host);
    request.setFullHttpMessage(httpResponse);
    return request;
  }
}
