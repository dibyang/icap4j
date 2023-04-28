package net.xdob.icap4j.example.simple;

import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import net.xdob.icap4j.IcapCallback;
import net.xdob.icap4j.IcapConstants;
import net.xdob.icap4j.IcapFuture;
import net.xdob.icap4j.codec.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class IcapClient {

  public static final int MAX_SIZE = 16 * 1024;
  private final Bootstrap bootstrap;
  private final String host;
  private final int port;
  private final NioEventLoopGroup eventLoopGroup;

  public IcapClient(String host, int port) {
    this.host = host;
    this.port = port;

    eventLoopGroup = new NioEventLoopGroup();
    bootstrap = new Bootstrap()
        .group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new IcapRequestEncoder());
            ch.pipeline().addLast(new IcapResponseDecoder());
            ch.pipeline().addLast(new HttpObjectAggregator(MAX_SIZE));
            ch.pipeline().addLast(new IcapClientHandler());
          }
        });
  }

  public IcapFuture<FullResponse> sendRequest(FullIcapRequest request, IcapCallback<FullResponse> callback) {
    IcapFuture future = new IcapFuture(callback);
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

  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
  }

  public static void main(String[] args) {
    String host = "13.13.114.227";
    IcapClient client = new IcapClient(host, 1344);
    reqmod(host, client);
    client.shutdown();
  }

  private static void respmod(String host, IcapClient client) {
    try {
      // Prepare the ICAP request.
      FullIcapRequest request = DefaultFullIcapRequest.respmod(host, "srv_clamav");
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
      httpResponse.headers().set(HttpHeaderNames.HOST, host);
      byte[] bytes = Files.readAllBytes(Paths.get("d:/rrrr.zip"));
      httpResponse.content().writeBytes(bytes);
      httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,
          httpResponse.content().readableBytes());
//      httpResponse.headers().set(HttpHeaderNames.CONTENT_DISPOSITION,
//          "attachment;filename=\"rrrr.zip;\"");
//      httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
      request.setFullHttpMessage(httpResponse);
      IcapFuture<FullResponse> future = client.sendRequest(request, null);
      FullResponse response = future.get(8, TimeUnit.SECONDS);
      System.out.println("response = " + response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void reqmod(String host, IcapClient client) {
    try {
      // Prepare the ICAP request.
      FullIcapRequest request = DefaultFullIcapRequest.reqmod(host, "srv_clamav");
      FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.POST, "");
      httpRequest.headers().set(HttpHeaderNames.HOST, host);
      //httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      //httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      byte[] bytes = Files.readAllBytes(Paths.get("d:/rrrr.zip"));

      httpRequest.content().writeBytes(bytes);

//      httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH,
//          httpRequest.content().readableBytes());
//      httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");

      request.setFullHttpMessage(httpRequest);
      Stopwatch stopwatch = Stopwatch.createStarted();
      for (int i = 0; i < 1; i++) {
        IcapFuture<FullResponse> future = client.sendRequest(request, null);
        FullResponse response = future.get(4, TimeUnit.SECONDS);
      }
      stopwatch.stop();
      System.out.println("stopwatch = " + stopwatch.toString());
      //System.out.println("response = " + response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

