package au.id.katharos.robominions;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

/**
 * Experimental and incomplete robot using a Custom Entity. (not currently in use anywhere).
 */
public class CustomEntityRobot extends AbstractRobot {

	private final CustomChickenEntity customBat;	
	
	public CustomEntityRobot(Player player, Location loc, Logger logger) {
		super(player, loc, logger);

		// Spawn facing north
		this.location.setYaw(directionYawMap.get(Direction.NORTH));
		this.facingDirection = Direction.NORTH;

		// EXPERIMENTAL: Spawn a true custom entity.
		Bukkit.getLogger().info("Spawning custom chicken");
		customBat = CustomChickenEntity.spawn(player.getWorld(), this.location, logger);
		Bukkit.getLogger().info("Here it is: " + customBat);
	}
	
	@Override
	public void tick() {
		super.tick();
	}
	
	@Override
	public void flyingTick() {
	}
	
	@Override
	public void die() {
		customBat.die();
	}
	
	@Override
	public boolean turn(Direction direction) {
		boolean success = super.turn(direction);
		//TODO
		return success;
	}

	@Override
	public boolean move(Direction direction) {
		boolean success = super.move(direction);
		return success;
	}
}
