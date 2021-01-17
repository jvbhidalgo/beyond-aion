package ai.instance.eternalBastion;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.AIActions;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.AggressiveNpcAI;

/**
 * @author Cheatkiller
 */
@AIName("eb_wallsattacker")
public class PashidSiegeVolatileAI extends AggressiveNpcAI {

	public PashidSiegeVolatileAI(Npc owner) {
		super(owner);
	}

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		attackWall();
		detonation();
	}

	private void attackWall() {
		ThreadPoolManager.getInstance().schedule(() -> {
			Npc wall = getPosition().getWorldMapInstance().getNpc(831333);
			if (wall != null && wall.isSpawned())
				getOwner().getAggroList().addHate(wall, 1000);
		}, 3000);
	}

	private void detonation() {
		ThreadPoolManager.getInstance().schedule(() -> {
			if (!isDead() && getOwner().isSpawned())
				AIActions.useSkill(PashidSiegeVolatileAI.this, getOwner().getNpcId() == 231150 ? 21259 : 21272);
		}, Rnd.get(10000, 30000));
	}
}