package com.recursive_pineapple.nuclear_horizons.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.basic.BasicBreederRodItem;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileFluidPort;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileRedstonePort;
import com.recursive_pineapple.nuclear_horizons.recipes.GTMats;

import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemRadioactiveCell;

/** Runs all five NH-only reactor grid components in a complete fluid multiblock. */
@GameTestHolder(value = "nuclear_horizons", clientOnly = true, requiredMods = { "gregtech" })
public final class ClientFluidReactorTests {

    private static final int X = 4;
    private static final int Y = 3;
    private static final int Z = 5;
    private static final int FUEL = 2 * TileReactorCore.COL_COUNT + 2;
    private static final int BREEDER = FUEL + 1;
    private static final int PLATING = FUEL + 2;
    private static final int COPPER = 3 * TileReactorCore.COL_COUNT + 2;
    private static final int TUNGSTENSTEEL = COPPER + 1;
    private static final int NAQUADRIA = COPPER + 2;

    private ClientFluidReactorTests() {}

    @GameTest(template = "fluid_reactor", timeoutTicks = 700)
    public static void coolantAndNhOnlyComponentsWorkInFluidReactor(GameTestHelper helper) {
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(100)
            .server("build the fluid reactor and six chambers", () -> buildReactor(helper))
            .withinTicks(220)
            .awaitServer("complete fluid structure links its ports", () -> {
                TileReactorCore core = core(helper);
                if (!core.isFluid() || core.getColumnCount() != TileReactorCore.COL_COUNT
                    || input(helper).getReactor() != core
                    || output(helper).getReactor() != core)
                    throw new AssertionError(
                        "Fluid multiblock has not formed: fluid=" + core.isFluid()
                            + ", columns="
                            + core.getColumnCount()
                            + ", input="
                            + (input(helper).getReactor() == core)
                            + ", output="
                            + (output(helper).getReactor() == core));
            })
            .server("load NH-only parts, coolant and power", () -> loadAndPower(helper))
            .withinTicks(80)
            .awaitServer("fuel rod completes one active cycle", () -> {
                TileReactorCore core = core(helper);
                ItemStack rod = core.getStackInSlot(FUEL);
                TileRedstonePort port = helper.assertTileEntityPresent(TileRedstonePort.class, X, Y, Z - 2);
                if (!core.isActive()) throw new AssertionError("Fluid reactor is inactive: " + port.getEnableState());
                if (rod == null || !(rod.getItem() instanceof ItemRadioactiveCell fuel)
                    || fuel.getDamageOfStack(rod) < 1)
                    throw new AssertionError("Fluid reactor fuel has not completed an active cycle");
            })
            .server("all five parts and coolant conversion changed state", () -> assertOperation(helper))
            .server("face the access hatch", () -> faceHatch(helper))
            .withinTicks(100)
            .awaitClient("crosshair points at the access hatch", c -> {
                TestPos hatch = helper.absolute(X, Y, Z + 2);
                MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
                if (c.screen() != null || hit == null
                    || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                    || hit.blockX != hatch.x()
                    || hit.blockY != hatch.y()
                    || hit.blockZ != hatch.z())
                    throw new AssertionError("Access hatch is not under the crosshair: " + hit);
            })
            .useHeldItem()
            .awaitClient("fluid reactor GUI opens", c -> c.screen(GuiContainerWrapper.class))
            .succeed();
    }

