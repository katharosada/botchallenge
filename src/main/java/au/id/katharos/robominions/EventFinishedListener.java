package au.id.katharos.robominions;

import au.id.katharos.robominions.ActionQueue.ActionResult;

public interface EventFinishedListener {
	public void call(ActionResult result);
}
