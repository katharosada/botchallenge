package au.id.katharos.robominions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.google.protobuf.ByteString;

import au.id.katharos.robominions.api.RobotStorage;
import au.id.katharos.robominions.api.RobotStorage.PluginState;
import au.id.katharos.robominions.api.RobotStorage.RobotState;

public class RobotStateManager {

	private static final String FILE_NAME = "robominions-persist.pb";
	
	// The complete set of all bots in the game, maped by the name of the player that owns them. 
	private final HashMap<UUID, AbstractRobot> robotMap;
	
	// We need to be able to map a player name to the UUID even when they've gone offline.
	private final HashMap<String, UUID> uuidCache;
	private final HashMap<UUID, String> nameCache;
	
	private final Logger logger;
	
	public RobotStateManager(Logger logger) {
		robotMap = new HashMap<UUID, AbstractRobot>();
		uuidCache = new HashMap<String, UUID>();
		nameCache = new HashMap<UUID, String>();
		this.logger = logger;
	}
	
	public AbstractRobot getRobot(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		UUID uuid = uuidCache.get(playerName);
		if (player != null) {
			uuid = player.getUniqueId();
			uuidCache.put(playerName, uuid);
			nameCache.put(uuid, playerName);
		}
		return getRobot(uuid);
	}
	
	public AbstractRobot getRobot(UUID playerId) {
		if (robotMap.containsKey(playerId)) {
			return robotMap.get(playerId);
		}
		return null;
	}
	
	public void loadState() {
		File file = new File(FILE_NAME);
		try {
			FileInputStream fis = new FileInputStream(file);
			PluginState state = PluginState.parseFrom(fis);
			for (RobotState robotState : state.getRobotStateList()) {
				UUID playerId = UUID.fromString(robotState.getUuid());
				World world = Bukkit.getWorld(robotState.getWorldName());
				AbstractRobot robot = new PumpkinRobot(
						world,
						playerId,
						Util.locationFromCoords(world, robotState.getRobotLocation()),
						logger);
				robot.turn(robotState.getRobotDirection());
				robotMap.put(playerId, robot);
				String playerName = robotState.getPlayerName();
				uuidCache.put(playerName, playerId);
				nameCache.put(playerId, playerName);
				
				// Load inventory:
				for (RobotStorage.ItemStack itemStack : robotState.getRobotInventoryList()) {
					int index = itemStack.getIndex();
					Material material = Util.toBukkitMaterial(itemStack.getMaterial());
					ItemStack newItemStack = new ItemStack(material, itemStack.getCount());
				    
					newItemStack.setData(new MaterialData(material, itemStack.getData().byteAt(0)));
					robot.getInventory().setItem(index, newItemStack);
				}
			}
			fis.close();
		} catch (FileNotFoundException e) {
			logger.info("No robot minions saved data found. Will start from scratch.");
		} catch (IOException e) {
			logger.warning("Error reading from saved robot minions data.");
			e.printStackTrace();
		}
	}
	
	public void saveState() {
	    PluginState.Builder stateBuilder = PluginState.newBuilder();
		for (UUID playerId : robotMap.keySet()) {
	    	RobotState.Builder robotState = RobotState.newBuilder();
	    	String playerName = null;
	    	Player player = Bukkit.getPlayer(playerId);
	    	if (player != null) {
	    		playerName = player.getName();
	    	} else {
	    		playerName = nameCache.get(playerId);
	    	}
	    	robotState.setPlayerName(playerName);
	    	robotState.setUuid(playerId.toString());
	    	AbstractRobot robot = robotMap.get(playerId);
	    	
	    	robotState.setRobotLocation(Util.coordsFromLocation(robot.getLocation()));
	    	robotState.setRobotDirection(robot.getFacingDirection());
	    	robotState.setWorldName(robot.getWorld().getName());

			// Save inventory:
	    	Inventory inventory = robot.getInventory();
	    	for (int i = 0; i < inventory.getSize(); i++) {
	    		ItemStack itemStack = inventory.getItem(i);
	    		if (itemStack != null) {
	    			RobotStorage.ItemStack.Builder protoItemStack =
	    					RobotStorage.ItemStack.newBuilder();
	    			protoItemStack.setIndex(i);
	    			protoItemStack.setMaterial(Util.toProtoMaterial(itemStack.getType(), true));
	    			protoItemStack.setCount(itemStack.getAmount());

	    			byte[] data = new byte[1];
	    			data[0] = itemStack.getData().getData();
	    			protoItemStack.setData(ByteString.copyFrom(data));
	    			robotState.addRobotInventory(protoItemStack.build());
	    		}
			}
	    	
	    	stateBuilder.addRobotState(robotState.build());
	    }
		PluginState state = stateBuilder.build();
		try {
			File file = new File(FILE_NAME);
			FileOutputStream fos = new FileOutputStream(file);
			state.writeTo(fos);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			logger.warning("IO Exception when trying to write out file!");
			e.printStackTrace();
		}
	}
	
	public boolean hasRobot(UUID playerId) {
		return robotMap.containsKey(playerId);
	}
	
	/**
	 * Kill the robot that belongs to the given player and remove it from the map.
	 */
	public void removeRobot(UUID playerId) {
		if (robotMap.containsKey(playerId)) {
			robotMap.get(playerId).die();
			robotMap.remove(playerId);
		}
	}
	
	public void shutDown() {
		saveState();
		for (UUID playerId : robotMap.keySet()) {
    		removeRobot(playerId);
    	}
	}
	
	public void addRobot(Player player, AbstractRobot robot) {
		robotMap.put(player.getUniqueId(), robot);
		uuidCache.put(player.getName(), player.getUniqueId());
		nameCache.put(player.getUniqueId(), player.getName());
	}
	
	public HashMap<UUID, AbstractRobot> getRobotMap() {
		return robotMap;
	}
}
