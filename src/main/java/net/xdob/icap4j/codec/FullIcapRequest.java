package net.xdob.icap4j.codec;


/**
 * Defines a ICAP Request.
 *
 * @see IcapMessage
 * @see DefaultFullIcapRequest
 */
public interface FullIcapRequest extends FullIcapMessage {

  /**
   * Sets the operation method for this icap request.
   *
   * @param method the @see {@link IcapMethod} provided by @see {@link IcapMethod}
   * @return self in order to chain the method calls
   */
  FullIcapRequest setMethod(IcapMethod method);

  /**
   * @return This operations method
   */
  IcapMethod getMethod();

  /**
   * Sets the operations uri.
   *
   * @param uri
   * @return self in order to chain the method calls
   */
  FullIcapRequest setUri(String uri);

  /**
   * @return String uri for this message
   */
  String getUri();

  String getHost();

  FullIcapRequest setHost(String host);

  String getService();

  FullIcapRequest setService(String service);
}
