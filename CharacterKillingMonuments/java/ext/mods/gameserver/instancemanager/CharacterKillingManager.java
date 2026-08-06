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
package ext.mods.gameserver.instancemanager;

import ext.mods.Config;
import ext.mods.commons.pool.ConnectionPool;
import ext.mods.commons.pool.ThreadPool;
import ext.mods.gameserver.model.World;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.network.serverpackets.AbstractNpcInfo;
import ext.mods.gameserver.network.serverpackets.DeleteObject;
import ext.mods.gameserver.model.actor.instance.L2PcPolymorph;
import ext.mods.gameserver.model.actor.instance.L2TopPKMonumentInstance;
import ext.mods.gameserver.model.actor.instance.L2TopPvPMonumentInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author paytaly
 */
public final class CharacterKillingManager {
    private static final Logger _log = Logger.getLogger(CharacterKillingManager.class.getName());

    private long _cycleStart = 0L;
    private int _winnerPvPKills;
    private int _winnerPvPKillsCount;
    private int _winnerPKKills;
    private int _winnerPKKillsCount;

    private volatile Player _winnerPvPKillsInfo;
    private volatile Player _winnerPKKillsInfo;

    private ScheduledFuture<?> _scheduledKillingCycleTask = null;

    private final List<L2PcPolymorph> pvpMorphListeners = new CopyOnWriteArrayList<>();
    private final List<L2PcPolymorph> pkMorphListeners = new CopyOnWriteArrayList<>();

    private CharacterKillingManager()
    {
    }

    public synchronized void init()
    {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement(
                     "SELECT cycle, cycle_start, winner_pvpkills, winner_pvpkills_count, winner_pkkills, winner_pkkills_count FROM character_kills_info ORDER BY cycle_start DESC LIMIT 1");
             ResultSet rs = st.executeQuery())
        {
            if (rs.next())
            {
                _log.info("CKM: Carregando último ciclo salvo.");
                _cycleStart = rs.getLong("cycle_start");

                _winnerPvPKills = rs.getInt("winner_pvpkills");
                _winnerPvPKillsCount = rs.getInt("winner_pvpkills_count");

                _winnerPKKills = rs.getInt("winner_pkkills");
                _winnerPKKillsCount = rs.getInt("winner_pkkills_count");

                _log.info("Vencedor PvP ID: " + _winnerPvPKills);
                _log.info("Vencedor PK ID: " + _winnerPKKills);
            }
        }
        catch(Exception e)
        {
            _log.log(Level.WARNING, "Could not load characters killing cycle: " + e.getMessage(), e);
        }

        broadcastMorphUpdate();

        if (_scheduledKillingCycleTask != null) _scheduledKillingCycleTask.cancel(true);

        long millisToNextCycle = (_cycleStart + Config.CKM_CYCLE_LENGTH) - System.currentTimeMillis();

        if (millisToNextCycle <= 0) millisToNextCycle = Config.CKM_CYCLE_LENGTH;

