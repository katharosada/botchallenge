package au.id.katharos.robominions;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.google.protobuf.Internal.EnumLiteMap;

import au.id.katharos.robominions.api.RobotApi.WorldLocation.Direction;

/**
 * Main Plugin class which sets everything up and handles normal plugin interaction.
 *
 * This includes starting up the API server and scheduling regular tasks, also handles
 * spawning of new Robots and generally any server or player text commands. 
 */
public class RoboMinionsPlugin extends JavaPlugin implements Listener {
	
	private static final int API_PORT = 26656;
	
	// For spawning a robot, the player sets up the action with a command then it only 
	// actually happens when the player right-clicks. This state is stored here. 
	private HashMap<String, String> actionMap;
	
	private RobotStateManager stateManager;
	
	// The Robot API server
	private RobotApiServer apiServer;
	
	// The Bukkit Async task which runs the server (essentially a thread)
	private BukkitTask apiServerTask;
	
	// The thread-safe queue of actions recieved by the API that have not yet been executed.
	private ActionQueue actionQueue;
	
	/**
	 * Some types of bots require a regular reminder to work against gravity. 
	 * This task runs every 3rd tick which experimentally is the right frequency to
	 * keep an object hovering. 
	 */
	private class FlyingTickRepeatingTask implements Runnable {

		public void run() {
			for (AbstractRobot robot : stateManager.getRobotMap().values()) {
				robot.flyingTick();
			}
		}
	}
	
	/**
	 * If a robot needs to do something every tick, then this is what calls it.
	 */
	private class RobotTickRepeatingTask implements Runnable {

		public void run() {
			for (AbstractRobot robot : stateManager.getRobotMap().values()) {
				robot.tick();
			}
		}
	}
	
	private class SaveStateTask implements Runnable {
		public void run() {
			stateManager.saveState();
		}
	}
	
	/**
	 * Get the bot that belongs to this player
	 */
	private AbstractRobot getRobot(UUID playerId) {
		return stateManager.getRobot(playerId);
	}
	
	/**
	 * The main enable function which starts up the plugin and makes everything work.
	 * 
	 * It does the following:
	 *  - Initialize state variables
	 *  - Registers event listening functions
	 *  - Starts all the repeated tasks for executing API actions and keeping bots flying
	 *  - Starts up the network API server
	 */
	@Override
    public void onEnable() {
		
		actionMap = new HashMap<String, String>();
		actionQueue = new ActionQueue(getLogger());
		stateManager = new RobotStateManager(getLogger());
		stateManager.loadState();

		this.getServer().getPluginManager().registerEvents(this, this);

		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new FlyingTickRepeatingTask(), 1, 10);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new ActionExecutor(actionQueue, stateManager, getLogger()), 1, 2);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new RobotTickRepeatingTask(), 1, 1);
		getServer().getScheduler().scheduleSyncRepeatingTask(
				this, new SaveStateTask(), 10, 20);

		ReadExecutor readExecutor = new ReadExecutor(getLogger(), stateManager);
		this.apiServer = new RobotApiServer(API_PORT, getLogger(), actionQueue, readExecutor);
		apiServerTask = getServer().getScheduler().runTaskAsynchronously(this, apiServer);
	}
	
	/**
	 * Spawn a new robot.
	 * 
	 * @param player The player that owns the robot
	 * @param location The location to spawn in
	 * @param type The string name for the type of robot to create e.g. "pumpkin" or "chicken"
	 * @return The new robot.
	 */
	private AbstractRobot spawnRobot(Player player, Location location, String type) {
		World world = player.getWorld();
		UUID playerId = player.getUniqueId();
		if (type == null) {
			// Default spawn a Pumpkin bot (it works the best).
			return new PumpkinRobot(world, playerId, location, getLogger());
		}
		if (type.toLowerCase().equals("pumpkin")) {
			return new PumpkinRobot(world, playerId, location, getLogger());
		}
		return new PumpkinRobot(world, playerId, location, getLogger());
	}

	/**
	 * Called every time a player right clicks a block, used only for spawning robots.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockRightClick(PlayerInteractEvent event) {
		if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(actionMap.containsKey(event.getPlayer().getName())) {
				stateManager.removeRobot(event.getPlayer().getUniqueId());
				// Spawn a chicken next to the block face that was clicked.
				AbstractRobot robot = spawnRobot(
						event.getPlayer(),
						event.getClickedBlock().getRelative(event.getBlockFace()).getLocation(),
						actionMap.get(event.getPlayer().getName()));
				stateManager.addRobot(event.getPlayer(), robot);
				actionMap.remove(event.getPlayer().getName());
				event.setCancelled(true);
			} else if (event.hasBlock() &&
					event.getClickedBlock().getType() == Material.PUMPKIN) {
				for (AbstractRobot robot : stateManager.getRobotMap().values()) {
					if (robot.getLocation().getBlock().equals(event.getClickedBlock())) {
						event.getPlayer().openInventory(robot.getInventory());
						event.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Listens for everytime an item spawns, if a robot is nearby the nearest
	 * robot captures the item (adds to the robot's inventory) 
	 */
	@EventHandler
	public void onItemSpawnEvent(ItemSpawnEvent spawnEvent) {
		AbstractRobot winner = null;
		double minDistance = 3.0;
		for (AbstractRobot robot : stateManager.getRobotMap().values()) {
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
 
	/**
	 * Called when the plugin is disabled. Kills all robots and shuts down the API server cleanly.
	 */
    @Override
    public void onDisable() {
    	apiServer.shutDown();
    	stateManager.shutDown();
    	actionMap.clear();
    }

    /**
     * Listens for text commands from either the player or the server and executes them.
     */
    @Override
    public boolean onCommand(
    		CommandSender sender, Command cmd, String label, String[] args) {
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
    		if (args.length < 2) {
    			sender.sendMessage("You must also provide a move and a direction");
    			return false;
    		}
    		if (sender instanceof Player && stateManager.hasRobot(((Player) sender).getUniqueId())) {
    			AbstractRobot chicken = getRobot(((Player) sender).getUniqueId());
    			String dir = args[1].toUpperCase();
    			Direction direction = Direction.UP;
    			try {
    				direction = Direction.valueOf(dir);
    			} catch (IllegalArgumentException ex) {
    				sender.sendMessage("Invalid direction.");
    				return false;
    			}
        		boolean moveSuccess = false;
        		if (args[0].equalsIgnoreCase("move")) {
        			moveSuccess = chicken.move(direction);
        			if (!moveSuccess) {
            			sender.sendMessage("Can't move there, there's something in the way.");
            			return true;
            		}
        		} else if (args[0].equalsIgnoreCase("turn")) {
        			moveSuccess = chicken.turn(direction);
        			if (!moveSuccess) {
            			sender.sendMessage("Can't turn up/down.");
            			return true;
            		}
        		} else if (args[0].equalsIgnoreCase("mine")) {
        			moveSuccess = chicken.mine(direction);
        			if (!moveSuccess) {
            			sender.sendMessage("Can't mine that with a pickaxe.");
            			return true;
            		}
        		}
        		if (!moveSuccess) {
        			sender.sendMessage("Unrecognised command");
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
