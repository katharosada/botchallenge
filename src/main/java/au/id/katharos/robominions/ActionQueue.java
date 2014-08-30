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
		private final int key;
		private final boolean success;
		public EventResult(int key, boolean success) {
			this.key = key;
			this.success = success;
		}
		
		public int getKey() {
			return key;
		}
		
		public boolean getSuccess() {
			return success;
		}
	}
	
	public static class ActionEvent {
		private final String playerName;
		private final RobotActionRequest actionRequest;
		private final EventFinishedListener listener;
		
		public ActionEvent(
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
	
	private final ConcurrentLinkedQueue<ActionEvent> actionQueue;
	private final Logger logger;
	
	public ActionQueue(Logger logger) {
		actionQueue = new ConcurrentLinkedQueue<ActionEvent>();
		this.logger = logger;
	}
	
	public void addAction(ActionEvent action) {
		actionQueue.add(action);
	}

	/**
	 * Return the next event, this event is removed from the queue.
	 * If there are no more events, it returns null.
	 * 
	 * @return The next event in the queue or null.
	 */
	public ActionEvent getNextEvent() {
		return actionQueue.poll();
	}

}
