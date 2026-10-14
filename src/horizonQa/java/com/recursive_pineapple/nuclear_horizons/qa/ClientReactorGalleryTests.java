package com.recursive_pineapple.nuclear_horizons.qa;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces.IHeatContainer;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemRadioactiveCell;
import ic2.api.item.IC2Items;
import ic2.api.item.ICustomDamageItem;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;

/** Compares five live reactor pairs and captures their normal GUIs. */
@GameTestHolder(value = "nuclear_horizons", clientOnly = true, requiredMods = { "IC2", "gregtech" })
public final class ClientReactorGalleryTests {

    private static final int PAIRS = 5;
    private static final int COLUMNS = 4;
    private static final int REACTOR_Y = 0;
    private static final int REACTOR_Z = 4;
    private static final int PLAYER_Z = 7;
    private static final String[] NAMES = { "uranium", "reflected-uranium", "cooling", "mox-exchanger",
        "thorium-reactor-vent" };
    private static final String[][] LAYOUTS = { { "....", ".U..", "...." }, { "....", ".UR.", "...." },
        { "....", ".UCV", "...." }, { "....", ".MX.", ".CW." }, { "....", ".TH.", ".DA." } };

    private ClientReactorGalleryTests() {}

    @GameTest(template = "reactor_gallery", timeoutTicks = 4000)
    public static void compareFivePairsThroughTheirGuis(GameTestHelper helper) {
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(120)
            .server("place five NH and IC2 reactor pairs", () -> placeGallery(helper))
            .withinTicks(200)
            .awaitServer("NH chambers attach", () -> {
                for (int pair = 0; pair < PAIRS; pair++) {
                    if (nh(helper, pair).getColumnCount() != COLUMNS)
                        throw new AssertionError("NH chamber did not attach at station " + pair);
                }
            })
            .server("load and power every reactor", () -> {
                for (int pair = 0; pair < PAIRS; pair++) loadPair(helper, pair);
                for (int pair = 0; pair < PAIRS; pair++) power(helper, pair, Blocks.redstone_block);
            })
            .withinTicks(200)
            .awaitServer("all ten reactors complete three matching cycles", () -> {
                for (int pair = 0; pair < PAIRS; pair++) {
                    int nhCycles = fuelCycles(nh(helper, pair).getStackInSlot(COLUMNS + 1));
                    int ic2Cycles = fuelCycles(ic2(helper, pair).getItemAt(1, 1));
                    if (nhCycles < 3 || nhCycles != ic2Cycles)
                        throw new AssertionError(NAMES[pair] + " fuel cycles: NH=" + nhCycles + ", IC2=" + ic2Cycles);
                }
            })
            .server(
                "compare every NH and IC2 pair",
                () -> { for (int pair = 0; pair < PAIRS; pair++) compare(helper, pair); });

        for (int pair = 0; pair < PAIRS; pair++) {
            openGui(scenario, helper, pair, false);
            openGui(scenario, helper, pair, true);
        }
        scenario.succeed();
    }

    private static void placeGallery(GameTestHelper helper) {
        ItemStack ic2Core = IC2Items.getItem("nuclearReactor");
        ItemStack ic2Chamber = IC2Items.getItem("reactorChamber");
        if (ic2Core == null || ic2Chamber == null) throw new AssertionError("IC2 reactor blocks are unavailable");
        for (int pair = 0; pair < PAIRS; pair++) {
            int nhX = nhX(pair);
            int ic2X = ic2X(pair);
            helper.setBlock(nhX, REACTOR_Y, REACTOR_Z, BlockList.REACTOR_CORE);
            helper.setBlock(nhX + 1, REACTOR_Y, REACTOR_Z, BlockList.REACTOR_CHAMBER);
            helper.setBlock(
                ic2X,
                REACTOR_Y,
                REACTOR_Z,
                Block.getBlockFromItem(ic2Core.getItem()),
                ic2Core.getItemDamage());
            helper.setBlock(
                ic2X + 1,
                REACTOR_Y,
                REACTOR_Z,
                Block.getBlockFromItem(ic2Chamber.getItem()),
                ic2Chamber.getItemDamage());
        }
        EntityPlayerMP player = player(helper);
        int heldSlot = player.inventory.currentItem;
        ItemStack oldHeld = player.inventory.getStackInSlot(heldSlot);
        helper.afterTest(() -> {
            player.inventory.setInventorySlotContents(heldSlot, oldHeld);
            player.inventoryContainer.detectAndSendChanges();
        });
        player.inventory.setInventorySlotContents(heldSlot, new ItemStack(Items.stick));
        player.inventoryContainer.detectAndSendChanges();
    }

