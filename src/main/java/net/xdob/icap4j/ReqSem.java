package net.xdob.icap4j;

import java.util.concurrent.Semaphore;

public class ReqSem {
  private long nanoTime = System.nanoTime();
  private Semaphore semaphore;
  private String host;

  public ReqSem(Semaphore semaphore, String host) {
    this.semaphore = semaphore;
    this.host = host;
  }

  public String getHost() {
    return host;
  }

  public long getNanoTime() {
    return nanoTime;
  }

  public Semaphore getSemaphore() {
    return semaphore;
  }

}
