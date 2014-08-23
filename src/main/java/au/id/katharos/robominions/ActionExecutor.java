package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.logging.Logger;

import au.id.katharos.robominions.ActionQueue.ChickenEvent;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;

public class ActionExecutor implements Runnable {

	private final ActionQueue actionQueue;
	private final HashMap<String, RobotChicken> chickenMap;
	private final Logger logger;
	
	public ActionExecutor(ActionQueue actionQueue, HashMap<String, RobotChicken> chickenMap, Logger logger) {
		this.actionQueue = actionQueue;
		this.chickenMap = chickenMap;
		this.logger = logger;
	}

	private RobotChicken getChicken(String playerName) {
		if (chickenMap.containsKey(playerName)) {
			return chickenMap.get(playerName);
		}
		return null;
	}
	
	@Override
	public void run() {
		ChickenEvent event = actionQueue.getNextEvent();
		while (event != null) {
			
			RobotChicken chicken = getChicken(event.getPlayerName());
			RobotActionRequest actionRequest = event.getActionRequest();
			if (chicken != null) {
				// Move chicken according to instruction.
				boolean success = false;
				if (actionRequest.hasMoveDirection()) {
					success = chicken.move(event.getActionRequest().getMoveDirection());
				} else if (actionRequest.hasTurnDirection()) {
					success = chicken.turn(event.getActionRequest().getTurnDirection());
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
