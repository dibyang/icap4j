package net.xdob.icap4j.codec;

import io.netty.buffer.ByteBuf;

public interface FullResponse extends FullIcapMessage {

  /**
   * Sets the response status
   *
   * @param status @see {@link IcapStatusCode} value like 200 OK.
   */
  void setStatus(IcapStatusCode status);

  /**
   * Gets the response status for this message.
   *
   * @return the response status as @see {@link IcapStatusCode}
   */
  IcapStatusCode getStatus();

  /**
   * Sets an OPTIONS body to this message.
   *
   * @param optionsContent @see {@link ByteBuf} containing the body.
   */
  void setContent(ByteBuf optionsContent);

  /**
   * Gets an OPTIONS body if present
   *
   * @return @see {@link ByteBuf} or null
   */
  ByteBuf getContent();
}
