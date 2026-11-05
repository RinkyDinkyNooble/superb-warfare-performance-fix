# Superb Warfare Performance Fix <span style="color:#e03e2d">(Unofficial Patch)</span>

A companion patch for [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) 0.8.9.2 on Minecraft 1.20.1 (Forge). It removes <span style="color:#2dc26b"><strong>most of the time Superb Warfare spends working out what you are looking at</strong></span>, which grows with the number of mobs around you. <span style="color:#e03e2d"><strong>Superb Warfare itself is still required.</strong></span>

***

## Requirements

<table>
  <tr>
    <th align="left">Requirement</th>
    <th align="left">Version</th>
  </tr>
  <tr>
    <td><strong>Minecraft</strong></td>
    <td>1.20.1 (Forge)</td>
  </tr>
  <tr>
    <td><strong>Superb Warfare</strong></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/superb-warfare">0.8.9.2</a></td>
  </tr>
</table>

***

## Before And After

### Looking over the horde: <span style="color:#e03e2d">39 FPS</span> to <span style="color:#2dc26b">102 FPS</span>

<table>
  <tr>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before2.png" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before2.png" alt="Before, looking over the horde" width="400"></a></td>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After2.png" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After2.png" alt="After, looking over the horde" width="400"></a></td>
  </tr>
  <tr>
    <td align="center"><strong><span style="color:#e03e2d">Before</span></strong>: <strong>39 FPS</strong></td>
    <td align="center"><strong><span style="color:#2dc26b">After</span></strong>: <strong>102 FPS</strong></td>
  </tr>
</table>

### Looking at a zombie: <span style="color:#e03e2d">50 FPS</span> to <span style="color:#2dc26b">103 FPS</span>

<table>
  <tr>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before1.png" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/Before1.png" alt="Before, aiming at a zombie" width="400"></a></td>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After1.png" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/superb-warfare-performance-fix/main/curseforge/images/After1.png" alt="After, aiming at a zombie" width="400"></a></td>
  </tr>
  <tr>
    <td align="center"><strong><span style="color:#e03e2d">Before</span></strong>: <strong>50 FPS</strong></td>
    <td align="center"><strong><span style="color:#2dc26b">After</span></strong>: <strong>103 FPS</strong></td>
  </tr>
</table>

***

## Faster

*   Every tick, whether or not you hold anything from Superb Warfare, it checks each mob along your view for a **smoke grenade cloud** nearby. Each check went through every mob within 8 blocks, so in a crowd the cost grew with the square of its size: <span style="color:#e03e2d"><strong>22% of the render thread</strong></span> in a 515-mob scene. The check now looks only at smoke clouds and <span style="color:#2dc26b"><strong>gives the same answers</strong></span>.

## Option

*   <code>hideAmmoCountOverlay</code> in <code>config/superbwarfareperformancefix-client.toml</code> hides the **ammo count overlay** shown while you hold ammo or an ammo box. <span style="color:#3598db"><strong>Off by default.</strong></span>

Apart from that option, everything looks and plays the same.

***

## Credit

This is an **unofficial patch**, not affiliated with Superb Warfare. [Superb Warfare](https://www.curseforge.com/minecraft/mc-mods/superb-warfare) is made by Atsuishio, Roki27, Light_Quanta and other contributors.
