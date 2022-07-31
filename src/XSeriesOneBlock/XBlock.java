package XSeriesOneBlock;

import org.bukkit.Bukkit;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Crypto Morin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.*;

import java.util.*;

/**
 * <b>XBlock</b> - MaterialData/BlockData Support<br>
 * BlockState (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/BlockState.html
 * BlockData (New): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/data/BlockData.html
 * MaterialData (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/material/MaterialData.html
 * <p>
 * All the parameters are non-null except the ones marked as nullable.
 * This class doesn't and shouldn't support materials that are {@link Material#isLegacy()}.
 *
 * @author Crypto Morin
 * @version 2.2.0
 * @see Block
 * @see BlockState
 * @see MaterialData
 * @see XMaterial
 */
@SuppressWarnings("deprecation")
public final class XBlock {
    public static final Set<XMaterial> CROPS = Collections.unmodifiableSet(EnumSet.of(
            XMaterial.CARROT, XMaterial.POTATO, XMaterial.NETHER_WART, XMaterial.WHEAT_SEEDS, XMaterial.PUMPKIN_SEEDS,
            XMaterial.MELON_SEEDS, XMaterial.BEETROOT_SEEDS, XMaterial.SUGAR_CANE, XMaterial.BAMBOO_SAPLING, XMaterial.CHORUS_PLANT,
            XMaterial.KELP, XMaterial.SEA_PICKLE, XMaterial.BROWN_MUSHROOM, XMaterial.RED_MUSHROOM
    ));
    private static final boolean ISFLAT = XMaterial.supports(13);
    private static final Map<XMaterial, XMaterial> ITEM_TO_BLOCK = new EnumMap<>(XMaterial.class);

    static {
        ITEM_TO_BLOCK.put(XMaterial.MELON_SLICE, XMaterial.MELON_STEM);
        ITEM_TO_BLOCK.put(XMaterial.MELON_SEEDS, XMaterial.MELON_STEM);

        ITEM_TO_BLOCK.put(XMaterial.CARROT_ON_A_STICK, XMaterial.CARROTS);
        ITEM_TO_BLOCK.put(XMaterial.GOLDEN_CARROT, XMaterial.CARROTS);
        ITEM_TO_BLOCK.put(XMaterial.CARROT, XMaterial.CARROTS);

        ITEM_TO_BLOCK.put(XMaterial.POTATO, XMaterial.POTATOES);
        ITEM_TO_BLOCK.put(XMaterial.BAKED_POTATO, XMaterial.POTATOES);
        ITEM_TO_BLOCK.put(XMaterial.POISONOUS_POTATO, XMaterial.POTATOES);

        ITEM_TO_BLOCK.put(XMaterial.PUMPKIN_SEEDS, XMaterial.PUMPKIN_STEM);
        ITEM_TO_BLOCK.put(XMaterial.PUMPKIN_PIE, XMaterial.PUMPKIN);
    }

