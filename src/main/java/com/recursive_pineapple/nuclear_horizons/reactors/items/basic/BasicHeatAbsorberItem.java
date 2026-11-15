package com.recursive_pineapple.nuclear_horizons.reactors.items.basic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;
import com.recursive_pineapple.nuclear_horizons.reactors.components.adapters.HeatAbsorberAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces.IHeatContainer;

public class BasicHeatAbsorberItem extends ReactorItem implements IHeatContainer {

    private final boolean consumable;

    private ItemStack product;

    public BasicHeatAbsorberItem(String name, String textureName, int maxHeat, boolean consumable) {
        super(name, textureName, "heat", maxHeat);

        this.consumable = consumable;
    }

    @Override
    public int getStoredHeat(@Nonnull ItemStack itemStack) {
        // Condensator damage is spent absorption capacity, not heat available for cooling.
        return consumable ? IHeatContainer.super.getStoredHeat(itemStack) : 0;
    }

    @Override
    public int getUsedHeatCapacity(@Nonnull ItemStack itemStack) {
        return itemStack.getItemDamage();
    }

    @Override
    public void setUsedHeatCapacity(@Nonnull ItemStack itemStack, int usedHeatCapacity) {
        if (usedHeatCapacity < 0 || usedHeatCapacity > getMaxHeat(itemStack)) {
            throw new IllegalArgumentException("Initial heat is outside the component's capacity: " + usedHeatCapacity);
        }
        itemStack.setItemDamage(usedHeatCapacity);
    }

    public void setProduct(ItemStack product) {
        this.product = product;
    }

    @Override
    public @Nonnull IComponentAdapter getAdapter(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor, int x,
        int y) {
        return new HeatAbsorberAdapter(reactor, x, y, itemStack, this);
    }

    @Override
    public boolean isConsumable(@Nonnull ItemStack itemStack) {
        return consumable;
    }

    @Override
    public @Nullable ItemStack getProduct(@Nonnull ItemStack itemStack) {
        return product;
    }

    @Override
    protected String getDurabilityTooltip(ItemStack itemStack) {
        return I18n.format("nh_tooltip.stored_heat", itemStack.getItemDamage(), itemStack.getMaxDamage());
    }

    @Override
    protected boolean hasMoreInfo(ItemStack itemStack) {
        return false;
    }
}
