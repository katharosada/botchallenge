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
	
	public RobotResponse execute(String playerName, int key, RobotReadRequest readRequest) {
		RobotResponse.Builder response = RobotResponse.newBuilder();
		response.setKey(key);
		
		AbstractRobot robot = robotMap.get(playerName);
		if (robot == null) {
			response.setSuccess(false);
			ErrorMessage.Builder message = ErrorMessage.newBuilder();
			message.setMessage("The robot does not exist.");
			message.setReason(Reason.ROBOT_DOES_NOT_EXIST);
			message.setAction(Action.EXIT_CLIENT);
			response.setErrorMessage(message.build());
			return response.build();
		}
		
		if (readRequest.hasLocateEntity()) {
			// Not yet implemented			
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
					// TODO: Throw an exception which generates the response
					response.setSuccess(false);
					ErrorMessage.Builder message = ErrorMessage.newBuilder();
					message.setMessage("That block is not visible to the robot.");
					message.setReason(Reason.BLOCK_IS_NOT_VISIBLE);
					message.setAction(Action.FAIL_ACTION);
					response.setErrorMessage(message.build());
					return response.build();
				}
			} else {
				// Invalid request
			}
			// TODO: Put this enum conversion logic in a util somewhere
			Materials.Material material = Materials.Material.newBuilder()
					.setType(Type.valueOf(block.getTypeId())).build();
			response.setSuccess(true);
			response.setMaterialResponse(MaterialResponse.newBuilder().addMaterials(material).build());
		} else if (readRequest.hasLocateMaterialNearby()) {
			// Not yet implemented
		} else {
			// this is an invalid request. Fail.
			response.setSuccess(false);
			ErrorMessage.Builder message = ErrorMessage.newBuilder();
			message.setMessage("The read request has no recognised request in it.");
			message.setReason(Reason.INVALID_REQUEST);
			message.setAction(Action.FAIL_ACTION);
			response.setErrorMessage(message.build());
		}
		
		return response.build();
	}
	

}
