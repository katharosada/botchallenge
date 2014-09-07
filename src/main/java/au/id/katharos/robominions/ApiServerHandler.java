package au.id.katharos.robominions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
	private final ReadExecutor readExecutor;
	
	public ApiServerHandler(Logger logger, ActionQueue actionQueue, ReadExecutor readExecutor) {
		super();
		this.logger = logger;
		this.actionQueue = actionQueue;
		this.readExecutor = readExecutor;
	}
	
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
        	RobotRequest request = (RobotRequest) msg;
        	if (request.hasActionRequest()) {
	        	ActionEvent event = new ActionEvent(request.getName(), request.getKey(), request.getActionRequest(),
	        			new EventFinishedListener() {
	        		
	        		/*
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
        	} else if (request.hasReadRequest()) {
        		// We do reads asynchronously for speed. This is risky since the world data
        		// might be in an inconsistent state but we'll see if it brings up any problems.
        		RobotResponse response;
				try {
					response = readExecutor.execute(request.getName(), request.getKey(), request.getReadRequest());
				} catch (RobotRequestException e) {
					logger.warning(e.getMessage());
					response = e.getResponse();
				}
        		ctx.write(response);
        		ctx.flush();
        	}        	
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
