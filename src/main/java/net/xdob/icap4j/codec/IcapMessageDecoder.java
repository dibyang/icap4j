package net.xdob.icap4j.codec;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

import java.util.List;

public abstract class IcapMessageDecoder extends ByteToMessageDecoder {
  private static final String SYNTHETIC_ENCAPSULATED_HEADER_VALUE = "null-body=0";

  private static final int MAX_INITIAL_LINE_LENGTH = 1024;
  private static final int MAX_HEADER_SIZE = 8192;
  private static final int MAX_CHUNK_SIZE = 8192;

  private enum State {
    READ_INITIAL, READ_HEADER, READ_HTTP_HEADER, READ_CONTENT, READ_CHUNK_SIZE, READ_CHUNK_CONTENT, READ_CHUNK_END, BAD_MESSAGE
  }

  private State state = State.READ_INITIAL;

  private int initialLineLength;
  private int contentSize;

  private int chunkSize;
  private int chunkContentSize;

  protected FullIcapMessage message;

  protected abstract FullIcapMessage createMessage(String[] initialLine);

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    switch (state) {
      case READ_INITIAL:
        if (!readInitialLine(in)) {
          return;
        }
        state = State.READ_HEADER;
      case READ_HEADER:
        if (!readHeader(in)) {
          return;
        }
      case READ_HTTP_HEADER:
        if (!readHttpHeader(in)) {
          return;
        }
        if (message.isChunked()) {
          state = State.READ_CHUNK_SIZE;
        } else {
          state = State.READ_CONTENT;
        }
      case READ_CONTENT:
        if (!readContent(in)) {
          return;
        }
        out.add(message);
        reset();
        state = State.READ_INITIAL;
        break;
      case READ_CHUNK_SIZE:
        if (!readChunkSize(in)) {
          return;
        }
        state = State.READ_CHUNK_CONTENT;
      case READ_CHUNK_CONTENT:
        if (!readChunkContent(in)) {
          return;
        }
        if (chunkContentSize == chunkSize) {
          state = State.READ_CHUNK_END;
        }
        break;
      case READ_CHUNK_END:
        if (!readChunkEnd(in)) {
          return;
        }
        if (chunkSize == 0) {
          out.add(message);
          reset();
          state = State.READ_INITIAL;
        } else {
          state = State.READ_CHUNK_SIZE;
        }
        break;
      case BAD_MESSAGE:
        in.skipBytes(in.readableBytes());
        break;
    }
  }

  private boolean readInitialLine(ByteBuf in) throws Exception {
    int readerIndex = in.readerIndex();
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    int end = in.writerIndex();
    if (length > MAX_INITIAL_LINE_LENGTH) {
      in.readerIndex(end);
      state = State.BAD_MESSAGE;
      throw new TooLongFrameException("initial line too long");
    }

    String initialLine = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
    String[] initialLineParts = initialLine.split(" ");
    if (initialLineParts.length != 3) {
      in.readerIndex(end);
      state = State.BAD_MESSAGE;
      throw new CorruptedFrameException("invalid initial line: " + initialLine);
    }

    message = this.createMessage(initialLineParts);
    initialLineLength = length + 2;
    in.readerIndex(readerIndex + initialLineLength);

    return true;
  }

  private void handleEncapsulationHeaderVolatility(IcapMessage message) {
    // Pseudo code
    // IF Encapsulated header is missing
    // IF OPTIONS request OR 100 Continue response OR 204 No Content response
    // THEN inject synthetic null-body Encapsulated header.
    boolean requiresSynthecticEncapsulationHeader = false;
    if (!message.headers().contains(IcapHeaders.Names.ENCAPSULATED)) {
      if (message instanceof FullIcapRequest && ((FullIcapRequest) message).getMethod().equals(IcapMethod.OPTIONS)) {
        requiresSynthecticEncapsulationHeader = true;
      } else if (message instanceof FullResponse) {
        FullResponse response = (FullResponse) message;
        IcapStatusCode status = response.getStatus();
        if (status.equals(IcapStatusCode.CONTINUE) | status.equals(IcapStatusCode.NO_CONTENT)) {
          requiresSynthecticEncapsulationHeader = true;
        }
      }
    }

    if (requiresSynthecticEncapsulationHeader) {
      message.headers().set(IcapHeaders.Names.ENCAPSULATED, SYNTHETIC_ENCAPSULATED_HEADER_VALUE);
    }
  }

  private boolean readHeader(ByteBuf in) throws Exception {

    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    int end = in.writerIndex();
    if (length > MAX_HEADER_SIZE) {
      in.readerIndex(end);
      state = State.BAD_MESSAGE;
      throw new TooLongFrameException("header too long");
    }

    List<String[]> headerList = IcapDecoderUtil.readHeaders(in, MAX_HEADER_SIZE);

    for (String[] header : headerList) {
      message.headers().set(header[0], header[1]);
    }

    handleEncapsulationHeaderVolatility(message);

    return true;
  }

  private boolean readHttpHeader(ByteBuf in) throws Exception {
    //每次获取都是新的
    Encapsulated encapsulated = message.getEncapsulated();
    IcapElEnum nextEntry = encapsulated.getNextEntry();
    while (nextEntry != null) {
      if (nextEntry.equals(IcapElEnum.REQHDR)) {
        int readerIndex = in.readerIndex();
        int length = findCRLF(in);
        if (length < 0) {
          return false;
        }
        int end = in.writerIndex();
        if (length > MAX_HEADER_SIZE) {
          in.readerIndex(end);
          state = State.BAD_MESSAGE;
          throw new TooLongFrameException("header too long");
        }

        String line = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
        in.readerIndex(readerIndex + length + 2);
        List<String> initialLine = Splitter.on(' ').splitToList(line);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf(initialLine.get(2)), HttpMethod.valueOf(initialLine.get(0)), initialLine.get(1));
        message.setFullHttpMessage(request);
        FullHttpMessage httpMessage = message.getFullHttpMessage();
        List<String[]> headerList = IcapDecoderUtil.readHeaders(in, MAX_HEADER_SIZE);

        for (String[] header : headerList) {
          httpMessage.headers().set(header[0], header[1]);
        }

      }
      if (nextEntry.equals(IcapElEnum.RESHDR)) {

        int readerIndex = in.readerIndex();
        int length = findCRLF(in);
        if (length < 0) {
          return false;
        }
        int end = in.writerIndex();
        if (length > MAX_HEADER_SIZE) {
          in.readerIndex(end);
          state = State.BAD_MESSAGE;
          throw new TooLongFrameException("header too long");
        }

        String line = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
        in.readerIndex(readerIndex + length + 2);
        List<String> initialLine = Splitter.on(' ').splitToList(line);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.valueOf(initialLine.get(0)), HttpResponseStatus.valueOf(Integer.parseInt(initialLine.get(1))));
        message.setFullHttpMessage(response);
        FullHttpMessage httpMessage = message.getFullHttpMessage();
        List<String[]> headerList = IcapDecoderUtil.readHeaders(in, MAX_HEADER_SIZE);
        for (String[] header : headerList) {
          httpMessage.headers().set(header[0], header[1]);
        }
      }
      encapsulated.setEntryAsProcessed(nextEntry);
      nextEntry = encapsulated.getNextEntry();
    }

    return true;
  }

  private boolean readContent(ByteBuf in) throws Exception {
    FullHttpMessage httpMessage = message.getFullHttpMessage();
    int contentLength = httpMessage.content().readableBytes();
    int length = Math.min(in.readableBytes(), contentSize - contentLength);
    if (length > 0) {
      httpMessage.content().writeBytes(in, length);
    }

    if (contentLength + length >= contentSize) {
      return true;
    }

    return false;
  }

  private boolean readChunkSize(ByteBuf in) throws Exception {
    int readerIndex = in.readerIndex();
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    String chunkSizeHex = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
    try {
      chunkSize = Integer.parseInt(chunkSizeHex, 16);
    } catch (NumberFormatException e) {
      state = State.BAD_MESSAGE;
      throw new CorruptedFrameException("invalid chunk size: " + chunkSizeHex);
    }

    if (chunkSize > MAX_CHUNK_SIZE) {
      state = State.BAD_MESSAGE;
      throw new TooLongFrameException("chunk size too long: " + chunkSize);
    }

    if (chunkSize == 0) {
      in.readerIndex(readerIndex + length + 2);
      return true;
    }

    chunkContentSize = 0;
    in.readerIndex(readerIndex + length + 2);
    return true;
  }

  private boolean readChunkContent(ByteBuf in) throws Exception {
    ByteBuf content = message.getFullHttpMessage().content();
    int contentLength = content.readableBytes();
    int length = Math.min(in.readableBytes(), chunkSize - chunkContentSize);
    if (length > 0) {
      content.writeBytes(in, length);
    }
    chunkContentSize += length;
    return chunkContentSize == chunkSize;
  }

  private boolean readChunkEnd(ByteBuf in) throws Exception {
    int readerIndex = in.readerIndex();
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }

    if (length > 0) {
      in.readerIndex(readerIndex + length + 2);
    } else {
      in.readerIndex(readerIndex + 2);
    }

    return true;
  }

  private int findCRLF(ByteBuf in) {
    int i = in.forEachByte(ByteProcessor.FIND_LF);
    if (i > 0 && in.getByte(i - 1) == '\r') {
      return i - 1 - in.readerIndex();
    } else {
      return -1;
    }
  }

  private void reset() {
    initialLineLength = 0;
    contentSize = 0;
    chunkSize = 0;
    chunkContentSize = 0;
    message = null;
  }
}

