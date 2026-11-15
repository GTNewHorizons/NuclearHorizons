package com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.items.HeatUtils;

public interface IHeatContainer {

    public default int getStoredHeat(@Nonnull ItemStack itemStack) {
        return itemStack.getItemDamage();
    }

    /** Capacity already used, including heat that cannot be transferred to a vent. */
    public default int getUsedHeatCapacity(@Nonnull ItemStack itemStack) {
        return getStoredHeat(itemStack);
    }

    /** Set the capacity used by a simulator input, independently of heat transfer. */
    public default void setUsedHeatCapacity(@Nonnull ItemStack itemStack, int usedHeatCapacity) {
        int rejected = addHeat(itemStack, usedHeatCapacity - getUsedHeatCapacity(itemStack));
        if (rejected != 0) {
            throw new IllegalArgumentException("Initial heat is outside the component's capacity: " + usedHeatCapacity);
        }
    }

    public default boolean canVentHeat(@Nonnull ItemStack itemStack) {
        return isConsumable(itemStack);
    }

    public default int getRemainingHealth(@Nonnull ItemStack itemStack) {
        if (!itemStack.isItemStackDamageable()) {
            return 1;
        }

        return itemStack.getMaxDamage() - itemStack.getItemDamage();
    }

    public default int addHeat(@Nonnull ItemStack itemStack, int heat) {
        if (!itemStack.isItemStackDamageable()) {
            return heat;
        }

        int stored = itemStack.getItemDamage();

        int consumed = HeatUtils.getConsumableHeat(itemStack.getMaxDamage(), stored, heat);

        itemStack.setItemDamage(stored + consumed);

        return heat - consumed;
    }

    public default int getMaxHeat(@Nonnull ItemStack itemStack) {
        return itemStack.getMaxDamage();
    }

    public boolean isConsumable(@Nonnull ItemStack itemStack);

    public @Nullable ItemStack getProduct(@Nonnull ItemStack itemStack);
}
