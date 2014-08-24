package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class AbstractRobot {
	
	protected Logger logger;
	
	protected Location location;
	protected Direction direction;
	protected World world;
	protected final String playerName;
	protected final HashMap<Material, Integer> inventory;
	
	
	protected static final HashMap<Direction, Vector> directionMap = Maps.newHashMap();
	protected static final HashMap<Direction, Float> directionYawMap = Maps.newHashMap();
	protected static final List<Direction> compass = Lists.newArrayList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	protected static final HashMap<Direction, Integer> rotationMap = Maps.newHashMap();
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
	
	protected AbstractRobot(Player player, Location location, Logger logger) {
		this.playerName = player.getName();
		this.world = player.getWorld();
		this.inventory = new HashMap<Material, Integer>();
		this.location = location.getBlock().getLocation().add(0.5, 0.99, 0.5).clone();
		this.logger = logger;
	}

	protected Direction getRelativeDirection(Direction looking, Direction move) {
		if (directionMap.containsKey(move)) {
			return move;
		}
		// Find how many compass points right do we rotate to find the direction of movement
		Integer rotation = rotationMap.get(move);
		// Get the current direction we're looking, then move 'rotation' places around the compass
		return compass.get((compass.indexOf(looking) + rotation) % compass.size());		
	}
	
	protected void tick() {
		// Please override but call super() first in case we need at add something here.
	}
	
	protected void flyingTick() {
		// Please override but call super() first in case we need at add something here.
	}
	
	protected void die() {
		// TODO: Dump out the inventory as collectables :)
	}

	public boolean mine(Block block) {
		logger.info("Mining block: " + block + ", I'ts a: " + block.getType());
		return false;
	}
	
	public Block getBlockAt(Location loc) {
		return world.getBlockAt(loc);
	}
	
	public boolean turn(Direction direction) {
		direction = getRelativeDirection(this.direction, direction);
		if (directionYawMap.containsKey(direction)) {
			this.direction = direction;
			location.setYaw(directionYawMap.get(direction));
			return true;
		}
		return false;
	}
	
	public boolean move(Direction direction) {
		Vector directionVector = directionMap.get(getRelativeDirection(this.direction, direction));
		Location loc = this.location.clone().add(directionVector);
		Block block = getBlockAt(loc);
		boolean success = !block.getType().isSolid();
		if (success) {
			this.location.add(directionVector);
		}
		return success;
	}

}
