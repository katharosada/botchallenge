package au.id.katharos.robominions;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import au.id.katharos.robominions.ActionQueue.ChickenEvent;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest;
import au.id.katharos.robominions.api.RobotApi.RobotActionRequest.Direction;

public class RoboMinionsPlugin extends JavaPlugin implements Listener {
	
	private static final int API_PORT = 26656;
	
	private HashMap<String, Boolean> actionMap;
	private HashMap<String, RobotChicken> chickenMap;
	private RobotApiServer apiServer;
	private BukkitTask apiServerTask;
	private ActionQueue actionQueue;
	
	private class AdjustChickenHeight implements Runnable {

		public void run() {
			for (RobotChicken chicken : chickenMap.values()) {
				chicken.fly();
			}
		}
	}
	
	private class MoveChickenToLocation implements Runnable {

		public void run() {
			for (RobotChicken chicken : chickenMap.values()) {
				chicken.setVelocityToTargetLocation();
			}
		}
	}
	
	
	
	private boolean hasChicken(String playerName) {
		return chickenMap.containsKey(playerName);
	}
	
	private RobotChicken getChicken(String playerName) {
		if (chickenMap.containsKey(playerName)) {
			return chickenMap.get(playerName);
		}
		return null;
	}
	
	@Override
    public void onEnable() {
		chickenMap = new HashMap<String, RobotChicken>();
		actionMap = new HashMap<String, Boolean>();
		actionQueue = new ActionQueue(getLogger());

		this.getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new AdjustChickenHeight(), 1, 10);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new ActionExecutor(actionQueue, chickenMap, getLogger()), 0, 4);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new MoveChickenToLocation(), 1, 2);

		this.apiServer = new RobotApiServer(API_PORT, getLogger(), actionQueue);
		apiServerTask = getServer().getScheduler().runTaskAsynchronously(this, apiServer);
	}
	
	private void removeChicken(String playerName) {
		if (chickenMap.containsKey(playerName)) {
			chickenMap.get(playerName).die();
			chickenMap.remove(playerName);
		}
	}

	@EventHandler
	public void onBlockRightClick(PlayerInteractEvent event) {
		if(actionMap.containsKey(event.getPlayer().getName()) 
				&& event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			removeChicken(event.getPlayer().getName());
			// Spawn a chicken next to the block face that was clicked.
			RobotChicken chicken = new RobotChicken(
					event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
			chickenMap.put(event.getPlayer().getName(), chicken);
			actionMap.remove(event.getPlayer().getName());
		}
	}
 
    @Override
    public void onDisable() {
    	apiServer.shutDown();
    	
    	for (String playerName : chickenMap.keySet()) {
    		removeChicken(playerName);
    	}
    	chickenMap.clear();
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
    			actionMap.put(((Player) sender).getName(), true);
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
    			RobotChicken chicken = getChicken(((Player) sender).getName());
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
