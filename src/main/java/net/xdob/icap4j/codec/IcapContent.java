package net.xdob.icap4j.codec;

import io.netty.handler.codec.http.FullHttpMessage;

import java.io.File;


public interface IcapContent {


  boolean containsHttpMessage();

  FullHttpMessage getFullHttpMessage();

  void setFullHttpMessage(FullHttpMessage httpMessage);

}
