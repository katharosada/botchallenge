package au.id.katharos.robominions;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.Future;

import java.util.logging.Logger;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;

public class RobotApiServer implements Runnable {

    private int port;
    private Logger logger;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    boolean running;
    private final ActionQueue actionQueue;

    public RobotApiServer(int port, Logger logger, ActionQueue actionQueue) {
        this.port = port;
        this.logger = logger;
        this.actionQueue = actionQueue;
    }

    public void run() {
    	bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        running = true;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                	 ChannelPipeline pipeline = ch.pipeline();
                	 pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                     pipeline.addLast("protobufDecoder", new ProtobufDecoder(RobotActionRequest.getDefaultInstance()));

                     pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                     pipeline.addLast("protobufEncoder", new ProtobufEncoder());
                	 ch.pipeline().addLast(new ApiServerHandler(logger, actionQueue));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {;
        	logger.warning("API Server was shut down by interruption");
			e.printStackTrace();
		} finally {
			shutDown();
        }
    }
    
    synchronized public void shutDown() {
    	if (running) {
    		logger.warning("Shutting down the API Server.");
    		Future<?> future1 = workerGroup.shutdownGracefully();
    		Future<?> future2 = bossGroup.shutdownGracefully();
    		future1.syncUninterruptibly();
    		future2.syncUninterruptibly();
    		logger.warning("API Server is shut down.");
    		running = false;
    	}
    }
}
