package net.xdob.icap4j.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import net.xdob.icap4j.IcapConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;

public abstract class IcapMessageDecoder extends ByteToMessageDecoder {
  static final Logger LOG = LoggerFactory.getLogger(IcapMessageDecoder.class);

  private static final String SYNTHETIC_ENCAPSULATED_HEADER_VALUE = "null-body=0";

  private static final int MAX_INITIAL_LINE_LENGTH = 1024;
  private static final int MAX_HEADER_SIZE = 8192;
  private static final int MAX_CHUNK_SIZE = 8192;



  private enum State {
    READ_INITIAL, READ_HEADER, READ_HTTP_HEADER, READ_CONTENT, READ_CHUNK_SIZE, READ_CHUNK_CONTENT, READ_CHUNK_END, BAD_MESSAGE
  }

  private class Context{
    private State state = State.READ_INITIAL;

    private int initialLineLength;
    private int contentSize;

    private int chunkSize;
    private int chunkContentSize;

    private FullIcapMessage message;

    public void reset(){
      initialLineLength = 0;
      contentSize = 0;
      chunkSize = 0;
      chunkContentSize = 0;
      message = null;
    }
  }

  protected Context context = new Context();



  protected abstract FullIcapMessage createMessage(String[] initialLine);

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    switch (context.state) {
      case READ_INITIAL:
        if (!readInitialLine(context, in)) {
          return;
        }
        context.state = State.READ_HEADER;
      case READ_HEADER:
        if (!readHeader(context, in)) {
          return;
        }
      case READ_HTTP_HEADER:
        if (!readHttpHeader(context, in)) {
          return;
        }
        //out.add(context.message);
        //context.reset();
        //context.state = State.READ_INITIAL;
        Encapsulated encapsulated = context.message.getEncapsulated();
        if(encapsulated!=null&&
            (encapsulated.containsEntry(IcapElEnum.REQBODY)
            ||encapsulated.containsEntry(IcapElEnum.RESBODY)
            ||encapsulated.containsEntry(IcapElEnum.OPTBODY))) {
          if (context.message.isChunked()) {
            context.state = State.READ_CHUNK_SIZE;
          } else {
            context.state = State.READ_CONTENT;
          }
        }else{
          out.add(context.message);
          context.reset();
          context.state = State.READ_INITIAL;
        }
      case READ_CONTENT:
        if (!readContent(context, in)) {
          return;
        }
        out.add(context.message);
        context.reset();
        context.state = State.READ_INITIAL;
        break;
      case READ_CHUNK_SIZE:
        if (!readChunkSize(context, in)) {
          return;
        }
        context.state = State.READ_CHUNK_CONTENT;
      case READ_CHUNK_CONTENT:
        if (!readChunkContent(context, in)) {
          return;
        }
        if (context.chunkContentSize == context.chunkSize) {
          context.state = State.READ_CHUNK_END;
        }
        break;
      case READ_CHUNK_END:
        if (!readChunkEnd(in)) {
          return;
        }
        if (context.chunkSize == 0) {
          out.add(context.message);
          context.reset();
          context.state = State.READ_INITIAL;
        } else {
          context.state = State.READ_CHUNK_SIZE;
        }
        break;
      case BAD_MESSAGE:
        in.skipBytes(in.readableBytes());
        break;
    }
  }

  private boolean readInitialLine(Context context, ByteBuf in) throws Exception {
    if(context.message!=null){
      return true;
    }
    int readerIndex = in.readerIndex();
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    int end = in.writerIndex();
    if (length > MAX_INITIAL_LINE_LENGTH) {
      in.readerIndex(end);
      context.state = State.BAD_MESSAGE;
      throw new TooLongFrameException("initial line too long");
    }

    String initialLine = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
    if (!initialLine.startsWith("ICAP/")) {
      in.readerIndex(end);
      context.state = State.BAD_MESSAGE;
      //System.out.println("initialLine = " + initialLine);
      throw new CorruptedFrameException("invalid initial line: " + initialLine);
    }
    String[] initialLineParts = initialLine.split(" ");

    context.message = this.createMessage(initialLineParts);
    context.initialLineLength = length + 2;
    in.readerIndex(readerIndex + context.initialLineLength);

    return true;
  }

  private void handleEncapsulationHeaderVolatility(IcapMessage message) {
    // Pseudo code
    // IF Encapsulated header is missing
    // IF OPTIONS request OR 100 Continue response OR 204 No Content response
    // THEN inject synthetic null-body Encapsulated header.
    boolean requiresSynthecticEncapsulationHeader = false;
    if (!message.headers().contains(IcapHeaderNames.ENCAPSULATED)) {
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
      message.headers().set(IcapHeaderNames.ENCAPSULATED, SYNTHETIC_ENCAPSULATED_HEADER_VALUE);
    }
  }

  private boolean readHeader(Context context, ByteBuf in) throws Exception {
    if(context.message!=null&&!context.message.headers().isEmpty()){
      return true;
    }
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    int end = in.writerIndex();
    if (length > MAX_HEADER_SIZE) {
      in.readerIndex(end);
      context.state = State.BAD_MESSAGE;
      throw new TooLongFrameException("header too long");
    }

    List<String[]> headerList = IcapDecoderUtil.readHeaders(in, MAX_HEADER_SIZE);

    for (String[] header : headerList) {
      context.message.headers().set(header[0], header[1]);
    }
    if(!context.message.headers().contains(IcapHeaderNames.ENCAPSULATED)){
      LOG.warn("not find encapsulated, headers = {}", context.message.headers());
    }

    handleEncapsulationHeaderVolatility(context.message);

    return true;
  }

  private boolean readHttpHeader(Context context, ByteBuf in) throws Exception {
    //每次获取都是新的
    Encapsulated encapsulated = context.message.getEncapsulated();
    if(encapsulated==null){
      return true;
    }
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
          context.state = State.BAD_MESSAGE;
          throw new TooLongFrameException("header too long");
        }

        String line = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
        in.readerIndex(readerIndex + length + 2);
        String[] initialLine = line.split(" ");
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]), initialLine[1]);
        context.message.setFullHttpMessage(request);
        FullHttpMessage httpMessage = context.message.getFullHttpMessage();
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
          context.state = State.BAD_MESSAGE;
          throw new TooLongFrameException("header too long");
        }

        String line = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
        if(!line.startsWith("HTTP")) {
          LOG.warn("not start with http, line = ", line);
        }
        in.readerIndex(readerIndex + length + 2);
        String[] initialLine = line.split(" ");
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.valueOf(initialLine[0]), HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1])));
        context.message.setFullHttpMessage(response);
        FullHttpMessage httpMessage = context.message.getFullHttpMessage();
        List<String[]> headerList = IcapDecoderUtil.readHeaders(in, MAX_HEADER_SIZE);
        for (String[] header : headerList) {
          httpMessage.headers().set(header[0], header[1]);
        }
      }
      encapsulated.setEntryAsProcessed(nextEntry);
      nextEntry = encapsulated.getNextEntry();
    }

    return context.message!=null;
  }

  private boolean readContent(Context context, ByteBuf in) throws Exception {
    FullHttpMessage httpMessage = context.message.getFullHttpMessage();
    if(context.contentSize==0) {
      int readerIndex = in.readerIndex();
      int sizeLength = findCRLF(in);
      if (sizeLength > 0) {
        String sizeHex = in.toString(readerIndex, sizeLength, CharsetUtil.US_ASCII);
        context.contentSize = Integer.parseInt(sizeHex, 16);
        in.readerIndex(readerIndex + sizeLength);
      }
    }
    if(context.contentSize>0) {
      int contentLength = httpMessage.content().readableBytes();
      int length = Math.min(in.readableBytes(), context.contentSize - contentLength);
      if (length > 0) {
        httpMessage.content().writeBytes(in, length);
      }
      if (contentLength + length >= context.contentSize) {
        return true;
      }
      return false;
    }else{
      httpMessage.content().writeBytes(in, in.readableBytes());
      return true;
    }
  }

  private boolean readChunkSize(Context context, ByteBuf in) throws Exception {
    int readerIndex = in.readerIndex();
    int length = findCRLF(in);
    if (length < 0) {
      return false;
    }
    String chunkSizeHex = in.toString(readerIndex, length, CharsetUtil.US_ASCII);
    try {
      context.chunkSize = Integer.parseInt(chunkSizeHex, 16);
    } catch (NumberFormatException e) {
      context.state = State.BAD_MESSAGE;
      throw new CorruptedFrameException("invalid chunk size: " + chunkSizeHex);
    }

    if (context.chunkSize > MAX_CHUNK_SIZE) {
      context.state = State.BAD_MESSAGE;
      throw new TooLongFrameException("chunk size too long: " + context.chunkSize);
    }

    if (context.chunkSize == 0) {
      in.readerIndex(readerIndex + length + 2);
      return true;
    }

    context.chunkContentSize = 0;
    in.readerIndex(readerIndex + length + 2);
    return true;
  }

  private boolean readChunkContent(Context context, ByteBuf in) throws Exception {
    ByteBuf content = context.message.getFullHttpMessage().content();
    int contentLength = content.readableBytes();
    int length = Math.min(in.readableBytes(), context.chunkSize - context.chunkContentSize);
    if (length > 0) {
      content.writeBytes(in, length);
    }
    context.chunkContentSize += length;
    return context.chunkContentSize == context.chunkSize;
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

}

