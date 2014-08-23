package au.id.katharos.robominions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ChickenEvent;
import au.id.katharos.robominions.ActionQueue.EventResult;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;
import au.id.katharos.robominions.api.RobotApi.RobotActionResponse;

public class ApiServerHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger;
	private final ActionQueue actionQueue;
	
	public ApiServerHandler(Logger logger, ActionQueue actionQueue) {
		super();
		this.logger = logger;
		this.actionQueue = actionQueue;
	}
	
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        // Discard the received data silently.
    	logger.info("New channel.");
        try {
        	//ByteBuf buf = (ByteBuf) msg;
        	RobotActionRequest request = (RobotActionRequest) msg;
        	logger.info(request.getName());
        	ChickenEvent event = new ChickenEvent(request.getName(), request, new EventFinishedListener() {					
        		@Override
        		public void call(EventResult result) {
        			boolean success = result.getSuccess();
        			RobotActionResponse response = RobotActionResponse.newBuilder().setSuccess(success).build();
        			ctx.write(response);
        			ctx.flush();
        		}
        	});
        	actionQueue.addAction(event);
        } finally {
        	ReferenceCountUtil.release(msg);
        }
        logger.info("Channel closed.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
	
}
