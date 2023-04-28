package net.xdob.icap4j.codec;


import io.netty.handler.codec.http.*;
import io.netty.util.internal.StringUtil;

import java.util.Map;
import java.util.Optional;

/**
 * This is the main Icap message implementation where
 * all common @see {@link DefaultFullIcapRequest} and @see {@link DefaultFullResponse} member are present.
 */
public abstract class AbstractIcapMessage implements FullIcapMessage {

  private IcapVersion version;
  private final HttpHeaders headers = new DefaultHttpHeaders();

  private FullHttpMessage httpMessage;

  public AbstractIcapMessage(IcapVersion version) {
    this.version = version;
  }

  public HttpHeaders headers() {
    return headers;
  }


  @Override
  public int getPreviewAmount() {
    return Optional.ofNullable(headers.getInt(IcapHeaders.Names.PREVIEW)).orElse(-1);
  }

  @Override
  public IcapVersion getProtocolVersion() {
    return version;
  }

  @Override
  public void setProtocolVersion(IcapVersion version) {
    this.version = version;
  }

  @Override
  public boolean containsHttpMessage() {
    return httpMessage != null;
  }

  @Override
  public FullHttpMessage getFullHttpMessage() {
    return httpMessage;
  }

  @Override
  public void setFullHttpMessage(FullHttpMessage httpMessage) {
    this.httpMessage = httpMessage;
  }

  @Override
  public void setEncapsulated(Encapsulated encapsulated) {
    headers.set(IcapHeaders.Names.ENCAPSULATED, encapsulated.getString());
  }

  @Override
  public Encapsulated getEncapsulated() {
    Encapsulated encapsulated = null;
    String headerValue = headers.get(IcapHeaders.Names.ENCAPSULATED);
    if (headerValue != null) {
      encapsulated = new Encapsulated(headerValue);
    }
    return encapsulated;
  }

  @Override
  public boolean isPreviewMessage() {
    return getPreviewAmount() > 0;
  }

  public boolean isChunked() {
    String transferEncoding = getFullHttpMessage().headers().get(HttpHeaderNames.TRANSFER_ENCODING);
    if (transferEncoding != null) {
      return HttpHeaderValues.CHUNKED.contentEqualsIgnoreCase(transferEncoding);
    }
    return false;
  }


  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getClass().getSimpleName());
    buf.append("(version: ");
    buf.append(getProtocolVersion().getText());
    buf.append(')');
    buf.append(StringUtil.NEWLINE);
    appendHeaders(buf);

    if (httpMessage != null) {
      if (httpMessage instanceof FullHttpRequest) {
        FullHttpRequest httpRequest = (FullHttpRequest) httpMessage;
        buf.append("--- encapsulated HTTP Request ---").append(StringUtil.NEWLINE);
        buf.append(httpRequest.toString());
        if (httpRequest.content() != null && httpRequest.content().readableBytes() > 0) {
          buf.append(StringUtil.NEWLINE).append("--> HTTP Request contains [" + httpRequest.content().readableBytes() + "] bytes of data").append(StringUtil.NEWLINE);
        }
      }
      if (httpMessage instanceof FullHttpResponse) {
        FullHttpResponse httpResponse = (FullHttpResponse) httpMessage;
        buf.append("--- encapsulated HTTP Response ---").append(StringUtil.NEWLINE);
        buf.append(httpResponse.toString());
        if (httpResponse.content() != null && httpResponse.content().readableBytes() > 0) {
          buf.append(StringUtil.NEWLINE).append("--> HTTP Response contains [" + httpResponse.content().readableBytes() + "] bytes of data").append(StringUtil.NEWLINE);

        }
      }
    }


    if (isPreviewMessage()) {
      buf.append("--- Preview ---").append(StringUtil.NEWLINE);
      buf.append("Preview size: " + getPreviewAmount());
    }

    return buf.toString();
  }

  private void appendHeaders(StringBuilder buf) {
    for (Map.Entry<String, String> e : headers) {
      buf.append(e.getKey());
      buf.append(": ");
      buf.append(e.getValue());
      buf.append(StringUtil.NEWLINE);
    }
  }

}
