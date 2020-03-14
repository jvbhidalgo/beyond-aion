package ai.instance.illuminaryObelisk;

import java.util.concurrent.Future;

import com.aionemu.gameserver.ai.AIActions;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.NpcAI;
import com.aionemu.gameserver.ai.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * @author M.O.G. Dision. Estrayl
 */
@AIName("dynatoum_maintenance_device")
public class DynatoumMaintenanceDeviceAI extends NpcAI {

	private Future<?> skillTask;

	public DynatoumMaintenanceDeviceAI(Npc owner) {
		super(owner);
	}

	@Override
	public boolean canThink() {
		return false;
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		Npc dynatoum = getPosition().getWorldMapInstance().getNpc(getPosition().getMapId() == 301230000 ? 233740 : 234686);
		AIActions.targetCreature(this, dynatoum);
		scheduleSkillTask();
	}

	private void scheduleSkillTask() {
		skillTask = ThreadPoolManager.getInstance().schedule(() -> {
			if (!isDead())
				AIActions.useSkill(this, 21535);
		}, 10000);
	}

	@Override
	public void onEndUseSkill(SkillTemplate skillTemplate, int skillLevel) {
		switch (skillTemplate.getSkillId()) {
			case 21535:
				scheduleSkillTask();
				break;
		}
	}

	@Override
	protected void handleDespawned() {
		skillTask.cancel(true);
		super.handleDespawned();
	}

	@Override
	public boolean ask(AIQuestion question) {
		switch (question) {
			case SHOULD_DECAY:
			case SHOULD_RESPAWN:
			case SHOULD_REWARD:
				return false;
			default:
				return super.ask(question);
		}
	}
}
