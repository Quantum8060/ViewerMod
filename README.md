# Viewer Mod

This mod maps chunks loaded by the client into a world that can be played in single-player in real-time.

While connected to a server, you can toggle the save function enable or disable by pressing the `V` key.
When enabled, each chunk received from the server is written directly to a standard Anvil-format single-player world located at `.minecraft/saves/Viewer World <server-hash>/`.

The world uses a void generator outside the received chunks.
No datapack is created.
Saving is off by default and is retained in
`.minecraft/config/viewer-mod.json`.