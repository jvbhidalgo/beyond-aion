package ai.instance.danuarReliquary;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.AIActions;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.NpcAI;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * @author Ritsu
 * @modified Estrayl October 28th, 2017.
 */
@AIName("vengeful_orb")
public class VengefulOrbAI extends NpcAI {

	public VengefulOrbAI(Npc owner) {
		super(owner);
	}

	/**
	 * Currently capped the damage value. Seems to be some defensive calculations are not even retail.
	 * Damage on retail: 12000 - 13000
	 * Damage here: 17000+
	 */
	@Override
	public int modifyOwnerDamage(int damage, Effect effect) {
		return damage > 12500 ? Rnd.get(12000, 13000) : damage;
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		AIActions.useSkill(VengefulOrbAI.this, 21178);
	}

	@Override
	public void onEndUseSkill(SkillTemplate skillTemplate) {
		if (skillTemplate.getSkillId() == 21178)
			getOwner().getController().delete();
	}
}
