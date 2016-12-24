package quest.pandaemonium;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Altaress
 * @reworked vlog
 */
public class _2953DeliveringSupplyRequest extends QuestHandler {

	private final static int questId = 2953;

	public _2953DeliveringSupplyRequest() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204191).addOnQuestStart(questId);
		qe.registerQuestNpc(204191).addOnTalkEvent(questId);
		qe.registerQuestNpc(204071).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		DialogAction dialog = env.getDialog();

		if (qs == null || qs.isStartable()) {
			if (targetId == 204191) { // Doman
				if (dialog == DialogAction.QUEST_SELECT) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env, 182207039, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 204071) { // Veldina
				if (dialog == DialogAction.QUEST_SELECT) {
					return sendQuestDialog(env, 1352);
				} else if (dialog == DialogAction.SETPRO1) {
					return defaultCloseDialog(env, 0, 1); // 1
				}
			} else if (targetId == 204191) { // Doman
				if (dialog == DialogAction.QUEST_SELECT) {
					if (var == 1) {
						return sendQuestDialog(env, 2375);
					}
				} else if (dialog == DialogAction.SELECT_QUEST_REWARD) {
					changeQuestStep(env, 1, 1, true);
					return sendQuestDialog(env, 5);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204191) { // Doman
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
