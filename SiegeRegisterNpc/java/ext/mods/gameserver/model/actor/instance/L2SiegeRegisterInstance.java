/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ext.mods.gameserver.model.actor.instance;

import ext.mods.Config;
import ext.mods.gameserver.data.manager.CastleManager;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.template.NpcTemplate;
import ext.mods.gameserver.model.residence.castle.Castle;
import ext.mods.gameserver.network.serverpackets.ActionFailed;
import ext.mods.gameserver.network.serverpackets.NpcHtmlMessage;
import ext.mods.gameserver.network.serverpackets.SiegeInfo;

public class L2SiegeRegisterInstance extends Folk
{
    public L2SiegeRegisterInstance(int objectId, NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public synchronized void onBypassFeedback(Player player, String command)
    {
        if (command.startsWith("gludio_castle"))
            showSiegeInfoWindow(player, 1);
        else if (command.startsWith("dion_castle"))
            showSiegeInfoWindow(player, 2);
        else if (command.startsWith("giran_castle"))
            showSiegeInfoWindow(player, 3);
        else if (command.startsWith("oren_castle"))
            showSiegeInfoWindow(player, 4);
        else if (command.startsWith("aden_castle"))
            showSiegeInfoWindow(player, 5);
        else if (command.startsWith("innadril_castle"))
            showSiegeInfoWindow(player, 6);
        else if (command.startsWith("goddard_castle"))
            showSiegeInfoWindow(player, 7);
        else if (command.startsWith("rune_castle"))
            showSiegeInfoWindow(player, 8);
        else if (command.startsWith("schuttgart_castle"))
            showSiegeInfoWindow(player, 9);
        else
            super.onBypassFeedback(player, command);
    }

    @Override
    public void showChatWindow(Player player, int val)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("<html><title>Siege Register NPC</title><body><center>");
        sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
        sb.append("<font color=\"LEVEL\">NPC Register Siege</font><br>");
        sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_giran_castle\">Giran Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_aden_castle\">Aden Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_rune_castle\">Rune Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_goddard_castle\">Goddard Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_dion_castle\">Dion Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_schuttgart_castle\">Schuttgart Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_innadril_castle\">Innadril Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_oren_castle\">Oren Castle</a><br>");

        sb.append("<a action=\"bypass -h npc_");
        sb.append(getObjectId());
        sb.append("_gludio_castle\">Gludio Castle</a><br><br>");

        sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
        sb.append("<img src=\"L2UI.bbs_lineage2\" height=16 width=80><br>");
        sb.append("<font color=\"808080\">");
        sb.append(Config.DEFAULT_LOCALE);
        sb.append("</font>");

        sb.append("</center></body></html>");

        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setHtml(sb.toString());

        player.sendPacket(html);
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    public void showSiegeInfoWindow(Player player, int castleId)
    {
        final Castle castle = CastleManager.getInstance().getCastleById(castleId);

        if (castle != null)
        {
            player.sendPacket(new SiegeInfo(castle));
        }
    }
}