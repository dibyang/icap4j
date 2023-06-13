package net.xdob.icap4j;

import io.netty.buffer.ByteBuf;
import net.xdob.icap4j.codec.FullResponse;


public interface IcapClient {
  IcapFuture<FullResponse> options(String service, IcapCallback<FullResponse> callback);
  IcapFuture<FullResponse> reqmod(String service, ByteBuf context, IcapCallback<FullResponse> callback);
  IcapFuture<FullResponse> respmod(String service, ByteBuf context, IcapCallback<FullResponse> callback);

  IcapFuture<FullResponse> reqmod(String service, byte[] context, IcapCallback<FullResponse> callback);
  IcapFuture<FullResponse> respmod(String service, byte[] context, IcapCallback<FullResponse> callback);
}
