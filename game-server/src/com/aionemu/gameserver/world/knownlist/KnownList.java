package com.aionemu.gameserver.world.knownlist;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.animations.ObjectDeleteAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.MapRegion;

/**
 * KnownList.
 * 
 * @author -Nemesiss-
 * @modified kosyachok, Neon
 */
public class KnownList {

	private static final Logger log = LoggerFactory.getLogger(KnownList.class);

	/**
	 * Owner of this KnownList.
	 */
	protected final VisibleObject owner;

	/**
	 * List of objects that this KnownList owner knows
	 */
	protected final Map<Integer, VisibleObject> knownObjects = new ConcurrentHashMap<>();

	/**
	 * List of player that this KnownList owner knows
	 */
	protected volatile Map<Integer, Player> knownPlayers;

	/**
	 * List of objects that this KnownList owner sees
	 */
	protected final Map<Integer, VisibleObject> visualObjects = new ConcurrentHashMap<>();

	private ReentrantLock lock = new ReentrantLock();

	/**
	 * @param owner
	 */
	public KnownList(VisibleObject owner) {
		this.owner = owner;
	}

	/**
	 * Do KnownList update.
	 */
	public void doUpdate() {
		lock.lock();
		try {
			forgetObjects();
			findVisibleObjects();
		} catch (Exception e) {
			log.error("Exception during KnownList update of {}", owner, e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Clear known list. Used when object is despawned.
	 * 
	 * @param animation
	 *          - the animation others will see on despawn
	 */
	public void clear(ObjectDeleteAnimation animation) {
		for (VisibleObject object : knownObjects.values())
			object.getKnownList().del(owner, animation);

		knownObjects.clear();
		if (knownPlayers != null) {
			knownPlayers.clear();
		}
		visualObjects.clear();
	}

	/**
	 * Checks if owner knows the object.
	 */
	public boolean knows(VisibleObject object) {
		return knownObjects.containsKey(object.getObjectId());
	}

	/**
	 * Checks if owner sees the object.
	 */
	public boolean sees(VisibleObject object) {
		return visualObjects.containsKey(object.getObjectId());
	}

	/**
	 * Add VisibleObject to this KnownList.
	 * 
	 * @param object
	 */
	protected boolean add(VisibleObject object) {
		if (!isAwareOf(object))
			return false;

		if (knownObjects.put(object.getObjectId(), object) == null) {
			if (object instanceof Player) {
				checkKnownPlayersInitialized();
				knownPlayers.put(object.getObjectId(), (Player) object);
			}

			if (object instanceof Player && !owner.canSee((Player) object))
				return true;
			if (object instanceof Pet && !sees(((Pet) object).getMaster()) && !owner.equals(((Pet) object).getMaster()))
				return true; // pet won't display if client doesn't already see the master (spawn order is important, so playercontroller must handle it)

			addVisualObject(object);
			return true;
		}

		return false;
	}

	public void addVisualObject(VisibleObject object) {
		if (visualObjects.put(object.getObjectId(), object) == null)
			owner.getController().see(object);
	}

	/**
	 * Removes VisibleObject from this KnownList and deletes it.
	 * 
	 * @param object
	 * @param animation
	 *          - the disappear animation others will see
	 */
	private void del(VisibleObject object, ObjectDeleteAnimation animation) {
		if (knownObjects.remove(object.getObjectId()) != null) {
			if (knownPlayers != null)
				knownPlayers.remove(object.getObjectId());
			delVisualObject(object, animation);
		}
	}

	/**
	 * Deletes VisibleObject.
	 * 
	 * @param object
	 * @param animation
	 *          - the disappear animation others will see
	 */
	public void delVisualObject(VisibleObject object, ObjectDeleteAnimation animation) {
		if (visualObjects.remove(object.getObjectId()) != null)
			owner.getController().notSee(object, animation);
	}

	/**
	 * forget out of distance objects.
	 */
	private void forgetObjects() {
		for (VisibleObject object : knownObjects.values()) {
			if (!MathUtil.isIn3dRange(owner, object, object.getVisibleDistance()) && !object.getKnownList().checkReversedObjectInRange(owner)) {
				del(object, ObjectDeleteAnimation.NONE);
				object.getKnownList().del(owner, ObjectDeleteAnimation.NONE);
			}
		}
	}

	/**
	 * Find objects that are in visibility range.
	 */
	protected void findVisibleObjects() {
		if (owner == null || !owner.isSpawned())
			return;

		if (owner instanceof Npc) {
			if (((Creature) owner).isFlag()) {
				owner.getPosition().getWorldMapInstance().forEachPlayer(new Consumer<Player>() {

					@Override
					public void accept(Player player) {
						if (add(player)) {
							player.getKnownList().add(owner);
						}
					}
				});
			}
		} else if (owner instanceof Player) {
			for (Npc npc : owner.getPosition().getWorldMapInstance().getNpcs()) {
				if (npc.isFlag()) {
					if (add(npc)) {
						npc.getKnownList().add(owner);
					}
				}
			}
		}
		for (MapRegion region : owner.getActiveRegion().getNeighbours()) {
			for (VisibleObject newObject : region.getObjects().values()) {
				if (newObject == null || owner.equals(newObject))
					continue;

				if (!isAwareOf(newObject))
					continue;

				if (knownObjects.containsKey(newObject.getObjectId()))
					continue;

				if (!MathUtil.isIn3dRange(owner, newObject, newObject.getVisibleDistance()) && !newObject.getKnownList().checkReversedObjectInRange(owner))
					continue;

				/**
				 * New object is not known.
				 */
				if (add(newObject)) {
					newObject.getKnownList().add(owner);
				}
			}
		}
	}

	/**
	 * Whether knownlist owner aware of found object (should be kept in knownlist)
	 * 
	 * @param newObject
	 * @return
	 */
	protected boolean isAwareOf(VisibleObject newObject) {
		return true;
	}

	/**
	 * Check can be overriden if new object has different known range and that value should be used
	 * 
	 * @param newObject
	 * @return
	 */
	protected boolean checkReversedObjectInRange(VisibleObject newObject) {
		return false;
	}

	public void forEachNpc(Consumer<Npc> function) {
		forEachNpc(function, Integer.MAX_VALUE);
	}

	public int forEachNpc(Consumer<Npc> function, int iterationLimit) {
		int counter = 0;
		try {
			for (VisibleObject newObject : knownObjects.values()) {
				if (newObject instanceof Npc) {
					if (++counter > iterationLimit)
						break;
					function.accept((Npc) newObject);
				}
			}
		} catch (Exception ex) {
			log.error("Exception when iterating over npcs known by " + owner, ex);
		}
		return counter;
	}

	public void forEachNpcWithOwner(BiConsumer<Npc, VisibleObject> function) {
		forEachNpcWithOwner(function, Integer.MAX_VALUE);
	}

	public int forEachNpcWithOwner(BiConsumer<Npc, VisibleObject> function, int iterationLimit) {
		int counter = 0;
		try {
			for (VisibleObject newObject : knownObjects.values()) {
				if (newObject instanceof Npc) {
					if (++counter > iterationLimit)
						break;
					function.accept((Npc) newObject, owner);
				}
			}
		} catch (Exception ex) {
			log.error("Exception when iterating over npcs known by " + owner, ex);
		}
		return counter;
	}

	public void forEachPlayer(Consumer<Player> function) {
		if (knownPlayers == null)
			return;

		try {
			knownPlayers.values().forEach(player -> {
				if (player != null) // can be null if entry got removed after iterator allocation
					function.accept(player);
			});
		} catch (Exception ex) {
			log.error("Exception when iterating over players known by " + owner, ex);
		}
	}

	public void forEachObject(Consumer<VisibleObject> function) {
		try {
			knownObjects.values().forEach(object -> {
				if (object != null) // can be null if entry got removed after iterator allocation
					function.accept(object);
			});
		} catch (Exception ex) {
			log.error("Exception when iterating over objects known by " + owner, ex);
		}
	}

	public void forEachVisibleObject(Consumer<VisibleObject> function) {
		try {
			visualObjects.values().forEach(object -> {
				if (object != null) // can be null if entry got removed after iterator allocation
					function.accept(object);
			});
		} catch (Exception ex) {
			log.error("Exception when iterating over visual objects seen by " + owner, ex);
		}
	}

	public Map<Integer, VisibleObject> getKnownObjects() {
		return knownObjects;
	}

	public Map<Integer, Player> getKnownPlayers() {
		return knownPlayers != null ? knownPlayers : Collections.<Integer, Player> emptyMap();
	}

	final void checkKnownPlayersInitialized() {
		if (knownPlayers == null) {
			synchronized (this) {
				if (knownPlayers == null) {
					knownPlayers = new ConcurrentHashMap<>();
				}
			}
		}
	}

	/**
	 * @param templateId
	 * @return The visible object with the given template ID or null if not found.
	 */
	public VisibleObject findObject(int templateId) {
		for (VisibleObject v : knownObjects.values()) {
			if (v != null && v.getObjectTemplate().getTemplateId() == templateId) // can be null if entry got removed after iterator allocation
				return v;
		}
		return null;
	}

	public VisibleObject getObject(int targetObjectId) {
		return knownObjects.get(targetObjectId);
	}

	public Player getPlayer(int targetObjectId) {
		return knownPlayers == null ? null : knownPlayers.get(targetObjectId);
	}

	/**
	 * Updates visibility of all known players, depending on the current see state of the owner of this {@link KnownList}.
	 */
	public void updateVisiblePlayers() {
		if (!(owner instanceof Player))
			return;

		forEachPlayer(other -> {
			boolean canSee = owner.canSee(other);
			boolean isSee = sees(other);

			if (canSee && !isSee) {
				addVisualObject(other);
				if (other.getPet() != null)
					addVisualObject(other.getPet());
			} else if (!canSee && isSee) {
				delVisualObject(other, ObjectDeleteAnimation.FADE_OUT);
				if (other.getPet() != null)
					delVisualObject(other.getPet(), ObjectDeleteAnimation.FADE_OUT);
			}
		});
	}
}
