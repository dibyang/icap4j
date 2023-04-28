package net.xdob.icap4j;

public interface IcapCallback<T> {

  void completed(T result);

  void failed(Throwable ex);

  void cancelled();

}
