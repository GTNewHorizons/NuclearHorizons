package com.recursive_pineapple.nuclear_horizons.qa;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.basic.BasicBreederRodItem;
import com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces.IHeatContainer;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemRadioactiveCell;
import ic2.api.item.IC2Items;
import ic2.api.item.ICustomDamageItem;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;

/** Compares two placed, powered reactors through their normal game tick and client paths. */
@GameTestHolder(value = "nuclear_horizons", clientOnly = true, requiredMods = { "IC2", "gregtech" })
public final class ClientReactorParityTests {

    private static final int NH_X = 2;
    private static final int IC2_X = 6;
    private static final int Y = 1;
    private static final int Z = 1;
    private static final int COLUMNS = 4;
    private static final String[] LAYOUT = { "ULMr", "RVDA", "cYHX", "OZkp", "SC.P", "dWeT" };

    private ClientReactorParityTests() {}

    @GameTest(template = "reactor_parity", timeoutTicks = 600)
    public static void mixedComponentsMatchIc2AfterThreeCycles(GameTestHelper helper) {
        TestPos nhPosition = helper.absolute(NH_X, Y, Z);
        TestPos ic2Position = helper.absolute(IC2_X, Y, Z);

        ClientTest.scenario(helper)
            .defaultTimeoutTicks(100)
            .server("place both reactor structures", () -> placeReactors(helper))
            .withinTicks(200)
            .awaitServer("NH chamber attaches", () -> {
                if (nh(helper).getColumnCount() != COLUMNS) {
                    throw new AssertionError("NH chamber did not attach");
                }
            })
            .server("load matching component layouts and power both cores", () -> loadAndPower(helper))
            .withinTicks(200)
            .awaitServer("matching layouts finish three active cycles", () -> {
                TileReactorCore nh = nh(helper);
                IReactor ic2 = ic2(helper);
                int nhCycles = fuelCycles(nh.getStackInSlot(0));
                int ic2Cycles = fuelCycles(ic2.getItemAt(0, 0));
                if (!nh.isActive() || !ic2.produceEnergy() || nhCycles < 3 || nhCycles != ic2Cycles) {
                    throw new AssertionError(
                        "Reactor cycles: NH=" + nhCycles
                            + ", IC2="
                            + ic2Cycles
                            + ", NH columns="
                            + nh.getColumnCount()
                            + ", NH active="
                            + nh.isActive()
                            + ", IC2 active="
                            + ic2.produceEnergy());
                }
                compare(helper);
            })
            .withinTicks(80)
            .awaitClient("both cores and the held item reach the client", c -> {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld.getBlock(nhPosition.x(), nhPosition.y(), nhPosition.z()) != BlockList.REACTOR_CORE
                    || mc.theWorld.getBlock(ic2Position.x(), ic2Position.y(), ic2Position.z()) != ic2Block()) {
                    throw new AssertionError("Both placed reactor cores must be visible to the client");
                }
                if (mc.thePlayer.getHeldItem() == null || mc.thePlayer.getHeldItem()
                    .getItem() != Items.stick) {
                    throw new AssertionError("The use-item input is not ready");
                }
            })
            .useHeldItem()
            .awaitClient(
                "NH reactor GUI opens through normal block use",
                c -> {
                    if (c.screen() == null) throw new AssertionError("Right-click did not open the NH reactor GUI");
                })
            .succeed();
    }

    private static void placeReactors(GameTestHelper helper) {
        helper.setBlock(NH_X, Y, Z, BlockList.REACTOR_CORE);
        helper.setBlock(NH_X + 1, Y, Z, BlockList.REACTOR_CHAMBER);
        ItemStack ic2Core = IC2Items.getItem("nuclearReactor");
        if (ic2Core == null) throw new AssertionError("IC2 nuclear reactor is unavailable");
        helper.setBlock(IC2_X, Y, Z, ic2Block(), ic2Core.getItemDamage());
        ItemStack ic2Chamber = IC2Items.getItem("reactorChamber");
        if (ic2Chamber == null) throw new AssertionError("IC2 reactor chamber is unavailable");
        helper.setBlock(IC2_X + 1, Y, Z, Block.getBlockFromItem(ic2Chamber.getItem()), ic2Chamber.getItemDamage());
        helper.assertBlockPresent(Block.getBlockFromItem(ic2Chamber.getItem()), IC2_X + 1, Y, Z);

        if (helper.getWorld().playerEntities.size() != 1) throw new AssertionError("Expected one client test player");
        EntityPlayerMP player = (EntityPlayerMP) helper.getWorld().playerEntities.get(0);
        int heldSlot = player.inventory.currentItem;
        ItemStack previous = player.inventory.getStackInSlot(heldSlot);
        helper.afterTest(() -> {
            player.inventory.setInventorySlotContents(heldSlot, previous);
            player.inventoryContainer.detectAndSendChanges();
        });
        player.inventory.setInventorySlotContents(heldSlot, new ItemStack(Items.stick));
        player.inventoryContainer.detectAndSendChanges();
        player.playerNetServerHandler.setPlayerLocation(
            helper.absolute(NH_X, Y, Z + 3)
                .x() + 0.5,
            nh(helper).yCoord,
            nh(helper).zCoord + 3.5,
            180.0f,
            20.0f);

    }

    private static void loadAndPower(GameTestHelper helper) {
        TileReactorCore nh = nh(helper);
        IReactor ic2 = ic2(helper);
        for (int row = 0; row < LAYOUT.length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                char component = LAYOUT[row].charAt(col);
                if (component == '.') continue;
                ItemStack nhStack = nhItem(component);
                ItemStack ic2Stack = ic2Item(component);
                if (!nh.isItemValidForSlot(row * COLUMNS + col, nhStack)) {
                    throw new AssertionError("NH rejects " + component + " at " + col + "," + row);
                }
                if (!(ic2Stack.getItem() instanceof IReactorComponent)) {
                    throw new AssertionError("IC2 rejects " + component + " at " + col + "," + row);
                }
                int initialHeat = initialHeat(component);
                if (initialHeat > 0) {
                    if (!(nhStack.getItem() instanceof IHeatContainer nhHeat)
                        || !(ic2Stack.getItem() instanceof ICustomDamageItem ic2Heat)) {
                        throw new AssertionError("Cannot preload heat in component " + component);
                    }
                    if (initialHeat > nhHeat.getMaxHeat(nhStack)) {
                        throw new AssertionError("Initial heat exceeds capacity in component " + component);
                    }
                    nhStack.setItemDamage(initialHeat);
                    ic2Heat.setCustomDamage(ic2Stack, initialHeat);
                }
                nh.setInventorySlotContents(row * COLUMNS + col, nhStack);
                ic2.setItemAt(col, row, ic2Stack);
            }
        }
        nh.setHullHeat(200);
        ic2.setHeat(200);
        helper.setBlock(NH_X, Y + 1, Z, Blocks.redstone_block);
        helper.setBlock(IC2_X, Y + 1, Z, Blocks.redstone_block);
    }

    private static void compare(GameTestHelper helper) {
        TileReactorCore nh = nh(helper);
        IReactor ic2 = ic2(helper);
        int nhCycles = fuelCycles(nh.getStackInSlot(0));
        int ic2Cycles = fuelCycles(ic2.getItemAt(0, 0));
        double nhEUPerTick = ((S35PacketUpdateTileEntity) nh.getDescriptionPacket()).func_148857_g()
            .getDouble("addedEU") / 20.0;
        double ic2EUPerTick = ic2.getReactorEUEnergyOutput();
        int nhHeat = nh.getHullHeat();
        int ic2Heat = ic2.getHeat();

        helper.recordDiagnostic(
            "NH versus IC2 after " + nhCycles
                + " fuel cycles: EU/t "
                + nhEUPerTick
                + " versus "
                + ic2EUPerTick
                + ", hull heat "
                + nhHeat
                + " versus "
                + ic2Heat
                + ", max hull heat "
                + nh.getMaxHullHeat()
                + " versus "
                + ic2.getMaxHeat());
        StringBuilder differences = new StringBuilder();
        if (nhCycles < 3 || nhCycles != ic2Cycles) differences.append("Fuel cycles differ. ");
        if (nhEUPerTick <= 0 || ic2EUPerTick <= 0) differences.append("Both reactors must produce EU. ");
        if (Math.abs(nhEUPerTick - ic2EUPerTick) > 0.001) differences.append("EU/t differs. ");
        if (nhHeat != ic2Heat) differences.append("Hull heat differs. ");
        if (nh.getMaxHullHeat() != ic2.getMaxHeat()) differences.append("Reactor plating capacity differs. ");
        boolean coolantChanged = false;
        boolean ventChanged = false;
        boolean exchangerChanged = false;
        boolean condensatorChanged = false;
        boolean reflectorChanged = false;
        boolean lithiumChanged = false;
        for (int row = 0; row < LAYOUT.length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                char component = LAYOUT[row].charAt(col);
                if (component == '.') continue;
                ItemStack nhStack = nh.getStackInSlot(row * COLUMNS + col);
                ItemStack ic2Stack = ic2.getItemAt(col, row);
                if (nhStack == null || ic2Stack == null) {
                    throw new AssertionError("Component " + component + " disappeared from " + col + "," + row);
                }
                int nhState = nhState(component, nhStack);
                int ic2State = ic2State(component, ic2Stack);
                helper.recordDiagnostic(
                    "Slot " + col
                        + ","
                        + row
                        + " "
                        + component
                        + ": initial="
                        + initialHeat(component)
                        + ", NH="
                        + nhState
                        + ", IC2="
                        + ic2State);
                if (nhState != ic2State) {
                    differences.append("Component ")
                        .append(component)
                        .append(" state differs at ")
                        .append(col)
                        .append(',')
                        .append(row)
                        .append(". ");
                }
                if (component == 'L') lithiumChanged = nhState > 0;
                if ((component == 'R' || component == 'r') && nhState > 0) reflectorChanged = true;
                if (component == 'U' || component == 'M' || component == 'T') {
                    if (nhState < 3) differences.append("Fuel ")
                        .append(component)
                        .append(" did not wear. ");
                }
                switch (component) {
                    case 'C', 'c', 'd' -> coolantChanged |= nhState != initialHeat(component);
                    case 'V', 'W', 'O', 'S', 'H' -> ventChanged |= nhState != initialHeat(component);
                    case 'X', 'Y', 'Z', 'k' -> exchangerChanged |= nhState != initialHeat(component);
                    case 'D', 'e' -> condensatorChanged |= nhState != initialHeat(component);
                    default -> {}
                }
                if (component == 'd' && nhState >= initialHeat(component)) {
                    differences.append("The component vent did not cool its adjacent 60k cell. ");
                }
            }
        }
        if (!lithiumChanged) differences.append("The lithium breeder received no neutron pulses. ");
        if (!reflectorChanged) differences.append("The reflector received no neutron pulses. ");
        if (!coolantChanged) differences.append("No coolant cell stored heat. ");
        if (!ventChanged) differences.append("No heat vent moved heat. ");
        if (!exchangerChanged) differences.append("No heat exchanger moved heat. ");
        if (!condensatorChanged) differences.append("No condensator absorbed heat. ");
        if (nh.getMaxHullHeat() <= 10_000) differences.append("Reactor plating did not raise hull capacity. ");
        if (differences.length() > 0) throw new AssertionError(differences.toString());
    }

    private static ItemStack nhItem(char component) {
        return switch (component) {
            case 'U' -> ItemList.RodUranium.get(1);
            case 'M' -> ItemList.RodMOX.get(1);
            case 'T' -> ItemList.RodThorium.get(1);
            case 'L' -> NHItemList.LITHIUM_BREEDER_CELL.get(1);
            case 'C' -> NHItemList.COOLANT_CELL_10k.get(1);
            case 'c' -> NHItemList.COOLANT_CELL_30k.get(1);
            case 'd' -> NHItemList.COOLANT_CELL_60k.get(1);
            case 'V' -> NHItemList.BASIC_HEAT_VENT.get(1);
            case 'W' -> NHItemList.ADVANCED_HEAT_VENT.get(1);
            case 'O' -> NHItemList.OVERCLOCKED_HEAT_VENT.get(1);
            case 'S' -> NHItemList.COMPONENT_HEAT_VENT.get(1);
            case 'X' -> NHItemList.BASIC_HEAT_EXCHANGER.get(1);
            case 'Y' -> NHItemList.ADVANCED_HEAT_EXCHANGER.get(1);
            case 'Z' -> NHItemList.REACTOR_HEAT_EXCHANGER.get(1);
            case 'k' -> NHItemList.COMPONENT_HEAT_EXCHANGER.get(1);
            case 'R' -> NHItemList.NEUTRON_REFLECTOR.get(1);
            case 'r' -> NHItemList.THICK_NEUTRON_REFLECTOR.get(1);
            case 'A' -> NHItemList.REACTOR_PLATING.get(1);
            case 'p' -> NHItemList.REACTOR_PLATING_HEAT.get(1);
            case 'P' -> NHItemList.REACTOR_PLATING_EXPLOSIVE.get(1);
            case 'D' -> NHItemList.RSH_CONDENSATOR.get(1);
            case 'e' -> NHItemList.LZH_CONDENSATOR.get(1);
            case 'H' -> NHItemList.REACTOR_HEAT_VENT.get(1);
            default -> throw new IllegalArgumentException("Unknown reactor component " + component);
        };
    }

    private static ItemStack ic2Item(char component) {
        if (component == 'U' || component == 'M' || component == 'T') return nhItem(component);
        String name = switch (component) {
            case 'L' -> "reactorLithiumCell";
            case 'C' -> "reactorCoolantSimple";
            case 'c' -> "reactorCoolantTriple";
            case 'd' -> "reactorCoolantSix";
            case 'V' -> "reactorVent";
            case 'W' -> "reactorVentDiamond";
            case 'O' -> "reactorVentGold";
            case 'S' -> "reactorVentSpread";
            case 'X' -> "reactorHeatSwitch";
            case 'Y' -> "reactorHeatSwitchDiamond";
            case 'Z' -> "reactorHeatSwitchCore";
            case 'k' -> "reactorHeatSwitchSpread";
            case 'R' -> "reactorReflector";
            case 'r' -> "reactorReflectorThick";
            case 'A' -> "reactorPlating";
            case 'p' -> "reactorPlatingHeat";
            case 'P' -> "reactorPlatingExplosive";
            case 'D' -> "reactorCondensator";
            case 'e' -> "reactorCondensatorLap";
            case 'H' -> "reactorVentCore";
            default -> throw new IllegalArgumentException("Unknown reactor component " + component);
        };
        ItemStack item = IC2Items.getItem(name);
        if (item == null) throw new AssertionError("IC2 component " + name + " is unavailable");
        return item.copy();
    }

    private static int nhState(char component, ItemStack stack) {
        if (component == 'U' || component == 'M' || component == 'T') return fuelCycles(stack);
        if (component == 'L') return ((BasicBreederRodItem) stack.getItem()).getStoredNeutrons(stack);
        if (isPlating(component)) return 0;
        if (component == 'R' || component == 'r') return stack.getItem()
            .getDamage(stack);
        if (stack.getItem() instanceof IHeatContainer) return stack.getItemDamage();
        throw new AssertionError("NH heat component " + component + " lacks a heat capacity view");
    }

    private static int ic2State(char component, ItemStack stack) {
        if (component == 'U' || component == 'M' || component == 'T') return fuelCycles(stack);
        if (component == 'L') return stack.getItemDamage();
        if (isPlating(component)) return 0;
        // The spread vent has no internal heat or wear. Its adjacent coolant cell shows its effect.
        if (component == 'S') return 0;
        if (stack.getItem() instanceof ICustomDamageItem gradual) return gradual.getCustomDamage(stack);
        throw new AssertionError("IC2 component " + component + " lacks a wear view");
    }

    private static boolean isPlating(char component) {
        return component == 'A' || component == 'p' || component == 'P';
    }

    private static int initialHeat(char component) {
        return switch (component) {
            case 'C', 'c', 'd', 'V', 'W', 'O', 'H', 'X', 'Y', 'Z', 'k' -> 80;
            default -> 0;
        };
    }

    private static int fuelCycles(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemRadioactiveCell rod)) {
            throw new AssertionError("The uranium fuel rod was missing or replaced");
        }
        return rod.getDamageOfStack(stack);
    }

    private static TileReactorCore nh(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileReactorCore.class, NH_X, Y, Z);
    }

    private static IReactor ic2(GameTestHelper helper) {
        TileEntity tile = helper.getWorld()
            .getTileEntity(
                helper.absolute(IC2_X, Y, Z)
                    .x(),
                helper.absolute(IC2_X, Y, Z)
                    .y(),
                helper.absolute(IC2_X, Y, Z)
                    .z());
        if (!(tile instanceof IReactor reactor)) throw new AssertionError("The IC2 reactor tile is missing");
        return reactor;
    }

    private static Block ic2Block() {
        ItemStack core = IC2Items.getItem("nuclearReactor");
        if (core == null) throw new AssertionError("IC2 nuclear reactor is unavailable");
        return Block.getBlockFromItem(core.getItem());
    }
}
