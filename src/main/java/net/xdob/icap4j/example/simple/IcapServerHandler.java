//package ch.mimo.netty.example.icap.simple;
//
//import ch.mimo.netty.handler.codec.icap.*;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.MessageEvent;
//import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
//import org.jboss.netty.handler.codec.http.HttpHeaders;
//
//import java.nio.charset.Charset;
//
//public class IcapServerHandler extends SimpleChannelUpstreamHandler {
//
//  @Override
//  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//    FullIcapRequest request = (FullIcapRequest) e.getMessage();
//
//    System.out.println(request.toString());
//
//    FullResponse response = new DefaultFullResponse(IcapVersion.ICAP_1_0, IcapStatusCode.OK);
//    IcapElEnum bodyType = request.getBodyType();
//    if (bodyType == null) {
//      bodyType = IcapElEnum.NULLBODY;
//    }
//
//    if (!request.getMethod().equals(IcapMethod.RESPMOD) & request.getHttpRequest() != null) {
//      request.getHttpRequest().addHeader(HttpHeaders.Names.VIA, "icap://my.icap.server");
//      response.setHttpRequest(request.getHttpRequest());
//    }
//    if (request.getHttpResponse() != null) {
//      request.getHttpResponse().addHeader(HttpHeaders.Names.VIA, "icap://my.icap.server");
//      response.setHttpResponse(request.getHttpResponse());
//    }
//    response.addHeader(IcapHeaders.Names.ISTAG, "SimpleServer-version-1.0");
//
//    ChannelBuffer buffer = null;
//    switch (bodyType) {
//      case NULLBODY:
//        // No body in request
//        break;
//      case REQBODY:
//        // http request body in request
//        buffer = request.getHttpRequest().getContent();
//        break;
//      case RESBODY:
//        // http response body in request
//        buffer = request.getHttpResponse().getContent();
//        break;
//      default:
//        // cannot reach here.
//        break;
//    }
//
//    /*
//     * There is also a convenience method that extracts a body from any http message.
//     * @See IcapChunkAggregator#extractHttpBodyContentFromIcapMessage(IcapMessage message).
//     */
//
//    if (buffer != null) {
//      System.out.println(buffer.toString(Charset.defaultCharset()));
//    }
//
//    ctx.getChannel().write(response);
//  }
//
//}
