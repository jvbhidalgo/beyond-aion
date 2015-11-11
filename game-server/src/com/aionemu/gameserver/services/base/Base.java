package com.aionemu.gameserver.services.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.base.StainedBaseLocation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * @author Source
 * @reworked Estrayl
 */
public abstract class Base<T extends BaseLocation> {
	private final BaseBossDeathListener bossDeathListener = new BaseBossDeathListener(this);
	private final List<SpawnGroup2> spawns;	
	private final T bLoc;
	private final int id;
	private List<Npc> assaulter = new ArrayList<>();
	private Future<?> assaultTask, assaultDespawnTask, bossSpawnTask, outriderSpawnTask;
	private AtomicBoolean isStarted = new AtomicBoolean(false);
	private AtomicBoolean isFinished = new AtomicBoolean(false);
	private Npc boss, flag;

	protected abstract int getAssaultDelay();
	protected abstract int getAssaultDespawnDelay();
	protected abstract int getBossSpawnDelay();
	protected abstract int getNpcSpawnDelay();
	
	Base(T bLoc) {
		this.bLoc = bLoc;
		this.id = bLoc.getId();
		spawns = initSpawns();
	}
	
	public final void start() throws BaseException {
		if (isStarted.compareAndSet(false, true))
			handleStart();
		else
			throw new BaseException("Attempt to start Base twice! ID:" + id);
	}
	
	public final void stop() throws BaseException {
		if (isFinished.compareAndSet(false, true))
			handleStop();
		else
			throw new BaseException("Attempt to stop Base twice! ID:" + id);
	}

	protected void handleStart() {		
		spawnBySpawnHandler(SpawnHandlerType.FLAG, getRace());
		spawnBySpawnHandler(SpawnHandlerType.MERCHANT, getRace());
		spawnBySpawnHandler(SpawnHandlerType.SENTINEL, getRace());
		scheduleOutriderSpawn();
		scheduleBossSpawn();
	}

	protected void handleStop() {
		cancelAssaultTask();
		cancelAssaultDespawnTask();
		cancelBossSpawnTask();
		cancelOutriderSpawnTask();
		unregDeathListener();
		despawnAllNpcs();
	}
	
	private void despawnAllNpcs() {
		List<Npc> spawned = World.getInstance().getBaseSpawns(id);
		if (spawned != null) {
			for (Npc npc : spawned) {
				if (npc != null)
					npc.getController().onDelete();
			}
		}
	}
	
