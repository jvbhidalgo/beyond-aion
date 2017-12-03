package ai.worlds.panesterra.ahserionsflight;

import com.aionemu.gameserver.ai.AIActions;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.AIRequest;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.panesterra.ahserion.AhserionRaid;
import com.aionemu.gameserver.services.panesterra.ahserion.PanesterraFaction;
import com.aionemu.gameserver.services.panesterra.ahserion.PanesterraTeam;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.GeneralNpcAI;

/**
 * @author Yeats
 * @reworked Estrayl October 29th, 2017.
 */
@AIName("ahserion_registration_corridor")
public class AhserionRegistrationCorridor extends GeneralNpcAI {

	private PanesterraFaction faction;

	public AhserionRegistrationCorridor(Npc owner) {
		super(owner);
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_SVS_INVADE_DIRECT_PORTAL_OPEN());
		switch (getNpcId()) {
			case 802219:
				faction = PanesterraFaction.BELUS;
				break;
			case 802221:
				faction = PanesterraFaction.ASPIDA;
				break;
			case 802223:
				faction = PanesterraFaction.ATANATOS;
				break;
			case 802225:
				faction = PanesterraFaction.DISILLON;
				break;
		}
		ThreadPoolManager.getInstance().schedule(() -> getOwner().getController().delete(), 10 * 60000);
	}

	@Override
	public void handleDialogStart(Player player) {
		if (player.getLevel() < 65) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_SVS_DIRECT_PORTAL_LEVEL_LIMIT());
			return;
		}
		AIActions.addRequest(this, player, 905067, 0, new AIRequest() {

			@Override
			public void acceptRequest(Creature requester, Player responder, int requestId) {
				if (!AhserionRaid.getInstance().isStarted()) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_READY_PANGAEA());
					return;
				} else if (AhserionRaid.getInstance().getTeamMemberCountByFaction(faction) >= SiegeConfig.AHSERION_MAX_PLAYERS_PER_TEAM) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_SVS_DIRECT_PORTAL_USE_COUNT_LIMIT());
					return;
				}
				PanesterraTeam team = AhserionRaid.getInstance().getFactionTeam(faction);
				team.addTeamMemberIfAbsent(player.getObjectId());
				team.movePlayerToStartPosition(player);
			}
		});
	}

}
