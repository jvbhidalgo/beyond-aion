package ai.worlds.panesterra.ahserionsflight;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.NpcAI;
import com.aionemu.gameserver.model.templates.spawns.panesterra.AhserionsFlightSpawnTemplate;
import com.aionemu.gameserver.services.panesterra.ahserion.AhserionRaid;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * @author Yeats
 *
 */
@AIName("ahserion_troopers")
public class AhserionBalaurTroopersAI extends NpcAI {

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		spawnAttackers();
		scheduleDespawn();
	}
	
	private void spawnAttackers() {
		if (getOwner().getNpcId() == 297187) { //297188 spawns already some attackers
			return;
		}
		if (getOwner().getSpawn() instanceof AhserionsFlightSpawnTemplate) {
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					if (AhserionRaid.getInstance().isStarted()
						&& AhserionRaid.getInstance().isTeamNotEliminated(((AhserionsFlightSpawnTemplate) getOwner().getSpawn()).getTeam())) {
							AhserionRaid.getInstance().spawnStage(5, ((AhserionsFlightSpawnTemplate) getOwner().getSpawn()).getTeam());
						}
				}
			}, 6500);
		}
	}
	
	private void scheduleDespawn() {
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (getOwner() != null)
					getOwner().getController().delete();
			}
		}, 25000);
	}
}