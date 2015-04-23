package au.id.katharos.robominions;

import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ActionEvent;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;

public class ActionExecutor implements Runnable {

	private final ActionQueue actionQueue;
	private final RobotStateManager stateManager;
	private final Logger logger;
	
	public ActionExecutor(
			ActionQueue actionQueue,
			RobotStateManager stateManager,
			Logger logger) {
		this.actionQueue = actionQueue;
		this.stateManager = stateManager;
		this.logger = logger;
		
	}
	
	@Override
	public void run() {
		ActionEvent event = actionQueue.getNextEvent();
		while (event != null) {
			
			AbstractRobot robot = stateManager.getRobot(event.getPlayerName());
			RobotActionRequest actionRequest = event.getActionRequest();
			if (robot != null) {
				// Move chicken according to instruction.
				boolean success = false;
				if (actionRequest.hasMoveDirection()) {
					success = robot.move(actionRequest.getMoveDirection());
				} else if (actionRequest.hasTurnDirection()) {
					success = robot.turn(actionRequest.getTurnDirection());
				} else if (actionRequest.hasMineDirection()) {
					success = robot.mine(actionRequest.getMineDirection());
				} else if (actionRequest.hasPlaceDirection() && actionRequest.hasPlaceMaterial()) {
					success = robot.place(actionRequest.getPlaceDirection(),
							Util.toBukkitMaterial(actionRequest.getPlaceMaterial()));
				} else if (actionRequest.hasChatMessage()) {
					if(actionRequest.getIsPublicMessage()) {
						success = robot.message_all(actionRequest.getChatMessage());
					} else {
						success = robot.message_owner(actionRequest.getChatMessage());
					}
				} else if (actionRequest.hasTeleportLocation()) {
					success = robot.teleport(actionRequest.getTeleportLocation());
				}
				event.getListener().call(new ActionQueue.ActionResult(event.getKey(), success));
			} else {
				logger.info("Attempted to move nonexistant chicken for " + event.getPlayerName());
			}
			// Get next event from queue (null if there is none);
			event = actionQueue.getNextEvent();
		}
	}
}
