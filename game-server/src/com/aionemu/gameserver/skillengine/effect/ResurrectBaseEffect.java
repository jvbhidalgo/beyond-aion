package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.custom.pvpmap.PvpMapService;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResurrectBaseEffect")
public class ResurrectBaseEffect extends ResurrectEffect {

	@Override
	public void calculate(Effect effect) {
		calculate(effect, null, null);
	}

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	@Override
	public void startEffect(Effect effect) {
		Creature effected = effect.getEffected();

		if (effected instanceof Player) {
			Player player = (Player) effected;
			ActionObserver observer = new ActionObserver(ObserverType.DEATH) {

				@Override
				public void died(Creature lastAttacker) {
					if (!PvpMapService.getInstance().isOnPvPMap(player)) {
						player.getController().addTask(TaskId.TELEPORT, ThreadPoolManager.getInstance().schedule(() -> {
							PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT));
							if (player.isInInstance())
								PlayerReviveService.instanceRevive(player, skillId);
							else if (player.getKisk() != null)
								PlayerReviveService.kiskRevive(player, skillId);
							else
								PlayerReviveService.bindRevive(player, skillId);
						}, 2500));
					}
				}
			};
			player.getObserveController().attach(observer);
			effect.setActionObserver(observer, position);
		}
	}

	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);

		if (effect.getEffected() instanceof Player) {
			if (!effect.getEffected().isDead() && effect.getActionObserver(position) != null) {
				effect.getEffected().getObserveController().removeObserver(effect.getActionObserver(position));
			}
		}
	}
}
