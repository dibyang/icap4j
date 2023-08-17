package net.xdob.icap4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicSemaphore extends Semaphore {
  private volatile int maxPermits;
  private final AtomicInteger offsetPermits = new AtomicInteger();
  public DynamicSemaphore(int maxPermits) {
   this(maxPermits,false);
  }

  public DynamicSemaphore(int maxPermits, boolean fair) {
    super(maxPermits, fair);
    this.maxPermits = maxPermits;
  }

  public int getMaxPermits() {
    return maxPermits;
  }

  public void setMaxPermits(int maxPermits) {
    if (maxPermits < 0) throw new IllegalArgumentException();
    //负数表示表示要缩减许可，正数表示要增加许可
    int offset = maxPermits - this.maxPermits;
    this.offsetPermits.updateAndGet(old -> old + offset);
    this.maxPermits = maxPermits;
    handleOffsetPermits();
  }

  private void handleOffsetPermits() {
    int offset = 0;
    while((offset=this.offsetPermits.get()) != 0 ){
      if(offset>0){
        if(offsetPermits.compareAndSet(offset,0)) {
          this.release(offset);
          break;
        }
      }else {
        int permits = this.drainPermits();
        if(permits>0) {
          this.offsetPermits.updateAndGet(old -> old + permits);
        }else{
          break;
        }
      }
    }
  }

  @Override
  public void release() {
    release(1);
  }

  @Override
  public void release(int permits) {
    super.release(permits);
    handleOffsetPermits();
  }

}
