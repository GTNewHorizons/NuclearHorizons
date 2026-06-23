package com.recursive_pineapple.nuclear_horizons.reactors.tile;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;

public class TileThermalSensor extends TileEntity implements IGuiHolder<PosGuiData>, IReactorBlock {

    public int reactorRelX, reactorRelY, reactorRelZ;

    private int threshold;

    private ThermalSensorOp op = ThermalSensorOp.GTE;

    private Boolean wasActive = null;

    private enum ThermalSensorOp {

        LT,
        LTE,
        GT,
        GTE;

        public String getDisplayString() {
            return switch (this) {
                case LT -> "<";
                case LTE -> "<=";
                case GT -> ">";
                case GTE -> ">=";
            };
        }

        public String getComparisonPhrase() {
            return switch (this) {
                case LT -> "less than";
                case LTE -> "less than or equal to";
                case GT -> "greater than";
                case GTE -> "greater than or equal to";
            };
        }

        public boolean test(int threshold, int temp) {
            return switch (this) {
                case GT -> temp > threshold;
                case GTE -> temp >= threshold;
                case LT -> temp < threshold;
                case LTE -> temp <= threshold;
            };
        }
    }

    @Override
    public @Nullable TileReactorCore getReactor() {
        // spotless:off
        if (worldObj.getTileEntity(xCoord + reactorRelX, yCoord + reactorRelY, zCoord + reactorRelZ) instanceof TileReactorCore reactor) {
            return reactor;
        } else {
            return null;
        }
        // spotless:on
    }

    @Override
    public void setReactor(TileReactorCore reactor) {
        if (getReactor() != reactor) {
            this.reactorRelX = reactor == null ? 0 : reactor.xCoord - xCoord;
            this.reactorRelY = reactor == null ? 0 : reactor.yCoord - yCoord;
            this.reactorRelZ = reactor == null ? 0 : reactor.zCoord - zCoord;
            this.markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void onHeatTick(IReactorGrid reactor) {
        var shouldBeActive = op.test(threshold, reactor.getHullHeat());

        if (wasActive == null || shouldBeActive != wasActive) {
            wasActive = shouldBeActive;
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
        }
    }

    private void onSettingChanged() {
        this.markDirty();
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);

            TileReactorCore reactor = getReactor();
            if (reactor != null) {
                onHeatTick(reactor);
            }
        }
    }

    public boolean isActive() {
        return wasActive == null ? false : wasActive;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("version", 1);

        compound.setInteger("reactorRelX", this.reactorRelX);
        compound.setInteger("reactorRelY", this.reactorRelY);
        compound.setInteger("reactorRelZ", this.reactorRelZ);

        compound.setInteger("threshold", this.threshold);
        compound.setInteger("reactorRelZ", this.reactorRelZ);
        compound.setString("op", op.name());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        switch (compound.getInteger("version")) {
            case 1: {
                this.reactorRelX = compound.getInteger("reactorRelX");
                this.reactorRelY = compound.getInteger("reactorRelY");
                this.reactorRelZ = compound.getInteger("reactorRelZ");
                this.threshold = compound.getInteger("threshold");
                this.op = ThermalSensorOp.valueOf(compound.getString("op"));
                break;
            }
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        var data = new NBTTagCompound();

        data.setInteger("reactorRelX", reactorRelX);
        data.setInteger("reactorRelY", reactorRelY);
        data.setInteger("reactorRelZ", reactorRelZ);
        data.setInteger("threshold", threshold);
        data.setString("op", op.name());

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, data);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        var data = pkt.func_148857_g();

        this.reactorRelX = data.getInteger("reactorRelX");
        this.reactorRelY = data.getInteger("reactorRelY");
        this.reactorRelZ = data.getInteger("reactorRelZ");
        this.threshold = data.getInteger("threshold");
        this.op = ThermalSensorOp.valueOf(data.getString("op"));
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        EnumSyncValue<ThermalSensorOp, ?> opValue = new EnumSyncValue<>(ThermalSensorOp.class, () -> this.op, v -> {
            this.op = v;
            onSettingChanged();
        }).allowC2S();

        IntSyncValue thresholdValue = new IntSyncValue(() -> this.threshold, v -> {
            this.threshold = v;
            onSettingChanged();
        }).allowC2S();

        CycleButtonWidget opButton = new CycleButtonWidget().value(opValue)
            .size(24, 16)
            .pos(8, 24);

        for (ThermalSensorOp value : ThermalSensorOp.values()) {
            opButton.stateOverlay(value.ordinal(), IKey.str(value.getDisplayString()));
            opButton.addTooltip(
                value.ordinal(),
                "Emit a redstone signal when reactor temperature is " + value.getComparisonPhrase() + " the threshold");
        }

        return ModularPanel.defaultPanel("thermal_sensor", 144, 48)
            .child(new TextWidget<>(IKey.lang("tile.reactor_thermal_sensor.name")).pos(8, 8))
            .child(opButton)
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .numbersInt(0, Integer.MAX_VALUE)
                    .value(thresholdValue)
                    .size(96, 16)
                    .pos(40, 24));
    }
}
