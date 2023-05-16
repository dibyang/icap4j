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
import net.xdob.icap4j.codec.FullResponse;
import net.xdob.icap4j.codec.IcapRequestEncoder;
import net.xdob.icap4j.codec.IcapResponseDecoder;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IcapClientFactoryImpl implements IcapClientFactory, IcapClientContext{
  public static final int MAX_SIZE = 16 * 1024;

  private Semaphore semaphore;
  private final NioEventLoopGroup eventLoopGroup;

  public IcapClientFactoryImpl() {
    this.eventLoopGroup = new NioEventLoopGroup();
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
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new IcapRequestEncoder());
            ch.pipeline().addLast(new IcapResponseDecoder());
            ch.pipeline().addLast(new HttpObjectAggregator(MAX_SIZE));
            ch.pipeline().addLast(new IcapClientHandler());
          }
        });
    return bootstrap;
  }


  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
  }

  public static void main(String[] args) {
    IcapClientFactoryImpl factory = new IcapClientFactoryImpl();
    IcapClient client = factory.getClient("13.13.114.227", DEFAULT_PORT);
    Stopwatch stopwatch = Stopwatch.createStarted();
    AtomicInteger finds = new AtomicInteger();
    int count = 50;


    //CountDownLatch countDownLatch = new CountDownLatch(count);
    for (int i = 0; i < count; i++) {
      int finalI = i;
      IcapFuture<FullResponse> clamav = client.respmod("srv_clamav", Paths.get("d:/rrrr.zip").toFile(), null);
      try {
        FullResponse response = clamav.get();
        if(!response.headers().contains("X-Infection-Found")) {
          System.out.println("response = " + response);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
//      client.respmod("srv_clamav", Paths.get("d:/rrrr.zip").toFile(), new IcapCallback<FullResponse>() {
//        @Override
//        public void completed(FullResponse result) {
//
//          countDownLatch.countDown();
////          FullHttpMessage httpMessage = result.getFullHttpMessage();
////          int readableBytes = httpMessage.content().readableBytes();
////          System.out.println("readableBytes = " + readableBytes);
//          String s = result.headers().get("X-Infection-Found");
//          if(s!=null){
//            finds.incrementAndGet();
//            //System.out.println(finalI +" response = " + result);
//          }else{
//            System.out.println(finalI +" response = " + result);
//          }
//
//        }
//
//        @Override
//        public void failed(Throwable ex) {
//          countDownLatch.countDown();
//          ex.printStackTrace();
//        }
//
//        @Override
//        public void cancelled() {
//
//        }
//      });
    }
//    try {
//      countDownLatch.await(40, TimeUnit.SECONDS);
//      stopwatch.stop();
//      System.out.println(String.format("count=%s, finds=%s, stopwatch=%s", count,finds,stopwatch.toString()));
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
    factory.shutdown();
  }

  @Override
  public Semaphore getSemaphore() {
    return semaphore;
  }
}
