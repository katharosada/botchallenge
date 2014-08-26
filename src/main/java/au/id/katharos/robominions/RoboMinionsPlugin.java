package au.id.katharos.robominions;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

public class RoboMinionsPlugin extends JavaPlugin implements Listener {
	
	private static final int API_PORT = 26656;
	
	private HashMap<String, String> actionMap;
	private HashMap<String, AbstractRobot> robotMap;
	private RobotApiServer apiServer;
	private BukkitTask apiServerTask;
	private ActionQueue actionQueue;
	
	private class FlyingTickRepeatingTask implements Runnable {

		public void run() {
			for (AbstractRobot robot : robotMap.values()) {
				robot.flyingTick();
			}
		}
	}
	
	private class RobotTickRepeatingTask implements Runnable {

		public void run() {
			for (AbstractRobot robot : robotMap.values()) {
				robot.tick();
			}
		}
	}
	
	
	
	private boolean hasChicken(String playerName) {
		return robotMap.containsKey(playerName);
	}
	
	private AbstractRobot getChicken(String playerName) {
		if (robotMap.containsKey(playerName)) {
			return robotMap.get(playerName);
		}
		return null;
	}
	
	@Override
    public void onEnable() {
		robotMap = new HashMap<String, AbstractRobot>();
		actionMap = new HashMap<String, String>();
		actionQueue = new ActionQueue(getLogger());

		this.getServer().getPluginManager().registerEvents(this, this);

		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new FlyingTickRepeatingTask(), 1, 10);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new ActionExecutor(actionQueue, robotMap, getLogger()), 1, 4);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new RobotTickRepeatingTask(), 1, 2);

		this.apiServer = new RobotApiServer(API_PORT, getLogger(), actionQueue);
		apiServerTask = getServer().getScheduler().runTaskAsynchronously(this, apiServer);
		
		//CustomEntityType.registerEntity(CustomEntityType.CHICKEN);
		//CustomEntityType.registerEntity(CustomEntityType.BAT);
	}
	
	private void removeChicken(String playerName) {
		if (robotMap.containsKey(playerName)) {
			robotMap.get(playerName).die();
			robotMap.remove(playerName);
		}
	}
	
	private AbstractRobot spawnRobot(Player player, Location location, String type) {
		if (type == null) {
			// Default spawn a Robot Chicken
			return new RobotChicken(player, location, getLogger());
		}
		if (type.toLowerCase().equals("pumpkin")) {
			return new PumpkinRobot(player, location, getLogger());
		}
		return new RobotChicken(player, location, getLogger());
	}

	@EventHandler
	public void onBlockRightClick(PlayerInteractEvent event) {
		if(actionMap.containsKey(event.getPlayer().getName()) 
				&& event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			removeChicken(event.getPlayer().getName());
			// Spawn a chicken next to the block face that was clicked.
			AbstractRobot robot = spawnRobot(
					event.getPlayer(),
					event.getClickedBlock().getRelative(event.getBlockFace()).getLocation(),
					actionMap.get(event.getPlayer().getName()));
			robotMap.put(event.getPlayer().getName(), robot);
			actionMap.remove(event.getPlayer().getName());
		}
	}
	
	@EventHandler
	public void onItemSpawnEvent(ItemSpawnEvent spawnEvent) {
		AbstractRobot winner = null;
		double minDistance = 3.0;
		for (AbstractRobot robot : robotMap.values()) {
			double distance = spawnEvent.getLocation().distance(robot.getLocation());
			if (distance < minDistance) {
				minDistance = distance;
				winner = robot;
			}
		}
		if (winner != null) {
			winner.pickUp(spawnEvent.getEntity());
		}
	}
 
    @Override
    public void onDisable() {
    	apiServer.shutDown();
    	
    	for (String playerName : robotMap.keySet()) {
    		removeChicken(playerName);
    	}
    	robotMap.clear();
    	actionMap.clear();
    }

    @Override
    public boolean onCommand(
    		CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("beestart")) {
    		return true;
    	}
    	if (cmd.getName().equalsIgnoreCase("spawnrobot")) {
    		if (sender instanceof Player) {
    			String type = "";
    			if (args.length == 1) {
    				type = args[0];
    			}
    			actionMap.put(((Player) sender).getName(), type);
    			sender.sendMessage("Your next rightclick will spawn a robot.");
    			return true;
    		} else {
    			sender.sendMessage("You can only use this command in-game as a player.");
    			return false;
    		}
    	}
    	if (cmd.getName().equalsIgnoreCase("robot")) {
    		if (args.length < 1) {
    			sender.sendMessage("You must also provide a direction");
    			return false;
    		}
    		
    		if (sender instanceof Player && hasChicken(((Player) sender).getName())) {
    			AbstractRobot chicken = getChicken(((Player) sender).getName());
        		Direction direction = Direction.valueOf(args[0].toUpperCase());
        		if (direction == null) {
        			sender.sendMessage("Invalid direction");
        			return false;
        		}
        		boolean moveSuccess = chicken.move(direction);
        		if (!moveSuccess) {
        			sender.sendMessage("Chicken can't move there, there's something in the way.");
        			return true;
        		}
        		return true;
    		} else {
    			sender.sendMessage("You must be a player to use this command");
    			return false;
    		}
    	}
    	return false; 
    }
}
