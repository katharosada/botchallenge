package au.id.katharos.robominions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import au.id.katharos.robominions.api.Materials;
import au.id.katharos.robominions.api.Materials.Material.Type;
import au.id.katharos.robominions.api.RobotApi.Coordinate;

/**
 * Class for static utility methods.
 */
public class Util {

	public static Location locationFromCoords(World world, Coordinate coords) {
		return new Location(world, coords.getX(), coords.getY(), coords.getZ());
	}
	
	public static boolean materialsEqual(Material a, Material b) {
		// Special case, LOG and LOG_2 are equivalent
		if (a == Material.LOG || a == Material.LOG_2) {
			return b == Material.LOG || b == Material.LOG_2;
		} 
		return a == b;
	}
	
	public static Coordinate coordsFromLocation(Location location) {
		return Coordinate.newBuilder()
				.setX(location.getBlockX())
				.setY(location.getBlockY())
				.setZ(location.getBlockZ())
				.build();
	}
	
	@SuppressWarnings("deprecation") // No alternative
	public static Materials.Material toProtoMaterial(Material material) {
		Materials.Material protoMaterial = Materials.Material.newBuilder()
				.setType(Type.valueOf(material.getId())).build();
		return protoMaterial;
	}

	@SuppressWarnings("deprecation") // No alternative
	public static Material toBukkitMaterial(Materials.Material protoMaterial) {
		return Material.getMaterial(protoMaterial.getType().getNumber());
	}
}
