package net.xdob.icap4j.codec;

/**
 * Reports a Decoding error of some sort. Like numbers that cannot be parsed.
 */
public class IcapDecodingError extends Error {

  private static final long serialVersionUID = 485693202925398675L;

  public IcapDecodingError(String arg0) {
    super(arg0);
  }
}
