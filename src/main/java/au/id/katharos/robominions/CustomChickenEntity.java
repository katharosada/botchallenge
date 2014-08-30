package au.id.katharos.robominions;


import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

import net.minecraft.server.v1_7_R3.ChunkCoordinates;
import net.minecraft.server.v1_7_R3.EntityBat;
import net.minecraft.server.v1_7_R3.MathHelper;
import net.minecraft.server.v1_7_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;


/**
 * A robot type which uses a custom chicken entity (looks like a chicken, acts like a bat).
 * 
 * WARNING: Incomplete and experimental. Also it depends on internal obfuscated NMS code which
 *  	is likely to change with the next version.
 */
public class CustomChickenEntity extends EntityBat {

	private Logger logger;
	private ChunkCoordinates chunkCoordinates;
	
	public static Object getPrivateField(String fieldname, Class klass, Object object) {
		Field field;
		Object obj = null;
		try {
			field = klass.getDeclaredField(fieldname);
			field.setAccessible(true);
			obj = field.get(object);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public static CustomChickenEntity spawn(org.bukkit.World bukkitWorld, Location location, Logger logger) {
		World world = ((CraftWorld) bukkitWorld).getHandle();

    	CustomChickenEntity entity = new CustomChickenEntity(world);
    	entity.logger = logger;
    	entity.setPositionRotation(location.getX(), location.getY(), location.getZ(),
    							   location.getYaw(), location.getPitch());
    	entity.chunkCoordinates = new ChunkCoordinates(
    			(int)entity.locX, (int)entity.locY, (int)entity.locZ);
    	world.addEntity(entity);
    	return entity;
	}
	
	public CustomChickenEntity(World world) {
		super(world);
		this.logger = logger;
		((List) getPrivateField("b", PathfinderGoalSelector.class, goalSelector)).clear();
		((List) getPrivateField("c", PathfinderGoalSelector.class, goalSelector)).clear();
		((List) getPrivateField("b", PathfinderGoalSelector.class, targetSelector)).clear();
		((List) getPrivateField("c", PathfinderGoalSelector.class, targetSelector)).clear();
		chunkCoordinates = new ChunkCoordinates((int)this.locX, (int)this.locY, (int)this.locZ);
	}
	
	@Override
	public void bm() {
		logger.info("The bm() function was called");
		double d0 = (double) this.chunkCoordinates.x + 0.5D - this.locX;
        double d1 = (double) this.chunkCoordinates.y + 0.1D - this.locY;
        double d2 = (double) this.chunkCoordinates.z + 0.5D - this.locZ;

        this.motX += (Math.signum(d0) * 0.5D - this.motX) * 0.10000000149011612D;
        this.motY += (Math.signum(d1) * 0.699999988079071D - this.motY) * 0.10000000149011612D;
        this.motZ += (Math.signum(d2) * 0.5D - this.motZ) * 0.10000000149011612D;
        float f = (float) (Math.atan2(this.motZ, this.motX) * 180.0D / 3.1415927410125732D) - 90.0F;
        float f1 = MathHelper.g(f - this.yaw);

        this.be = 0.5F;
        this.yaw += f1;
	}
	
	// Seems to actually be isAsleep()
	public boolean isStartled() {
		return true;
	}
	
	@Override
	public void e() {
		super.e();
	}
}
