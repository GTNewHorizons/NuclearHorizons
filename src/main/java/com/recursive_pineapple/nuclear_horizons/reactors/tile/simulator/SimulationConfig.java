package com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator;

import java.io.IOException;
import java.util.Base64;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.recursive_pineapple.nuclear_horizons.reactors.components.ComponentRegistry;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

import io.netty.buffer.ByteBuf;

public class SimulationConfig implements IItemHandlerModifiable {

    // spotless:off

    public boolean
        pulsed = false,
        automated = true,
        fluid = false,
        usingReactorCoolantInjectors;

    public int
        initialHeat = 0,
        onPulse = 0,
        offPulse = 0,
        suspendTemp = 100_000,
        resumeTemp = 100_000,
        maxSimulationTicks = 1_000_000;

    public SimComponentConfig[] components = new SimComponentConfig[TileReactorCore.COL_COUNT * TileReactorCore.ROW_COUNT];

    // spotless:on

    @Nullable
    public transient SimulationResult result;

    public SimulationConfig() {

    }

    public String getCode() {
        return "nh=" + Base64.getEncoder()
            .encodeToString(save().toByteArray());
    }

    /** Exports only configurations that the external planner can represent without data loss. */
    public String getPlannerCode() {
        BigintStorage storage = new BigintStorage();

        storage.store(maxSimulationTicks, 5000000);
        storage.store(usingReactorCoolantInjectors ? 1 : 0, 1);
        storage.store(fluid ? 1 : 0, 1);

        if (pulsed) {
            storage.store(resumeTemp, 120000);
            storage.store(suspendTemp, 120000);
            storage.store(offPulse, 5000000);
            storage.store(onPulse, 5000000);
        }

        storage.store(initialHeat, 120000);

        final int maxComponentHeat = 1000000000;

        for (int i = components.length - 1; i >= 0; i--) {
            var c = components[i];

            if (c == null) {
                storage.store(0, 72);
                continue;
            }

            Integer componentId = SimulationItems.getSimulationItemId(c.stack);
            if (componentId != null && componentId == 73) componentId = 71;
            if (componentId == null || componentId > 72) {
                throw new IllegalArgumentException("The planner does not support the component in slot " + (i + 1));
            }

            if (c.hasAutomation) {
                if (automated) {
                    storage.store(c.reactorPause, 10000);
                    storage.store(c.replacementThreshold, maxComponentHeat);
                }

                storage.store(c.initialHeat, maxComponentHeat);
            }

            storage.store(c.hasAutomation ? 1 : 0, 1);
            storage.store(componentId, 72);
        }

        storage.store(automated ? 1 : 0, 1);
        storage.store(pulsed ? 1 : 0, 1);

        storage.store(4, 255);

        String code = "erp=" + storage.outputBase64();
        if (!fromCode(code).save()
            .equals(save())) {
            throw new IllegalArgumentException(
                "The planner cannot preserve all settings or item state. Use the configuration code.");
        }
        return code;
    }

    public static SimulationConfig fromCode(String code) {
        if (code != null && code.startsWith("nh=")) {
            try {
                return load(
                    SimulatorProtos.SimulationConfig.parseFrom(
                        Base64.getDecoder()
                            .decode(code.substring(3))));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Invalid Nuclear Horizons configuration code", e);
            }
        }
        return fromPlannerCode(code, false);
    }

    /** Version 1 worlds used the historical NH mapping, where planner ID 71 meant a single rod. */
    public static SimulationConfig fromLegacyCode(String code) {
        return fromPlannerCode(code, true);
    }

