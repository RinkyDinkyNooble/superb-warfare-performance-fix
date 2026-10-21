# Superb Warfare Performance Fix (Unofficial Patch)

A companion patch for [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) 0.8.9.2 on Minecraft 1.20.1 (Forge). It removes most of the time Superb Warfare spends working out what you are looking at, which grows with the number of mobs around you. Superb Warfare itself is still required.

## Before And After

Looking over the horde: **39 FPS** before, **102 FPS** after.

![Before, looking over the horde](https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before2.png)
![After, looking over the horde](https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After2.png)

Looking at a zombie: **50 FPS** before, **103 FPS** after.

![Before, aiming at a zombie](https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before1.png)
![After, aiming at a zombie](https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After1.png)

## Faster

- Every tick, whether or not you hold anything from Superb Warfare, it checks each mob along your view for a smoke grenade cloud nearby. Each check went through every mob within 8 blocks, so in a crowd the cost grew with the square of its size: 22% of the render thread in a 515-mob scene. The check now looks only at smoke clouds and gives the same answers.

## Bug Fixes

- With thermal imaging on, a large white arm no longer stretches across the view in packs with playerAnimator (for example with TaCZ guns).

## Option

- `hideAmmoCountOverlay` in `config/superbwarfareperformancefix-client.toml` hides the ammo count overlay shown while you hold ammo or an ammo box. Off by default.

Apart from that option and the thermal arm fix, everything looks and plays the same.

### Credit

This is an unofficial patch, not affiliated with Superb Warfare. [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) is made by Atsuishio, Roki27, Light_Quanta and other contributors.
