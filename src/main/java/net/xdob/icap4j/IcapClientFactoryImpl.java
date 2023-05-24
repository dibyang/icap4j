package net.xdob.icap4j;

import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.xdob.icap4j.codec.FullResponse;
import net.xdob.icap4j.codec.IcapRequestEncoder;
import net.xdob.icap4j.codec.IcapResponseDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IcapClientFactoryImpl implements IcapClientFactory, IcapClientContext{
  public static final int MAX_SIZE = 16 * 1024;
  public static final int DEFAULT_TIMEOUT = 5000;
  public static final int CONNECT_TIMEOUT = 2000;

  protected Semaphore semaphore;
  protected final NioEventLoopGroup eventLoopGroup;

  public IcapClientFactoryImpl() {
    this.eventLoopGroup = new NioEventLoopGroup(4, new DefaultThreadFactory("nio_event_icap"));
    semaphore = new Semaphore(10);
  }

  @Override
  public IcapClient getClient(String ip, int port) {
    return new IcapClientImpl(this, ip, port);
  }

  @Override
  public Bootstrap newBootstrap() {
    Bootstrap bootstrap = new Bootstrap()
        .group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new WriteTimeoutHandler(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
            ch.pipeline().addLast(new IcapRequestEncoder());
            ch.pipeline().addLast(new ReadTimeoutHandler(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
            ch.pipeline().addLast(new IcapResponseDecoder());
            ch.pipeline().addLast(new HttpObjectAggregator(MAX_SIZE));
            ch.pipeline().addLast(new IcapClientHandler());
          }
        });
    return bootstrap;
  }

  @Override
  public Semaphore getSemaphore() {
    return semaphore;
  }

  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
  }

  public static void main(String[] args) {
    IcapClientFactoryImpl factory = new IcapClientFactoryImpl();
    IcapClient client = factory.getClient("13.13.114.227", DEFAULT_PORT);
    Stopwatch stopwatch = Stopwatch.createStarted();
    AtomicInteger finds = new AtomicInteger();

    int count = 100;

    byte[] context = getContext();

    CountDownLatch countDownLatch = new CountDownLatch(count);
    for (int i = 0; i < count; i++) {
     int finalI  = i;
      client.respmod("srv_clamav", context, new IcapCallback<FullResponse>() {
        @Override
        public void completed(FullResponse result) {

          countDownLatch.countDown();
//          FullHttpMessage httpMessage = result.getFullHttpMessage();
//          int readableBytes = httpMessage.content().readableBytes();
//          System.out.println("readableBytes = " + readableBytes);
          String s = result.headers().get("X-Infection-Found");
          if(s!=null){
            finds.incrementAndGet();
            //System.out.println(finalI +" response = " + result);
          }else{
            System.out.println(finalI +" response = " + result);
          }

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
      System.out.println(String.format("count=%s, finds=%s, stopwatch=%s", count,finds,stopwatch.toString()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    factory.shutdown();
  }

  private static byte[] getContext() {
    try {
      return Files.readAllBytes(Paths.get("d:/rrrr.zip"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[]{};
  }


}
