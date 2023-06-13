package net.xdob.icap4j;

import java.util.concurrent.Semaphore;

public class ReqSem {
  private long nanoTime = System.nanoTime();
  private Semaphore semaphore;

  public ReqSem(Semaphore semaphore) {
    this.semaphore = semaphore;
  }

  public long getNanoTime() {
    return nanoTime;
  }

  public Semaphore getSemaphore() {
    return semaphore;
  }

}
