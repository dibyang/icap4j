package net.xdob.icap4j;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.*;
import net.xdob.icap4j.codec.DefaultFullIcapRequest;
import net.xdob.icap4j.codec.FullIcapRequest;
import net.xdob.icap4j.codec.FullResponse;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class IcapClientImpl implements IcapClient{
  private final String host;
  private final int port;
  private final Bootstrap bootstrap;

  public IcapClientImpl(Bootstrap bootstrap, String host, int port) {
    this.host = host;
    this.port = port;
    this.bootstrap = bootstrap;
  }

  IcapFuture<FullResponse> sendRequest(FullIcapRequest request, IcapCallback<FullResponse> callback) {
    IcapFuture future = new IcapFuture(callback);

    try {
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
    }
    return future;
  }


  @Override
  public IcapFuture<FullResponse> options(String service, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = DefaultFullIcapRequest.options("", service);
    return sendRequest(request,callback);
  }

  @Override
  public IcapFuture<FullResponse> reqmod(String service, File file, IcapCallback<FullResponse> callback) {
    FullIcapRequest request = getFullIcapRequest4Req();
    request.setFile(file);
    return sendRequest(request, callback);
  }

  private FullIcapRequest getFullIcapRequest4Req() {
    FullIcapRequest request = DefaultFullIcapRequest.reqmod(host, "srv_clamav");
    FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.POST, "");
    httpRequest.headers().set(HttpHeaderNames.HOST, host);
    request.setFullHttpMessage(httpRequest);
    return request;
  }

  @Override
  public IcapFuture<FullResponse> respmod(String service, File file, IcapCallback<FullResponse> callback)  {
    FullIcapRequest request = getFullIcapRequest4Resp();
    request.setFile(file);
    return sendRequest(request, callback);
  }

  private FullIcapRequest getFullIcapRequest4Resp() {
    FullIcapRequest request = DefaultFullIcapRequest.respmod(host, "srv_clamav");
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
    httpResponse.headers().set(HttpHeaderNames.HOST, host);
    request.setFullHttpMessage(httpResponse);
    return request;
  }
}
