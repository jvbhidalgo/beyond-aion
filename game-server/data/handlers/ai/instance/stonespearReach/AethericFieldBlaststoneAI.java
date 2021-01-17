package ai.instance.stonespearReach;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.NpcAI;
import com.aionemu.gameserver.ai.handler.MoveEventHandler;
import com.aionemu.gameserver.ai.manager.WalkManager;
import com.aionemu.gameserver.ai.poll.AIQuestion;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Created by Yeats on 20.02.2016.
 */
@AIName("atheric_field_blaststone")
public class AethericFieldBlaststoneAI extends NpcAI {

	public AethericFieldBlaststoneAI(Npc owner) {
		super(owner);
	}

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		if (getNpcId() == 856305) {
			getOwner().getSpawn().setWalkerId("301500000_clown_path");
			WalkManager.startWalking(this);
			getOwner().setState(CreatureState.WALK_MODE);
			PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getOwner().getObjectId()));
		}
	}

	@Override
	public void handleDied() {
		super.handleDied();
		getOwner().getController().delete();
	}

	@Override
	protected void handleMoveArrived() {
		super.handleMoveArrived();
		MoveEventHandler.onMoveArrived(this);
	}

	@Override
	public boolean ask(AIQuestion question) {
		switch (question) {
			case SHOULD_RESPAWN:
			case SHOULD_REWARD:
			case SHOULD_LOOT:
				return false;
			default:
				return super.ask(question);
		}
	}
}