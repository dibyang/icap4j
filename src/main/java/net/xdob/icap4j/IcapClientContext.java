package net.xdob.icap4j;

import io.netty.bootstrap.Bootstrap;

import java.util.concurrent.Semaphore;

public interface IcapClientContext {
  Bootstrap newBootstrap();
  Semaphore getSemaphore(String host);
  void addReqSem(String channelId, ReqSem reqSem);
  ReqSem removeReqSem(String channelId);

}
