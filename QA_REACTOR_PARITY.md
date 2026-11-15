# Live reactor parity scenario

`ClientReactorParityTests.mixedComponentsMatchIc2AfterThreeCycles` runs in Horizon-QA PR #259's integrated client. It places NH and IC2 electric reactor cores side by side on a fresh stone platform. Each core has one chamber and the same 4-column component layout. Both receive redstone power after the components are loaded.

The layout occupies 23 of the 24 zero-based slots in the 4×6 grid.

| Slot | NH component | IC2 component |
| --- | --- | --- |
| (0,0) | GT uranium rod | Same GT uranium rod |
| (1,0) | NH lithium breeder cell | IC2 lithium cell |
| (2,0) | GT MOX rod | Same GT MOX rod |
| (3,0) | NH thick neutron reflector | IC2 thick neutron reflector |
| (0,1) | NH neutron reflector | IC2 neutron reflector |
| (1,1) | NH basic heat vent | IC2 heat vent |
| (2,1) | NH RSH condensator | IC2 condensator |
| (3,1) | NH reactor plating | IC2 reactor plating |
| (0,2) | NH 30k coolant cell | IC2 triple coolant cell |
| (1,2) | NH advanced heat exchanger | IC2 diamond heat exchanger |
| (2,2) | NH reactor heat vent | IC2 reactor heat vent |
| (3,2) | NH basic heat exchanger | IC2 heat exchanger |
| (0,3) | NH overclocked heat vent | IC2 gold heat vent |
| (1,3) | NH reactor heat exchanger | IC2 core heat exchanger |
| (2,3) | NH component heat exchanger | IC2 spread heat exchanger |
| (3,3) | NH heat reactor plating | IC2 heat reactor plating |
| (0,4) | NH component heat vent | IC2 spread heat vent |
| (1,4) | NH 10k coolant cell | IC2 coolant cell |
| (3,4) | NH explosive reactor plating | IC2 explosive reactor plating |
| (0,5) | NH 60k coolant cell | IC2 sixfold coolant cell |
| (1,5) | NH advanced heat vent | IC2 diamond heat vent |
| (2,5) | NH LZH condensator | IC2 lap condensator |
| (3,5) | GT thorium rod | Same GT thorium rod |

Both reactors start with 200 hull heat. Matched coolant cells, heat vents, and heat exchangers start with 80 heat where the component can store it. This gives the lower cooling network a real state to process. The scenario waits until the uranium rods have completed the same three or more active cycles. It compares EU per tick, hull heat, maximum hull heat, fuel wear, lithium charge, reflector wear, and every cooling component's observable heat or wear. The component heat vent has no internal wear, so its effect is checked through the adjacent 60k coolant cell. Fuel, lithium, reflector, coolant, vent, exchanger, and condensator families must change state. Plating must increase hull capacity. It then uses normal client input to open the NH reactor GUI.

The source and fixture live under `src/horizonQa`. Pass `-PhorizonQaJar=<path to the PR #259 dev JAR>` to compile and include them in a `runClient` launch. Without that property, the source set and its test-only dependencies are absent from the normal build. The scenario is not packaged in the mod JAR.

From a Horizon-QA PR #259 checkout, build its `*-dev.jar` for the checked-out commit. Then run its supervised launcher against this repository:

```powershell
.\gradlew.bat -p scripts/client-tests runClientTests `
    --project-root C:/path/to/NuclearHorizons `
    --client-task :runClient `
    --tests nuclear_horizons:ClientReactorParityTests.mixedComponentsMatchIc2AfterThreeCycles `
    --horizon-qa-jar C:/path/to/horizonqa-dev.jar `
    --timeout-seconds 600
```

The launcher creates a new scratch world and report directory under the NH `build/client-tests` directory. Check `launch-result.json`, `horizonqa-result.json`, and `TEST-horizonqa.xml` there. An infrastructure failure may leave no Horizon-QA result files, in which case `launch.log` records where Minecraft stopped.
