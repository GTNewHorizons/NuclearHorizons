package com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;

public interface IHeatMover extends IHeatContainer {

    public int getTransferFromReactor(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor);

    public int getTransferFromNeighbour(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor,
        @Nonnull IComponentAdapter neighbour);

    public int getTransferNeighbourToAir(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor,
        @Nonnull IComponentAdapter neighbour);

    public int getTransferToAir(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor);

    /** Ideal air cooling in HU/s, or null when this mover does not dissipate heat. */
    public @Nullable Long getMaxAirCooling(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor,
        int ventableNeighbours);

    @Override
    public default boolean isConsumable(@Nonnull ItemStack itemStack) {
        return true;
    }

    @Nullable
    public default ItemStack getProduct(@Nonnull ItemStack itemStack) {
        return null;
    }
}
