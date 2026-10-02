package com.recursive_pineapple.nuclear_horizons.reactors.tile;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.recursive_pineapple.nuclear_horizons.reactors.fluids.CoolantRegistry;

public class TileFluidPort extends TileEntity implements IFluidHandler, IReactorBlock, IInventory {

    public int reactorRelX, reactorRelY, reactorRelZ;
    private ItemStack recoverySlot;

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
    public Packet getDescriptionPacket() {
        var data = new NBTTagCompound();

        data.setInteger("reactorRelX", reactorRelX);
        data.setInteger("reactorRelY", reactorRelY);
        data.setInteger("reactorRelZ", reactorRelZ);

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, data);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        var data = pkt.func_148857_g();

        this.reactorRelX = data.getInteger("reactorRelX");
        this.reactorRelY = data.getInteger("reactorRelY");
        this.reactorRelZ = data.getInteger("reactorRelZ");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("version", 1);

        compound.setInteger("reactorRelX", this.reactorRelX);
        compound.setInteger("reactorRelY", this.reactorRelY);
        compound.setInteger("reactorRelZ", this.reactorRelZ);

        if (this.recoverySlot != null) {
            compound.setTag("recoverySlot", this.recoverySlot.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        switch (compound.getInteger("version")) {
            case 1: {
                this.reactorRelX = compound.getInteger("reactorRelX");
                this.reactorRelY = compound.getInteger("reactorRelY");
                this.reactorRelZ = compound.getInteger("reactorRelZ");

                if (compound.hasKey("recoverySlot", 10)) {
                    this.recoverySlot = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("recoverySlot"));
                } else {
                    this.recoverySlot = null;
                }
                break;
            }
        }
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        var reactor = this.getReactor();

        if (reactor == null) {
            return 0;
        }

        // this should only be called a few times a second, so we don't need to cache it
        if (!CoolantRegistry.isColdCoolant(resource.getFluid())) {
            return 0;
        }

        var tank = reactor.coolantTank;

        if (tank.getFluid() == null || resource.getFluid() == tank.getFluid()
            .getFluid()) {
            return tank.fill(resource, doFill);
        } else {
            return 0;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        var reactor = this.getReactor();

        if (reactor == null) {
            return null;
        }

        var tank = reactor.hotCoolantTank;

        if (tank.getFluid() != null && resource.getFluid() == tank.getFluid()
            .getFluid()) {
            return tank.drain(resource.amount, doDrain);
        } else {
            return null;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        var reactor = this.getReactor();

        if (reactor == null) {
            return null;
        }

        var tank = reactor.hotCoolantTank;

        if (tank.getFluid() != null) {
            return tank.drain(maxDrain, doDrain);
        } else {
            return null;
        }
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return CoolantRegistry.isColdCoolant(fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return CoolantRegistry.isHotCoolant(fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        var reactor = this.getReactor();

        if (reactor == null) {
            return new FluidTankInfo[0];
        }

        return new FluidTankInfo[] { reactor.coolantTank.getInfo(), reactor.hotCoolantTank.getInfo() };
    }

    public ItemStack dropRecoverySlot() {
        ItemStack item = this.recoverySlot;
        this.recoverySlot = null;
        return item;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index == 0 ? this.recoverySlot : null;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index != 0 || this.recoverySlot == null) return null;

        this.markDirty();

        int consumed = Math.min(this.recoverySlot.stackSize, count);
        ItemStack out = this.recoverySlot.splitStack(consumed);

        if (this.recoverySlot.stackSize <= 0) {
            this.recoverySlot = null;
        }

        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        if (index != 0) return null;

        ItemStack item = this.recoverySlot;
        this.recoverySlot = null;
        this.markDirty();
        return item;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index != 0) return;

        this.recoverySlot = stack;

        if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
            stack.stackSize = this.getInventoryStackLimit();
        }

        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "reactor_fluid_port";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }
}
