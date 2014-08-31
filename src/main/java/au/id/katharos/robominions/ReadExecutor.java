package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import au.id.katharos.robominions.api.Materials;
import au.id.katharos.robominions.api.Materials.Material.Type;
import au.id.katharos.robominions.api.RobotApi.Coordinate;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Action;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Reason;
import au.id.katharos.robominions.api.RobotApi.MaterialResponse;
import au.id.katharos.robominions.api.RobotApi.WorldLocation;
import au.id.katharos.robominions.api.RobotApi.RobotReadRequest;
import au.id.katharos.robominions.api.RobotApi.RobotResponse;

/**
 * The executor of read requests. These read requests happen asynchronously so that read-only
 * world state queries don't have to wait for the every 50ms server tick. 
 * 
 * Calling any of the Bukkit api functions outside of the Bukkit execution context is strictly
 * forbidden. We're ignoring this but it is vital that nothing in this class changes world state,
 * this is READ-ONLY.
 */
public class ReadExecutor {

	private final Logger logger;
	private final HashMap<String, AbstractRobot> robotMap;
	
	public ReadExecutor(Logger logger, HashMap<String, AbstractRobot> robotMap) {
		this.logger = logger;
		this.robotMap = robotMap;
	}
	
	private static Location coordToLocation(World world, Coordinate coord) {
		return new Location(world, coord.getX(), coord.getY(), coord.getZ());
	}
	
	public RobotResponse execute(String playerName, int key, RobotReadRequest readRequest) 
		throws ReadException {
		RobotResponse.Builder response = RobotResponse.newBuilder();
		response.setKey(key);
		
		AbstractRobot robot = robotMap.get(playerName);
		if (robot == null) {
			throw new ReadException(
					Reason.ROBOT_DOES_NOT_EXIST, 
					"The robot does not exist.",
					Action.EXIT_CLIENT);
		}
		
		if (readRequest.hasLocateEntity()) {
			// TODO:
			throw new ReadException(Reason.NOT_IMPLEMENTED, "Locating entities is not yet implemented.");	
		} else if (readRequest.hasIdentifyMaterial()) {
			WorldLocation loc = readRequest.getIdentifyMaterial();
			Block block = null;
			if (loc.hasDirection()) {
				block = robot.getBlockFromDirection(loc.getDirection());
			} else if (loc.hasAbsoluteLocation()) {
				Coordinate coord = loc.getAbsoluteLocation();
				World world = robot.getWorld();
				Location location = coordToLocation(world, coord);
				boolean canSee = robot.isLocationVisible(location);
				if (canSee) {
					block = location.getBlock();
				} else {
					throw new ReadException(Reason.BLOCK_IS_NOT_VISIBLE, "The robot can't see that block.");
				}
			} else {
				throw new ReadException(Reason.INVALID_REQUEST, "Location not recognised.");
			}
			// TODO: Put this enum conversion logic in a util somewhere
			Materials.Material material = Materials.Material.newBuilder()
					.setType(Type.valueOf(block.getTypeId())).build();
			response.setSuccess(true);
			response.setMaterialResponse(MaterialResponse.newBuilder().addMaterials(material).build());
		} else if (readRequest.hasLocateMaterialNearby()) {
			// TODO:
			throw new ReadException(Reason.NOT_IMPLEMENTED, "Searching nearby locations is not implemented yet.")
		} else {
			throw new ReadException(Reason.INVALID_REQUEST, "The read request has no recognised request in it.");
		}
		return response.build();
	}
	
	public static class ReadException extends Exception {
		private static final long serialVersionUID = 1L;
		private Reason reason = Reason.UNKNOWN;
		private Action action = Action.FAIL_ACTION;
		private String message = "The request failed on the server.";
		
		public ReadException(Reason reason, String message) {
			super(message);
			this.message = message;
			this.reason = reason;
		}
		
		public ReadException(Reason reason, String message, Action action) {
			super(message);
			this.message = message;
			this.reason = reason;
			this.action = action;
		}
		
		public RobotResponse getResponse() {
			RobotResponse.Builder response = RobotResponse.newBuilder();
			response.setSuccess(false);
			ErrorMessage errMessage = ErrorMessage.newBuilder()
				.setMessage(message)
				.setReason(reason)
				.setAction(action)
				.build();
			response.setErrorMessage(errMessage);
			return response.build();
		}
	}

}
