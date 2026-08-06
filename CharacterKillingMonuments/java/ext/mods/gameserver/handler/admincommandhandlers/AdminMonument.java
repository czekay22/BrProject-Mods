package ext.mods.gameserver.handler.admincommandhandlers;

import ext.mods.gameserver.handler.IAdminCommandHandler;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.instancemanager.CharacterKillingManager; // Importa o seu gerenciador real

/**
 * Comando de GM para forçar a atualização dos Monumentos de PvP/PK.
 * Baseado no mod real CharacterKillingManager.
 */
public class AdminMonument implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS = { "admin_updatemonument" };

    @Override
    public void useAdminCommand(String command, Player activeChar)
    {
        if (command.startsWith("admin_updatemonument"))
        {
            activeChar.sendMessage("Recalculando monumentos...");
            CharacterKillingManager.getInstance().refreshMonuments();
            activeChar.sendMessage("Monumentos atualizados.");
        }
    }

    @Override
    public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
}