    private static void buildReactor(GameTestHelper helper) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dy) == 2 || Math.abs(dz) == 2)
                        helper.setBlock(X + dx, Y + dy, Z + dz, BlockList.PRESSURE_VESSEL);
                }
            }
        }

        helper.setBlock(X, Y, Z + 2, BlockList.REACTOR_ACCESS_HATCH);
        helper.setBlock(X + 2, Y, Z, BlockList.REACTOR_FLUID_PORT);
        helper.setBlock(X - 2, Y, Z, BlockList.REACTOR_FLUID_PORT);
        helper.setBlock(X, Y, Z - 2, BlockList.REACTOR_REDSTONE_PORT);

        helper.setBlock(X, Y, Z, BlockList.REACTOR_CORE);
        helper.setBlock(X + 1, Y, Z, BlockList.REACTOR_CHAMBER);
        helper.setBlock(X - 1, Y, Z, BlockList.REACTOR_CHAMBER);
        helper.setBlock(X, Y + 1, Z, BlockList.REACTOR_CHAMBER);
        helper.setBlock(X, Y - 1, Z, BlockList.REACTOR_CHAMBER);
        helper.setBlock(X, Y, Z + 1, BlockList.REACTOR_CHAMBER);
        helper.setBlock(X, Y, Z - 1, BlockList.REACTOR_CHAMBER);
        core(helper).queueStructureCheck();

        EntityPlayerMP player = player(helper);
        int heldSlot = player.inventory.currentItem;
        ItemStack previous = player.inventory.getStackInSlot(heldSlot);
        helper.afterTest(() -> {
            player.inventory.setInventorySlotContents(heldSlot, previous);
            player.inventoryContainer.detectAndSendChanges();
        });
        player.inventory.setInventorySlotContents(heldSlot, new ItemStack(Items.stick));
        player.inventoryContainer.detectAndSendChanges();
        faceHatch(helper);
    }

    private static void loadAndPower(GameTestHelper helper) {
        TileReactorCore core = core(helper);
        FluidStack cold = GTMats.COOLANT.getFluid(10_000);
        if (cold == null || input(helper).fill(ForgeDirection.UNKNOWN, cold, true) != 10_000)
            throw new AssertionError("Fluid port did not accept 10,000 mB of NH coolant");

        insert(core, FUEL, ItemList.RodUranium.get(1));
        insert(core, BREEDER, NHItemList.THORIUM_BREEDER_ROD.get(1));
        insert(core, PLATING, NHItemList.REACTOR_PLATING_HEAT_2.get(1));
        insert(core, COPPER, NHItemList.COOLANT_HEAT_EXCHANGER_1.get(1));
        insert(core, TUNGSTENSTEEL, NHItemList.COOLANT_HEAT_EXCHANGER_2.get(1));
        insert(core, NAQUADRIA, NHItemList.COOLANT_HEAT_EXCHANGER_3.get(1));

        core.setHullHeat(1_000);
        helper.setBlock(X, Y, Z - 3, Blocks.redstone_block);
    }

    private static void insert(TileReactorCore core, int slot, ItemStack stack) {
        if (!core.isItemValidForSlot(slot, stack))
            throw new AssertionError("Reactor rejected component in slot " + slot);
        core.setInventorySlotContents(slot, stack);
    }

    private static void assertOperation(GameTestHelper helper) {
        TileReactorCore core = core(helper);
        int[] vents = { COPPER, TUNGSTENSTEEL, NAQUADRIA };
        for (int slot : vents) {
            ItemStack stack = core.getStackInSlot(slot);
            if (stack == null || stack.getItemDamage() <= 0)
                throw new AssertionError("Coolant exchanger did not move heat in slot " + slot);
        }

        ItemStack breeder = core.getStackInSlot(BREEDER);
        if (breeder == null || ((BasicBreederRodItem) breeder.getItem()).getStoredNeutrons(breeder) != 2)
            throw new AssertionError("Thorium breeder did not store two neutron pulses");
        if (core.getMaxHullHeat() != 16_800) throw new AssertionError("Naquadah plating changed hull capacity");
        if (core.getHullHeat() >= 1_000) throw new AssertionError("Fluid cooling did not lower hull heat");

        FluidStack hot = output(helper).drain(ForgeDirection.UNKNOWN, 10_000, false);
        FluidStack expected = GTMats.HOT_COOLANT.getFluid(1);
        if (hot == null || expected == null || hot.getFluid() != expected.getFluid() || hot.amount != 780)
            throw new AssertionError("Fluid port did not output 780 mB of hot NH coolant");
        var cold = input(helper).getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
        if (cold == null || cold.amount != 9_220)
            throw new AssertionError("Fluid reactor did not consume 780 mB of cold NH coolant");
        helper.recordDiagnostic(
            "Fluid reactor: hot coolant=" + hot.amount
                + " mB, hull="
                + core.getHullHeat()
                + " HU, max hull="
                + core.getMaxHullHeat()
                + " HU, breeder="
                + ((BasicBreederRodItem) breeder.getItem()).getStoredNeutrons(breeder));
    }

    private static void faceHatch(GameTestHelper helper) {
        TestPos position = helper.absolute(X, 1, Z + 5);
        player(helper).playerNetServerHandler
            .setPlayerLocation(position.x() + 0.5, position.y(), position.z() + 0.5, 180.0f, -16.0f);
    }

    private static EntityPlayerMP player(GameTestHelper helper) {
        if (helper.getWorld().playerEntities.size() != 1) throw new AssertionError("Expected one client test player");
        return (EntityPlayerMP) helper.getWorld().playerEntities.get(0);
    }

    private static TileReactorCore core(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileReactorCore.class, X, Y, Z);
    }

    private static TileFluidPort input(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileFluidPort.class, X + 2, Y, Z);
    }

    private static TileFluidPort output(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileFluidPort.class, X - 2, Y, Z);
    }
}
