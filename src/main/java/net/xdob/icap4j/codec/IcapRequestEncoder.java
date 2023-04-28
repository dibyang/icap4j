package net.xdob.icap4j.codec;

import io.netty.buffer.ByteBuf;


public class IcapRequestEncoder extends IcapMessageEncoder {

  public IcapRequestEncoder() {
    super();
  }

  @Override
  protected int encodeInitialLine(ByteBuf buffer, IcapMessage message) throws Exception {
    FullIcapRequest request = (FullIcapRequest) message;
    int index = buffer.readableBytes();
    buffer.writeBytes(request.getMethod().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
    buffer.writeByte(IcapCodecUtil.SPACE);
    buffer.writeBytes(request.getUri().getBytes(IcapCodecUtil.ASCII_CHARSET));
    buffer.writeByte(IcapCodecUtil.SPACE);
    buffer.writeBytes(request.getProtocolVersion().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
    buffer.writeBytes(IcapCodecUtil.CRLF);
    return buffer.readableBytes() - index;
  }

}
