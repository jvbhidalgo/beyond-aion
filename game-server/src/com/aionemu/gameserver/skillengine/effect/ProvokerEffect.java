package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.skillengine.model.ProvokeTarget;
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * @author ATracer, kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProvokerEffect")
public class ProvokerEffect extends ShieldEffect {

	@XmlAttribute(name = "provoke_target")
	protected ProvokeTarget provokeTarget;
	@XmlAttribute(name = "skill_id")
	protected int skillId;

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	@Override
	public void startEffect(Effect effect) {
		Creature effector = effect.getEffector();
		ActionObserver observer;

		if (hitType == HitType.EVERYHIT) {
			observer = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature attacker, int attackSkillId) {
					if (shouldApply(effector, attacker, attackSkillId)) {
						if (effector instanceof Player player) {
							PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_SKILL_PROC_EFFECT_OCCURRED(DataManager.SKILL_DATA.getSkillTemplate(skillId).getL10n()));
						}
						SkillEngine.getInstance().applyEffectDirectly(skillId, effector, getProvokeTarget(effector, attacker));
					}
				}
			};
		} else {
			observer = new ActionObserver(ObserverType.ATTACK) {
				@Override
				public void attack(Creature attacked, int attackSkillId) {
					if (shouldApply(effector, attacked, attackSkillId)) {
						if (effector instanceof Player player) {
							PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_SKILL_PROC_EFFECT_OCCURRED(DataManager.SKILL_DATA.getSkillTemplate(skillId).getL10n()));
						}
						SkillEngine.getInstance().applyEffectDirectly(skillId, effector, getProvokeTarget(effector, attacked));
					}
				}
			};
		}

		effect.addObserver(effect.getEffected(), observer);
	}

	private boolean shouldApply(Creature effector, Creature target, int attackSkillId) {
		if (provokeTarget == ProvokeTarget.OPPONENT && target == effector)
			return false;
		if (radius > 0 && !PositionUtil.isInRange(effector, target, radius, false))
			return false;
		if (Rnd.chance() >= hitTypeProb)
			return false;
		return switch (hitType) {
			case PHHIT -> attackSkillId == 0 || DataManager.SKILL_DATA.getSkillTemplate(attackSkillId).getType() == SkillType.PHYSICAL;
			case MAHIT -> attackSkillId != 0 && DataManager.SKILL_DATA.getSkillTemplate(attackSkillId).getType() == SkillType.MAGICAL;
			case BACKATK -> PositionUtil.isBehind(effector, target);
			default -> true;
		};
	}

	private Creature getProvokeTarget(Creature effector, Creature target) {
		return switch (provokeTarget) {
			case ME -> effector;
			case OPPONENT -> target;
		};
	}

	@Override
	public void endEffect(Effect effect) {
	}
}
