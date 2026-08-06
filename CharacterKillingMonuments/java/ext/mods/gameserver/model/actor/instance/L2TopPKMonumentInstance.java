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

import ext.mods.gameserver.instancemanager.CharacterKillingManager;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.template.NpcTemplate; // Verifique o pacote exato do NpcTemplate no seu core
import ext.mods.Config; // Ajuste para o pacote correto do seu arquivo Config

/**
 * Instância customizada para o Monumento de Top PK.
 * Mantida a compatibilidade com o ciclo de vida clássico (deleteMe).
 */
public class L2TopPKMonumentInstance extends L2PcPolymorph
{
	// Construtor alinhado com o padrão de criação de instâncias do Brproject
	public L2TopPKMonumentInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(Player player, int npcId, int val)
	{
		String filename = "";

		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;

		return "html/polymorph/" + filename + ".htm";
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if (Config.CKM_ENABLED)
		{
			CharacterKillingManager.getInstance().addPKMorphListener(this);
		}
	}

	@Override
	public void deleteMe()
	{
		// Primeiro executa a lógica de remoção do listener do mod
		if (Config.CKM_ENABLED)
		{
			CharacterKillingManager.getInstance().removePKMorphListener(this);
		}
		// Depois chama a superclasse para remover o NPC do mundo de forma segura
		super.deleteMe();
	}
}