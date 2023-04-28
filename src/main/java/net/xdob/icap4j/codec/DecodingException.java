package net.xdob.icap4j.codec;

/**
 * Root decoding exception
 */
public class DecodingException extends Exception {

  private static final long serialVersionUID = 1318955320625997060L;

  public DecodingException(Throwable cause) {
    super(cause);
  }
}