    private static void loadPair(GameTestHelper helper, int pair) {
        TileReactorCore nh = nh(helper, pair);
        IReactor ic2 = ic2(helper, pair);
        for (int row = 0; row < LAYOUTS[pair].length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                char code = LAYOUTS[pair][row].charAt(col);
                if (code == '.') continue;
                ItemStack nhStack = component(code, false);
                ItemStack ic2Stack = component(code, true);
                if (!nh.isItemValidForSlot(row * COLUMNS + col, nhStack)
                    || !(ic2Stack.getItem() instanceof IReactorComponent))
                    throw new AssertionError(
                        "Invalid " + code + " component at " + NAMES[pair] + " " + col + "," + row);
                if (pair == 4 && code == 'D') {
                    int usedCapacity = ((IHeatContainer) nhStack.getItem()).getMaxHeat(nhStack) - 2;
                    nhStack.setItemDamage(usedCapacity);
                    ICustomDamageItem ic2Item = (ICustomDamageItem) ic2Stack.getItem();
                    ic2Item.setCustomDamage(ic2Stack, usedCapacity);
                    if (nhStack.getItemDamage() != usedCapacity || ic2Item.getCustomDamage(ic2Stack) != usedCapacity)
                        throw new AssertionError("Failed to preload condensators to C-2");
                }
                nh.setInventorySlotContents(row * COLUMNS + col, nhStack);
                ic2.setItemAt(col, row, ic2Stack);
            }
        }
        assertLayout(helper, pair, false);
        assertLayout(helper, pair, true);
    }

    private static void openGui(ClientScenario scenario, GameTestHelper helper, int pair, boolean ic2Side) {
        String side = ic2Side ? "IC2" : "NH";
        scenario.server("face active " + side + " " + NAMES[pair], () -> face(helper, pair, ic2Side))
            .withinTicks(100)
            .awaitClient(
                "crosshair points at active " + side + " reactor",
                c -> assertCrosshair(helper, pair, ic2Side, c))
            .useHeldItem()
            .awaitClient("active " + side + " GUI opens", c -> assertGui(c, ic2Side))
            .escape();
    }

    private static void face(GameTestHelper helper, int pair, boolean ic2Side) {
        TestPos position = helper.absolute(ic2Side ? ic2X(pair) : nhX(pair), REACTOR_Y + 1, PLAYER_Z);
        player(helper).playerNetServerHandler
            .setPlayerLocation(position.x() + 0.5, position.y(), position.z() + 0.5, 180.0f, 29.0f);
    }

    private static void assertCrosshair(GameTestHelper helper, int pair, boolean ic2Side, ClientTest client) {
        TestPos target = helper.absolute(ic2Side ? ic2X(pair) : nhX(pair), REACTOR_Y, REACTOR_Z);
        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
        if (client.screen() != null || hit == null
            || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
            || hit.blockX != target.x()
            || hit.blockY != target.y()
            || hit.blockZ != target.z())
            throw new AssertionError("Crosshair has not reached reactor " + target + ": " + hit);
    }

    private static void assertGui(ClientTest client, boolean ic2Side) {
        GuiContainer screen = client.screen(GuiContainer.class);
        if (ic2Side == (screen instanceof GuiContainerWrapper)) throw new AssertionError(
            "Wrong reactor GUI opened: " + screen.getClass()
                .getName());
    }

    private static void assertLayout(GameTestHelper helper, int pair, boolean ic2Side) {
        for (int row = 0; row < LAYOUTS[pair].length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                char component = LAYOUTS[pair][row].charAt(col);
                ItemStack placed = ic2Side ? ic2(helper, pair).getItemAt(col, row)
                    : nh(helper, pair).getStackInSlot(row * COLUMNS + col);
                if (component == '.' ? placed != null : placed == null) throw new AssertionError(
                    "Fixture failed to load " + NAMES[pair]
                        + " "
                        + (ic2Side ? "IC2" : "NH")
                        + " slot "
                        + col
                        + ","
                        + row);
            }
        }
    }

    private static void power(GameTestHelper helper, int pair, Block block) {
        helper.setBlock(nhX(pair), REACTOR_Y, REACTOR_Z - 1, block);
        helper.setBlock(ic2X(pair), REACTOR_Y, REACTOR_Z - 1, block);
    }

    private static void compare(GameTestHelper helper, int pair) {
        TileReactorCore nh = nh(helper, pair);
        IReactor ic2 = ic2(helper, pair);
        double nhEU = ((S35PacketUpdateTileEntity) nh.getDescriptionPacket()).func_148857_g()
            .getDouble("addedEU") / 20.0;
        double ic2EU = ic2.getReactorEUEnergyOutput();
        int nhHeat = nh.getHullHeat();
        int ic2Heat = ic2.getHeat();
        helper.recordDiagnostic(
            NAMES[pair] + ": EU/t NH=" + nhEU + " IC2=" + ic2EU + ", hull heat NH=" + nhHeat + " IC2=" + ic2Heat);
        if (!nh.isActive() || !ic2.produceEnergy()
            || nhEU <= 0
            || ic2EU <= 0
            || Math.abs(nhEU - ic2EU) > 0.001
            || nhHeat != ic2Heat
            || nh.getMaxHullHeat() != ic2.getMaxHeat())
            throw new AssertionError("Reactor output or hull state differs at " + NAMES[pair]);
        for (int row = 0; row < LAYOUTS[pair].length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                char component = LAYOUTS[pair][row].charAt(col);
                if (component == '.' || component == 'A') continue;
                ItemStack nhStack = nh.getStackInSlot(row * COLUMNS + col);
                ItemStack ic2Stack = ic2.getItemAt(col, row);
                if (nhStack == null || ic2Stack == null)
                    throw new AssertionError("Component disappeared at " + col + "," + row);
                int nhState = state(component, nhStack, false);
                int ic2State = state(component, ic2Stack, true);
                helper.recordDiagnostic(
                    NAMES[pair] + " " + component + " " + col + "," + row + ": NH=" + nhState + " IC2=" + ic2State);
                if (nhState != ic2State) throw new AssertionError(
                    NAMES[pair] + " component " + component + " state differs at " + col + "," + row);
                if (pair == 4 && component == 'D' && ic2State != 20_000)
                    throw new AssertionError("Near-full IC2 condensator did not fill to capacity: " + ic2State);
            }
        }
    }

    private static int state(char component, ItemStack stack, boolean ic2Side) {
        if (component == 'U' || component == 'M' || component == 'T') return fuelCycles(stack);
        if (ic2Side) return ((ICustomDamageItem) stack.getItem()).getCustomDamage(stack);
        if (component == 'R') return stack.getItem()
            .getDamage(stack);
        if (stack.getItem() instanceof IHeatContainer) return stack.getItemDamage();
        throw new AssertionError("No state view for component " + component);
    }

    private static int fuelCycles(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemRadioactiveCell rod))
            throw new AssertionError("Fuel rod is missing or replaced");
        return rod.getDamageOfStack(stack);
    }

    private static ItemStack component(char code, boolean ic2Side) {
        if (code == 'U') return ItemList.RodUranium.get(1);
        if (code == 'M') return ItemList.RodMOX.get(1);
        if (code == 'T') return ItemList.RodThorium.get(1);
        if (!ic2Side) return switch (code) {
            case 'R' -> NHItemList.NEUTRON_REFLECTOR.get(1);
            case 'C' -> NHItemList.COOLANT_CELL_10k.get(1);
            case 'V' -> NHItemList.BASIC_HEAT_VENT.get(1);
            case 'X' -> NHItemList.BASIC_HEAT_EXCHANGER.get(1);
            case 'W' -> NHItemList.ADVANCED_HEAT_VENT.get(1);
            case 'H' -> NHItemList.REACTOR_HEAT_VENT.get(1);
            case 'D' -> NHItemList.RSH_CONDENSATOR.get(1);
            case 'A' -> NHItemList.REACTOR_PLATING.get(1);
            default -> throw new AssertionError("Unknown NH component " + code);
        };
        String name = switch (code) {
            case 'R' -> "reactorReflector";
            case 'C' -> "reactorCoolantSimple";
            case 'V' -> "reactorVent";
            case 'X' -> "reactorHeatSwitch";
            case 'W' -> "reactorVentDiamond";
            case 'H' -> "reactorVentCore";
            case 'D' -> "reactorCondensator";
            case 'A' -> "reactorPlating";
            default -> throw new AssertionError("Unknown IC2 component " + code);
        };
        ItemStack stack = IC2Items.getItem(name);
        if (stack == null) throw new AssertionError("IC2 component " + name + " is unavailable");
        return stack.copy();
    }

    private static int nhX(int pair) {
        return 2 + pair * 5;
    }

    private static int ic2X(int pair) {
        return 4 + pair * 5;
    }

    private static EntityPlayerMP player(GameTestHelper helper) {
        if (helper.getWorld().playerEntities.size() != 1) throw new AssertionError("Expected one client test player");
        return (EntityPlayerMP) helper.getWorld().playerEntities.get(0);
    }

    private static TileReactorCore nh(GameTestHelper helper, int pair) {
        return helper.assertTileEntityPresent(TileReactorCore.class, nhX(pair), REACTOR_Y, REACTOR_Z);
    }

    private static IReactor ic2(GameTestHelper helper, int pair) {
        TestPos position = helper.absolute(ic2X(pair), REACTOR_Y, REACTOR_Z);
        TileEntity tile = helper.getWorld()
            .getTileEntity(position.x(), position.y(), position.z());
        if (!(tile instanceof IReactor reactor)) throw new AssertionError("IC2 reactor tile is missing");
        return reactor;
    }
}
