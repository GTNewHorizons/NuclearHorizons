package com.recursive_pineapple.nuclear_horizons.reactors.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class HeatUtils {

    private HeatUtils() {}

    public static int getConsumableHeat(int maxHeat, int currentHeat, int addedHeat) {
        // if we have 50 heat and we subtract 25, we should be able to consume 25 heat
        // 50 stored - 75 removed = 50 removed
        // 25 out of 50 + 50 added = 25 added

        if (addedHeat > 0) {
            int remaining = maxHeat - currentHeat;

            return Math.min(addedHeat, remaining);
        } else {
            int remaining = currentHeat;

            return -Math.min(-addedHeat, remaining);
        }
    }

    public static int getNBTInt(ItemStack stack, String name, int def) {
        NBTTagCompound tag = stack.getTagCompound();

        return tag == null ? def : tag.getInteger(name);
    }

    public static void setNBTInt(ItemStack stack, String name, int value) {
        getOrCreateTag(stack).setInteger(name, value);
    }

    public static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();

        if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());

        return tag;
    }
}
