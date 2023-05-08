package net.xdob.icap4j;

import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import net.xdob.icap4j.codec.FullResponse;
import net.xdob.icap4j.codec.IcapRequestEncoder;
import net.xdob.icap4j.codec.IcapResponseDecoder;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IcapClientFactoryImpl implements IcapClientFactory{
  public static final int MAX_SIZE = 16 * 1024;

  private final NioEventLoopGroup eventLoopGroup;

  public IcapClientFactoryImpl() {
    this.eventLoopGroup = new NioEventLoopGroup();
  }

  @Override
  public IcapClient getClient(String ip, int port) {
    Bootstrap bootstrap = new Bootstrap()
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
    return new IcapClientImpl(bootstrap, ip,port);
  }


  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
  }

  public static void main(String[] args) {
    IcapClientFactoryImpl factory = new IcapClientFactoryImpl();
    IcapClient client = factory.getClient("13.13.114.227", DEFAULT_PORT);
    Stopwatch stopwatch = Stopwatch.createStarted();
    int count = 50;
    CountDownLatch countDownLatch = new CountDownLatch(count);
    for (int i = 0; i < count; i++) {
      client.respmod("srv_clamav", Paths.get("d:/rrrr.zip").toFile(), new IcapCallback<FullResponse>() {
        @Override
        public void completed(FullResponse result) {
          System.out.println("response = " + result);
          countDownLatch.countDown();
        }

        @Override
        public void failed(Throwable ex) {
          countDownLatch.countDown();
          ex.printStackTrace();
        }

        @Override
        public void cancelled() {

        }
      });
    }
    try {
      countDownLatch.await(40, TimeUnit.SECONDS);
      stopwatch.stop();
      System.out.println(String.format("count=%s, stopwatch=%s", count,stopwatch.toString()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    factory.shutdown();
  }

}