    private static SimulationConfig fromPlannerCode(String code, boolean legacy) {
        SimulationConfig config = new SimulationConfig();

        if (code == null || code.isEmpty()) {
            return config;
        }

        if (code.startsWith("erp=")) {
            code = code.substring(4);
        }

        BigintStorage storage = BigintStorage.inputBase64(code);

        int revision = storage.extract(255);

        if (revision < 0 || revision > 4) {
            throw new IllegalArgumentException("Unsupported planner code revision: " + revision);
        }

        if (revision >= 1) {
            config.pulsed = storage.extract(1) > 0;
            config.automated = storage.extract(1) > 0;
        }

        int maxComponentHeat = switch (revision) {
            case 4 -> 1000000000;
            case 3 -> 1080000;
            default -> 360000;
        };

        for (int row = 0; row < TileReactorCore.ROW_COUNT; row++) {
            for (int col = 0; col < TileReactorCore.COL_COUNT; col++) {
                int componentId = switch (revision) {
                    case 0, 1 -> storage.extract(38);
                    case 2 -> storage.extract(44);
                    case 3 -> storage.extract(58);
                    default -> storage.extract(72);
                };

                if (componentId != 0) {
                    var compConfig = new SimComponentConfig();
                    compConfig.hasAutomation = storage.extract(1) > 0;

                    if (compConfig.hasAutomation) {
                        compConfig.initialHeat = storage.extract(maxComponentHeat);

                        if (revision == 0 || (revision >= 1 && config.automated)) {
                            compConfig.replacementThreshold = storage.extract(maxComponentHeat);
                            compConfig.reactorPause = storage.extract(10000);
                        }
                    }

                    compConfig.stack = SimulationItems
                        .getSimulationItem(!legacy && componentId == 71 ? 73 : componentId);

                    if (compConfig.stack == null) {
                        throw new IllegalArgumentException("Unsupported planner component: " + componentId);
                    }

                    config.components[row * TileReactorCore.COL_COUNT + col] = compConfig;
                }
            }
        }

        config.initialHeat = storage.extract(120000);

        if (revision == 0 || (revision >= 1 && config.pulsed)) {
            config.onPulse = storage.extract(5000000);
            config.offPulse = storage.extract(5000000);
            config.suspendTemp = storage.extract(120000);
            config.resumeTemp = storage.extract(120000);
        }

        config.fluid = storage.extract(1) > 0;
        config.usingReactorCoolantInjectors = storage.extract(1) > 0;

        if (revision == 0) {
            config.pulsed = storage.extract(1) > 0;
            config.automated = storage.extract(1) > 0;
        }

        config.maxSimulationTicks = storage.extract(5000000);

        if (!storage.isEmpty()) {
            throw new IllegalArgumentException("Planner code contains unexpected trailing data");
        }

        config.validate();
        return config;
    }

    public static @Nonnull SimulationConfig read(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length < 0 || length > buffer.readableBytes() || length > 2_097_152) {
            throw new IllegalArgumentException("Invalid configuration payload length");
        }
        byte[] data = new byte[length];

        if (data.length == 0) {
            return new SimulationConfig();
        }

        buffer.readBytes(data);