        _scheduledKillingCycleTask = ThreadPool.schedule(new CharacterKillingCycleTask(), millisToNextCycle);
    }

    public synchronized void refreshMonuments()
    {
        computateCyclePvPWinner();
        computateCyclePKWinner();
        broadcastMorphUpdate();
    }

    public synchronized void newKillingCycle()
    {
        _cycleStart = System.currentTimeMillis();
        computateCyclePvPWinner();
        computateCyclePKWinner();
        _log.info("--------------------------------------=[ Character Monument ]");
        _log.info("CKM: Novo ciclo atualizado.");
        _log.info("Novo vencedor PvP ID: " + _winnerPvPKills + " | Kills: " + _winnerPvPKillsCount);

        _log.info("Novo vencedor PK ID: " + _winnerPKKills + " | Kills: " + _winnerPKKillsCount);

        refreshKillingSnapshot();
        saveCycle();
        broadcastMorphUpdate();

        if (_scheduledKillingCycleTask != null)
        {
            _scheduledKillingCycleTask.cancel(false);
        }

        _scheduledKillingCycleTask = ThreadPool.schedule(new CharacterKillingCycleTask(), Config.CKM_CYCLE_LENGTH);
    }

    private void saveCycle()
    {
        try(Connection con = ConnectionPool.getConnection();
            PreparedStatement st = con.prepareStatement(
                    "INSERT INTO character_kills_info " +
                            "(cycle_start, winner_pvpkills, winner_pvpkills_count, winner_pkkills, winner_pkkills_count) " +
                            "VALUES (?, ?, ?, ?, ?)"))
        {
            st.setLong(1,_cycleStart);
            st.setInt(2,_winnerPvPKills);
            st.setInt(3,_winnerPvPKillsCount);
            st.setInt(4,_winnerPKKills);
            st.setInt(5,_winnerPKKillsCount);

            st.execute();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void computateCyclePvPWinner() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement(
                     "SELECT obj_Id, char_name, pvpkills FROM characters WHERE accesslevel = 0 ORDER BY pvpkills DESC LIMIT 1");
             ResultSet rs = st.executeQuery()) {
            if (rs.next())
            {
                _winnerPvPKills = rs.getInt("obj_Id");
                _winnerPvPKillsCount = rs.getInt("pvpkills");
                _winnerPvPKillsInfo = null;
            }
            else
            {
                _log.warning("CKM: Nenhum registro PvP encontrado no banco de dados.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void computateCyclePKWinner() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement st = con.prepareStatement(
                     "SELECT obj_Id, char_name, pkkills FROM characters WHERE accesslevel = 0 ORDER BY pkkills DESC LIMIT 1");
             ResultSet rs = st.executeQuery()) {
            if (rs.next())
            {
                _winnerPKKills = rs.getInt("obj_Id");
                _winnerPKKillsCount = rs.getInt("pkkills");
                _winnerPKKillsInfo = null;
            }
            else
            {
                _log.warning("CKM: Nenhum registro PK encontrado no banco de dados.");
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "Erro ao computar vencedor de PK: " + e.getMessage(), e);
        }
    }

    private static void refreshKillingSnapshot() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement stTruncate = con.prepareStatement("TRUNCATE TABLE character_kills_snapshot");
             PreparedStatement stRefresh = con.prepareStatement("INSERT INTO character_kills_snapshot (charId, pvpkills, pkkills) SELECT obj_Id, pvpkills, pkkills FROM characters WHERE (pvpkills > 0 OR pkkills > 0) AND accesslevel = 0")) {
            stTruncate.executeUpdate();
            stRefresh.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "Could not refresh characters killing snapshot: " + e.getMessage(), e);
        }
    }

    public void broadcastMorphUpdate() {
        final Player winnerPvPKillsInfo = getWinnerPvPKillsInfo();
        for (L2PcPolymorph npc : pvpMorphListeners) {
            broadcastPvPMorphUpdate(npc, winnerPvPKillsInfo);
        }

        final Player winnerPKKillsInfo = getWinnerPKKillsInfo();
        for (L2PcPolymorph npc : pkMorphListeners) {
            broadcastPKMorphUpdate(npc, winnerPKKillsInfo);
        }
    }

    /**
     * Conferir essas classes broadcastPvPMorphUpdate, broadcastPKMorphUpdate
     */
    private void broadcastPvPMorphUpdate(L2PcPolymorph npc, Player winnerPvPKillsInfo)
    {
        if (winnerPvPKillsInfo == null)
        {
            _log.warning("CKM: Nenhum vencedor PvP encontrado.");
            return;
        }

        npc.setVisibleTitle(Config.CKM_PVP_NPC_TITLE.replaceAll("%kills%", String.valueOf(_winnerPvPKillsCount)));
        npc.setTitleColor(Config.CKM_PVP_NPC_TITLE_COLOR);
        npc.setNameColor(Config.CKM_PVP_NPC_NAME_COLOR);
        npc.setPolymorphInfo(winnerPvPKillsInfo);

        for (Player player : World.getInstance().getPlayers())
        {
            if (player.isInStrictRadius(npc, 300))
            {
                player.sendPacket(new DeleteObject(npc));
                ThreadPool.schedule(() ->
                {
                    player.sendPacket(new AbstractNpcInfo.NpcInfo(npc, player));
                }, 1000);
            }
        }
    }

    private void broadcastPKMorphUpdate(L2PcPolymorph npc, Player winnerPKKillsInfo)
    {
        if (winnerPKKillsInfo == null)
        {
            _log.warning("CKM: Nenhum vencedor PK encontrado.");
            return;
        }

        npc.setVisibleTitle(Config.CKM_PK_NPC_TITLE.replaceAll("%kills%", String.valueOf(_winnerPKKillsCount)));
        npc.setTitleColor(Config.CKM_PK_NPC_TITLE_COLOR);
        npc.setNameColor(Config.CKM_PK_NPC_NAME_COLOR);
        npc.setPolymorphInfo(winnerPKKillsInfo);

        for (Player player : World.getInstance().getPlayers())
        {
            if (player.isInStrictRadius(npc, 300))
            {
                player.sendPacket(new DeleteObject(npc));
                ThreadPool.schedule(() ->
                {
                    player.sendPacket(new AbstractNpcInfo.NpcInfo(npc, player));
                }, 1000);
            }
        }
    }

    public boolean addPvPMorphListener(L2TopPvPMonumentInstance npc)
    {
        if (npc == null)
            return false;

        if (!pvpMorphListeners.contains(npc))
        {
            pvpMorphListeners.add(npc);
        }

        if (npc.getPolymorphInfo() == null)
        {
            broadcastPvPMorphUpdate(npc, getWinnerPvPKillsInfo());
        }
        return true;
    }

    public void removePvPMorphListener(L2TopPvPMonumentInstance npc) {
        pvpMorphListeners.remove(npc);
    }

    public boolean addPKMorphListener(L2TopPKMonumentInstance npc)
    {
        if (npc == null)
            return false;

        if (!pkMorphListeners.contains(npc))
        {
            pkMorphListeners.add(npc);
        }

        if (npc.getPolymorphInfo() == null)
        {
            broadcastPKMorphUpdate(npc, getWinnerPKKillsInfo());
        }
        return true;
    }

    public boolean removePKMorphListener(L2TopPKMonumentInstance npc) {
        return pkMorphListeners.remove(npc);
    }

    // Caso L2TopPKMonumentInstance não exista no core, mude para Npc ou para o nome exato da classe do seu NPC customizado
    public boolean removePKMorphListener(ext.mods.gameserver.model.actor.Npc npc) {
        return pkMorphListeners.remove(npc);
    }

    private Player getWinnerPvPKillsInfo() {

        if (_winnerPvPKills != 0 && _winnerPvPKillsInfo == null) {
            _winnerPvPKillsInfo = (Player) L2PcPolymorph.loadMonumentPlayer(_winnerPvPKills);
            _log.fine("CKM: Carregando dados do vencedor PvP ID: " + _winnerPvPKills);
        }
        return _winnerPvPKillsInfo;
    }

    private Player getWinnerPKKillsInfo() {

        if (_winnerPKKills != 0 && _winnerPKKillsInfo == null) {
            _winnerPKKillsInfo = (Player) L2PcPolymorph.loadMonumentPlayer(_winnerPKKills);
            _log.fine("CKM: Carregando dados do vencedor PvP ID: " + _winnerPKKills);
        }
        return _winnerPKKillsInfo;
    }

    public int getWinnerPKKillsCount()
    {
        return _winnerPKKillsCount;
    }

    public int getWinnerPvPKillsCount()
    {
        return _winnerPvPKillsCount;
    }


    protected static class CharacterKillingCycleTask implements Runnable {
        @Override
        public void run() {
            CharacterKillingManager.getInstance().newKillingCycle();
        }
    }

    public static CharacterKillingManager getInstance() {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder {
        protected static final CharacterKillingManager _instance = new CharacterKillingManager();
    }
}