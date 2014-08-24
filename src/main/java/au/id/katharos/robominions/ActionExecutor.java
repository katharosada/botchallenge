package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ChickenEvent;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;

public class ActionExecutor implements Runnable {

	private final ActionQueue actionQueue;
	private final HashMap<String, AbstractRobot> robotMap;
	private final Logger logger;
	
	public ActionExecutor(ActionQueue actionQueue, HashMap<String, AbstractRobot> chickenMap, Logger logger) {
		this.actionQueue = actionQueue;
		this.robotMap = chickenMap;
		this.logger = logger;
	}

	private AbstractRobot getChicken(String playerName) {
		if (robotMap.containsKey(playerName)) {
			return robotMap.get(playerName);
		}
		return null;
	}
	
	@Override
	public void run() {
		ChickenEvent event = actionQueue.getNextEvent();
		while (event != null) {
			
			AbstractRobot robot = getChicken(event.getPlayerName());
			RobotActionRequest actionRequest = event.getActionRequest();
			if (robot != null) {
				// Move chicken according to instruction.
				boolean success = false;
				if (actionRequest.hasMoveDirection()) {
					success = robot.move(event.getActionRequest().getMoveDirection());
				} else if (actionRequest.hasTurnDirection()) {
					success = robot.turn(event.getActionRequest().getTurnDirection());
				}
				event.getListener().call(new ActionQueue.EventResult(success));
			} else {
				logger.info("Attempted to move nonexistant chicken for " + event.getPlayerName());
			}
			// Get next event from queue (null if there is none);
			event = actionQueue.getNextEvent();
		}
	}
}
