package au.id.katharos.robominions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_7_R3.EntityBat;
import net.minecraft.server.v1_7_R3.EntityChicken;
import net.minecraft.server.v1_7_R3.EntityInsentient;
import net.minecraft.server.v1_7_R3.EntityTypes;

import org.bukkit.entity.EntityType;

public enum CustomEntityType {
	
	//CHICKEN("Chicken", 93, EntityType.CHICKEN, EntityChicken.class, CustomChickenEntity.class),
	BAT("Chicken", 93, EntityType.BAT, EntityBat.class, CustomChickenEntity.class);
	
	private String name;
    private int id;
    private EntityType entityType;
    private Class<? extends EntityInsentient> nmsClass;
    private Class<? extends EntityInsentient> customClass;

    private CustomEntityType(
    		String name, int id, EntityType entityType, 
    		Class<? extends EntityInsentient> nmsClass,
    		Class<? extends EntityInsentient> customClass){
        this.name = name;
        this.id = id;
        this.entityType = entityType;
        this.nmsClass = nmsClass;
        this.customClass = customClass;
    }
 
    public String getName(){
        return this.name;
    }
 
    public int getID(){
        return this.id;
    }
 
    public EntityType getEntityType(){
        return this.entityType;
    }
 
    public Class<? extends EntityInsentient> getNMSClass(){
        return this.nmsClass;
    }
 
    public Class<? extends EntityInsentient> getCustomClass(){
        return this.customClass;
    }

    public static void registerEntity(CustomEntityType entityType) {
    	try {
    		/*
             * First, we make a list of all HashMap's in the EntityTypes class
             * by looping through all fields. I am using reflection here so we
             * have no problems later when minecraft changes the field's name.
             * By creating a list of these maps we can easily modify them later
             * on.
             */
    		List<Map<?, ?>> dataMaps = new ArrayList<Map<?, ?>>();
    		for (Field field : EntityTypes.class.getDeclaredFields()) {
    			if (field.getType().getSimpleName().equals(Map.class.getSimpleName())) {
    				field.setAccessible(true);

    				dataMaps.add((Map<?, ?>) field.get(null));

    			}
    		}

    		/*
    		 * since minecraft checks if an id has already been registered, we
    		 * have to remove the old entity class before we can register our
    		 * custom one
    		 *
    		 * map 0 is the map with names and map 2 is the map with ids
    		 */
    		if (dataMaps.get(2).containsKey(entityType.id)) {
    			dataMaps.get(0).remove(entityType.name);
    			dataMaps.get(2).remove(entityType.id);
    		}
    		
    		/*
             * now we call the method which adds the entity to the lists in the
             * EntityTypes class, now we are actually 'registering' our entity
             */
    		Method method = EntityTypes.class.getDeclaredMethod("a", Class.class, String.class, int.class);
            method.setAccessible(true);
            method.invoke(null, entityType.customClass, entityType.name, entityType.id);

    	} catch (IllegalArgumentException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (IllegalAccessException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
