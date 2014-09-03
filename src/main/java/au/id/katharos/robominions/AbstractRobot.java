package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import au.id.katharos.robominions.api.RobotApi.WorldLocation.Direction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Abstract Robot logic that all types of Robots should inherit from.
 * 
 * This defines the common logic and interaction, subclasses should only define the visual appearance.
 */
public abstract class AbstractRobot {
	
	protected final Logger logger;
	
	// Location of the robot
	protected Location location;

	// Direction the robot is facing
	protected Direction facingDirection;
	
	// The world which the robot is in
	// TODO: Support moving to the Netherworld.
	protected World world;
	
	// Name of the player which owns this bot.
	protected final UUID playerId;
	
	// All the items this robot is holding
	protected final HashMap<Material, Integer> inventory;
	
	// The type of pickaxe this bot has (used only for deciding what item are spawned when mining)
	protected final ItemStack pickAxe;
	
	
	// Map of directions to the corresponding x,y,z vector
	protected static final HashMap<Direction, Vector> directionMap = Maps.newHashMap();
	
	// Map from compass direction to Entity yaw (look) direction (in degrees from SOUTH)
	protected static final HashMap<Direction, Float> directionYawMap = Maps.newHashMap();
	
	// List of compass directions in order (N, E, S, W)
	protected static final List<Direction> compass = Lists.newArrayList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	
	// Map of relative direction (left, forward etc.) to how many compass points to rotate to the right to reach that direction.
	protected static final HashMap<Direction, Integer> rotationMap = Maps.newHashMap();
	
	// Set up the utility maps.
	static {
		directionMap.put(Direction.UP, new Vector(0, 1, 0));
		directionMap.put(Direction.DOWN, new Vector(0, -1, 0));
		directionMap.put(Direction.NORTH, new Vector(0, 0, -1)); 
		directionMap.put(Direction.SOUTH, new Vector(0, 0, 1));
		directionMap.put(Direction.EAST, new Vector(1, 0, 0));
		directionMap.put(Direction.WEST, new Vector(-1, 0, 0));
		
		rotationMap.put(Direction.FORWARD, 0);
		rotationMap.put(Direction.RIGHT, 1);
		rotationMap.put(Direction.BACKWARD, 2);
		rotationMap.put(Direction.LEFT, 3);
		
		directionYawMap.put(Direction.SOUTH, 0f);
		directionYawMap.put(Direction.WEST, 90f);
		directionYawMap.put(Direction.NORTH, 180f);
		directionYawMap.put(Direction.EAST, 270f);		
	}
	
	/**
	 * Construct a Robot.
	 * @param player The owner player
	 * @param location The initial location
	 * @param logger A logger to log any info/etc. messages to.
	 */
	protected AbstractRobot(Player player, Location location, Logger logger) {
		this.playerId = player.getUniqueId();
		this.world = player.getWorld();
		this.inventory = new HashMap<Material, Integer>();
		// Start off with 100 dirt blocks
		this.inventory.put(Material.DIRT, 100);
		this.location = location.getBlock().getLocation().add(0.5, 0.99, 0.5).clone();
		this.logger = logger;
		this.facingDirection = Direction.SOUTH;
		this.pickAxe = new ItemStack(Material.DIAMOND_PICKAXE);
	}
	
	/**
	 * Get the current robot location
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Get the world which the robot is in.
	 */
	public World getWorld() {
		return world;
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(this.playerId);
	}
	/**
	 * Checks if a location is 'visible' to the robot, if it's close enough.
	 * 
	 * TODO: Make this logic more sensible (e.g. not seeing through walls)
	 */
	public boolean isLocationVisible(Location loc) {
		if (loc.getWorld() != world) {
			return false;
		}
		// Any block less than 10 blocks away is visible.
		if (loc.distance(location.getBlock().getLocation()) > 10) {
			return false;
		}
		return true;
	}

	/**
	 * Convert a possibly relative direction into an absolute direction (East, North, up, etc.)
	 *  
	 * @param facingDirection The direction the bot is currently looking
	 * @param direction The relative direction (also works for non-relative directions)
	 * @return The absolute direction (same as given direction if already Absolute).
	 */
	protected static Direction getAbsoluteDirection(Direction facingDirection, Direction direction) {
		if (directionMap.containsKey(direction)) {
			return direction;
		}
		// Find how many compass points right do we rotate to find the direction of movement
		Integer rotation = rotationMap.get(direction);
		// Get the current direction we're looking, then move 'rotation' places around the compass
		return compass.get((compass.indexOf(facingDirection) + rotation) % compass.size());		
	}
	
