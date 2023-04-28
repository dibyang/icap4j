package net.xdob.icap4j;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import net.xdob.icap4j.codec.DefaultFullIcapRequest;
import net.xdob.icap4j.codec.FullIcapRequest;
import net.xdob.icap4j.codec.FullResponse;
import net.xdob.icap4j.codec.IcapMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IcapClientImpl implements IcapClient{
  private final String host;
  private final int port;
  private final Bootstrap bootstrap;

  public IcapClientImpl(Bootstrap bootstrap, String host, int port) {
    this.host = host;
    this.port = port;
    this.bootstrap = bootstrap;
  }

  IcapFuture<FullResponse> sendRequest(FullIcapRequest request, File file, IcapCallback<FullResponse> callback) {
    IcapFuture future = new IcapFuture(callback);
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      FullHttpMessage httpMessage = request.getFullHttpMessage();
      httpMessage.content().writeBytes(bytes);
      httpMessage.headers().set(HttpHeaderNames.CONTENT_LENGTH,
          httpMessage.content().readableBytes());
    } catch (IOException e) {
      future.failed(e);
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
    return future;
  }


  @Override
  public IcapFuture<FullResponse> options(String service, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = DefaultFullIcapRequest.options("", service);
    return sendRequest(request,null,callback);
  }

  @Override
  public IcapFuture<FullResponse> reqmod(String service, File file, IcapCallback<FullResponse> callback) {

    FullIcapRequest request = DefaultFullIcapRequest.reqmod(host, "srv_clamav");
    FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.POST, "");
    httpRequest.headers().set(HttpHeaderNames.HOST, host);


    request.setFullHttpMessage(httpRequest);
    return sendRequest(request,file,callback);
  }

  @Override
  public IcapFuture<FullResponse> respmod(String service, File file, IcapCallback<FullResponse> callback)  {
    FullIcapRequest request = DefaultFullIcapRequest.respmod(host, "srv_clamav");
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
    httpResponse.headers().set(HttpHeaderNames.HOST, host);


    request.setFullHttpMessage(httpResponse);
    return sendRequest(request, file, callback);
  }
}
