package quest.crafting;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;

/**
 * @author Gigi, Pad
 */
public class _2972ExpertWeaponsmithsTest extends AbstractQuestHandler {

	public _2972ExpertWeaponsmithsTest() {
		super(2972);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204104).addOnQuestStart(questId);
		qe.registerQuestNpc(204104).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int dialogActionId = env.getDialogActionId();
		int targetId = env.getTargetId();

		if (dialogActionId == QUEST_SELECT && !CraftSkillUpdateService.getInstance().canLearnMoreExpertCraftingSkill(player)) {
			return sendQuestSelectionDialog(env);
		}

		if (qs == null || qs.isStartable()) {
			if (targetId == 204104) {
				if (dialogActionId == QUEST_SELECT)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204104:
					switch (dialogActionId) {
						case QUEST_SELECT: {
							long itemCount1 = player.getInventory().getItemCountByItemId(182207964);
							if (itemCount1 > 0) {
								removeQuestItem(env, 182207964, 1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 2375);
							} else
								return sendQuestDialog(env, 2716);
						}
					}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204104) {
				if (dialogActionId == CHECK_USER_HAS_QUEST_ITEM)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
