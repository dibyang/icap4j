package net.xdob.icap4j.codec;

/**
 * ICAP Response decoder which creates an @see {@link FullResponse} instance.
 */
public class IcapResponseDecoder extends IcapMessageDecoder {

  public IcapResponseDecoder() {
    super();
  }

  /**
   * @param maxInitialLineLength
   * @param maxIcapHeaderSize
   * @param maxHttpHeaderSize
   * @param maxChunkSize
   * @see IcapMessageDecoder IcapMessageDecoder constructor for more details.
   */
//  public IcapResponseDecoder(int maxInitialLineLength, int maxIcapHeaderSize, int maxHttpHeaderSize, int maxChunkSize) {
//    super(maxInitialLineLength, maxIcapHeaderSize, maxHttpHeaderSize, maxChunkSize);
//  }
  @Override
  protected FullIcapMessage createMessage(String[] initialLine) {
    return new DefaultFullResponse(IcapVersion.valueOf(initialLine[0]), IcapStatusCode.fromCode(initialLine[1]));
  }


}
