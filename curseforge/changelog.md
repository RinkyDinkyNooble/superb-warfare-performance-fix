## 1.1.0 (2026-10-21)

- Fixed: with thermal imaging on, a large white arm no longer stretches across the view when playerAnimator is installed (TaCZ's third-person gun poses). The fix is on the client and only applies with playerAnimator installed.

## 1.0.0 (2026-10-13)

First version, for Minecraft 1.20.1, Forge 47 and Superb Warfare 0.8.9.2.

- Faster: Superb Warfare's smoke check looks only at smoke clouds instead of every entity within 8 blocks, with the same results. It runs on every mob along your view each tick.
- Added: a client option to hide the ammo count overlay (shown by default).
- Works on the client, the server, or both; they don't have to match.
