package net.xdob.icap4j.codec;


import io.netty.handler.codec.http.HttpHeaders;


public interface IcapMessage {

  IcapVersion getProtocolVersion();

  void setProtocolVersion(IcapVersion version);

  boolean isPreviewMessage();

  HttpHeaders headers();

  void setEncapsulated(Encapsulated encapsulated);

  /**
   * 注意每次获取都是新的对象
   *
   * @return
   */
  Encapsulated getEncapsulated();


  /**
   * @return the @see {@link Integer} preview header value.
   */
  int getPreviewAmount();


  boolean isChunked();
}
