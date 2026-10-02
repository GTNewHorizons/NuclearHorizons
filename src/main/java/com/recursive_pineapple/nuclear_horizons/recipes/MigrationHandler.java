package com.recursive_pineapple.nuclear_horizons.recipes;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.postea.api.BlockAccessCompat;
import com.gtnewhorizons.postea.api.BlockReplacementManager;
import com.gtnewhorizons.postea.api.ItemStackReplacementManager;
import com.gtnewhorizons.postea.api.TileEntityReplacementManager;
import com.gtnewhorizons.postea.compat.Compat;
import com.gtnewhorizons.postea.compat.SubChunkAccess;
import com.gtnewhorizons.postea.utility.BlockInfo;
import com.gtnewhorizons.postea.utility.MissingMappingHandler;
import com.gtnewhorizons.postea.utility.TransformerRegistry;
import com.recursive_pineapple.nuclear_horizons.Config;
import com.recursive_pineapple.nuclear_horizons.NuclearHorizons;
import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

import gregtech.api.enums.ItemList;

public class MigrationHandler {

    private static final int SAME_META = OreDictionary.WILDCARD_VALUE;
    private static final int IC2_REACTOR_CORE_META = 5;
    private static final int REACTOR_GRID_SIZE = 54;

    private static int ic2GeneratorBlockId = -1;

    public static void postInit() {
        if (!Config.ENABLE_MIGRATIONS) return;

        registerItemMigrations();
        registerReactorBlockMigrations();
    }

