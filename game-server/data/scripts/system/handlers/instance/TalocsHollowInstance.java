package instance;

import static com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * @author xTz
 */
@InstanceID(300190000)
public class TalocsHollowInstance extends GeneralInstanceHandler {

	private List<Integer> movies = new ArrayList<>();

	@Override
	public void onEnterInstance(Player player) {
		addItems(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		player.getEffectController().removeEffect(10251);
		player.getEffectController().removeEffect(10252);
	}

	private void addItems(Player player) {
		QuestState qs1 = player.getQuestStateList().getQuestState(10032);
		QuestState qs2 = player.getQuestStateList().getQuestState(20032);
		if ((qs1 != null && qs1.getStatus() == QuestStatus.START) || (qs2 != null && qs2.getStatus() == QuestStatus.START))
			return;
		switch (player.getRace()) {
			case ELYOS:
				ItemService.addItem(player, 160001286, 1);
				ItemService.addItem(player, 164000099, 1);
				break;
			case ASMODIANS:
				ItemService.addItem(player, 160001287, 1);
				ItemService.addItem(player, 164000099, 1);
				break;
		}
	}

	private void removeItems(Player player) {
		int[] items = new int[] { 164000099, // Taloc's Tears
			164000137, // Shishir's Powerstone
			164000138, // Gellmar's Wardstone
			164000139, // Neith's Sleepstone
			185000088, // Shishir's Corrosive Fluid
			185000108 // Dorkin's Pocket Knife
		};
		Storage storage = player.getInventory();
		for (int item : items)
			storage.decreaseByItemId(item, storage.getItemCountByItemId(item));
	}

	@Override
	public void onDie(Npc npc) {
		switch (npc.getNpcId()) {
			case 215467:
				instance.setDoorState(48, true);
				instance.setDoorState(49, true);
				break;
			case 215457:
				Npc newNpc = getNpc(700633);
				if (newNpc != null) {
					newNpc.getController().delete();
				}
				break;
			case 700739:
				sendMsg(SM_SYSTEM_MESSAGE.STR_MSG_IDELIM_WIND_INFO());
				SpawnTemplate template = npc.getSpawn();
				spawn(281817, template.getX(), template.getY(), template.getZ(), template.getHeading(), 9);
				npc.getController().delete();
				break;
			case 215488:
				Player player = npc.getAggroList().getMostPlayerDamage();
				if (player != null) {
					PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, 10021, 437, 0));
				}
				Npc newNpc2 = getNpc(700740);
				if (newNpc2 != null) {
					SpawnTemplate template2 = newNpc2.getSpawn();
					spawn(700741, template2.getX(), template2.getY(), template2.getZ(), template2.getHeading(), 92);
					newNpc2.getController().delete();
				}
				spawn(799503, 548f, 811f, 1375f, (byte) 0);
				break;
		}
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700940:
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 20000, npc);
				NpcActions.delete(npc);
				break;
			case 700941:
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 30000, npc);
				NpcActions.delete(npc);
				break;
		}
	}

	private void sendMovie(Player player, int movie) {
		if (!movies.contains(movie)) {
			movies.add(movie);
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		player.getEffectController().removeEffect(10251);
		player.getEffectController().removeEffect(10252);
	}

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instance.setDoorState(48, true);
		instance.setDoorState(7, true);
		spawnRings();
	}

	private void spawnRings() {
		FlyRing f1 = new FlyRing(new FlyRingTemplate("TALOCS_1", mapId, new Point3D(253.85039, 649.23535, 1171.8772),
			new Point3D(253.85039, 649.23535, 1177.8772), new Point3D(262.84872, 649.4091, 1171.8772), 8), instanceId);
		f1.spawn();
		FlyRing f2 = new FlyRing(new FlyRingTemplate("TALOCS_2", mapId, new Point3D(592.32275, 844.056, 1295.0966),
			new Point3D(592.32275, 844.056, 1301.0966), new Point3D(595.2305, 835.5387, 1295.0966), 8), instanceId);
		f2.spawn();
	}

	@Override
	public boolean onPassFlyingRing(Player player, String flyingRing) {
		if (flyingRing.equals("TALOCS_1")) {
			sendMovie(player, 463);
		} else if (flyingRing.equals("TALOCS_2")) {
			sendMovie(player, 464);
		}
		return false;
	}

	@Override
	public boolean onDie(final Player player, Creature lastAttacker) {
		PacketSendUtility.sendPacket(player, new SM_DIE(player, 8));
		return true;
	}

	@Override
	public boolean onReviveEvent(Player player) {
		PlayerReviveService.revive(player, 25, 25, false, 0);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, STR_REBIRTH_MASSAGE_ME());
		TeleportService.teleportTo(player, mapId, instanceId, 202.26694f, 226.0532f, 1098.236f, (byte) 30);
		return true;
	}

}
