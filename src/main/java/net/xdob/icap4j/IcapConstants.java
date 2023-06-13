package net.xdob.icap4j;

import io.netty.util.AttributeKey;
import net.xdob.icap4j.codec.FullResponse;

import java.util.concurrent.Semaphore;

public interface IcapConstants {
  AttributeKey<IcapFuture<FullResponse>> FUTURE = AttributeKey.newInstance("future");
  AttributeKey<Semaphore> SEMAPHORE = AttributeKey.newInstance("semaphore");

}
