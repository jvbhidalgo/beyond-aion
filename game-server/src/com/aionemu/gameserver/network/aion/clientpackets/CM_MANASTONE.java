package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.actions.EnchantItemAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.services.StigmaService;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * @author ATracer, Wakizashi
 */
public class CM_MANASTONE extends AionClientPacket {

	private int npcObjId;
	private int slotNum;
	private int actionType;
	private int targetFusedSlot;
	private int stoneUniqueId;
	private int targetItemUniqueId;
	private int supplementUniqueId;

	/**
	 * @param opcode
	 */
	public CM_MANASTONE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		actionType = readUC();
		targetFusedSlot = readUC();
		targetItemUniqueId = readD();
		switch (actionType) {
			case 1:
			case 2:
			case 4:
			case 8:
				stoneUniqueId = readD();
				supplementUniqueId = readD();
				break;
			case 3:
				slotNum = readUC();
				readC();
				readH();
				npcObjId = readD();
				break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		switch (actionType) {
			case 1: // enchant stone
			case 2: // add manastone
				Item stone = player.getInventory().getItemByObjId(stoneUniqueId);
				Item targetItem = player.getEquipment().getEquippedItemByObjId(targetItemUniqueId);
				if (targetItem == null)
					targetItem = player.getInventory().getItemByObjId(targetItemUniqueId);

				if (stone.getItemTemplate().isStigma() && targetItem.getItemTemplate().isStigma()) {
					StigmaService.chargeStigma(player, targetItem, stone);
				} else {
					EnchantItemAction action = new EnchantItemAction();
					if (action.canAct(player, stone, targetItem)) {
						Item supplement = player.getInventory().getItemByObjId(supplementUniqueId);
						if (supplement != null) {
							if (supplement.getItemId() / 100000 != 1661) // suppliment id check
								return;
						}
						action.act(player, stone, targetItem, supplement, targetFusedSlot);
					}
				}
				break;
			case 3: // remove manastone
				VisibleObject npc = player.getTarget();
				if (npc instanceof Npc && npc.getObjectId() == npcObjId && PositionUtil.isInTalkRange(player, (Npc) npc)) {
					if (targetFusedSlot == 1)
						ItemSocketService.removeManastone(player, targetItemUniqueId, slotNum);
					else
						ItemSocketService.removeFusionstone(player, targetItemUniqueId, slotNum);
				}
				break;
			case 4: // add godstone
				Item weaponItem = player.getInventory().getItemByObjId(targetItemUniqueId);
				if (weaponItem == null) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_PROC_CANNOT_GIVE_PROC_TO_EQUIPPED_ITEM());
					return;
				}
				ItemSocketService.socketGodstone(player, weaponItem, stoneUniqueId);
				break;
			case 8: // amplification
				EnchantService.amplifyItem(player, targetItemUniqueId, supplementUniqueId, stoneUniqueId);
				break;
		}
	}

}
