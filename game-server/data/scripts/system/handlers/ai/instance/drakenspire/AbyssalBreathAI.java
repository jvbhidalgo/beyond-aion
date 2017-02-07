package ai.instance.drakenspire;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.GeneralNpcAI;

/**
 * @author Estrayl
 */
@AIName("abyssal_breath")
public class AbyssalBreathAI extends GeneralNpcAI {

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(() -> {
			SkillEngine.getInstance().getSkill(getOwner(), 21620, 1, getOwner()).useSkill();
			ThreadPoolManager.getInstance().schedule(() -> {
				getOwner().getKnownList().forEachPlayer(p -> {
						if (isInRange(p, 11))
							SkillEngine.getInstance().getSkill(getOwner(), 21874, 1, p).useSkill();
				});
				ThreadPoolManager.getInstance().schedule(() -> getOwner().getController().delete(), 3000);
			}, 4250);
		}, 4000);
	}
}