    private static void registerItemMigrations() {
        migrateAdvDmgTo("IC2:reactorVent", "heat");
        replace("IC2:reactorVent", NHItemList.BASIC_HEAT_VENT.getItem());
        migrateAdvDmgTo("IC2:reactorVentDiamond", "heat");
        replace("IC2:reactorVentDiamond", NHItemList.ADVANCED_HEAT_VENT.getItem());
        migrateAdvDmgTo("IC2:reactorVentCore", "heat");
        replace("IC2:reactorVentCore", NHItemList.REACTOR_HEAT_VENT.getItem());
        migrateAdvDmgTo("IC2:reactorVentSpread", "heat");
        replace("IC2:reactorVentSpread", NHItemList.COMPONENT_HEAT_VENT.getItem());
        migrateAdvDmgTo("IC2:reactorVentGold", "heat");
        replace("IC2:reactorVentGold", NHItemList.OVERCLOCKED_HEAT_VENT.getItem());

        migrateAdvDmgTo("IC2:reactorHeatSwitch", "heat");
        replace("IC2:reactorHeatSwitch", NHItemList.BASIC_HEAT_EXCHANGER.getItem());
        migrateAdvDmgTo("IC2:reactorHeatSwitchDiamond", "heat");
        replace("IC2:reactorHeatSwitchDiamond", NHItemList.ADVANCED_HEAT_EXCHANGER.getItem());
        migrateAdvDmgTo("IC2:reactorHeatSwitchCore", "heat");
        replace("IC2:reactorHeatSwitchCore", NHItemList.REACTOR_HEAT_EXCHANGER.getItem());
        migrateAdvDmgTo("IC2:reactorHeatSwitchSpread", "heat");
        replace("IC2:reactorHeatSwitchSpread", NHItemList.COMPONENT_HEAT_EXCHANGER.getItem());

        migrateAdvDmgTo("IC2:reactorCoolantSimple", "heat");
        replace("IC2:reactorCoolantSimple", NHItemList.COOLANT_CELL_10k.getItem());
        migrateAdvDmgTo("IC2:reactorCoolantTriple", "heat");
        replace("IC2:reactorCoolantTriple", NHItemList.COOLANT_CELL_30k.getItem());
        migrateAdvDmgTo("IC2:reactorCoolantSix", "heat");
        replace("IC2:reactorCoolantSix", NHItemList.COOLANT_CELL_60k.getItem());
        migrateAdvDmgTo("IC2:reactorCondensator", "heat");
        replace("IC2:reactorCondensator", NHItemList.RSH_CONDENSATOR.getItem());
        migrateAdvDmgTo("IC2:reactorCondensatorLap", "heat");
        replace("IC2:reactorCondensatorLap", NHItemList.LZH_CONDENSATOR.getItem());

        migrateAdvDmgTo("IC2:reactorReflector", "damage");
        replace("IC2:reactorReflector", NHItemList.NEUTRON_REFLECTOR.getItem());
        migrateAdvDmgTo("IC2:reactorReflectorThick", "damage");
        replace("IC2:reactorReflectorThick", NHItemList.THICK_NEUTRON_REFLECTOR.getItem());
        replace("IC2:reactorPlating", NHItemList.REACTOR_PLATING.getItem());
        replace("IC2:reactorPlatingHeat", NHItemList.REACTOR_PLATING_HEAT.getItem());
        replace("IC2:reactorPlatingExplosive", NHItemList.REACTOR_PLATING_EXPLOSIVE.getItem());

        replace("IC2:reactorLithiumCell", ItemList.RodLithium.getItem());
        replace("IC2:itemTritiumCell", ItemList.DepletedRodLithium.getItem());
        replace("gregtech:gt.Thoriumcell", ItemList.RodThorium.getItem());
        replace("gregtech:gt.Double_Thoriumcell", ItemList.RodThorium2.getItem());
        replace("gregtech:gt.Quad_Thoriumcell", ItemList.RodThorium4.getItem());
        replace("gregtech:gt.ThoriumcellDep", ItemList.DepletedRodThorium.getItem());
        replace("gregtech:gt.Double_ThoriumcellDep", ItemList.DepletedRodThorium2.getItem());
        replace("gregtech:gt.Quad_ThoriumcellDep", ItemList.DepletedRodThorium4.getItem());

        replace("IC2:reactorUraniumSimple", ItemList.RodUranium.getItem());
        replace("IC2:reactorUraniumDual", ItemList.RodUranium2.getItem());
        replace("IC2:reactorUraniumQuad", ItemList.RodUranium4.getItem());
        replace("IC2:reactorUraniumSimpledepleted", ItemList.DepletedRodUranium.getItem());
        replace("IC2:reactorUraniumDualdepleted", ItemList.DepletedRodUranium2.getItem());
        replace("IC2:reactorUraniumQuaddepleted", ItemList.DepletedRodUranium4.getItem());

        replace("IC2:reactorMOXSimple", ItemList.RodMOX.getItem());
        replace("IC2:reactorMOXDual", ItemList.RodMOX2.getItem());
        replace("IC2:reactorMOXQuad", ItemList.RodMOX4.getItem());
        replace("IC2:reactorMOXSimpledepleted", ItemList.DepletedRodMOX.getItem());
        replace("IC2:reactorMOXDualdepleted", ItemList.DepletedRodMOX2.getItem());
        replace("IC2:reactorMOXQuaddepleted", ItemList.DepletedRodMOX4.getItem());
    }

    private static void migrateAdvDmgTo(String originalId, String targetTag) {
        TransformerRegistry.addStackTransformer(originalId, (id, stack) -> {
            if (!stack.hasKey("tag", 10)) return false;

            NBTTagCompound itemTag = stack.getCompoundTag("tag");
            if (!itemTag.hasKey("advDmg")) return false;

            int advDmg = itemTag.getInteger("advDmg");
            itemTag.removeTag("advDmg");

            if (advDmg != 0) {
                itemTag.setInteger(targetTag, advDmg);
            } else if (itemTag.hasNoTags()) {
                stack.removeTag("tag");
            }

            return false;
        });
    }

    private static void replace(String originalId, Item replacement) {
        MissingMappingHandler.addIgnore(originalId);
        ItemStackReplacementManager.addSimpleReplacement(originalId, SAME_META, replacement);
        log(originalId, replacement);
    }

