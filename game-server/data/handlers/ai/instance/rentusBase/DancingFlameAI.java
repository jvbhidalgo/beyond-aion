package ai.instance.rentusBase;

import java.util.concurrent.Future;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.GeneralNpcAI;

/**
 * @author xTz
 * @modified Estrayl
 */
@AIName("dancing_flame")
public class DancingFlameAI extends GeneralNpcAI {

	private Future<?> buffTask;

	public DancingFlameAI(Npc owner) {
		super(owner);
	}

	@Override
	public int modifyDamage(Creature attacker, int damage, Effect effect) {
		return 0;
	}

	private void startBuffTask() {
		buffTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (isAnyPlayerInRange())
					SkillEngine.getInstance().getSkill(getOwner(), getNpcId() == 282998 ? 20536 : 20535, 60, getOwner()).useNoAnimationSkill();
			}

		}, 10000, 9000);
	}

	private boolean isAnyPlayerInRange() {
		for (Player player : getKnownList().getKnownPlayers().values())
			if (isInRange(player, 10))
				return true;
		return false;
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startBuffTask();
	}

	@Override
	protected void handleDespawned() {
		buffTask.cancel(true);
		super.handleDespawned();
	}

	@Override
	public boolean ask(AIQuestion question) {
		switch (question) {
			case CAN_ATTACK_PLAYER:
				return true;
			case SHOULD_RESPAWN:
			case SHOULD_LOOT:
			case SHOULD_DECAY:
				return false;
			default:
				return super.ask(question);
		}
	}

}