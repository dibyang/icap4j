package net.xdob.icap4j;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class IcapFuture<T> implements Future<T> {
  static final Logger LOG = LoggerFactory.getLogger(IcapFuture.class);

  private final IcapCallback<T> callback;

  private volatile boolean completed;
  private volatile boolean cancelled;
  private volatile T result;
  private volatile Throwable ex;

  public IcapFuture(final IcapCallback<T> callback) {
    this.callback = callback;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public boolean isDone() {
    return this.completed;
  }

  private T getResult() throws ExecutionException {
    if (this.ex != null) {
      throw new ExecutionException(this.ex);
    }
    if (cancelled) {
      throw new CancellationException();
    }
    return this.result;
  }

  @Override
  public synchronized T get() throws InterruptedException, ExecutionException {
    while (!this.completed) {
      wait();
    }
    return getResult();
  }

  @Override
  public synchronized T get(final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {


    final long msecs = unit.toMillis(timeout);
    final long startTime = System.nanoTime();
    long waitTime = msecs;
    if (this.completed) {
      return getResult();
    } else if (waitTime <= 0) {
      throw TimeoutValueException.fromMilliseconds(msecs, msecs + Math.abs(waitTime));
    } else {
      for (; ; ) {
        wait(waitTime);
        if (this.completed) {
          return getResult();
        }
        waitTime = msecs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        if (waitTime <= 0) {
          throw TimeoutValueException.fromMilliseconds(msecs, msecs + Math.abs(waitTime));
        }
      }
    }
  }

  public boolean completed(final T result) {
    synchronized (this) {
      if (this.completed) {
        return false;
      }
      this.completed = true;
      this.result = result;
      notifyAll();
    }
    if (this.callback != null) {
      try {
        this.callback.completed(result);
      } catch (Exception e) {
        LOG.warn("Exception catch", e);
      }
    }

    return true;
  }

  public boolean failed(final Throwable exception) {
    synchronized (this) {
      if (this.completed) {
        return false;
      }
      this.completed = true;
      this.ex = exception;
      notifyAll();
    }
    if (this.callback != null) {
      try {
        this.callback.failed(exception);
      } catch (Exception e) {
        LOG.warn("Exception catch", e);
      }
    }

    return true;
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    synchronized (this) {
      if (this.completed) {
        return false;
      }
      this.completed = true;
      this.cancelled = true;
      notifyAll();
    }
    if (this.callback != null) {
      this.callback.cancelled();
    }
    return true;
  }


}
