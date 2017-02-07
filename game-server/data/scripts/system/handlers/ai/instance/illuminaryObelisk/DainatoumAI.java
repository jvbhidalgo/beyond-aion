package ai.instance.illuminaryObelisk;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldPosition;

import ai.AggressiveNpcAI;
import javolution.util.FastTable;

/**
 * @author M.O.G. Dision
 * @reworked Estrayl
 */
@AIName("dainatum")
public class DainatoumAI extends AggressiveNpcAI {

	private AtomicBoolean isStarted = new AtomicBoolean(false);
	private List<VisibleObject> adds = new FastTable<>();
	protected List<Integer> percents = new FastTable<>();
	private Future<?> despawnTask;
	private int progress = 0;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		addPercent();
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isStarted.compareAndSet(false, true))
			scheduleDespawn();
		checkPercentage(getLifeStats().getHpPercentage());
	}

	protected synchronized void checkPercentage(int hpPercentage) {
		for (Integer percent : percents) {
			if (hpPercentage <= percent) {
				switch (percent) {
					case 70:
						removeBossEntry();
						break;
					case 50:
					case 10:
						spawnHealers();
						break;
					case 30:
					case 20:
						spawnBombs();
						break;
				}
				percents.remove(percent);
				break;
			}
		}
	}

	protected void removeBossEntry() {
		PacketSendUtility.broadcastToMap(getOwner(), 1402212);
		Npc portal = getPosition().getWorldMapInstance().getNpc(702216);
		if (portal != null)
			portal.getController().delete();
	}

	private void scheduleDespawn() {
		despawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (!isAlreadyDead()) {
					switch (progress) {
						case 0:
							PacketSendUtility.broadcastToMap(getOwner(), 1402143);
							break;
						case 1:
							PacketSendUtility.broadcastToMap(getOwner(), 1402144);
							break;
						case 4:
							PacketSendUtility.broadcastToMap(getOwner(), 1402145);
							break;
						case 5:
							PacketSendUtility.broadcastToMap(getOwner(), 1402146);
							onDespawn();
							break;
					}
					progress++;
				}
			}
		}, 1000, 60000);
	}
	
	private void onDespawn() {
		if (getOwner() != null && getOwner().getLifeStats().isAlreadyDead())
			SkillEngine.getInstance().getSkill(getOwner(), 21534, 1, getOwner()).useSkill();
		getOwner().getController().delete();
	}

	protected void addPercent() {
		percents.clear();
		Collections.addAll(percents, new Integer[] { 70, 50, 30, 20, 10 });
	}

	private void cancelDespawnTask() {
		if (despawnTask != null && !despawnTask.isCancelled())
			despawnTask.cancel(true);
	}

	protected int getBombId() {
		return 284859;
	}

	protected void spawnBombs() {
		float direction = Rnd.get(0, 199) / 100f;
		int distance = Rnd.get(0, 10);
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		int bomb = getBombId();
		adds.add(spawn(bomb, p.getX() + x1, p.getY() + y1, p.getZ(), p.getHeading()));
		adds.add(spawn(bomb, p.getX() + x1, p.getY() + y1, p.getZ(), p.getHeading()));
		adds.add(spawn(bomb, p.getX() + x1, p.getY() + y1, p.getZ(), p.getHeading()));
		adds.add(spawn(bomb, p.getX() + x1, p.getY() + y1, p.getZ(), p.getHeading()));
	}

	protected void spawnHealers() {
		adds.add(spawn(284861, 261.8195f, 261.0765f, 455.1237f, (byte) 75));
		adds.add(spawn(284861, 255.2968f, 245.7124f, 455.1236f, (byte) 30));
		adds.add(spawn(284861, 246.3872f, 254.5075f, 455.1237f, (byte) 0));
	}

	private void deleteAdds() {
		for (VisibleObject npc : adds) {
			if (npc != null)
				npc.getController().delete();
		}
	}

	@Override
	protected void handleDespawned() {
		cancelDespawnTask();
		deleteAdds();
		super.handleDespawned();
	}

	@Override
	protected void handleDied() {
		cancelDespawnTask();
		super.handleDied();
		deleteAdds();
	}

	@Override
	protected void handleBackHome() {
		cancelDespawnTask();
		deleteAdds();
		PacketSendUtility.broadcastToMap(getOwner(), 1402146);
		super.handleBackHome();
		getOwner().getController().delete(); // No Full Reset needed.
	}
}