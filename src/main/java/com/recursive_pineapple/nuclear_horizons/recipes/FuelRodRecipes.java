package com.recursive_pineapple.nuclear_horizons.recipes;

import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.DEPLETED_MOX_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.DEPLETED_THORIUM_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.DEPLETED_URANIUM_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.ENRICHED_MOX_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.ENRICHED_THORIUM_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.ENRICHED_URANIUM_FUEL;
import static com.recursive_pineapple.nuclear_horizons.recipes.GTMats.REFINED_THORIUM;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.thermalCentrifugeRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.items.NHItemList;

import goodgenerator.loader.Loaders;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTUtility;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

public class FuelRodRecipes {

    public static void registerRecipes() {
        registerCanning();
        registerRecyclingRecipes();
    }

    public static void registerCanning() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                new ItemStack(Loaders.advancedFuelRod, 1),
                ENRICHED_URANIUM_FUEL.getDust(4))
            .itemOutputs(ItemList.RodUranium.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(2),
                new ItemStack(Loaders.advancedFuelRod, 2),
                ENRICHED_URANIUM_FUEL.getDust(8))
            .itemOutputs(ItemList.RodUranium2.get(1))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(4),
                new ItemStack(Loaders.advancedFuelRod, 4),
                ENRICHED_URANIUM_FUEL.getDust(16))
            .itemOutputs(ItemList.RodUranium4.get(1))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                NHItemList.EMPTY_FUEL_ROD_BASIC.get(1),
                ENRICHED_THORIUM_FUEL.getDust(4))
            .itemOutputs(ItemList.RodThorium.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(2),
                NHItemList.EMPTY_FUEL_ROD_BASIC.get(2),
                ENRICHED_THORIUM_FUEL.getDust(8))
            .itemOutputs(ItemList.RodThorium2.get(1))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(4),
                NHItemList.EMPTY_FUEL_ROD_BASIC.get(4),
                ENRICHED_THORIUM_FUEL.getDust(16))
            .itemOutputs(ItemList.RodThorium4.get(1))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(1),
                new ItemStack(Loaders.advancedFuelRod, 1),
                ENRICHED_MOX_FUEL.getDust(4))
            .itemOutputs(ItemList.RodMOX.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(2),
                new ItemStack(Loaders.advancedFuelRod, 2),
                ENRICHED_MOX_FUEL.getDust(8))
            .itemOutputs(ItemList.RodMOX2.get(1))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.getIntegratedCircuit(4),
                new ItemStack(Loaders.advancedFuelRod, 4),
                ENRICHED_MOX_FUEL.getDust(16))
            .itemOutputs(ItemList.RodMOX4.get(1))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(NHItemList.EMPTY_FUEL_ROD_BASIC.get(1), REFINED_THORIUM.getDust(2))
            .itemOutputs(NHItemList.THORIUM_BREEDER_ROD.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);
    }

    public static void registerRecyclingRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodUranium.get(1))
            .itemOutputs(DEPLETED_URANIUM_FUEL.getDust(4), new ItemStack(Loaders.advancedFuelRod, 1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodUranium2.get(1))
            .itemOutputs(DEPLETED_URANIUM_FUEL.getDust(8), new ItemStack(Loaders.advancedFuelRod, 2))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodUranium4.get(1))
            .itemOutputs(DEPLETED_URANIUM_FUEL.getDust(16), new ItemStack(Loaders.advancedFuelRod, 4))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodThorium.get(1))
            .itemOutputs(DEPLETED_THORIUM_FUEL.getDust(4), NHItemList.EMPTY_FUEL_ROD_BASIC.get(1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodThorium2.get(1))
            .itemOutputs(DEPLETED_THORIUM_FUEL.getDust(8), NHItemList.EMPTY_FUEL_ROD_BASIC.get(2))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodThorium4.get(1))
            .itemOutputs(DEPLETED_THORIUM_FUEL.getDust(16), NHItemList.EMPTY_FUEL_ROD_BASIC.get(4))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodMOX.get(1))
            .itemOutputs(DEPLETED_MOX_FUEL.getDust(4), new ItemStack(Loaders.advancedFuelRod, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodMOX2.get(1))
            .itemOutputs(DEPLETED_MOX_FUEL.getDust(8), new ItemStack(Loaders.advancedFuelRod, 2))
            .duration(40 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.DepletedRodMOX4.get(1))
            .itemOutputs(DEPLETED_MOX_FUEL.getDust(16), new ItemStack(Loaders.advancedFuelRod, 4))
            .duration(80 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(NHItemList.THORIUM_BREEDER_ROD_FINISHED.get(1))
            .itemOutputs(NHItemList.EMPTY_FUEL_ROD_BASIC.get(1), GregtechItemList.Protactinium233Dust.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(thermalCentrifugeRecipes);
    }
}
