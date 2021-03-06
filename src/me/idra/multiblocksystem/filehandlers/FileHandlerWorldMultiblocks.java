package me.idra.multiblocksystem.filehandlers;

import me.idra.multiblocksystem.bases.BaseWorldMultiblock;
import me.idra.multiblocksystem.helpers.ConstantPlaceholders;
import me.idra.multiblocksystem.helpers.Logger;
import me.idra.multiblocksystem.lists.ListAbstractMultiblocks;
import me.idra.multiblocksystem.lists.ListWorldMultiblocks;
import me.idra.multiblocksystem.managers.ManagerPlugin;
import me.idra.multiblocksystem.objects.AbstractMultiblock;
import me.mrCookieSlime.Slimefun.cscorelib2.blocks.BlockPosition;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class FileHandlerWorldMultiblocks {

	static final String RECIPE = "recipe";
	static final String POSITIONS = "positions";

	static FileConfiguration world_multiblock_config;
	static File world_multiblock_file;


	private FileHandlerWorldMultiblocks() {
		// Empty constructor
	}


	public static void loadFile() {

		// Load the file
		world_multiblock_file = new File(new File(ManagerPlugin.plugin.getDataFolder(), "data"), "WorldMultiblocks.yml");

		// Check the WorldMultiblocks.yml file exists
		if (!world_multiblock_file.exists()) {
			Logger.fileNotFoundError(world_multiblock_file);
			return;
		}

		// Load the config
		world_multiblock_config = YamlConfiguration.loadConfiguration(world_multiblock_file);
	}


	public static void saveFile() {

		try {
			world_multiblock_config.save(world_multiblock_file);
		} catch (IOException e) {
			Logger.log(
					Logger.getWarning("save-fail-player-data"),
					true);
		}
	}


	public static void deleteMultiblock(int ID) {

		// Reload the file, just in case
		loadFile();

		// Delete the section that starts with the ID
		world_multiblock_config.set(String.valueOf(ID), null);

		// Save the new config
		saveFile();
	}


	public static void saveMultiblocks() {
		// Reload the file, just in case
		loadFile();

		// For every multiblock
		for (BaseWorldMultiblock multiblock : ListWorldMultiblocks.multiblock_objects.values()) {

			// Create the config sections if necessary
			ConfigurationSection multiblock_section = world_multiblock_config.getConfigurationSection(String.valueOf(multiblock.ID));

			if (multiblock_section == null) {
				world_multiblock_config.createSection(String.valueOf(multiblock.ID));
				multiblock_section = world_multiblock_config.getConfigurationSection(String.valueOf(multiblock.ID));
			}

			// Create input position array
			List<Long> inputs = new ArrayList<> ();

			for (Inventory inv : multiblock.inputs) {
				inputs.add(BlockPosition.getAsLong(inv.getLocation()));
			}

			// Set basic variables
			assert multiblock_section != null;
			multiblock_section.set("Owner", multiblock.owner.toString());
			multiblock_section.set("World", multiblock.world.getName());
			multiblock_section.set("AbstractMultiblock", multiblock.abstract_multiblock.name_of_structure_block.toUpperCase());
			multiblock_section.set("FuelTicks", multiblock.fuel_ticks);
			multiblock_section.set("Inputs", inputs);

			// Check current recipe section exists
			ConfigurationSection recipe_section = multiblock_section.getConfigurationSection(RECIPE);

			if (recipe_section == null) {
				multiblock_section.createSection(RECIPE);
				recipe_section = multiblock_section.getConfigurationSection(RECIPE);
			}

			// Store recipe
			assert recipe_section != null;
			recipe_section.set("Index", multiblock.abstract_multiblock.recipes.indexOf(multiblock.active_recipe));
			if (multiblock.active_recipe != null) {
				recipe_section.set("Time", multiblock.active_recipe.crafting_time);
			} else {
				recipe_section.set("Time", 0);
			}

			// Store blocks
			List<Long> positions_as_longs = new ArrayList<> ();

			for (BlockPosition position : multiblock.blocks) {
				positions_as_longs.add(position.getPosition());
			}

			multiblock_section.set(POSITIONS, positions_as_longs);
		}

		// Save the new config
		saveFile();
	}


	public static void loadMultiblocks() { //TODO: Finish this
		// Reload the file, just in case
		loadFile();

		// For every multiblock
		for (String id_as_string : world_multiblock_config.getKeys(false)) {

			// Convert string to int
			int ID = Integer.parseInt(id_as_string);

			// Get the config section
			ConfigurationSection multiblock_section = world_multiblock_config.getConfigurationSection(id_as_string);

			// Get basic variables
			assert multiblock_section != null;
			String player_uuid = multiblock_section.getString("Owner");
			if (player_uuid == null) {
				Logger.log(
						Logger.getWarning("uuid-not-found")
						.replace(ConstantPlaceholders.arg1, id_as_string),
						true);
				continue;
			}
			UUID uuid = UUID.fromString(player_uuid);

			String name = multiblock_section.getString("AbstractMultiblock");
			int fuel_ticks = multiblock_section.getInt("FuelTicks");
			AbstractMultiblock abstract_multiblock = ListAbstractMultiblocks.structures.get(name);
			World world = Bukkit.getWorld(multiblock_section.getString("World"));
			List<Long> inputs = multiblock_section.getLongList("Inputs");

			// Check map sections exist
			List<BlockPosition> blocks = new ArrayList<> ();
			if (multiblock_section.getList(POSITIONS) == null) {
				multiblock_section.createSection(POSITIONS);
			}


			List<Long> blocks_as_longs = (List<Long>) multiblock_section.getList(POSITIONS); // TODO handle unchecked cast

			for (Long position : blocks_as_longs) {
				blocks.add(new BlockPosition(world, position));
			}

			// Get current recipe information
			ConfigurationSection recipe_section = multiblock_section.getConfigurationSection(RECIPE);
			if (recipe_section == null) {
				Logger.log(
						Logger.getWarning("recipe-section-not-found")
								.replace(ConstantPlaceholders.arg1, id_as_string),
						true);
				continue;
			}

			// Store recipe
			int recipe_index = recipe_section.getInt("Index");
			int recipe_time = recipe_section.getInt("Time");

			// Generate WorldMultiblock
			ListWorldMultiblocks.instantiateWorldMultiblock(abstract_multiblock, blocks, uuid, ID, fuel_ticks, recipe_index, recipe_time);

			// Set inputs on new multiblock
			for (Long position : inputs) {
				BaseWorldMultiblock new_multiblock = ListWorldMultiblocks.multiblock_objects.get(ID);
				new_multiblock.flipIO(((Container) (new BlockPosition(world, position).getBlock().getState())).getInventory());
				ListWorldMultiblocks.multiblock_objects.put(ID, new_multiblock);
			}
		}
	}
}
