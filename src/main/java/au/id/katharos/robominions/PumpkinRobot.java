package au.id.katharos.robominions;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

public class PumpkinRobot extends AbstractRobot {

	private Material oldMaterial; 
	private Block currentBlock;
	
	protected PumpkinRobot(Player player, Location location, Logger logger) {
		super(player, location, logger);
		Block block = world.getBlockAt(location);
		oldMaterial = block.getType();
		block.setType(Material.PUMPKIN);
		currentBlock = block;
		setDirectionOnCurrentBlock();
	}
	
	@Override
	protected void die() {
		super.die();
		currentBlock.setType(oldMaterial);
	}
	
	@Override
	protected void tick() {
		super.tick();
	}
		
	@SuppressWarnings("deprecation")
	private void setDirectionOnCurrentBlock() {
		switch(direction) {
			case NORTH:
				currentBlock.setData((byte)0x2);
				return;
			case EAST:
				currentBlock.setData((byte)0x3);
				return;
			case SOUTH:
				currentBlock.setData((byte)0x0);
				return;
			case WEST:
				currentBlock.setData((byte)0x1);
				return;
			default:
				return;
		}
	}
	
	@Override
	public boolean turn(Direction direction) {
		boolean success = super.turn(direction);
		if (success) {
			setDirectionOnCurrentBlock();
			currentBlock.setType(Material.PUMPKIN);
		}
		return success;
	}
	
	@Override
	public boolean move(Direction direction) {
		boolean success = super.move(direction);
		if (success) {
			currentBlock.setType(oldMaterial);
			currentBlock = world.getBlockAt(location);

			oldMaterial = currentBlock.getType();
			logger.info("Walking over: " + oldMaterial.name());

			currentBlock.setType(Material.PUMPKIN);
			setDirectionOnCurrentBlock();
		}
		return success;
	}

}
