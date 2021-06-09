package me.idra.multiblocksystem.commands;



import java.util.List;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BlockIterator;

import me.idra.multiblocksystem.bases.BaseCommand;
import me.idra.multiblocksystem.bases.BaseWorldMultiblock;
import me.idra.multiblocksystem.filehandlers.FileHandlerPlayerData;
import me.idra.multiblocksystem.helpers.MessageHandler;
import me.idra.multiblocksystem.lists.ListWorldMultiblocks;
import me.mrCookieSlime.Slimefun.cscorelib2.blocks.BlockPosition;



public class CommandTag extends BaseCommand{
	

	public CommandTag() {
		super();
		
		name = new String[] {"tag"};
		description = ChatColor.DARK_AQUA + "Change the tags of blocks on an existing multiblock";
		arguments = new String[] {"tag"};
		hidden = false;
		console = false;
		
		addPermission();
		addName();
	}
	
	@Override
	public boolean commandFunction(CommandSender sender, Command command, String label, String[] args) {
		
		// Find the target block
		Player player = (Player) sender;
		BlockIterator block_iterator = new BlockIterator(player, 5);
		Block target_block = null;
				
		while (block_iterator.hasNext()) {
					
			Block next_block = block_iterator.next(); 
					
			if (next_block.getType() != Material.AIR) {
				target_block = next_block;
				break;
			}
		}
		
		// Does the target block exist?
		if (target_block == null || target_block.getType() == Material.AIR) {
			MessageHandler.send(player, 
					MessageHandler.getError("invalid-block"));
			return false;
		}
		
		BlockPosition position = new BlockPosition(target_block.getLocation());
		
		// Get the multiblock the player is trying to interface with
		BaseWorldMultiblock multiblock = ListWorldMultiblocks.getMultiblockFromLocation(target_block.getLocation());
		
		// Is the target block part of a multiblock
		if (multiblock == null) {
				MessageHandler.send(player, 
						MessageHandler.getError("not-part-of-multiblock"));
				return false;
		}
		
		// Does the sender have access to said multiblock?
		if (!(multiblock.owner == player.getUniqueId() || Arrays.asList(FileHandlerPlayerData.getTrustedPlayers(multiblock.owner)).contains(player))) {
			MessageHandler.send(player, 
					MessageHandler.getError("cannot-modify-multiblock")
					.replace("%player", player.getName()));
			return false;
		}
		
		// Is the tag valid?
		if (!multiblock.abstract_multiblock.tags.contains(args[1])) {
			MessageHandler.send(player, 
					MessageHandler.getError("invalid-tag")
					.replace("%multiblock%", multiblock.abstract_multiblock.name)
					.replace("%tag%", args[1]));
			return false;
		}
		
		// Is the tag an inventory tag?
		boolean is_inventory_tag = false;
		
		if (BaseWorldMultiblock.isInventoryTag(args[1])) {
			is_inventory_tag = true;
		}
		
		// If so, try to get the attached inventory and add it to the multiblock's map
		if (is_inventory_tag) {
			
			// Is the target block valid?
			if (target_block.getType() != Material.CHEST && target_block.getType() != Material.BARREL) {
				MessageHandler.send(player, 
						MessageHandler.getError("inventory-tag-invalid-block"));
				return false;
			}
			
			// Get inventory of the block		
			Inventory inventory = ((Chest) target_block.getState()).getBlockInventory();
			
			// Remove existing inventory/position tags from the block
			for (String tag : multiblock.tags_inventory.keySet()) {
				multiblock.tags_inventory.get(tag).remove(inventory);
			}
				
			for (String tag : multiblock.tags_position.keySet()) {
				multiblock.tags_position.get(tag).remove(position);
			}
						
			// Add the inventory to our array
			List<Inventory> new_array = multiblock.tags_inventory.get(args[1]);
			new_array.add(inventory);
			multiblock.tags_inventory.put(args[1], new_array);
			
			// Successful execution, don't try and add the position as well
			MessageHandler.send(player, 
					MessageHandler.getSuccess("tag-set"));
			
			return true;
		}
		
		// Remove existing position tags from the block
		for (String tag : multiblock.tags_position.keySet()) {
			multiblock.tags_position.get(tag).remove(position);
		}
		
		// Position tag
		List<BlockPosition> new_array = multiblock.tags_position.get(args[1]);
		new_array.add(position);
		multiblock.tags_position.put(args[1], new_array);
		
		// Successful execution
		MessageHandler.send(player, 
				MessageHandler.getSuccess("tag-set"));
		
		return true;
	}
}
