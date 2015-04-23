package au.id.katharos.robominions;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import au.id.katharos.robominions.api.RobotApi.WorldLocation.Direction;
import au.id.katharos.robominions.api.RobotApi.WorldLocation;
import au.id.katharos.robominions.api.RobotApi.Coordinate;

/**
 * A simple robot type which uses a Pumpkin block to represent the robots location and
 * facing direction.
 */
public class PumpkinRobot extends AbstractRobot {
	
	public static final Material AVATAR = Material.PUMPKIN;
	public static final Material AVATAR_LIT = Material.JACK_O_LANTERN;

	// To not wipe out flowers/water etc. we store the old contents of the block
	// we are currently occupying
	private Material oldMaterial;
	private byte oldMetadata;
	
	// The world block which we are currently on.
	private Block currentBlock;
	
	private Material currentMaterial;
	
	/**
	 * Create a new pumpkin bot. See {@link AbstractRobot}
	 */
	protected PumpkinRobot(World world, UUID playerId, Location location, Logger logger) {
		super(world, playerId, location, logger);
		Block block = world.getBlockAt(location);
		oldMaterial = block.getType();
		oldMetadata = block.getData();
		// If the previous block is like me, it's probably a glitch. Remove it.
		if (oldMaterial == AVATAR) {
			oldMaterial = Material.AIR;
			oldMetadata = 0x0;
		}
		currentMaterial = AVATAR;
		block.setType(currentMaterial);
		currentBlock = block;
		setDirectionOnCurrentBlock();
	}
	
	/**
	 * Switch to a jack-o-lantern or back to a pumpkin.
	 */
	public void light(boolean on) {
		if (on) {
			currentMaterial = AVATAR_LIT;
		} else {
			currentMaterial = AVATAR;
		}
	}
	
	@Override
	protected void die() {
		super.die();
		currentBlock.setType(oldMaterial);
		currentBlock.setData(oldMetadata);
	}
	
	@Override
	public void pickUp(ItemStack item) {
		// Don't pick up any self-like objects (usually caused by self-destruction)
		if (item.getType() == currentMaterial) {
			return;
		}
		super.pickUp(item);
	}
	
	@Override
	protected void tick() {
		super.tick();
		// Make sure the current block is still a pumpkin
		currentBlock.setType(currentMaterial);
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
			currentBlock.setType(currentMaterial);
		}
		return success;
	}
	
	@Override
	public boolean move(Direction direction) {
		boolean success = super.move(direction);
		if (success) {
			//Replace our old block with the stored material
			currentBlock.setType(oldMaterial);
			currentBlock.setData(oldMetadata);
			currentBlock = world.getBlockAt(location);

			// remember what we're stepping on so we can put it back.
			oldMaterial = currentBlock.getType();
			oldMetadata = currentBlock.getData();

			// Create pumpkin!
			currentBlock.setType(currentMaterial);
			// Set the facing direction of the pumpkin
			setDirectionOnCurrentBlock();
		}
		return success;
	}

	@Override
	public boolean teleport(WorldLocation world_loc) {
		boolean success = super.teleport(world_loc);
		if (success) {
			//Replace our old block with the stored material
			currentBlock.setType(oldMaterial);
			currentBlock.setData(oldMetadata);
			currentBlock = world.getBlockAt(location);

			// remember what we're stepping on so we can put it back.
			oldMaterial = currentBlock.getType();
			oldMetadata = currentBlock.getData();

			// Create pumpkin!
			currentBlock.setType(currentMaterial);
			// Set the facing direction of the pumpkin
			setDirectionOnCurrentBlock();
		}
		return success;
	}

}
