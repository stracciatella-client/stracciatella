The architecture is fabric for minecraft with the official mojang mappings.
The source code is located in the modules folder and written in java. The build scripts are written in kotlin.
Try to avoid doing a text search across all files.
Only modify code that needs to be changed. Try too keep the changes as small as possible
the minecraft coordinate format works like in other voxel games with y being the height
Always fix the logic if a certain use case is given assume that other same cases are also to be fixed

The logs are located in run/logs mainly use the newest one
Always check the latest logs for context on what was happening during the last execution. Only read the last 100 lines unless you need to investigate more

The Pathfinding module aims to automate player movements and pathfinding in an as authentic as possible way.
The goal of this module is to be able to have the player pathfind and parkour completely automatically
