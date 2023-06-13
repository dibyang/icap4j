package net.xdob.icap4j;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.xdob.icap4j.codec.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IcapClientFactoryImpl implements IcapClientFactory, IcapClientContext{
  public static final int MAX_SIZE = 16 * 1024;
  public static final int DEFAULT_TIMEOUT = 20*1000;
  public static final int CONNECT_TIMEOUT = 10*1000;

  private final Map<String, ReqSem> reqSemMap = Maps.newConcurrentMap();

  private final Map<String, Semaphore> semMap = Maps.newConcurrentMap();
  protected NioEventLoopGroup eventLoopGroup;
  private int nodeMaxConn = 24;

  public IcapClientFactoryImpl() {
    this.eventLoopGroup = new NioEventLoopGroup(5, new DefaultThreadFactory("nio_event_icap"));

    eventLoopGroup.scheduleWithFixedDelay(()->{
      for (String id : reqSemMap.keySet()) {
        ReqSem reqSem = reqSemMap.get(id);
        if(reqSem !=null){
          long offset = System.nanoTime()- reqSem.getNanoTime();
          if(TimeUnit.NANOSECONDS.toMillis(offset)>DEFAULT_TIMEOUT){
            reqSemMap.remove(id);
            reqSem.getSemaphore().release();
          }
        }
      }
    },5,5,TimeUnit.SECONDS);
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
            ch.pipeline().addLast(new IcapClientHandler());
          }
        });
    return bootstrap;
  }

  @Override
  public Semaphore getSemaphore(String host) {
    synchronized (semMap){
      Semaphore semaphore = semMap.get(host);
      if(semaphore!=null){
        semaphore = new Semaphore(nodeMaxConn);
        semMap.put(host, semaphore);
      }
      return semaphore;
    }
  }

  @Override
  public void addReqSem(String channelId, ReqSem reqSem) {
    reqSemMap.put(channelId, reqSem);
  }

  @Override
  public ReqSem removeReqSem(String channelId) {
    return reqSemMap.remove(channelId);
  }



  public void shutdown() {
    eventLoopGroup.shutdownGracefully();
  }

  public static void main(String[] args) {
    IcapClientFactoryImpl factory = new IcapClientFactoryImpl();
    IcapClient client = factory.getClient("13.13.114.227", DEFAULT_PORT);
    Stopwatch stopwatch = Stopwatch.createStarted();
    AtomicInteger finds = new AtomicInteger();

    int count = 1000;
    byte[] context = getContext();
    ExecutorService service = Executors.newFixedThreadPool(5);

    CountDownLatch countDownLatch = new CountDownLatch(count);
    for (int i = 0; i < count; i++) {
      int finalI  = i;
      service.submit(()->{
        client.respmod("srv_clamav", context, new IcapCallback<FullResponse>() {
          @Override
          public void completed(FullResponse result) {

            countDownLatch.countDown();
            String s = result.headers().get("X-Infection-Found");
            if(s!=null){
              finds.incrementAndGet();
              System.out.println(finalI +" find Infection");
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
      });

    }
    try {
      countDownLatch.await();
      stopwatch.stop();
      System.out.println(String.format("count=%s, finds=%s, stopwatch=%s", count,finds,stopwatch.toString()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    service.shutdown();
    factory.shutdown();
  }

  private static byte[] getContext() {
    try {
      return Files.readAllBytes(Paths.get("d:/test/pp.zip"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[]{};
  }


}
