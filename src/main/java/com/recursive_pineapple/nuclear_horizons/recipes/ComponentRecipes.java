package com.recursive_pineapple.nuclear_horizons.recipes;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.cannerRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFreezerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;

import java.util.Objects;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import ic2.core.Ic2Items;

public class ComponentRecipes {

    public static void registerRecipes() {
        registerComponentRecipes();
        registerCoolingRecipes();
        registerReflectorRecipes();
        registerCondensatorRecipes();
        registerCraftingRecipes();
        registerReactorRecipes();
    }

    private static void registerComponentRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.Silver.getPlates(2),
                Materials.Copper.getPlates(1),
                Materials.Aluminium.getPlates(2),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 1L))
            .itemOutputs(NHItemList.BASIC_HEAT_EXCHANGER.get(1))
            .duration(3 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.Silver.getPlates(2),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Copper, 2),
                NHItemList.BASIC_HEAT_EXCHANGER.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.REACTOR_HEAT_EXCHANGER.get(1))
            .duration(3 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.Lapis.getPlates(2),
                Materials.Diamond.getPlates(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2L),
                NHItemList.BASIC_HEAT_EXCHANGER.get(1),
                NHItemList.BASIC_HEAT_EXCHANGER.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.ADVANCED_HEAT_EXCHANGER.get(1))
            .duration(3 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.Gold.getPlates(2),
                NHItemList.BASIC_HEAT_EXCHANGER.get(1))
            .fluidInputs(Materials.StainlessSteel.getMolten(72))
            .itemOutputs(NHItemList.COMPONENT_HEAT_EXCHANGER.get(1))
            .duration(SECONDS + 10 * TICKS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.Aluminium.getPlates(2),
                ItemList.Electric_Motor_LV.get(1L),
                new ItemStack(Blocks.iron_bars, 2))
            .fluidInputs()
            .itemOutputs(NHItemList.BASIC_HEAT_VENT.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(22),
                Materials.Silver.getPlates(2),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Copper, 2),
                NHItemList.BASIC_HEAT_VENT.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.REACTOR_HEAT_VENT.get(1))
            .duration(15 * SECONDS)
            .eut(256)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Ic2Items.industrialDiamond,
                NHItemList.BASIC_HEAT_VENT.get(1),
                requiredCoreItem("StainlessSteelBars", 4))
            .fluidInputs()
            .itemOutputs(NHItemList.ADVANCED_HEAT_VENT.get(1))
            .duration(15 * SECONDS)
            .eut(256)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Tin, 2),
                requiredCoreItem("SteelBars", 2),
                NHItemList.BASIC_HEAT_VENT.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.COMPONENT_HEAT_VENT.get(1))
            .duration(15 * SECONDS)
            .eut(256)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(22),
                Materials.Gold.getPlates(2),
                NHItemList.ADVANCED_HEAT_VENT.get(1))
            .fluidInputs(Materials.StainlessSteel.getMolten(72))
            .itemOutputs(NHItemList.OVERCLOCKED_HEAT_VENT.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(22),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 1),
                new ItemStack(Ic2Items.advancedAlloy.getItem(), 4))
            .fluidInputs()
            .itemOutputs(NHItemList.REACTOR_PLATING.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Copper, 2),
                Materials.Copper.getPlates(1),
                Materials.Silver.getPlates(1),
                NHItemList.REACTOR_PLATING.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.REACTOR_PLATING_HEAT.get(1))
            .duration(30 * SECONDS)
            .eut(256)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(23),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 2),
                new ItemStack(Ic2Items.advancedAlloy.getItem(), 2),
                NHItemList.REACTOR_PLATING.get(1))
            .fluidInputs()
            .itemOutputs(NHItemList.REACTOR_PLATING_EXPLOSIVE.get(1))
            .duration(30 * SECONDS)
            .eut(256)
            .addTo(assemblerRecipes);
    }

    private static void registerCoolingRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(requiredCoreItem("TenKCell", 1))
            .fluidInputs(GTMats.COOLANT.getFluid(1000))
            .itemOutputs(NHItemList.COOLANT_CELL_10k.get(1))
            .duration(16)
            .eut(1)
            .addTo(cannerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(requiredCoreItem("ThirtyKCell", 1))
            .fluidInputs(GTMats.COOLANT.getFluid(3000))
            .itemOutputs(NHItemList.COOLANT_CELL_30k.get(1))
            .duration(48)
            .eut(1)
            .addTo(cannerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(requiredCoreItem("SixtyKCell", 1))
            .fluidInputs(GTMats.COOLANT.getFluid(6000))
            .itemOutputs(NHItemList.COOLANT_CELL_60k.get(1))
            .duration(96)
            .eut(1)
            .addTo(cannerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(NHItemList.COOLANT_CELL_10k.getItem(), 1, OreDictionary.WILDCARD_VALUE))
            .itemOutputs(NHItemList.COOLANT_CELL_10k.get(1))
            .duration(10)
            .eut(TierEU.RECIPE_MV)
            .addTo(vacuumFreezerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(NHItemList.COOLANT_CELL_30k.getItem(), 1, OreDictionary.WILDCARD_VALUE))
            .itemOutputs(NHItemList.COOLANT_CELL_30k.get(1))
            .duration(30)
            .eut(TierEU.RECIPE_MV)
            .addTo(vacuumFreezerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(NHItemList.COOLANT_CELL_60k.getItem(), 1, OreDictionary.WILDCARD_VALUE))
            .itemOutputs(NHItemList.COOLANT_CELL_60k.get(1))
            .duration(60)
            .eut(TierEU.RECIPE_MV)
            .addTo(vacuumFreezerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                NHItemList.REACTOR_HEAT_EXCHANGER.get(1),
                ItemList.Electric_Pump_HV.get(1L),
                GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.StainlessSteel, 2L),
                Materials.Copper.getPlates(2))
            .itemOutputs(NHItemList.COOLANT_HEAT_EXCHANGER_1.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                NHItemList.COOLANT_HEAT_EXCHANGER_1.get(3),
                ItemList.Electric_Pump_EV.get(1L),
                Materials.TungstenSteel.getPlates(2))
            .itemOutputs(NHItemList.COOLANT_HEAT_EXCHANGER_2.get(1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                NHItemList.COOLANT_HEAT_EXCHANGER_2.get(3),
                ItemList.Electric_Pump_IV.get(1L))
            .fluidInputs(Materials.Naquadria.getMolten(144))
            .itemOutputs(NHItemList.COOLANT_HEAT_EXCHANGER_3.get(1))
            .duration(45 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(22),
                NHItemList.REACTOR_PLATING_HEAT.get(4),
                Materials.Naquadah.getPlates(2))
            .itemOutputs(NHItemList.REACTOR_PLATING_HEAT_2.get(1))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);
    }

    private static void registerReflectorRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Copper, 1L),
                Materials.Tin.getPlates(4),
                GTOreDictUnificator.get(OrePrefixes.plateAlloy, Materials.Carbon, 2L),
                Materials.Graphite.getDust(16))
            .itemOutputs(NHItemList.NEUTRON_REFLECTOR.get(1))
            .requiresCleanRoom()
            .duration(60 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Beryllium, 1L),
                Materials.Tin.getPlates(4),
                GTOreDictUnificator.get(OrePrefixes.plateAlloy, Materials.Carbon, 2L),
                Materials.Graphite.getDust(8))
            .itemOutputs(NHItemList.NEUTRON_REFLECTOR.get(1))
            .requiresCleanRoom()
            .duration(45 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.TungstenCarbide, 1L),
                Materials.Tin.getPlates(4),
                GTOreDictUnificator.get(OrePrefixes.plateAlloy, Materials.Carbon, 2L),
                Materials.Graphite.getDust(4))
            .itemOutputs(NHItemList.NEUTRON_REFLECTOR.get(1))
            .requiresCleanRoom()
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Beryllium, 2L),
                NHItemList.NEUTRON_REFLECTOR.get(4))
            .itemOutputs(NHItemList.THICK_NEUTRON_REFLECTOR.get(1))
            .requiresCleanRoom()
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.TungstenCarbide, 2L),
                NHItemList.NEUTRON_REFLECTOR.get(4))
            .itemOutputs(NHItemList.THICK_NEUTRON_REFLECTOR.get(1))
            .requiresCleanRoom()
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);
    }

    private static void registerCondensatorRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                Materials.RedAlloy.getPlates(4),
                NHItemList.REACTOR_HEAT_EXCHANGER.get(2),
                NHItemList.REACTOR_HEAT_VENT.get(1))
            .itemOutputs(NHItemList.RSH_CONDENSATOR.get(1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lapis, 1L),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.RedAlloy, 2L),
                NHItemList.OVERCLOCKED_HEAT_VENT.get(1),
                NHItemList.COMPONENT_HEAT_EXCHANGER.get(1),
                NHItemList.RSH_CONDENSATOR.get(2))
            .itemOutputs(NHItemList.LZH_CONDENSATOR.get(1))
            .duration(45 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        registerCondensatorRefill(NHItemList.RSH_CONDENSATOR, "dustRedAlloy");
        registerCondensatorRefill(NHItemList.LZH_CONDENSATOR, "dustLapis");
    }

    private static void registerCondensatorRefill(NHItemList condensator, String dust) {
        ItemStack depleted = condensator.get(1);
        depleted.setItemDamage(OreDictionary.WILDCARD_VALUE);
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(condensator.get(1), depleted, dust, dust, dust, dust, dust, dust, dust, dust));
    }

    private static void registerCraftingRecipes() {
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.REACTOR_PLATING.get(1),
                "AAA",
                "ABA",
                "AAA",
                'A',
                "plateAlloyAdvanced",
                'B',
                "plateDenseLead"));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.REACTOR_PLATING_HEAT.get(1),
                "ABA",
                "CDC",
                "ABA",
                'A',
                "plateDenseCopper",
                'B',
                "plateCopper",
                'C',
                "plateSilver",
                'D',
                NHItemList.REACTOR_PLATING.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.REACTOR_PLATING_EXPLOSIVE.get(1),
                "ABA",
                "BDB",
                "ABA",
                'A',
                "plateDenseLead",
                'B',
                "plateAlloyAdvanced",
                'D',
                NHItemList.REACTOR_PLATING.get(1)));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.BASIC_HEAT_VENT.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                "plateAluminium",
                'B',
                new ItemStack(Blocks.iron_bars),
                'C',
                ItemList.Electric_Motor_LV.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.REACTOR_HEAT_VENT.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                "plateDoubleCopper",
                'B',
                "plateSilver",
                'C',
                NHItemList.BASIC_HEAT_VENT.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.ADVANCED_HEAT_VENT.get(1),
                "ABA",
                "ACA",
                "ABA",
                'A',
                requiredCoreItem("StainlessSteelBars", 1),
                'B',
                NHItemList.BASIC_HEAT_VENT.get(1),
                'C',
                Ic2Items.industrialDiamond));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.OVERCLOCKED_HEAT_VENT.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                "screwStainlessSteel",
                'B',
                "plateGold",
                'C',
                NHItemList.ADVANCED_HEAT_VENT.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.COMPONENT_HEAT_VENT.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                requiredCoreItem("SteelBars", 1),
                'B',
                "plateDenseTin",
                'C',
                NHItemList.BASIC_HEAT_VENT.get(1)));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.BASIC_HEAT_EXCHANGER.get(1),
                "ABA",
                "CDC",
                "ACA",
                'A',
                "plateSilver",
                'B',
                "circuitAdvanced",
                'C',
                "plateAluminium",
                'D',
                "plateCopper"));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.REACTOR_HEAT_EXCHANGER.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                "plateDoubleCopper",
                'B',
                "plateSilver",
                'C',
                NHItemList.BASIC_HEAT_EXCHANGER.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.COMPONENT_HEAT_EXCHANGER.get(1),
                "ABA",
                "BCB",
                "ABA",
                'A',
                "screwStainlessSteel",
                'B',
                "plateGold",
                'C',
                NHItemList.BASIC_HEAT_EXCHANGER.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.ADVANCED_HEAT_EXCHANGER.get(1),
                "ABA",
                "CDC",
                "ABA",
                'A',
                "plateLapis",
                'B',
                "circuitData",
                'C',
                NHItemList.BASIC_HEAT_EXCHANGER.get(1),
                'D',
                "plateDiamond"));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.RSH_CONDENSATOR.get(1),
                "ABA",
                "ACA",
                "ABA",
                'A',
                "plateRedAlloy",
                'B',
                NHItemList.REACTOR_HEAT_EXCHANGER.get(1),
                'C',
                NHItemList.REACTOR_HEAT_VENT.get(1)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                NHItemList.LZH_CONDENSATOR.get(1),
                "ABA",
                "CDC",
                "AEA",
                'A',
                "plateDoubleRedAlloy",
                'B',
                NHItemList.OVERCLOCKED_HEAT_VENT.get(1),
                'C',
                NHItemList.RSH_CONDENSATOR.get(1),
                'D',
                "plateDenseLapis",
                'E',
                NHItemList.COMPONENT_HEAT_EXCHANGER.get(1)));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(BlockList.REACTOR_SIMULATOR),
                " S ",
                "CRC",
                " T ",
                'S',
                ItemList.Cover_Screen.get(1L),
                'C',
                ItemList.Sensor_HV.get(1L),
                'R',
                new ItemStack(BlockList.REACTOR_CHAMBER),
                'T',
                new ItemStack(Blocks.crafting_table)));
    }

    private static ItemStack requiredCoreItem(String name, int amount) {
        return Objects.requireNonNull(
            GTModHandler.getModItem("dreamcraft", name, amount),
            "Missing required recipe ingredient dreamcraft:" + name);
    }

    private static void registerReactorRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(22),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 2),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Titanium, 2),
                new ItemStack(BlockList.REACTOR_CHAMBER, 3),
                GTOreDictUnificator.get(OrePrefixes.cableGt08, Materials.Platinum, 1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 1L))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_CORE, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(21),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 2),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Titanium, 2),
                new ItemStack(Ic2Items.advancedAlloy.getItem(), 2),
                Ic2Items.advancedMachine)
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_CHAMBER, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.Block_ReinforcedConcrete.get(1L), Materials.Lead.getPlates(2))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.PRESSURE_VESSEL, 1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BlockList.PRESSURE_VESSEL, 1), ItemList.Electric_Pump_HV.get(1L))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_FLUID_PORT, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BlockList.PRESSURE_VESSEL, 1), ItemList.Conveyor_Module_HV.get(1L))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_ACCESS_HATCH, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BlockList.PRESSURE_VESSEL, 1), ItemList.Cover_ActivityDetector.get(1L))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_REDSTONE_PORT, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        // based on nuclear control thermal monitor recipe (the non-wireless one)
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.ReinforcedGlass.get(3L),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 2),
                ItemList.Cover_ActivityDetector.get(1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 2L),
                ItemList.Cover_Screen.get(1L))
            .fluidInputs()
            .itemOutputs(new ItemStack(BlockList.REACTOR_THERMAL_SENSOR, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Cover_Screen.get(1L),
                ItemList.Sensor_HV.get(2L),
                new ItemStack(BlockList.REACTOR_CHAMBER),
                new ItemStack(Blocks.crafting_table))
            .itemOutputs(new ItemStack(BlockList.REACTOR_SIMULATOR))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(assemblerRecipes);

    }
}
