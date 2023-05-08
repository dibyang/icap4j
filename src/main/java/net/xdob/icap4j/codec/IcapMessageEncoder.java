package net.xdob.icap4j.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


public abstract class IcapMessageEncoder extends MessageToMessageEncoder<FullIcapMessage> {

  public static final int HEAD_MAX_SIZE = 256;
  private final InternalLogger LOG;

  public IcapMessageEncoder() {
    LOG = InternalLoggerFactory.getInstance(getClass());
  }


  @Override
  protected void encode(ChannelHandlerContext ctx, FullIcapMessage msg, List<Object> out) throws Exception {
    LOG.debug("Encoding [" + msg.getClass().getName() + "]");
    // 写入ICAP方法和URI
    ByteBuf headBuffer = ctx.alloc().buffer(HEAD_MAX_SIZE);
    out.add(headBuffer);
    encodeInitialLine(headBuffer, msg);
    encodeHeaders(headBuffer, msg);
    int index = 0;
    Encapsulated encapsulated = new Encapsulated();
    ByteBuf httpBuffer = ctx.alloc().buffer();
    out.add(httpBuffer);
    FullHttpMessage httpMessage = msg.getFullHttpMessage();
    if (httpMessage != null) {

      if (httpMessage instanceof FullHttpRequest) {
        encodeHttpRequestHeader(httpBuffer, (FullHttpRequest) httpMessage);
        encapsulated.addEntry(IcapElEnum.REQHDR, index);
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
        index += httpBuffer.readableBytes();
        if (httpMessage.content().readableBytes() > 0) {
          encapsulated.addEntry(IcapElEnum.REQBODY, index);
        }
      }
      if (httpMessage instanceof FullHttpResponse) {
        encodeHttpResponseHeader(httpBuffer, (FullHttpResponse) httpMessage);
        encapsulated.addEntry(IcapElEnum.RESHDR, index);
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
        index += httpBuffer.readableBytes();
        if (httpMessage.content().readableBytes() > 0) {
          encapsulated.addEntry(IcapElEnum.RESBODY, index);
        }
      }
      if (httpMessage.content().readableBytes() > 0) {
        httpBuffer.writeBytes(Integer.toHexString(httpMessage.content().readableBytes()).getBytes(IcapCodecUtil.ASCII_CHARSET));
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
        httpBuffer.writeBytes(httpMessage.content());
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
        httpBuffer.writeBytes(("0; ieof").getBytes(IcapCodecUtil.ASCII_CHARSET));
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
        httpBuffer.writeBytes(IcapCodecUtil.CRLF);
      }

    } else {
      encapsulated.addEntry(IcapElEnum.NULLBODY, index);
    }

    encapsulated.encode(headBuffer);

  }


  protected abstract int encodeInitialLine(ByteBuf buffer, IcapMessage message) throws Exception;

  private ByteBuf encodeHttpRequestHeader(ByteBuf buffer, FullHttpRequest httpRequest) throws UnsupportedEncodingException {
    if (httpRequest != null) {
      buffer.writeBytes(httpRequest.method().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeByte(IcapCodecUtil.SPACE);
      buffer.writeBytes(httpRequest.uri().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeByte(IcapCodecUtil.SPACE);
      buffer.writeBytes(httpRequest.protocolVersion().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeBytes(IcapCodecUtil.CRLF);
      for (Map.Entry<String, String> h : httpRequest.headers()) {
        encodeHeader(buffer, h.getKey(), h.getValue());
      }
    }
    return buffer;
  }

  private void encodeHttpResponseHeader(ByteBuf buffer, FullHttpResponse httpResponse) throws UnsupportedEncodingException {
    if (httpResponse != null) {
      buffer.writeBytes(httpResponse.protocolVersion().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeByte(IcapCodecUtil.SPACE);
      buffer.writeBytes(httpResponse.status().toString().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeBytes(IcapCodecUtil.CRLF);
      for (Map.Entry<String, String> h : httpResponse.headers()) {
        encodeHeader(buffer, h.getKey(), h.getValue());
      }
    }
  }

//  private int encodeTrailingHeaders(ByteBuf buffer, IcapChunkTrailer chunkTrailer) {
//    int index = buffer.readableBytes();
//    for (Map.Entry<String, String> h : chunkTrailer.getHeaders()) {
//      encodeHeader(buffer, h.getKey(), h.getValue());
//    }
//    return buffer.readableBytes() - index;
//  }

  private int encodeHeaders(ByteBuf buffer, IcapMessage message) {
    int index = buffer.readableBytes();
    for (Map.Entry<String, String> h : message.headers()) {
      encodeHeader(buffer, h.getKey(), h.getValue());
    }
    return buffer.readableBytes() - index;
  }

  private void encodeHeader(ByteBuf buf, String header, String value) {
    buf.writeBytes(header.getBytes(IcapCodecUtil.ASCII_CHARSET));
    buf.writeByte(IcapCodecUtil.COLON);
    buf.writeByte(IcapCodecUtil.SPACE);
    buf.writeBytes(value.getBytes(IcapCodecUtil.ASCII_CHARSET));
    buf.writeBytes(IcapCodecUtil.CRLF);
  }
}
