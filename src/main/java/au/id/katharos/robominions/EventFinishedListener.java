package au.id.katharos.robominions;

import au.id.katharos.robominions.ActionQueue.EventResult;

public interface EventFinishedListener {
	public void call(EventResult result);
}
