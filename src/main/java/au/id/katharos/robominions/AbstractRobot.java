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
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
import au.id.katharos.robominions.api.RobotApi.WorldLocation.Direction;
import au.id.katharos.robominions.api.RobotApi.WorldLocation;
import au.id.katharos.robominions.api.RobotApi.Coordinate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Abstract Robot logic that all types of Robots should inherit from.
 * 
 * This defines the common logic and interaction, subclasses should only define the visual appearance.
 */
public abstract class AbstractRobot implements InventoryHolder {
	
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
	protected final Inventory inventory;
	
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
	protected AbstractRobot(World world, UUID playerId, Location location, Logger logger) {
		this.playerId = playerId;
		this.world = world;
		//this.inventory = new HashMap<Material, Integer>();
		this.inventory = Bukkit.createInventory(this, 54, "Bot Inventory");
		// Start off with 100 dirt blocks
		// this.inventory.put(Material.DIRT, 100);
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
	 * Get the absolute direction (NSEW) that the robot is facing.
	 */
	public Direction getFacingDirection() {
		return facingDirection;
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
		logger.info("Mining block: " + block + ", It's a: " + block.getType());
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
		byte data = 0x0;
		if (success && inventory.contains(material)) {
			ItemStack itemStack = inventory.getItem(inventory.first(material));
			data = itemStack.getData().getData();
			if (itemStack.getAmount() > 1) {
				itemStack.setAmount(itemStack.getAmount() - 1);
			} else {
				inventory.remove(itemStack);
			}
		} else {
			// don't have that in your inventory
			return false;
		}
		if (success) {
			block.setType(material);
			block.setData(data);
		}
		return success;
	}
	
	/**
	 * Collect the given item. This does not check if the item is within range so make sure you
	 * check the distance first.
	 */
	public void pickUp(Item item) {
		ensureCorrectInventory();
		pickUp(item.getItemStack());
		item.remove();
	}
	
	public void pickUp(ItemStack stack) {
		ensureCorrectInventory();
		Material mat = stack.getType();
		if (mat != Material.DIRT && mat != Material.COBBLESTONE) {
			logger.info("Picked up item: " + mat);
			inventory.addItem(stack);
		}
	}
	
	public void ensureCorrectInventory() {
		if (!inventory.contains(Material.DIRT)) {
			inventory.addItem(new ItemStack(Material.DIRT, 64));
		} else {
			inventory.getItem(inventory.first(Material.DIRT)).setAmount(64);
		}
		if (!inventory.contains(Material.COBBLESTONE)) {
			inventory.addItem(new ItemStack(Material.COBBLESTONE, 64));
		} else {
			inventory.getItem(inventory.first(Material.COBBLESTONE)).setAmount(64);
		}
	}
	
	public Inventory getInventory() {
		ensureCorrectInventory();
		return inventory;
	}
	
	/**
	 * Get the block at this location in whatever world the robot is in.
	 */
	public Block getBlockAt(Location loc) {
		return world.getBlockAt(loc);
	}
	
	private Block getRelativeBlock(int x, int y, int z) {
		return world.getBlockAt(location.clone().add(x, y, z));
	}
	
	private static interface BlockChooser {
		public boolean match(Block block);
	}
	
	private static class NonSolidChooser implements BlockChooser {
		@Override
		public boolean match(Block block) {
			return !block.getType().isSolid();
		}
	}
	
	private static class MaterialChooser implements BlockChooser {

		private final Material material;
		
		public MaterialChooser(Material material) {
			this.material = material;
		}
		
		@Override
		public boolean match(Block block) {
			return Util.materialsEqual(material, block.getType());
		}
	}
	
	private List<Location> scanForBlocks(int max_distance, int limit, BlockChooser chooser) {
		List<Location> locations = Lists.newLinkedList();
		
		for (int d = 1; d <= max_distance; d++) {
			for (int x = -d; x <= d; x++) {
				for (int y =  - (d - Math.abs(x)); y <= (d - Math.abs(x)); y++) {
					int z = - (d - Math.abs(x) - Math.abs(y));
					Block b = getRelativeBlock(x, y, z);
					if (chooser.match(b)) {
						locations.add(b.getLocation());
						if (locations.size() >= limit) {
							return locations;
						}
					}
					if (z != 0){
						z = -z;
						b = getRelativeBlock(x, y, z);
						if (chooser.match(b)) {
							locations.add(b.getLocation());
							if (locations.size() >= limit) {
								return locations;
							}
						}
					}
				}
			}
		}
		return locations;
	}
	
	/**
	 * Scan blocks 1 distance away and return the locations which are non-solid.
	 */
	public List<Location> scanForNonSolid() {
		return scanForBlocks(1, 10, new NonSolidChooser());
	}

	/**
	 * Scans the area 5 blocks in every direction (by manhattan distance) and returns the
	 * locations of all blocks which match the given material (limited to 20 blocks). 
	 */
	public List<Location> scanForMaterial(Material material) {
		return scanForBlocks(10, 20, new MaterialChooser(material));
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

	/**
	 * Send a chat message to the Robot's owner.
	 * @param msg The message to send.
	 * @return True if sending was successful. 
	 */
	public boolean message_owner(String msg) {
		Player player = this.getPlayer();

		// getPlayer will return null for an offline player
		if(player == null) {
			return false;
		}

		// This uses the same syntax as Bukkit Essentials does for private messages
		String prefix = ChatColor.GOLD + "[My Robot -> Me] " + ChatColor.WHITE; 
		player.sendRawMessage(prefix + msg);
		return true;
	}

	/**
	 * Send a public chat message to all players on the server.
	 * @param msg The message to send.
	 * @return True if sending was successful. 
	 */
	public boolean message_all(String msg) {
		// The robot should be able to chat publicly regardless of whether the owner is online or offline.
		String ownerName = Bukkit.getOfflinePlayer(this.playerId).getName();
		String prefix = ChatColor.GOLD + "<" + ownerName + "'s Robot> " + ChatColor.WHITE; 
		Bukkit.getServer().broadcastMessage(prefix + msg);
		return true;
	}

	/**
	 * Teleports the robot to a location.
	 * @param world_loc The WorldLocation coordinate to teleport to
	 * @return True if teleporting was successful. 
	 */
	public boolean teleport(WorldLocation world_loc) {
		Location loc = new Location(this.world, 
									world_loc.getAbsoluteLocation().getX(), 
									world_loc.getAbsoluteLocation().getY(), 
									world_loc.getAbsoluteLocation().getZ());
		Block block = getBlockAt(loc);
		boolean success = !block.getType().isSolid();
		if (success) {
			this.location = loc;
			logger.warning("TELEPORTING to "+world_loc.getAbsoluteLocation().getX()+", "+world_loc.getAbsoluteLocation().getY()+", "+world_loc.getAbsoluteLocation().getZ());
		} else {
			logger.warning("ERROR TELEPORTING to "+world_loc.getAbsoluteLocation().getX()+", "+world_loc.getAbsoluteLocation().getY()+", "+world_loc.getAbsoluteLocation().getZ());
		}
		return success;
	}
}
