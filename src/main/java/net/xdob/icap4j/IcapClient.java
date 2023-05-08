package net.xdob.icap4j;

import net.xdob.icap4j.codec.FullResponse;

import java.io.File;

public interface IcapClient {
  IcapFuture<FullResponse> options(String service, IcapCallback<FullResponse> callback);
  IcapFuture<FullResponse> reqmod(String service, File file, IcapCallback<FullResponse> callback);
  IcapFuture<FullResponse> respmod(String service, File file, IcapCallback<FullResponse> callback);
}