        try {
            return SimulationConfig.load(SimulatorProtos.SimulationConfig.parseFrom(data));
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid configuration payload", e);
        }
    }

    public static void write(ByteBuf buffer, SimulationConfig result) {
        if (result == null) {
            buffer.writeInt(0);
        } else {
            var data = result.save()
                .toByteArray();
            buffer.writeInt(data.length);
            buffer.writeBytes(data);
        }
    }

    public SimulatorProtos.SimulationConfig save() {
        validate();
        var builder = SimulatorProtos.SimulationConfig.newBuilder()
            .setPulsed(pulsed)
            .setAutomated(automated)
            .setFluid(fluid)
            .setInitialHeat(initialHeat)
            .setOnPulse(onPulse)
            .setOffPulse(offPulse)
            .setSuspendTemp(suspendTemp)
            .setResumeTemp(resumeTemp)
            .setMaxSimulationTicks(maxSimulationTicks);

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            if (component == null) continue;
            Integer id = SimulationItems.getSimulationItemId(component.stack);
            if (id == null) {
                throw new IllegalArgumentException("Unsupported component in slot " + (i + 1));
            }
            builder.addComponents(
                SimulatorProtos.ComponentConfig.newBuilder()
                    .setIndex(i)
                    .setItem(id)
                    .setHasAutomation(component.hasAutomation)
                    .setInitialHeat(component.initialHeat)
                    .setReplacementThreshold(component.replacementThreshold)
                    .setReactorPause(component.reactorPause)
                    .setStackNbt(saveStackState(component.stack)));
        }
        return builder.build();
    }

    private static ByteString saveStackState(ItemStack stack) {
        NBTTagCompound state = stack.writeToNBT(new NBTTagCompound());
        // Runtime item IDs are not portable. The component catalog owns identity.
        state.removeTag("id");
        try {
            return ByteString.copyFrom(CompressedStreamTools.compress(state));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not save component state", e);
        }
    }

    private static ItemStack loadStackState(ItemStack template, ByteString data) {
        if (data.isEmpty()) return template;
        try {
            NBTTagCompound state = CompressedStreamTools
                .func_152457_a(data.toByteArray(), new NBTSizeTracker(2_097_152));
            int count = state.getByte("Count");
            int damage = state.getShort("Damage");
            if (count < 1 || damage < 0) throw new IllegalArgumentException("Invalid component stack state");
            ItemStack stack = new ItemStack(template.getItem(), count, damage);
            if (state.hasKey("tag", 10)) stack.setTagCompound(state.getCompoundTag("tag"));
            return stack;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Invalid component NBT", e);
        }
    }

    public static SimulationConfig load(SimulatorProtos.SimulationConfig data) {
        SimulationConfig config = new SimulationConfig();
        config.pulsed = data.getPulsed();
        config.automated = data.getAutomated();
        config.fluid = data.getFluid();
        config.initialHeat = data.getInitialHeat();
        config.onPulse = data.getOnPulse();
        config.offPulse = data.getOffPulse();
        config.suspendTemp = data.getSuspendTemp();
        config.resumeTemp = data.getResumeTemp();
        config.maxSimulationTicks = data.getMaxSimulationTicks();
        for (var component : data.getComponentsList()) {
            int index = component.getIndex();
            if (index < 0 || index >= config.components.length || config.components[index] != null) {
                throw new IllegalArgumentException("Invalid or duplicate component slot: " + index);
            }
            var comp = new SimComponentConfig();
            ItemStack template = SimulationItems.getSimulationItem(component.getItem());
            if (template == null) {
                throw new IllegalArgumentException("Unknown component ID: " + component.getItem());
            }
            comp.stack = loadStackState(template, component.getStackNbt());
            if (!SimulationItems.getSimulationItemId(template)
                .equals(SimulationItems.getSimulationItemId(comp.stack))) {
                throw new IllegalArgumentException("Component state changes its catalog identity");
            }
            comp.hasAutomation = component.getHasAutomation();
            comp.initialHeat = component.getInitialHeat();
            comp.replacementThreshold = component.getReplacementThreshold();
            comp.reactorPause = component.getReactorPause();
            config.components[index] = comp;
        }
        config.validate();
        return config;
    }

    private void validate() {
        if (components.length != TileReactorCore.COL_COUNT * TileReactorCore.ROW_COUNT || initialHeat < 0
            || onPulse < 0
            || offPulse < 0
            || suspendTemp < 0
            || resumeTemp < 0
            || maxSimulationTicks < 0
            || maxSimulationTicks > 5_000_000) {
            throw new IllegalArgumentException("Invalid reactor settings");
        }
        for (var component : components) {
            if (component != null && (component.stack == null || component.stack.stackSize < 1
                || component.stack.stackSize > 64
                || component.initialHeat < 0
                || component.replacementThreshold < 0
                || component.reactorPause < 0
                || component.reactorPause > 10_000)) {
                throw new IllegalArgumentException("Invalid component settings");
            }
            if (component != null && component.initialHeat > Simulator.maxComponentInitialHeat(component.stack)) {
                throw new IllegalArgumentException("Initial heat exceeds the component's capacity");
            }
        }
    }

    public void put(@NotNull SimulationConfig source) {
        this.pulsed = source.pulsed;
        this.automated = source.automated;
        this.fluid = source.fluid;
        this.usingReactorCoolantInjectors = source.usingReactorCoolantInjectors;
        this.initialHeat = source.initialHeat;
        this.onPulse = source.onPulse;
        this.offPulse = source.offPulse;
        this.suspendTemp = source.suspendTemp;
        this.resumeTemp = source.resumeTemp;
        this.maxSimulationTicks = source.maxSimulationTicks;

        for (int i = 0; i < source.components.length; i++) {
            this.components[i] = null;

            var c = source.components[i];

            if (c != null) {
                var dest = new SimComponentConfig();

                dest.stack = ItemStack.copyItemStack(c.stack);
                dest.hasAutomation = c.hasAutomation;
                dest.initialHeat = c.initialHeat;
                dest.replacementThreshold = c.replacementThreshold;
                dest.reactorPause = c.reactorPause;

                this.components[i] = dest;
            }
        }
    }

    @Override
    @Nullable
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        var item = components[slot];

        if (item == null || amount < 1) {
            return null;
        }

        if (!simulate) {
            components[slot] = null;
        }

        return withResults(item.stack.copy(), slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public int getSlots() {
        return components.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return extractItem(slot, 1, true);
    }

    @Override
    @Nullable
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (components[slot] != null || !isItemValid(slot, stack)) {
            return stack;
        }

        var result = stack.copy();
        var stored = result.splitStack(1);

        if (!simulate) {
            setStackInSlot(slot, stored);
        }

        if (result.stackSize <= 0) {
            return null;
        } else {
            return result;
        }
    }

    @Override
    public boolean isItemValid(int slot, @Nullable ItemStack stack) {
        return stack != null && stack.stackSize > 0
            && ComponentRegistry.isReactorItem(stack)
            && SimulationItems.getSimulationItemId(stack) != null;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (stack == null || !ComponentRegistry.isReactorItem(stack)) {
            components[slot] = null;
        } else {
            Integer id = SimulationItems.getSimulationItemId(stack);
            if (id == null) throw new IllegalArgumentException("Unsupported simulator component");
            if (components[slot] != null && id.equals(SimulationItems.getSimulationItemId(components[slot].stack))) {
                components[slot].stack = stack.copy();
                return;
            }
            var config = new SimComponentConfig();
            config.stack = stack.copy();

            components[slot] = config;
        }
    }

    private static void addResultLine(NBTTagList lines, String name, String unit, Object value) {
        var text = I18n.format(
            "nh_tooltip.sim.results." + name,
            unit != null
                ? I18n.format(value == null ? "nh_gui.sim.results.none" : ("nh_gui.sim.results." + unit), value)
                : value);

        for (var line : text.split("\\\\n")) {
            lines.appendTag(
                new NBTTagString(EnumChatFormatting.RESET.toString() + EnumChatFormatting.BLUE.toString() + line));
        }
    }

    private ItemStack withResults(ItemStack itemStack, int slot) {
        boolean hasResults = false;
        String format = EnumChatFormatting.RESET.toString() + EnumChatFormatting.BLUE.toString();

        var res = result;

        if (res != null) {
            var r = res.componentResults[slot];

            if (r != null) {
                var lore = new NBTTagList();

                lore.appendTag(new NBTTagString(""));

                lore.appendTag(new NBTTagString(format + I18n.format("nh_tooltip.results.title")));

                if (r.totalTempSecs > 0) {
                    addResultLine(lore, "avg_temp", "hu_total", r.totalTempSecs / res.simTime);
                    addResultLine(lore, "min_temp", "hu_total", r.minTemp);
                    addResultLine(lore, "max_temp", "hu_total", r.maxTemp);
                }

                if (r.totalHullCooling > 0) {
                    addResultLine(lore, "avg_hull_cooling", "hu_per_sec", r.totalHullCooling / res.simTime);
                }

                if (r.totalHullHeating > 0) {
                    addResultLine(lore, "avg_hull_heating", "hu_per_sec", r.totalHullHeating / res.simTime);
                    addResultLine(lore, "heating_per_item", "hu_total", r.totalHullHeating / (r.replaceCount + 1));
                }

                if (r.maxAirHeatingCapacity != null || r.totalAirHeating != 0) {
                    addResultLine(
                        lore,
                        "avg_heat_output",
                        "hu_per_sec",
                        res.simTime == 0 ? null : r.totalAirHeating / res.simTime);
                    if (r.maxAirHeatingCapacity != null) {
                        addResultLine(lore, "ideal_heat_output", "hu_per_sec", r.maxAirHeatingCapacity);
                    }
                }

                addResultLine(lore, "replace_count", null, r.replaceCount);

                if (r.totalEUOutput > 0) {
                    addResultLine(lore, "avg_power", "eu_per_tick", r.totalEUOutput / 20 / res.simTime);
                    addResultLine(lore, "power_per_item", "eu_total", r.totalEUOutput / (r.replaceCount + 1));
                }

                lore.appendTag(new NBTTagString(""));

                var display = new NBTTagCompound();
                display.setTag("Lore", lore);
                itemStack.setTagInfo("display", display);
                hasResults = true;
            }
        }

        if (!hasResults) {
            var lore = new NBTTagList();

            lore.appendTag(new NBTTagString(""));
            lore.appendTag(new NBTTagString(format + "No simulation results available."));
            lore.appendTag(new NBTTagString(""));

            var display = new NBTTagCompound();
            display.setTag("Lore", lore);
            itemStack.setTagInfo("display", display);
        }

        return itemStack;
    }

    public static class SimComponentConfig {

        public ItemStack stack;
        public boolean hasAutomation;
        public int initialHeat;
        public int replacementThreshold;
        public int reactorPause;
    }
}