    private static void registerReactorBlockMigrations() {
        replaceBlock("IC2:blockGenerator", IC2_REACTOR_CORE_META, BlockList.REACTOR_CORE, false);
        replaceBlock("IC2:blockReactorChamber", SAME_META, BlockList.REACTOR_CHAMBER);
        replaceBlock("IC2:blockreactorvessel", SAME_META, BlockList.PRESSURE_VESSEL);
        replaceBlock("IC2:blockReactorFluidPort", SAME_META, BlockList.REACTOR_FLUID_PORT);
        replaceBlock("IC2:blockReactorAccessHatch", SAME_META, BlockList.REACTOR_ACCESS_HATCH);
        replaceBlock("IC2:blockReactorRedstonePort", SAME_META, BlockList.REACTOR_REDSTONE_PORT);

        BlockReplacementManager.registerIDResolver("IC2:blockGenerator", id -> ic2GeneratorBlockId = id);

        TileEntityReplacementManager.tileEntityTransformer("Nuclear Reactor", MigrationHandler::transformReactorCore);
        TileEntityReplacementManager
            .tileEntityTransformer("Reactor Chamber", MigrationHandler::transformReactorChamber);
        TileEntityReplacementManager
            .tileEntityTransformer("Reactor Fluid Port", MigrationHandler::transformReactorFluidPort);
        TileEntityReplacementManager
            .tileEntityTransformer("Reactor Access Hatch", MigrationHandler::transformReactorAccessHatch);
        TileEntityReplacementManager
            .tileEntityTransformer("Reactor Redstone Port", MigrationHandler::transformReactorRedstonePort);
        log("Registered placed reactor migration");
    }

    private static BlockInfo transformReactorCore(NBTTagCompound tag, World world, Chunk chunk) {
        int[] oldBlock = BlockAccessCompat.getBlockIDAndMetaAtTE(tag, chunk);
        if (oldBlock[1] != IC2_REACTOR_CORE_META) return null;

        ic2GeneratorBlockId = oldBlock[0];
        return new BlockInfo(BlockList.REACTOR_CORE, 0, MigrationHandler::migrateReactorCoreNbt);
    }

    private static NBTTagCompound migrateReactorCoreNbt(NBTTagCompound oldTag) {
        NBTTagCompound newTag = TileEntityReplacementManager
            .createTETagAtSamePosition(BlockList.REACTOR_CORE_NAME, oldTag);
        int heat = oldTag.getInteger("heat");

        newTag.setInteger("version", 1);
        newTag.setInteger("heat", heat);
        newTag.setInteger("roundedHeat", 0);
        newTag.setInteger("storedEU", 0);
        newTag.setInteger("tickCooldown", 0);
        newTag.setBoolean("isActive", false);
        newTag.setBoolean("isFluid", false);
        newTag.setTag("coolantTank", copyCompound(oldTag, "inputTank"));
        newTag.setTag("hotCoolantTank", copyCompound(oldTag, "outputTank"));
        newTag.setTag("grid", migrateReactorGrid(oldTag));
        newTag.setTag("fluidItems", migrateReactorFluidItems(oldTag));

        log(
            "Migrated IC2 reactor core at " + oldTag
                .getInteger("x") + "," + oldTag.getInteger("y") + "," + oldTag.getInteger("z"));
        return newTag;
    }

    private static NBTTagCompound migrateReactorGrid(NBTTagCompound oldTag) {
        NBTTagCompound grid = new NBTTagCompound();
        NBTTagCompound invSlots = oldTag.getCompoundTag("InvSlots");
        NBTTagCompound reactor = invSlots.getCompoundTag("reactor");
        NBTTagList contents = reactor.getTagList("Contents", 10);

        for (int i = 0; i < contents.tagCount(); i++) {
            NBTTagCompound stack = (NBTTagCompound) contents.getCompoundTagAt(i)
                .copy();
            int index = stack.getByte("Index") & 0xFF;
            if (index < REACTOR_GRID_SIZE) {
                stack.removeTag("Index");
                grid.setTag(Integer.toString(index), stack);
            }
        }

        return grid;
    }

    private static NBTTagCompound migrateReactorFluidItems(NBTTagCompound oldTag) {
        NBTTagCompound fluidItems = new NBTTagCompound();
        NBTTagCompound invSlots = oldTag.getCompoundTag("InvSlots");

        setFluidItem(fluidItems, 0, invSlots, "coolantinputSlot");
        setFluidItem(fluidItems, 1, invSlots, "hotcoolinputSlot");
        setFluidItem(fluidItems, 2, invSlots, "coolantoutputSlot");
        setFluidItem(fluidItems, 3, invSlots, "hotcoolantoutputSlot");

        return fluidItems;
    }

