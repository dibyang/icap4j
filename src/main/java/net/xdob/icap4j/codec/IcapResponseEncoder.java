package net.xdob.icap4j.codec;

import io.netty.buffer.ByteBuf;

/**
 * ICAP Response encoder which takes an @see {@link FullResponse} to encode.
 */
public class IcapResponseEncoder extends IcapMessageEncoder {

  @Override
  protected int encodeInitialLine(ByteBuf buffer, IcapMessage message) {
    FullResponse request = (FullResponse) message;
    int index = buffer.readableBytes();
    buffer.writeBytes(request.getProtocolVersion().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
    buffer.writeByte(IcapCodecUtil.SPACE);
    request.getStatus().toRespInitialLineValue(buffer);
    buffer.writeBytes(IcapCodecUtil.CRLF);
    return buffer.readableBytes() - index;
  }
}
