//package ch.mimo.netty.example.icap.simple;
//
//import org.jboss.netty.bootstrap.ServerBootstrap;
//import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
//
//import java.net.InetSocketAddress;
//import java.util.concurrent.Executors;
//
///**
// * An ICAP Server that prints the request and sends back the content of the request that was receveid.
// * OPTIONS is not supported.
// */
//public class IcapServer {
//
//  public static void main(String[] args) {
//    // Configure the server.
//    ServerBootstrap bootstrap = new ServerBootstrap(
//        new OioServerSocketChannelFactory(
//            Executors.newCachedThreadPool(),
//            Executors.newCachedThreadPool()));
//
//    // Set up the event pipeline factory.
//    bootstrap.setPipelineFactory(new IcapServerChannelPipeline());
//
//    // Bind and start to accept incoming connections.
//    bootstrap.bind(new InetSocketAddress(8099));
//  }
//}
