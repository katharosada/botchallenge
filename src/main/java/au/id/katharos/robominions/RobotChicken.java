package au.id.katharos.robominions;

import java.util.LinkedList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;
import de.ntcomputer.minecraft.controllablemobs.api.ControllableMob;
import de.ntcomputer.minecraft.controllablemobs.api.ControllableMobs;

public class RobotChicken extends AbstractRobot {

	private final ControllableMob<Chicken> chicken;	
	private final LinkedList<Location> locationTargets;
	
	public RobotChicken(Player player, Location loc, Logger logger) {
		super(player, loc, logger);

		this.locationTargets = new LinkedList<Location>();

		// Spawn facing north
		this.location.setYaw(directionYawMap.get(Direction.NORTH));
		Chicken c = player.getWorld().spawn(this.location, Chicken.class);
		chicken = ControllableMobs.getOrPutUnderControl(c, true);
		turn(direction);
	}

	public void setVelocityToTargetLocation() {
		// Get current target location
		if (locationTargets.isEmpty()) {
			return;
		} else {
			if (locationTargets.size() > 3) {
				chicken.getEntity().teleport(location);
				locationTargets.clear();
				return;
			}
		}
		Location target = locationTargets.getFirst();
		// By default we aim for the middle of the target block.

		// Check if we are already in the right block.
		//if (chicken.getEntity().getLocation().getBlock().equals(target.getBlock())) {
		if (chicken.getEntity().getLocation().clone().add(0, 0.5, 0).toVector()
				.distance(target.toVector()) < 0.4) {

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
		v.multiply(0.4);
		
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
	
	@Override
	public void tick() {
		super.tick();
		setVelocityToTargetLocation();
	}
	
	@Override
	public void flyingTick() {
		fly();
	}
	
	@Override
	public void die() {
		chicken.getActions().die();
	}
	
	@Override
	public boolean turn(Direction direction) {
		boolean success = super.turn(direction);
		if (success) {
			chicken.getEntity().teleport(location);
			Vector directionVector = directionMap.get(getRelativeDirection(this.direction, direction)).clone().multiply(0.1);
			chicken.getEntity().setVelocity(directionVector);
			return true;
		}
		return false;
	}

	@Override
	public boolean move(Direction direction) {
		boolean success = super.move(direction);
		if (success) {
			locationTargets.add(this.location.clone().subtract(0, 0.49, 0));
			//setVelocityToTargetLocation();
		}
		return success;
	}
}
