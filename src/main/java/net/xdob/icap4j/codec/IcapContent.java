package net.xdob.icap4j.codec;

import io.netty.handler.codec.http.FullHttpMessage;

public interface IcapContent {

  boolean containsHttpMessage();

  FullHttpMessage getFullHttpMessage();

  void setFullHttpMessage(FullHttpMessage httpMessage);
}
