package net.xdob.icap4j.codec;


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

}