    public static boolean setCustomType(Location loc, String command) {
    	Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
    			String.format(command, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    	return true;
    }
    
    public static boolean setType(Block block, Object material_) {
    	return setType(block, material_, true);
    }
    
    public static boolean setType(Block block, Object material_, boolean physics) {
    	XMaterial material = null;
    	if (material_.getClass() == XMaterial.class)
    		material = (XMaterial)material_;
    	else
    		return setCustomType(block.getLocation(), (String)material_);
    	
        XMaterial smartConversion = ITEM_TO_BLOCK.get(material);
        if (smartConversion != null) material = smartConversion;
        if (material.parseMaterial() == null) return false;

        block.setType(material.parseMaterial());
        if (ISFLAT) return false;

        String parsedName = material.parseMaterial().name();
        if (parsedName.endsWith("_ITEM")) {
            String blockName = parsedName.substring(0, parsedName.length() - "_ITEM".length());
            Material blockMaterial = Objects.requireNonNull(Material.getMaterial(blockName),
                    () -> "Could not find block material for item '" + parsedName + "' as '" + blockName + '\'');
            block.setType(blockMaterial);
        } else if (parsedName.contains("CAKE")) {
            Material blockMaterial = Material.getMaterial("CAKE_BLOCK");
            block.setType(blockMaterial);
        }

        LegacyMaterial legacyMaterial = LegacyMaterial.getMaterial(parsedName);
        if (legacyMaterial == LegacyMaterial.BANNER) block.setType(LegacyMaterial.STANDING_BANNER.material);
        LegacyMaterial.Handling handling = legacyMaterial == null ? null : legacyMaterial.handling;

        BlockState state = block.getState();
        boolean update = false;

        if (handling == LegacyMaterial.Handling.COLORABLE) {
            state.setRawData(material.getData());
            update = true;
        } else if (handling == LegacyMaterial.Handling.WOOD_SPECIES) {
            // Wood doesn't exist in 1.8
            // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/material/Wood.java?until=7d83cba0f2575112577ed7a091ed8a193bfc261a&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fmaterial%2FWood.java
            // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/TreeSpecies.java

            String name = material.name();
            int firstIndicator = name.indexOf('_');
            if (firstIndicator < 0) return false;
            String woodType = name.substring(0, firstIndicator);

            TreeSpecies species;
            switch (woodType) {
                case "OAK":
                    species = TreeSpecies.GENERIC;
                    break;
                case "DARK":
                    species = TreeSpecies.DARK_OAK;
                    break;
                case "SPRUCE":
                    species = TreeSpecies.REDWOOD;
                    break;
                default: {
                    try {
                        species = TreeSpecies.valueOf(woodType);
                    } catch (IllegalArgumentException ex) {
                        throw new AssertionError("Unknown material " + legacyMaterial + " for wood species");
                    }
                }
            }

            // Doesn't handle stairs, slabs, fence and fence gates as they had their own separate materials.
            boolean firstType = false;
            switch (legacyMaterial) {
                case WOOD:
                case WOOD_DOUBLE_STEP:
                    state.setRawData(species.getData());
                    update = true;
                    break;
                case LOG:
                case LEAVES:
                    firstType = true;
                    // fall through to next switch statement below
                case LOG_2:
                case LEAVES_2:
                    switch (species) {
                        case GENERIC:
                        case REDWOOD:
                        case BIRCH:
                        case JUNGLE:
                            if (!firstType) throw new AssertionError("Invalid tree species " + species + " for block type" + legacyMaterial + ", use block type 2 instead");
                            break;
                        case ACACIA:
                        case DARK_OAK:
                            if (firstType) throw new AssertionError("Invalid tree species " + species + " for block type 2 " + legacyMaterial + ", use block type instead");
                            break;
                    }
                    state.setRawData((byte) ((state.getRawData() & 0xC) | (species.getData() & 0x3)));
                    update = true;
                    break;
                case SAPLING:
                case WOOD_STEP:
                    state.setRawData((byte) ((state.getRawData() & 0x8) | species.getData()));
                    update = true;
                    break;
                default:
                    throw new AssertionError("Unknown block type " + legacyMaterial + " for tree species: " + species);
            }
        } else if (material.getData() != 0) {
            state.setRawData(material.getData());
            update = true;
        }

        if (update) state.update();
        return update;
    }

    private enum LegacyMaterial {
        // Colorable
        STANDING_BANNER(Handling.COLORABLE), WALL_BANNER(Handling.COLORABLE), BANNER(Handling.COLORABLE),
        CARPET(Handling.COLORABLE), WOOL(Handling.COLORABLE), STAINED_CLAY(Handling.COLORABLE),
        STAINED_GLASS(Handling.COLORABLE), STAINED_GLASS_PANE(Handling.COLORABLE), THIN_GLASS(Handling.COLORABLE),

        // Wood Species
        WOOD(Handling.WOOD_SPECIES), WOOD_STEP(Handling.WOOD_SPECIES), WOOD_DOUBLE_STEP(Handling.WOOD_SPECIES),
        LEAVES(Handling.WOOD_SPECIES), LEAVES_2(Handling.WOOD_SPECIES),
        LOG(Handling.WOOD_SPECIES), LOG_2(Handling.WOOD_SPECIES),
        SAPLING(Handling.WOOD_SPECIES);

        private static final Map<String, LegacyMaterial> LOOKUP = new HashMap<>();

        static {
            for (LegacyMaterial legacyMaterial : values()) {
                LOOKUP.put(legacyMaterial.name(), legacyMaterial);
            }
        }

        private final Material material = Material.getMaterial(name());
        private final Handling handling;

        LegacyMaterial(Handling handling) {
            this.handling = handling;
        }

        private static LegacyMaterial getMaterial(String name) {
            return LOOKUP.get(name);
        }

        private enum Handling {COLORABLE, WOOD_SPECIES;}
    }
}