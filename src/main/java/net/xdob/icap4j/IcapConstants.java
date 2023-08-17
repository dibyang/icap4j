package net.xdob.icap4j;

import io.netty.util.AttributeKey;
import net.xdob.icap4j.codec.FullResponse;

import java.util.concurrent.Semaphore;

public interface IcapConstants {
  AttributeKey<IcapFuture<FullResponse>> FUTURE = AttributeKey.newInstance("icap-future");
  AttributeKey<Semaphore> SEMAPHORE = AttributeKey.newInstance("icap-semaphore");
  AttributeKey<IcapClientContext> CONTEXT = AttributeKey.newInstance("icap-context");

}
