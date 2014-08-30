package au.id.katharos.robominions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ActionEvent;
import au.id.katharos.robominions.ActionQueue.EventResult;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;
import au.id.katharos.robominions.api.RobotApi.RobotActionResponse;

/**
 * Handler for any incoming client requests.
 */
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
        try {
        	RobotActionRequest request = (RobotActionRequest) msg;
        	ActionEvent event = new ActionEvent(request.getName(), request, new EventFinishedListener() {
        		
        		/**
        		 * The action has to wait for next Bukkit tick, so answer the request when it happens.
        		 */
        		@Override
        		public void call(EventResult result) {
        			boolean success = result.getSuccess();
        			RobotActionResponse response = RobotActionResponse.newBuilder()
        					.setKey(result.getKey())
        					.setSuccess(success).build();
        			ctx.write(response);
        			ctx.flush();
        		}
        	});
        	actionQueue.addAction(event);
        } finally {
        	ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
	
}
