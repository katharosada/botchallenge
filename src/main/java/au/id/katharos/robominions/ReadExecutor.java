package au.id.katharos.robominions;

import java.util.List;
import java.util.logging.Logger;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import au.id.katharos.robominions.api.RobotApi.Coordinate;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Action;
import au.id.katharos.robominions.api.RobotApi.ErrorMessage.Reason;
import au.id.katharos.robominions.api.RobotApi.InventoryResponse;
import au.id.katharos.robominions.api.RobotApi.LocationResponse;
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
	private final RobotStateManager stateManager;
	
	public ReadExecutor(Logger logger, RobotStateManager stateManager) {
		this.logger = logger;
		this.stateManager = stateManager;
	}
	

	
	private Block getBlock(WorldLocation loc, AbstractRobot robot) throws RobotRequestException {
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
		return block;
	}
	
	private LocationResponse buildLocationResponse(List<Location> locations) {
		LocationResponse.Builder locResponse = LocationResponse.newBuilder(); 
		for (Location loc : locations) {
			WorldLocation worldLocation = WorldLocation.newBuilder().setAbsoluteLocation(
					Util.coordsFromLocation(loc)).build();
			locResponse.addLocations(worldLocation);
		}
		return locResponse.build();
	}
	
	public RobotResponse execute(String playerName, int key, RobotReadRequest readRequest) 
		throws RobotRequestException {
		RobotResponse.Builder response = RobotResponse.newBuilder();
		response.setKey(key);
		
		AbstractRobot robot = stateManager.getRobot(playerName);
		if (robot == null) {
			throw new RobotRequestException(
				Reason.ROBOT_DOES_NOT_EXIST, 
				"The robot does not exist.",
				Action.EXIT_CLIENT);
		}
		
		if (readRequest.hasLocateNonsolidNearby()) {
			List<Location> locations = robot.scanForNonSolid();
			LocationResponse locResponse = buildLocationResponse(locations);
			response.setSuccess(true);
			response.setLocationResponse(locResponse);
		} else if (readRequest.hasLocatePlayerTargetBlock()) {
			Player player = robot.getPlayer();
			HashSet<Byte> transparent_blocks = null;
			int maxDistance = 100;
			Location location = player.getTargetBlock(transparent_blocks, maxDistance).getLocation();
			WorldLocation worldLocation = WorldLocation.newBuilder().setAbsoluteLocation(
					Util.coordsFromLocation(location)).build();
			
			response.setLocationResponse(LocationResponse.newBuilder().addLocations(worldLocation).build());
			response.setSuccess(true);
		} else if (readRequest.hasLocateEntity()) {
			Location location = null;
			if (readRequest.getLocateEntity() == Entity.SELF) {
				location = robot.getLocation();
			} else if (readRequest.getLocateEntity() == Entity.OWNER) {
				Player player = robot.getPlayer();
				if (player == null) {
					throw new RobotRequestException(
						Reason.OWNER_DOES_NOT_EXIST,
						"The owner player is no longer on this server.");
				}
				location = player.getLocation();
			}
			
			WorldLocation worldLocation = WorldLocation.newBuilder().setAbsoluteLocation(
					Util.coordsFromLocation(location)).build();
			
			response.setLocationResponse(LocationResponse.newBuilder().addLocations(worldLocation).build());
			response.setSuccess(true);
		} else if (readRequest.hasIsSolid()) {
			WorldLocation loc = readRequest.getIsSolid();
            Block block = getBlock(loc, robot);
            response.setSuccess(true);
            response.setBooleanResponse(block.getType().isSolid());
	    } else if (readRequest.hasIdentifyMaterial()) {
			WorldLocation loc = readRequest.getIdentifyMaterial();
            Block block = getBlock(loc, robot);
			response.setSuccess(true);
			response.setMaterialResponse(Util.toProtoMaterial(block.getType()));
		} else if (readRequest.hasGetInventory()) {
			InventoryResponse.Builder inventoryBuilder = InventoryResponse.newBuilder();
			Inventory inv = robot.getInventory();
			for (ItemStack stack : inv.getContents()) {
				if (stack != null) {
					inventoryBuilder.addMaterials(Util.toProtoMaterial(stack.getType()));
					inventoryBuilder.addCounts(stack.getAmount());
				}
			}
			response.setInventoryResponse(inventoryBuilder);
			response.setSuccess(true);
		} else if (readRequest.hasLocateMaterialNearby()) {
			Material material = Util.toBukkitMaterial(readRequest.getLocateMaterialNearby());
			List<Location> locations = robot.scanForMaterial(material);
			LocationResponse locResponse = buildLocationResponse(locations);
			response.setSuccess(true);
			response.setLocationResponse(locResponse);
		} else {
			throw new RobotRequestException(Reason.INVALID_REQUEST, "The read request has no recognised request in it.");
		}
		return response.build();
	}
}
