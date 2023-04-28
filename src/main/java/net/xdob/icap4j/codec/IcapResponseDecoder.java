package net.xdob.icap4j.codec;


public class IcapResponseDecoder extends IcapMessageDecoder {

  public IcapResponseDecoder() {
    super();
  }


  @Override
  protected FullIcapMessage createMessage(String[] initialLine) {
    return new DefaultFullResponse(IcapVersion.valueOf(initialLine[0]), IcapStatusCode.fromCode(initialLine[1]));
  }


}
