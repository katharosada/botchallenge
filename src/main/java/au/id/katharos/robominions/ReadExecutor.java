package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import au.id.katharos.robominions.api.RobotApi.Coordinate;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Action;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Reason;
import au.id.katharos.robominions.api.RobotApi.LocationResponse;
import au.id.katharos.robominions.api.RobotApi.MaterialResponse;
import au.id.katharos.robominions.api.RobotApi.RobotReadRequest;
import au.id.katharos.robominions.api.RobotApi.RobotReadRequest.Entity;
import au.id.katharos.robominions.api.RobotApi.RobotResponse;
import au.id.katharos.robominions.api.RobotApi.WorldLocation;

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
	private final HashMap<UUID, AbstractRobot> robotMap;
	
	public ReadExecutor(Logger logger, HashMap<UUID, AbstractRobot> robotMap) {
		this.logger = logger;
		this.robotMap = robotMap;
	}
	
	public RobotResponse execute(UUID playerId, int key, RobotReadRequest readRequest) 
		throws RobotRequestException {
		RobotResponse.Builder response = RobotResponse.newBuilder();
		response.setKey(key);
		
		AbstractRobot robot = robotMap.get(playerId);
		if (robot == null) {
			throw new RobotRequestException(
				Reason.ROBOT_DOES_NOT_EXIST, 
				"The robot does not exist.",
				Action.EXIT_CLIENT);
		}
		
		if (readRequest.hasLocateNonsolidNearby()) {
			List<Location> locations = robot.scanForNonSolid();
			LocationResponse.Builder locResponse = LocationResponse.newBuilder(); 
			for (Location loc : locations) {
				WorldLocation worldLocation = WorldLocation.newBuilder().setAbsoluteLocation(
						Util.coordsFromLocation(loc)).build();
				locResponse.addLocations(worldLocation);
			}
			response.setLocationResponse(locResponse);
		} else if (readRequest.hasLocateEntity()) {
			Location location = null;
			if (readRequest.getLocateEntity() == Entity.SELF) {
				location = robot.getLocation();
			} else if (readRequest.getLocateEntity() == Entity.OWNER) {
				location = robot.getPlayer().getLocation();
			}
			
			WorldLocation worldLocation = WorldLocation.newBuilder().setAbsoluteLocation(
					Util.coordsFromLocation(location)).build();
			
			response.setLocationResponse(LocationResponse.newBuilder().addLocations(worldLocation).build());
			response.setSuccess(true);
		} else if (readRequest.hasIdentifyMaterial()) {
			WorldLocation loc = readRequest.getIdentifyMaterial();
			Block block = null;
			if (loc.hasDirection()) {
				block = robot.getBlockFromDirection(loc.getDirection());
			} else if (loc.hasAbsoluteLocation()) {
				Coordinate coord = loc.getAbsoluteLocation();
				World world = robot.getWorld();
				Location location = Util.locationFromCoords(world, coord);
				boolean canSee = robot.isLocationVisible(location);
				if (canSee) {
					block = location.getBlock();
				} else {
					throw new RobotRequestException(Reason.BLOCK_IS_NOT_VISIBLE, "The robot can't see that block.");
				}
			} else {
				throw new RobotRequestException(Reason.INVALID_REQUEST, "Location not recognised.");
			}
			// TODO: Put this enum conversion logic in a util somewhere
			response.setSuccess(true);
			response.setMaterialResponse(MaterialResponse.newBuilder()
					.addMaterials(Util.toProtoMaterial(block.getType())).build());
		} else if (readRequest.hasLocateMaterialNearby()) {
			// TODO: 
			throw new RobotRequestException(Reason.NOT_IMPLEMENTED, "Searching nearby locations is not implemented yet.");
		} else {
			throw new RobotRequestException(Reason.INVALID_REQUEST, "The read request has no recognised request in it.");
		}
		return response.build();
	}
}
