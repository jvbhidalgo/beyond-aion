package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author ATracer
 */
public class PlayerGroupInvite extends RequestResponseHandler<Player> {

	public PlayerGroupInvite(Player inviter) {
		super(inviter);
	}

	@Override
	public void acceptRequest(Player inviter, Player invited) {
		if (RestrictionsManager.canInviteToGroup(inviter, invited)) {
			PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_PARTY_INVITED_HIM(invited.getName()));
			PlayerGroup group = inviter.getPlayerGroup2();
			if (group != null) {
				PlayerGroupService.addPlayer(group, invited);
			} else {
				PlayerGroupService.createGroup(inviter, invited, TeamType.GROUP, 0);
			}
		}
	}

	@Override
	public void denyRequest(Player inviter, Player invited) {
		PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_PARTY_HE_REJECT_INVITATION(invited.getName()));
	}

}