    private static void setFluidItem(NBTTagCompound fluidItems, int newSlot, NBTTagCompound invSlots, String oldSlot) {
        NBTTagCompound stack = copyInvSlotStack(invSlots, oldSlot);
        if (stack != null && newSlot < TileReactorCore.FLUID_ITEM_SLOT_COUNT) {
            fluidItems.setTag(Integer.toString(newSlot), stack);
        }
    }

    private static BlockInfo transformReactorChamber(NBTTagCompound tag, World world, Chunk chunk) {
        int[] reactorOffset = findAdjacentCore(tag, chunk);
        return new BlockInfo(
            BlockList.REACTOR_CHAMBER,
            0,
            oldTag -> migrateReactorChamberNbt(oldTag, reactorOffset[0], reactorOffset[1], reactorOffset[2]));
    }

    private static BlockInfo transformReactorFluidPort(NBTTagCompound tag, World world, Chunk chunk) {
        return new BlockInfo(BlockList.REACTOR_FLUID_PORT, 0, MigrationHandler::migrateReactorFluidPortNbt);
    }

    private static BlockInfo transformReactorAccessHatch(NBTTagCompound tag, World world, Chunk chunk) {
        return new BlockInfo(
            BlockList.REACTOR_ACCESS_HATCH,
            0,
            oldTag -> migrateReactorBlockNbt(BlockList.REACTOR_ACCESS_HATCH_NAME, oldTag));
    }

    private static BlockInfo transformReactorRedstonePort(NBTTagCompound tag, World world, Chunk chunk) {
        return new BlockInfo(
            BlockList.REACTOR_REDSTONE_PORT,
            0,
            oldTag -> migrateReactorBlockNbt(BlockList.REACTOR_REDSTONE_PORT_NAME, oldTag));
    }

    private static NBTTagCompound migrateReactorChamberNbt(NBTTagCompound oldTag, int reactorRelX, int reactorRelY,
        int reactorRelZ) {
        NBTTagCompound newTag = TileEntityReplacementManager
            .createTETagAtSamePosition(BlockList.REACTOR_CHAMBER_NAME, oldTag);

        newTag.setInteger("version", 1);
        newTag.setInteger("reactorRelX", reactorRelX);
        newTag.setInteger("reactorRelY", reactorRelY);
        newTag.setInteger("reactorRelZ", reactorRelZ);

        if (reactorRelX == 0 && reactorRelY == 0 && reactorRelZ == 0) {
            log(
                "Migrated IC2 reactor chamber without adjacent core link at " + oldTag
                    .getInteger("x") + "," + oldTag.getInteger("y") + "," + oldTag.getInteger("z"));
        }

        return newTag;
    }

    private static NBTTagCompound migrateReactorBlockNbt(String newId, NBTTagCompound oldTag) {
        NBTTagCompound newTag = TileEntityReplacementManager.createTETagAtSamePosition(newId, oldTag);
        newTag.setInteger("version", 1);
        newTag.setInteger("reactorRelX", 0);
        newTag.setInteger("reactorRelY", 0);
        newTag.setInteger("reactorRelZ", 0);
        return newTag;
    }

    private static NBTTagCompound migrateReactorFluidPortNbt(NBTTagCompound oldTag) {
        NBTTagCompound newTag = migrateReactorBlockNbt(BlockList.REACTOR_FLUID_PORT_NAME, oldTag);
        NBTTagCompound upgrade = copyInvSlotStack(oldTag.getCompoundTag("InvSlots"), "upgrade");
        if (upgrade != null) {
            newTag.setTag("recoverySlot", upgrade);
        }
        return newTag;
    }

