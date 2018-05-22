package com.aionemu.gameserver.services.item;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.templates.item.actions.TuningAction;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
import com.aionemu.gameserver.model.templates.item.purification.ItemPurificationTemplate;
import com.aionemu.gameserver.model.templates.item.purification.PurificationResultItem;
import com.aionemu.gameserver.model.templates.item.purification.SubMaterialItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * @author Ranastic
 */
public class ItemPurificationService {

	private static final Logger log = LoggerFactory.getLogger(ItemPurificationService.class);

	public static boolean checkItemUpgrade(Player player, Item baseItem, int resultItemId) {
		ItemPurificationTemplate itemPurificationTemplate = DataManager.ITEM_PURIFICATION_DATA.getItemPurificationTemplate(baseItem.getItemId());
		if (itemPurificationTemplate == null) {
			log.warn(resultItemId + " item's purification template is null");
			return false;
		}

		Map<Integer, PurificationResultItem> resultItemMap = DataManager.ITEM_PURIFICATION_DATA.getResultItemMap(baseItem.getItemId());
		PurificationResultItem resultItem = resultItemMap.get(resultItemId);
		if (resultItem == null) {
			AuditLogger.log(player, "tried to upgrade baseItem " + baseItem.getItemId() + " to invalid resultItem " + resultItemId);
			return false;
		}

		if (baseItem.getEnchantLevel() < resultItem.getCheckEnchantCount()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT(baseItem.getL10n()));
			return false;
		}

		for (SubMaterialItem sub : resultItem.getRequiredMaterials().getSubMaterialItems()) {
			if (player.getInventory().getItemCountByItemId(sub.getId()) < sub.getCount()) {
				// sub material is not enough
				return false;
			}
		}

		if (resultItem.getRequiredAbyssPoints() != null) {
			if (player.getAbyssRank().getAp() < resultItem.getRequiredAbyssPoints().getCount()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_NEED_AP());
				return false;
			}
		}

		if (resultItem.getRequiredKinah() != null) {
			if (player.getInventory().getKinah() < resultItem.getRequiredKinah().getCount()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_NEED_QINA());
				return false;
			}
		}
		String resultItemL10n = DataManager.ITEM_DATA.getItemTemplate(resultItemId).getL10n();
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS(baseItem.getL10n(), resultItemL10n));
		return true;
	}

	public static boolean decreaseMaterial(Player player, Item baseItem, int resultItemId) {
		Map<Integer, PurificationResultItem> resultItemMap = DataManager.ITEM_PURIFICATION_DATA.getResultItemMap(baseItem.getItemId());

		PurificationResultItem resultItem = resultItemMap.get(resultItemId);

		for (SubMaterialItem item : resultItem.getRequiredMaterials().getSubMaterialItems()) {
			if (!player.getInventory().decreaseByItemId(item.getId(), item.getCount())) {
				AuditLogger.log(player, "tried item upgrade without sub material");
				return false;
			}
		}

		if (resultItem.getRequiredAbyssPoints() != null)
			AbyssPointsService.setAp(player, -resultItem.getRequiredAbyssPoints().getCount());

		if (resultItem.getRequiredKinah() != null)
			player.getInventory().decreaseKinah(-resultItem.getRequiredKinah().getCount());

		player.getInventory().decreaseByObjectId(baseItem.getObjectId(), 1);

		return true;
	}

	public static void upgradeItem(Player player, Item sourceItem, int targetItemId) {
		Item newItem = ItemFactory.newItem(targetItemId, 1);
		newItem.setOptionalSockets(sourceItem.getOptionalSockets());
		newItem.setItemCreator(sourceItem.getItemCreator());
		newItem.setEnchantLevel(sourceItem.getEnchantLevel() - 5);
		newItem.setEnchantBonus(sourceItem.getEnchantBonus());
		newItem.setAmplified(sourceItem.isAmplified() && newItem.getEnchantLevel() >= newItem.getMaxEnchantLevel());
		if (newItem.isAmplified() && newItem.getEnchantLevel() >= 20) {
			newItem.setBuffSkill(sourceItem.getBuffSkill());
		}
		if (sourceItem.hasFusionedItem()) {
			newItem.setFusionedItem(sourceItem.getFusionedItemTemplate(), sourceItem.getFusionedItemBonusStatsId(),
				sourceItem.getFusionedItemOptionalSockets());
		}
		if (sourceItem.hasManaStones()) {
			for (ManaStone manaStone : sourceItem.getItemStones())
				ItemSocketService.addManaStone(newItem, manaStone.getItemId());
		}
		if (sourceItem.hasFusionStones()) {
			for (ManaStone manaStone : sourceItem.getFusionStones())
				ItemSocketService.addFusionStone(newItem, manaStone.getItemId());
		}
		if (sourceItem.getGodStone() != null)
			newItem.addGodStone(sourceItem.getGodStone().getItemId(), sourceItem.getGodStone().getActivatedCount());
		if (sourceItem.getTempering() > 0)
			newItem.setTempering(sourceItem.getTempering());
		if (sourceItem.isSoulBound())
			newItem.setSoulBound(true);
		if (sourceItem.getBonusStatsId() > 0) {
			int statBonusId = sourceItem.getBonusStatsId();
			if (!DataManager.ITEM_RANDOM_BONUSES.areBonusSetsEqual(StatBonusType.INVENTORY, sourceItem.getItemTemplate().getStatBonusSetId(),
				newItem.getItemTemplate().getStatBonusSetId())) {
				statBonusId = TuningAction.getRandomStatBonusIdFor(newItem);
			}
			newItem.setBonusStats(statBonusId, true);
		}
		newItem.setTuneCount(sourceItem.getTuneCount());
		newItem.setItemColor(sourceItem.getItemColor());
		player.getInventory().add(newItem);
	}

}
