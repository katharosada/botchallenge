package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;
import de.ntcomputer.minecraft.controllablemobs.api.ControllableMob;
import de.ntcomputer.minecraft.controllablemobs.api.ControllableMobs;

public class RobotChicken {

	private final ControllableMob<Chicken> chicken;
	private final String playerName;
	private final HashMap<Material, Integer> inventory;
	private final LinkedList<Location> locationTargets;
	private Location location;
	private Direction direction;
	
	
	private static final HashMap<Direction, Vector> directionMap = Maps.newHashMap();
	private static final HashMap<Direction, Float> directionYawMap = Maps.newHashMap();
	private static final List<Direction> compass = Lists.newArrayList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	private static final HashMap<Direction, Integer> rotationMap = Maps.newHashMap();
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
	
	private Direction getRelativeDirection(Direction looking, Direction move) {
		if (directionMap.containsKey(move)) {
			return move;
		}
		// Find how many compass points right do we rotate to find the direction of movement
		Integer rotation = rotationMap.get(move);
		// Get the current direction we're looking, then move 'rotation' places around the compass
		return compass.get((compass.indexOf(looking) + rotation) % compass.size());		
	}
	
	public RobotChicken(Player player, Location loc) {
		this.playerName = player.getName();
		this.location = loc.getBlock().getLocation().add(0.5, 0.99, 0.5).clone();
		this.inventory = new HashMap<Material, Integer>();
		this.locationTargets = new LinkedList<Location>();

		// Spawn facing north
		this.location.setYaw(directionYawMap.get(Direction.NORTH));
		this.direction = Direction.NORTH;
		Chicken c = player.getWorld().spawn(this.location, Chicken.class);
		chicken = ControllableMobs.getOrPutUnderControl(c, true);
		turn(Direction.NORTH);
	}

	public void setVelocityToTargetLocation() {
		// Get current target location
		if (locationTargets.isEmpty()) {
			return;
		} else {
			if (locationTargets.size() > 3) {
				chicken.getEntity().teleport(location);
				locationTargets.clear();
			}
		}
		Location target = locationTargets.getFirst();
		// By default we aim for the middle of the target block.
		

		// Check if we are already in the right block.
		//if (chicken.getEntity().getLocation().getBlock().equals(target.getBlock())) {
		if (chicken.getEntity().getLocation().clone().add(0, 0.5, 0).toVector().distance(target.toVector()) < 0.2) {

			// only pop the location if we're close enough to the middle of the block.
			locationTargets.pop();
			// If we already reached that target, pick the next one
			if (locationTargets.isEmpty()) {
				Bukkit.getLogger().info("Close enough, no more targets");
				return;
			} else {
				target = locationTargets.getFirst();
				Bukkit.getLogger().info("Close enough, picking next target");
			}
		} else {
			Bukkit.getLogger().info("I'm at: " + chicken.getEntity().getLocation().toString());
			Bukkit.getLogger().info("Targeting: " + target.toString());
			Bukkit.getLogger().info("Still have " + locationTargets.size() + " targets in queue.");
		}
		
		Vector v = target.toVector().subtract(chicken.getEntity().getLocation().toVector());
		
		// if we're moving sideways and don't have to move up or down a block, remove the y component:
		Vector blockV = target.getBlock().getLocation().toVector().subtract(
				chicken.getEntity().getLocation().getBlock().getLocation().toVector());
		// if (blockV.getY() == 0 && (blockV.getX() != 0 || blockV.getZ() != 0)) {
		if (blockV.getY() == 0 && v.getY() > 0.15) {
			Bukkit.getLogger().info("Dropping Y");
			v.setY(0);
		} 
		v.multiply(0.2);
		
		// Set velocity to next target location
		
		chicken.getEntity().setVelocity(v);
	}
	
	public void fly() {
		// Enforce that chicken location is free
		chicken.getEntity().setHealth(chicken.getEntity().getMaxHealth());
		
		Block block = chicken.getEntity().getWorld().getBlockAt(location);
		if (!block.isEmpty() && !block.isLiquid()) {
			block.setType(Material.AIR);
		}
		// Only hover if we have nowhere to go.
		if (locationTargets.isEmpty()) {
			Vector v = location.toVector().subtract(
					chicken.getEntity().getLocation().toVector());

			v.multiply(0.2);
			chicken.getEntity().setVelocity(v);
		}	
		chicken.getEntity().setFallDistance(0);
	}
	
	public void die() {
		chicken.getActions().die();
	}
	
	public boolean turn(Direction direction) {
		direction = getRelativeDirection(this.direction, direction);
		if (directionYawMap.containsKey(direction)) {
			this.direction = direction;
			
			location.setYaw(directionYawMap.get(direction));
			chicken.getEntity().teleport(location);

			Vector directionVector = directionMap.get(getRelativeDirection(this.direction, direction)).clone().multiply(0.1);
			chicken.getEntity().setVelocity(directionVector);
			return true;
		}
		return false;
	}
	
	public boolean mine(Block block) {
		return false;
	}

	public boolean move(Direction direction) {
		Vector directionVector = directionMap.get(getRelativeDirection(this.direction, direction));
		Location loc = this.location.clone().add(directionVector);
		Block block = chicken.getEntity().getWorld().getBlockAt(loc);
		boolean success = block.isEmpty();
		if (success) {
			this.location.add(directionVector);
			locationTargets.add(this.location.clone().subtract(0, 0.49, 0));
			//setVelocityToTargetLocation();
		}
		return success;
	}
}
