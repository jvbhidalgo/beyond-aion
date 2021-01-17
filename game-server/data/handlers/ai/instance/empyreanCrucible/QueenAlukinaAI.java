package ai.instance.empyreanCrucible;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.AggressiveNpcAI;

/**
 * @author Luzien
 */
@AIName("alukina_emp")
public class QueenAlukinaAI extends AggressiveNpcAI {

	private List<Integer> percents = new ArrayList<>();
	private Future<?> task;

	public QueenAlukinaAI(Npc owner) {
		super(owner);
	}

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		addPercents();
	}

	@Override
	public void handleDespawned() {
		cancelTask();
		percents.clear();
		super.handleDespawned();
	}

	@Override
	public void handleDied() {
		cancelTask();
		super.handleDied();
	}

	@Override
	public void handleBackHome() {
		addPercents();
		cancelTask();
		super.handleBackHome();
	}

	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	private void startEvent(int percent) {

		SkillEngine.getInstance().getSkill(getOwner(), 17899, 41, getTarget()).useNoAnimationSkill();

		switch (percent) {
			case 75:
				scheduleSkill(17900, 4500);
				PacketSendUtility.broadcastMessage(getOwner(), 340487, 10000);
				scheduleSkill(17899, 14000);
				scheduleSkill(17900, 18000);
				break;
			case 50:
				scheduleSkill(17280, 4500);
				scheduleSkill(17902, 8000);
				break;
			case 25:
				task = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable() {

					@Override
					public void run() {
						if (isDead()) {
							cancelTask();
						} else {
							SkillEngine.getInstance().getSkill(getOwner(), 17901, 41, getTarget()).useNoAnimationSkill();
							scheduleSkill(17902, 5500);
							scheduleSkill(17902, 7500);
						}
					}
				}, 4500, 20000);
				break;
		}
	}

	private void cancelTask() {
		if (task != null && !task.isCancelled())
			task.cancel(true);
	}

	private void scheduleSkill(final int skill, int delay) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (!isDead()) {
					SkillEngine.getInstance().getSkill(getOwner(), skill, 41, getTarget()).useNoAnimationSkill();

				}
			}
		}, delay);
	}

	private void checkPercentage(int percentage) {
		for (Integer percent : percents) {
			if (percentage <= percent) {
				percents.remove(percent);
				startEvent(percent);
				break;
			}
		}
	}

	private void addPercents() {
		percents.clear();
		Collections.addAll(percents, new Integer[] { 75, 50, 25 });
	}
}