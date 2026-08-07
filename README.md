# Viewer Mod

Fabric 1.21.11 client mod. Press `V` while connected to a server to enable or
disable saving. When enabled, each chunk received from the server is written
directly to a normal Anvil singleplayer world at
`.minecraft/saves/Viewer World <server-hash>/`.

The world uses a void generator outside the received chunks. No datapack is
created. Saving is off by default and is retained in
`.minecraft/config/viewer-mod.json`.
