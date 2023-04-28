package net.xdob.icap4j.codec;


import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

/**
 * Main Icap Response implementation. This is the starting point to create any Icap response.
 */
public class DefaultFullResponse extends AbstractIcapMessage implements FullResponse {

  private IcapStatusCode status;
  private ByteBuf optionsContent;

  /**
   * Will create an instance of FullResponse.
   *
   * @param version the version of the response.
   * @param status  the Status code that has to be reported back. (200 OK...)
   */
  public DefaultFullResponse(IcapVersion version, IcapStatusCode status) {
    super(version);
    this.status = status;
  }

  @Override
  public void setStatus(IcapStatusCode status) {
    this.status = status;
  }

  @Override
  public IcapStatusCode getStatus() {
    return status;
  }

  public void setContent(ByteBuf optionsContent) {
    this.optionsContent = optionsContent;
  }

  public ByteBuf getContent() {
    return optionsContent;
  }

  @Override
  public String toString() {
    return super.toString() + StringUtil.NEWLINE + "Response Status: " + status.name();
  }
}