	/**
	 * Called once per Bukkit server tick (20 times per second), to update the visual state
	 * of the robot if necessary
	 */
	protected void tick() {
		// Please override but call super() first in case we need at add something here.
	}
	
	/** 
	 * less frequent server tick, for supporting flying bots attempts at overcoming gravity.
	 */
	protected void flyingTick() {
		// Please override but call super() first in case we need at add something here.
	}
	
	/**
	 * The robot is forcibly killed and removed from existence.
	 */
	protected void die() {
		// TODO: Dump out the inventory as collectable items :)
	}

	/**
	 * Destroy the indicated block (the resulting item is automatically collected)
	 * @param mineDirection The direction (relative to the robot) to mine in
	 * @return True if the block was successfully mined
	 */
	public boolean mine(Direction mineDirection) {
		Block block = getBlockFromDirection(mineDirection);
		logger.info("Mining block: " + block + ", I'ts a: " + block.getType());
		boolean success = block.breakNaturally(pickAxe);
		
		return success;
	}
	
	/**
	 * Place a block from the robot's inventoring in the indicated place.
	 * @param dir The Direction (from the robot) to place the block
	 * @param material The type of block to place
	 * @return True if the block was placed successfully
	 */
	public boolean place(Direction dir, Material material) {
		Block block = getBlockFromDirection(dir);
		boolean success = !block.getType().isSolid();
		if (success && inventory.containsKey(material) && inventory.get(material) > 0) {
			inventory.put(material, inventory.get(material) - 1);
		} else {
			// don't have that in your inventory
			return false;
		}
		if (success) {
			block.setType(material);
		}
		return success;
	}
	
	/**
	 * Collect the given item. This does not check if the item is within range so make sure you
	 * check the distance first.
	 */
	public void pickUp(Item item) {
		Material mat = item.getItemStack().getType();
		logger.info("Picked up item: " + mat);
		Integer cur = 0;
		if (inventory.containsKey(mat)) {
			cur = inventory.get(mat);
		}
		inventory.put(mat, cur + 1);
		item.remove();
	}
	
	/**
	 * Get the block at this location in whatever world the robot is in.
	 */
	public Block getBlockAt(Location loc) {
		return world.getBlockAt(loc);
	}
	
	public List<Location> scanForNonSolid() {
		// Scans cube area 1 block in every direction and returns the locations of all blocks
		// which are non-solid in that space.
		List<Location> locations = Lists.newLinkedList();
		int dist = 1; // One block in each direction (including diagonal)
		for (int x = location.getBlockX() - dist; x <= location.getBlockX() + dist; x++) {
			for (int y = location.getBlockY() - dist; y <= location.getBlockY() + dist; y++) {
				for (int z = location.getBlockZ() - dist; z <= location.getBlockZ() + dist; z++) {
					if (!world.getBlockAt(x, y, z).getType().isSolid()) {
						locations.add(new Location(world, x, y, z));
					}
				}
			}
		}
		return locations;
	}
	
	/**
	 * Turn the facing direction of the robot to the given direction.
	 * 
	 * @return True if the turn was valid (turns don't fail) but false if the direction made no
	 * 		sense (e.g. a robot can't turn up or down.)
	 */
	public boolean turn(Direction direction) {
		direction = getAbsoluteDirection(this.facingDirection, direction);
		if (directionYawMap.containsKey(direction)) {
			this.facingDirection = direction;
			location.setYaw(directionYawMap.get(direction));
			return true;
		}
		return false;
	}
	
	/**
	 * Convert a direction (relative to the robot) into a world block reference.
	 */
	public Block getBlockFromDirection(Direction direction) {
		Vector directionVector = directionMap.get(getAbsoluteDirection(this.facingDirection, direction));
		Location loc = this.location.clone().add(directionVector);
		return getBlockAt(loc);
	}
	
	/**
	 * Move the robot in the given direction.
	 * @param direction The direction to move in.
	 * @return True if the move happend successfully (fails if you try to move into a solid block)
	 */
	public boolean move(Direction direction) {
		Vector directionVector = directionMap.get(getAbsoluteDirection(this.facingDirection, direction));
		Location loc = this.location.clone().add(directionVector);
		Block block = getBlockAt(loc);
		boolean success = !block.getType().isSolid();
		if (success) {
			this.location.add(directionVector);
		}
		return success;
	}

}
