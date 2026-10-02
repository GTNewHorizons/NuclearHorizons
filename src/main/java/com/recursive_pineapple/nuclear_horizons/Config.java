package com.recursive_pineapple.nuclear_horizons;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static double ROD_EU_MULTIPLIER = 1;
    public static double ROD_HU_MULTIPLIER = 1;
    public static double MOX_EU_COEFFICIENT = 4;
    public static double NUCLEAR_ENERGY_GENERATION_MULTIPLIER = 5.0;
    public static double FLUID_REACTOR_OUTPUT_MODIFIER = 1.0;
    public static double REACTOR_EXPLOSION_POWER_LIMIT = 100.0;
    public static int REACTOR_BASE_MAX_HEAT = 10_000;
    public static int REACTOR_EU_MULTIPLIER = 500;
    public static int FLUID_NUKE_HU_MULTIPLIER = 2;
    // 1 HU converts this/160 mb of distilled water to this mb of steam
    // ex: 600 HU/s converts 1200mb/s of distilled water to 192000mb/s of steam
    // BWRs are limited by the size of the reactor output buffer and this value
    // which currently puts maximum power at just over 600 HU/s
    public static int BWR_STEAM_PER_HU_MULTIPLIER = 320;
    public static int COOLANT_SPECIFIC_HEAT = 1;
    public static int LEAD_SPECIFIC_HEAT = 8;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        // spotless:off
        NUCLEAR_ENERGY_GENERATION_MULTIPLIER = configuration.get(
            "balance.energy.generator",
            "nuclear",
            5.0,
            "IC2 [balance / energy / generator] nuclear. Base EU output is 5 EU/t times this value.")
            .getDouble(5.0);
        REACTOR_EU_MULTIPLIER = (int) Math.round(100 * NUCLEAR_ENERGY_GENERATION_MULTIPLIER);

        FLUID_REACTOR_OUTPUT_MODIFIER = configuration.get(
            "balance.energy.FluidReactor",
            "outputModifier",
            1.0,
            "IC2 [balance / energy / FluidReactor] outputModifier. Base HU output is 2 hU per reactor heat.")
            .getDouble(1.0);
        FLUID_NUKE_HU_MULTIPLIER = (int) Math.round(2 * FLUID_REACTOR_OUTPUT_MODIFIER);

        REACTOR_EXPLOSION_POWER_LIMIT = configuration.get(
            "protection",
            "reactorExplosionPowerLimit",
            100.0,
            "IC2 [protection] reactorExplosionPowerLimit. Set <= 0 to disable reactor heat damage.")
            .getDouble(100.0);

        REACTOR_BASE_MAX_HEAT = configuration.get(
            "balance.reactor",
            "baseMaxHeat",
            10_000,
            "Base reactor hull heat. IC2 default is 10000.")
            .getInt(10_000);

        // spotless:on

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
