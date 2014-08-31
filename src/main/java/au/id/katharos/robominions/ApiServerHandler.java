package au.id.katharos.robominions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ActionEvent;
import au.id.katharos.robominions.ActionQueue.ActionResult;
import au.id.katharos.robominions.api.RobotApi.RobotRequest;
import au.id.katharos.robominions.api.RobotApi.RobotResponse;

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
        	RobotRequest request = (RobotRequest) msg;
        	ActionEvent event = new ActionEvent(request.getName(), request.getKey(), request.getActionRequest(),
        			new EventFinishedListener() {
        		
        		/**
        		 * The action has to wait for next Bukkit tick, so answer the request when it happens.
        		 */
        		@Override
        		public void call(ActionResult result) {
        			boolean success = result.getSuccess();
        			RobotResponse response = RobotResponse.newBuilder()
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
