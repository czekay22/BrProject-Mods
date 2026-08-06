/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ext.mods.gameserver.model.actor.instance;

import ext.mods.commons.pool.ConnectionPool;
import ext.mods.gameserver.data.manager.HeroManager;
import ext.mods.gameserver.data.sql.ClanTable;
import ext.mods.gameserver.data.xml.PlayerData;
import ext.mods.gameserver.enums.Paperdoll;
import ext.mods.gameserver.enums.actors.Sex;
import ext.mods.gameserver.instancemanager.CharacterKillingManager;
import ext.mods.gameserver.model.World;
import ext.mods.gameserver.model.actor.Npc;
import ext.mods.gameserver.model.actor.container.player.Appearance;
import ext.mods.gameserver.model.actor.template.NpcTemplate;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.template.PlayerTemplate;
import ext.mods.gameserver.model.pledge.Clan;
import ext.mods.gameserver.network.serverpackets.AbstractNpcInfo;
import ext.mods.gameserver.network.serverpackets.ActionFailed;
import ext.mods.gameserver.network.serverpackets.NpcHtmlMessage;
import ext.mods.gameserver.custom.data.PolymorphData.Polymorph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * @author paytaly
 */
public class L2PcPolymorph extends Npc {
	private static final Logger _log = Logger.getLogger(L2PcPolymorph.class.getName());
	private Player _polymorphInfo;
	private Polymorph _fakePc;
	// Config nova passada para String
	private int _nameColor = 0xFFFFFF;
	private int _titleColor = 0xFFFF77;
	private String _visibleTitle = "";

	public L2PcPolymorph(int objectId, NpcTemplate template) {
		super(objectId, template);
		setInvul(true);
	}

	public boolean hasRandomAnimation() {
		return false;
	}
	public Player getPolymorphInfo() {
		return _polymorphInfo;
	}

	public void setPolymorphInfo(Player player)
	{
		if(player == null)
			return;

		_polymorphInfo = player;
		_fakePc = buildFakePc(player);
	}

	private Polymorph buildFakePc(Player player)
	{
		return new Polymorph(
				player.getName(),
				getVisibleTitle(),
				getNameColor(),
				getTitleColor(),
				player.getCollisionRadius(),
				player.getCollisionHeight(),
				player.getRace().ordinal(),
				player.getAppearance().getSex().ordinal(),
				(player.getClassIndex() == 0) ?
						player.getClassId().getId() :
						player.getBaseClass(),
				player.getAppearance().getHairStyle(),
				player.getAppearance().getHairColor(),
				player.getAppearance().getFace(),
				(byte)(player.isHero() ? 1 : 0),
				player.getEnchantEffect(),
				player.getInventory().getItemIdFrom(Paperdoll.RHAND),
				player.getInventory().getItemIdFrom(Paperdoll.LHAND),
				player.getInventory().getItemIdFrom(Paperdoll.CHEST),
				player.getInventory().getItemIdFrom(Paperdoll.LEGS),
				player.getInventory().getItemIdFrom(Paperdoll.GLOVES),
				player.getInventory().getItemIdFrom(Paperdoll.FEET),
				player.getInventory().getItemIdFrom(Paperdoll.HAIR),
				player.getInventory().getItemIdFrom(Paperdoll.HAIRALL),
				player.getClanId(),
				player.getClan() != null ? player.getClan().getCrestId() : 0,
				player.getClan() != null ? player.getClan().getAllyId() : 0,
				player.getClan() != null ? player.getClan().getAllyCrestId() : 0,
				player.getPledgeClass()
		);
	}

	public int getNameColor()
	{
		return _nameColor;
	}
	public void setNameColor(int color)
	{
		_nameColor = color;
	}
	public int getTitleColor()
	{
		return _titleColor;
	}
	public void setTitleColor(int color)
	{
		_titleColor = color;
	}
	public String getVisibleTitle() {
		return _visibleTitle;
	}
	public void setVisibleTitle(String title) {
		_visibleTitle = title == null ? "" : title;
	}
	@Override
	public void sendInfo(Player activeChar)
	{
		_log.fine("CKM SEND INFO");

		if (_fakePc == null && getPolymorphInfo() != null)
		{
			_log.fine("CKM REBUILD FAKE PC SEND INFO");
			_fakePc = buildFakePc(getPolymorphInfo());
		}

		activeChar.sendPacket(new AbstractNpcInfo.NpcInfo(this, activeChar));
	}

	@Override
	public Polymorph getFakePc()
	{
		return _fakePc;
	}

	public String getHtmlPath(int npcId, int val)
	{
		String file = Integer.toString(npcId);

		if (val > 0)
			file += "-" + val;

		return "html/polymorph/" + file + ".htm";
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		String filename = getHtmlPath(getNpcId(), val);

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		html.setFile(player.getLocale(), filename);

		html.replace("%objectId%", getObjectId());

		if (getPolymorphInfo() != null)
		{
			html.replace("%ownername%", getPolymorphInfo().getName());
		}
		else
		{
			html.replace("%ownername%", "Unknown");
		}
		// CKM PvP
		if (this instanceof L2TopPvPMonumentInstance)
		{
			html.replace("%kills%", String.valueOf(
					CharacterKillingManager.getInstance().getWinnerPvPKillsCount()
			));
		}
		// CKM PK
		if (this instanceof L2TopPKMonumentInstance)
		{
			html.replace("%kills%", String.valueOf(
					CharacterKillingManager.getInstance().getWinnerPKKillsCount()
			));
		}
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public static Player loadMonumentPlayer(int objectId)
	{
		// Se estiver online usa a instância existente
		Player player = World.getInstance().getPlayer(objectId);
		if ((player != null) && player.isOnline())
		{
			_log.fine("CKM PLAYER ONLINE: " + player.getName());
			return player;
		}

		try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(
					 "SELECT account_name, char_name, classid, base_class, sex, face, hairStyle, hairColor, clanid " +
							 "FROM characters WHERE obj_Id=?"))
		{
			ps.setInt(1, objectId);

			try (ResultSet rs = ps.executeQuery())
			{
				if (!rs.next())
				{
					_log.fine("CKM PLAYER NÃO ENCONTRADO.");
					return null;
				}

				PlayerTemplate template = PlayerData.getInstance().getTemplate(rs.getInt("classid"));

				Appearance app = new Appearance(
						rs.getByte("face"),
						rs.getByte("hairColor"),
						rs.getByte("hairStyle"),
						Sex.VALUES[rs.getInt("sex")]
				);

				player = new Player(
						objectId,
						template,
						rs.getString("account_name"),
						app
				);

				player.setName(rs.getString("char_name"));
				player.setBaseClass(rs.getInt("base_class"));
				player.setClassId(rs.getInt("classid"));

				Clan clan = ClanTable.getInstance().getClan(rs.getInt("clanid"));

				if (clan != null)
				{
					player.setClan(clan);
				}

				if (HeroManager.getInstance().isActiveHero(objectId))
				{
					player.setHero(true);
				}

				return player;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
}