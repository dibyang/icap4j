package net.xdob.icap4j.codec;

/**
 * Decodes an ICAP Request into @see {@link FullIcapRequest} instance.
 */
public class IcapRequestDecoder extends IcapMessageDecoder {

  public IcapRequestDecoder() {
    super();
  }

  /**
   * @param maxInitialLineLength
   * @param maxIcapHeaderSize
   * @param maxHttpHeaderSize
   * @param maxChunkSize
   * @see IcapMessageDecoder IcapMessageDecoder constructor for more details.
   */
  public IcapRequestDecoder(int maxInitialLineLength, int maxIcapHeaderSize, int maxHttpHeaderSize, int maxChunkSize) {
    //super(maxInitialLineLength, maxIcapHeaderSize, maxHttpHeaderSize, maxChunkSize);
  }

  @Override
  protected FullIcapRequest createMessage(String[] initialLine) {
    return new DefaultFullIcapRequest(IcapVersion.valueOf(initialLine[2]), IcapMethod.valueOf(initialLine[0]), initialLine[1], "");
  }

//  @Override
//  public boolean isDecodingResponse() {
//    return false;
//  }
}
