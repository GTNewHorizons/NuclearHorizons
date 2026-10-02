package com.recursive_pineapple.nuclear_horizons.reactors.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileFluidPort;

public class ReactorFluidPort extends BlockContainer {

    public ReactorFluidPort() {
        super(Material.rock);
        setBlockName(BlockList.REACTOR_FLUID_PORT_NAME);
        setBlockTextureName("nuclear_horizons:fluid_port");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFluidPort();
    }

    @Override
    public void onBlockPreDestroy(World worldIn, int x, int y, int z, int meta) {
        if (worldIn.isRemote) return;
        if (!(worldIn.getTileEntity(x, y, z) instanceof TileFluidPort port)) return;

        ItemStack item = port.dropRecoverySlot();
        if (item != null) {
            worldIn.spawnEntityInWorld(new EntityItem(worldIn, x + 0.5, y + 0.5, z + 0.5, item));
        }
    }
}