    private static int[] findAdjacentCore(NBTTagCompound tag, Chunk chunk) {
        int x = tag.getInteger("x");
        int y = tag.getInteger("y");
        int z = tag.getInteger("z");

        int[] core = findAdjacentCore(chunk, x, y, z, x, y - 1, z);
        if (core != null) return core;
        core = findAdjacentCore(chunk, x, y, z, x, y + 1, z);
        if (core != null) return core;
        core = findAdjacentCore(chunk, x, y, z, x, y, z - 1);
        if (core != null) return core;
        core = findAdjacentCore(chunk, x, y, z, x, y, z + 1);
        if (core != null) return core;
        core = findAdjacentCore(chunk, x, y, z, x - 1, y, z);
        if (core != null) return core;
        core = findAdjacentCore(chunk, x, y, z, x + 1, y, z);
        if (core != null) return core;

        return inferCrossChunkCoreOffset(x, z);
    }

    private static int[] findAdjacentCore(Chunk chunk, int chamberX, int chamberY, int chamberZ, int x, int y, int z) {
        int[] block = getOldBlockIdAndMeta(chunk, x, y, z);
        if (block[0] != 0 && (ic2GeneratorBlockId == -1 || block[0] == ic2GeneratorBlockId)
            && block[1] == IC2_REACTOR_CORE_META) {
            ic2GeneratorBlockId = block[0];
            return new int[] { x - chamberX, y - chamberY, z - chamberZ };
        }
        return null;
    }

    private static int[] inferCrossChunkCoreOffset(int x, int z) {
        // Do not load neighbor chunks during migration; infer only unambiguous edge links.
        int localX = x & 15;
        int localZ = z & 15;
        int[] offset = new int[] { 0, 0, 0 };
        int guesses = 0;

        if (localX == 0) {
            offset = new int[] { -1, 0, 0 };
            guesses++;
        } else if (localX == 15) {
            offset = new int[] { 1, 0, 0 };
            guesses++;
        }

        if (localZ == 0) {
            offset = new int[] { 0, 0, -1 };
            guesses++;
        } else if (localZ == 15) {
            offset = new int[] { 0, 0, 1 };
            guesses++;
        }

        return guesses == 1 ? offset : new int[] { 0, 0, 0 };
    }

    private static int[] getOldBlockIdAndMeta(Chunk chunk, int x, int y, int z) {
        if (y < 0 || y >= 256) return new int[] { 0, 0 };
        if ((x >> 4) != chunk.xPosition || (z >> 4) != chunk.zPosition) return new int[] { 0, 0 };

        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        ExtendedBlockStorage subChunk = storage[y >> 4];
        if (subChunk == null) return new int[] { 0, 0 };

        SubChunkAccess access = Compat.getSubChunkAccess(subChunk);
        return new int[] { access.getBlockId(x & 15, y & 15, z & 15), access.getMeta(x & 15, y & 15, z & 15) };
    }

    private static NBTTagCompound copyCompound(NBTTagCompound tag, String key) {
        return tag.hasKey(key, 10) ? (NBTTagCompound) tag.getCompoundTag(key)
            .copy() : new NBTTagCompound();
    }

    private static NBTTagCompound copyInvSlotStack(NBTTagCompound invSlots, String slotName) {
        NBTTagList contents = invSlots.getCompoundTag(slotName)
            .getTagList("Contents", 10);
        if (contents.tagCount() == 0) return null;

        NBTTagCompound stack = (NBTTagCompound) contents.getCompoundTagAt(0)
            .copy();
        stack.removeTag("Index");
        return stack;
    }

    private static void replaceBlock(String originalId, int originalMeta, Block replacement) {
        replaceBlock(originalId, originalMeta, replacement, true);
    }

    private static void replaceBlock(String originalId, int originalMeta, Block replacement, boolean ignoreMissing) {
        if (ignoreMissing) {
            MissingMappingHandler.addIgnore(originalId);
        }
        BlockReplacementManager.addSimpleReplacement(originalId, originalMeta, replacement, 0);
        log("Registered block migration " + originalId + ":" + originalMeta + " -> " + replacement);
    }

    private static void log(String originalId, Item replacement) {
        if (Config.LOG_MIGRATION_DETAILS) {
            NuclearHorizons.LOG.info("Registered item migration {} -> {}", originalId, replacement);
        }
    }

    private static void log(String message) {
        if (Config.LOG_MIGRATION_DETAILS) {
            NuclearHorizons.LOG.info(message);
        }
    }
}
