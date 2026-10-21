# Superb Warfare Performance Fix (Unofficial Patch)

A companion patch for [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) 0.8.9.2 on Minecraft 1.20.1 (Forge). It removes most of the time Superb Warfare spends working out what you are looking at, which grows with the number of mobs around you. Superb Warfare itself is still required.

## Before And After

Looking over the horde: **39 FPS** before, **102 FPS** after.

![Before, looking over the horde](curseforge/images/Before2.png)
![After, looking over the horde](curseforge/images/After2.png)

Looking at a zombie: **50 FPS** before, **103 FPS** after.

![Before, aiming at a zombie](curseforge/images/Before1.png)
![After, aiming at a zombie](curseforge/images/After1.png)

## Faster

- Every tick, whether or not you hold anything from Superb Warfare, it checks each mob along your view for a smoke grenade cloud nearby. Each check went through every mob within 8 blocks, so in a crowd the cost grew with the square of its size: 22% of the render thread in a 515-mob scene. The check now looks only at smoke clouds and gives the same answers.

## Bug Fixes

- With thermal imaging on and playerAnimator installed, a large white arm could stretch across the view. Superb Warfare draws the first-person arm a second time, white, in thermal, and sets the player model up again for it. playerAnimator keeps third-person animations off first-person arms by marking the model just before vanilla's setup, and Superb Warfare's second setup came after that mark was used up. So once a third-person animation was playing (TaCZ starts its gun poses whenever your whole body is drawn holding a gun, in the inventory or in F5), it posed the white arm. The patch marks the model again right before Superb Warfare's setup, the same way playerAnimator does, so the white arm sits where the normal arm is, as it does without playerAnimator. Nothing changes with thermal off or without playerAnimator.

## Option

- `hideAmmoCountOverlay` in `config/superbwarfareperformancefix-client.toml` hides the ammo count overlay shown while you hold ammo or an ammo box. Off by default.

Apart from that option and the thermal arm fix, everything looks and plays the same.

The log has one line for each patched class, saying how many changes applied. If a Superb Warfare version changes the smoke check or the thermal arm, the patch leaves that part alone and says so in the log. Without playerAnimator, the log says the thermal arm needs no fix.

## Building

Put Superb Warfare 0.8.9.2 (`superbwarfare-0.8.9.2-mc1.20.1-65d985a64-all.jar` from CurseForge) in `libs/` as `superbwarfare-0.8.9.2.jar`, and playerAnimator (`player-animation-lib-forge-1.0.2-rc1+1.20.jar`) in `libs/` as it is, then:

- `gradlew build` makes the mod in `build/libs/`.
- `gradlew verifySmoke` checks that the smoke check gives the same results before and after the patch, on generated worlds, and that Superb Warfare's smoke check is still the one the patch was written for.
- `gradlew verifyThermal` checks that Superb Warfare's thermal arm is still the one the patch was written for, in the dev and production jars. After `gradlew runData` it also checks, in the `PlayerRenderer` that Mixin exported with every mod's mixins applied, that the patch marks the model right after the arm is drawn and before Superb Warfare's thermal callback. `runData` opens no window; it needs the dev-mapped Superb Warfare and playerAnimator jars (from ForgeGradle's cache, `~/.gradle/caches/forge_gradle/deobf_dependencies/local/`, after a build) and Kotlin for Forge 4.12.0 in `run/mods/`.
- `gradlew verifyJar` makes a dev-only mod with `/sbwfixverify`, which compares the patched and original smoke check on every entity in a running world. Never shipped.

### Credit

This is an unofficial patch, not affiliated with Superb Warfare. [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) is made by Atsuishio, Roki27, Light_Quanta and other contributors.