	protected void scheduleOutriderSpawn() {
		outriderSpawnTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
			
			@Override
			public void run() {
				if (isFinished.get() || getNpcSpawnDelay() == 0)
					return;
				spawnBySpawnHandler(SpawnHandlerType.OUTRIDER, getRace());
				if (bLoc instanceof StainedBaseLocation) {
					if (((StainedBaseLocation) bLoc).isEnhanced()) {
						spawnBySpawnHandler(SpawnHandlerType.GUARDIAN, getRace());
						spawnBySpawnHandler(SpawnHandlerType.OUTRIDER_ENHANCED, getRace());
					}
				}
			}
		}, getNpcSpawnDelay());
	}
	
	protected void scheduleBossSpawn() {
		bossSpawnTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
			
			@Override
			public void run() {
				if (isFinished.get())
					return;
				spawnBySpawnHandler(SpawnHandlerType.BOSS, getRace());
				broadcastMessage(getBossSpawnMsg());
				scheduleAssault();
			}
		}, getBossSpawnDelay());
	}
	
	private void scheduleAssault() {
		assaultTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
			
			@Override
			public void run() {
				if (isFinished.get())
					return;
				spawnBySpawnHandler(SpawnHandlerType.ATTACKER, chooseAssaultRace());
				broadcastMessage(getAssaultMsg());
				scheduleAssaultDespawn();
			}
		}, getAssaultDelay());
	}
	
	private Race chooseAssaultRace() {
		List<Race> coll = new ArrayList<>();
		coll.add(Race.ASMODIANS);
		coll.add(Race.ELYOS);
		coll.add(Race.NPC);
		coll.remove(getRace());
		return coll.get(Rnd.get(0, 1));
	}
	
	private void scheduleAssaultDespawn() {
		assaultDespawnTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
			
			@Override
			public void run() {
				if (isFinished.get())
					return;
				despawnAssaulter();
				scheduleAssault();
			}
		}, getAssaultDespawnDelay());
	}
	
	private void despawnAssaulter() {
		for (Npc npc : assaulter) {
			if (npc != null && !npc.getLifeStats().isAlreadyDead())
				npc.getController().onDelete();
		}
		assaulter.clear();
	}
	
	public void spawnBySpawnHandler(SpawnHandlerType type, Race targetRace) {
		for (SpawnGroup2 group : spawns) {
			for (SpawnTemplate temp : group.getSpawnTemplates()) {
				final BaseSpawnTemplate template = (BaseSpawnTemplate) temp;
				if (template.getBaseRace().equals(targetRace)) {
					if (template.getHandlerType().equals(type)) {
						Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
						switch (type) {
							case ATTACKER:
								assaulter.add(npc);
								break;
							case BOSS:
								initBoss(npc);
								break;
							case FLAG:
								initFlag(npc);
								break;
						}
					}
				}
			}
		}
	}
	
	private void initBoss(Npc npc) throws BaseException, NullPointerException {
		if (npc == null)
			throw new NullPointerException("No boss found for base! ID:" + id);
		if (npc.getSpawn().getHandlerType().equals(SpawnHandlerType.BOSS)) {
			if (boss == null) {
				boss = npc;
				regDeathListener();
			} else {
				throw new BaseException("Tried to initialize boss twice! Base ID:" + id);
			}
		} else {
			throw new BaseException("Tried to initialize non-boss npc as boss for base ID:" + id);
		}
	}
	
	private void initFlag(Npc npc) throws BaseException, NullPointerException {
		if (npc == null)
			throw new NullPointerException("No flag found for base! ID:" + id);
		if (npc.getSpawn().getHandlerType().equals(SpawnHandlerType.FLAG)) {
			if (flag == null)
				flag = npc;
			else
				throw new BaseException("Tried to initialize flag twice! Base ID:" + id);
		} else {
			throw new BaseException("Tried to initialize non-flag npc as flag for base ID:" + id);
		}
	}
	
	private List<SpawnGroup2> initSpawns() throws NullPointerException {
		List<SpawnGroup2> temp = DataManager.SPAWNS_DATA2.getBaseSpawnsByLocId(id);			
		if (temp != null)
			return temp;
		else
		 throw new NullPointerException("No spawns found for base ID:" + id);
}
	
	private SM_SYSTEM_MESSAGE getBossSpawnMsg() {
		switch (id) {
			case 6101:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V01();
			case 6102:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V02();
			case 6103:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V03();
			case 6104:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V04();
			case 6105:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V05();
			case 6106:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V06();
			case 6107:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V07();
			case 6108:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V08();
			case 6109:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V09();
			case 6110:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V10();
			case 6111:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V11();
			case 6112:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V12();
			case 6113:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_CHIEF_V13();	
			default:
				return null;
		}
	}
	
	private SM_SYSTEM_MESSAGE getAssaultMsg() {
		switch (id) {
			case 6101:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V01();
			case 6102:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V02();
			case 6103:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V03();
			case 6104:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V04();
			case 6105:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V05();
			case 6106:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V06();
			case 6107:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V07();
			case 6108:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V08();
			case 6109:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V09();
			case 6110:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V10();
			case 6111:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V11();
			case 6112:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V12();
			case 6113:
				return SM_SYSTEM_MESSAGE.STR_MSG_LDF4_ADVANCE_KILLER_V13();	
			default:
				return null;
		}
	}
	
	private void broadcastMessage(SM_SYSTEM_MESSAGE msg) {
		if (msg != null) {
			World.getInstance().getWorldMap(getWorldId()).getMainWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, msg);
				}
			});
		}
	}
	
	private void regDeathListener() {
		if (boss == null)
			throw new BaseException("Tried to register DeathListener for null boss! BaseID:" + id);
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		ai.addEventListener(bossDeathListener);
	}
	
	protected void unregDeathListener() {
		if (boss == null)
			return;
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		ai.removeEventListener(bossDeathListener);
	}
	
	protected void cancelAssaultTask() {
		if (assaultTask != null && assaultTask.isCancelled())
			assaultTask.cancel(true);
	}
	
	protected void cancelAssaultDespawnTask() {
		if (assaultDespawnTask != null && assaultDespawnTask.isCancelled())
			assaultDespawnTask.cancel(true);
	}
	
	protected void cancelBossSpawnTask() {
		if (bossSpawnTask != null && bossSpawnTask.isCancelled())
			bossSpawnTask.cancel(true);
	}
	
	public void cancelOutriderSpawnTask() {
		if (outriderSpawnTask != null && outriderSpawnTask.isCancelled())
			outriderSpawnTask.cancel(true);
	}
	
	public Npc getBoss() {
		return boss;
	}
	
	public Npc getFlag() {
		return flag;
	}
	
	public T getLocation() {
		return bLoc;
	}
	
	public int getId() {
		return id;
	}
	
	public int getWorldId() {
		return bLoc.getWorldId();
	}
	
	public Race getRace() {
		return bLoc.getRace();
	}
	
	public void setLocRace(Race race) {
		bLoc.setRace(race);
	}
	
	public boolean isStarted() {
		return isStarted.get();
	}
	
	public boolean isFinished() {
		return isFinished.get();
	}
	
	public boolean isUnderAssault() {
		return !assaulter.isEmpty();
	}
}