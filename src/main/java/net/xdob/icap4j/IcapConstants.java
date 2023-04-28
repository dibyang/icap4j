package net.xdob.icap4j;

import io.netty.util.AttributeKey;
import net.xdob.icap4j.codec.FullResponse;

public interface IcapConstants {
  AttributeKey<IcapFuture<FullResponse>> FUTURE = AttributeKey.newInstance("future");

}
