package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestTarget;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PositionUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author ginho1
 * @modified Neon
 */
public class CM_QUEST_SHARE extends AionClientPacket {

	public int questId;

	public CM_QUEST_SHARE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.questId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		QuestTemplate questTemplate = DataManager.QUEST_DATA.getQuestById(questId);
		QuestState questState = player.getQuestStateList().getQuestState(questId);

		if (questTemplate == null || questTemplate.isCannotShare()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100001)); // This quest cannot be shared.
			return;
		}

		if (questState == null || questState.getStatus() == QuestStatus.COMPLETE)
			return;

		if (!player.isInAlliance() && questTemplate.getTarget() == QuestTarget.ALLIANCE) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100005)); // There are no Alliance members to share the quest with.
			return;
		}

		if (!player.isInTeam()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100000)); // There are no group members to share the quest with.
			return;
		}

		for (Player member : player.isInGroup() ? player.getPlayerGroup().getOnlineMembers() : player.getPlayerAllianceGroup().getOnlineMembers()) {
			if (player.equals(member) || !PositionUtil.isInRange(member, player, GroupConfig.GROUP_MAX_DISTANCE))
				continue;

			if (member.getQuestStateList().hasQuest(questId)) {
				QuestStatus qs = member.getQuestStateList().getQuestState(questId).getStatus();
				if (qs == QuestStatus.START || qs == QuestStatus.REWARD)
					continue;
			}

			if (!QuestService.checkStartConditions(member, questId, false)) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100003, member.getName())); // You failed to share the quest with %0.
			} else {
				PacketSendUtility.sendPacket(member, new SM_QUEST_ACTION(questId, player.getObjectId(), member.isInAlliance()));
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100002, member.getName())); // You shared the quest with %0.
			}
		}
	}
}
