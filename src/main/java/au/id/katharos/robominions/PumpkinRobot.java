package au.id.katharos.robominions;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

/**
 * A simple robot type which uses a Pumpkin block to represent the robots location and
 * facing direction.
 */
public class PumpkinRobot extends AbstractRobot {

	// To not wipe out flowers/water etc. we store the old contents of the block
	// we are currently occupying
	private Material oldMaterial;
	
	// The world block which we are currently on.
	private Block currentBlock;
	
	/**
	 * Create a new pumpkin bot. See {@link AbstractRobot}
	 */
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
		// Make sure the current block is still a pumpkin
		currentBlock.setType(Material.PUMPKIN);
		// Set the facing direction of the pumpkin
		setDirectionOnCurrentBlock();
	}
		
	@SuppressWarnings("deprecation")
	private void setDirectionOnCurrentBlock() {
		// These deprecated methods have NO WORKING REPLACEMENT (believe me I tried).
		switch(facingDirection) {
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
			//Replace our old block with the stored material
			currentBlock.setType(oldMaterial);
			currentBlock = world.getBlockAt(location);

			// remeber what we're stepping on so we can put it back.
			oldMaterial = currentBlock.getType();

			// Create pumpkin!
			currentBlock.setType(Material.PUMPKIN);
			// Set the facing direction of the pumpkin
			setDirectionOnCurrentBlock();
		}
		return success;
	}

}
