package net.xdob.icap4j.codec;


import io.netty.handler.codec.TooLongFrameException;

/**
 * This class is used to track the size in bytes of headers.
 *
 * @see IcapDecoderUtil
 * @see ReadTrailingHeadersState
 */
public class SizeDelimiter {

  private int counter = 0;
  private int limit;
  private String errorMessage;

  public SizeDelimiter(int limit) {
    this.limit = limit;
    this.errorMessage = "limit exeeded by: ";
  }

  public synchronized void increment(int count) throws DecodingException {
    counter += count;
    checkLimit();
  }

  public void increment() throws DecodingException {
    this.increment(1);
  }

  public int getSize() {
    return counter;
  }

  private void checkLimit() throws DecodingException {
    if (counter >= limit) {
      throw new DecodingException(new TooLongFrameException(errorMessage + "[" + (counter - limit) + "] counts"));
    }
  }
}
