package au.id.katharos.robominions;

import au.id.katharos.robominions.api.RobotApi.ErrorMessage;
import au.id.katharos.robominions.api.RobotApi.RobotResponse;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Action;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Reason;

public class RobotRequestException extends Exception {
	private static final long serialVersionUID = 1L;
	private Reason reason = Reason.UNKNOWN;
	private Action action = Action.FAIL_ACTION;
	private String message = "The request failed on the server.";
	
	public RobotRequestException(Reason reason, String message) {
		super(message);
		this.message = message;
		this.reason = reason;
	}
	
	public RobotRequestException(Reason reason, String message, Action action) {
		super(message);
		this.message = message;
		this.reason = reason;
		this.action = action;
	}
	
	public RobotResponse getResponse(int requestKey) {
		RobotResponse.Builder response = RobotResponse.newBuilder();
		response.setSuccess(false);
		ErrorMessage errMessage = ErrorMessage.newBuilder()
			.setMessage(message)
			.setReason(reason)
			.setAction(action)
			.build();
		response.setErrorMessage(errMessage);
		response.setKey(requestKey);
		return response.build();
	}
}