package au.id.katharos.robominions;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

/**
 * Thread safe action queue to be the communicator between the async Api Server
 * and the Bukkit world.
 */
public class ActionQueue {
	
	public static class EventResult {
		private final boolean success;
		public EventResult(boolean success) {
			this.success = success;
		}
		
		public boolean getSuccess() {
			return success;
		}
	}
	
	public static class ChickenEvent {
		private final String playerName;
		private final RobotActionRequest actionRequest;
		private final EventFinishedListener listener;
		
		public ChickenEvent(
				String playerName, 
				RobotActionRequest direction,
				EventFinishedListener finishedListener) {
			this.playerName = playerName;
			this.actionRequest = direction;
			this.listener = finishedListener;
		}
		
		public String getPlayerName() {
			return playerName;
		}
		
		public RobotActionRequest getActionRequest() {
			return actionRequest;
		}
		
		public EventFinishedListener getListener() {
			return listener;
		}
	}
	
	private final ConcurrentLinkedQueue<ChickenEvent> actionQueue;
	private final Logger logger;
	
	public ActionQueue(Logger logger) {
		actionQueue = new ConcurrentLinkedQueue<ChickenEvent>();
		this.logger = logger;
	}
	
	public void addAction(ChickenEvent action) {
		actionQueue.add(action);
	}

	/**
	 * Return the next event, this event is removed from the queue.
	 * If there are no more events, it returns null.
	 * 
	 * @return The next event in the queue or null.
	 */
	public ChickenEvent getNextEvent() {
		return actionQueue.poll();
	}

}
