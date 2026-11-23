# HyperPortal v0.2.0-ai13

## TransferDock Liquid Glass UI & HyperDock geometry alignment

- Refactor the TransferDock drawer panel to match HyperDock's liquid-glass material: translucent backdrop blur (44dp radius), cool glass surface gradient, luminous specular rim border, and faded glass section dividers.
- Remove the dark full-screen dim scrim when opening TransferDock so transitioning from HyperDock has no brightness flash.
- Support dynamic anchor bounds (`dock_left`, `dock_top`, `dock_width`, `dock_height`, `dock_corner_radius`) from `OPEN_TRANSFER_DOCK` broadcasts, caching the latest HyperDock geometry and aligning default dimensions/position to the two-column dock.
- Make cards and footer clear pill adapt smoothly to the active panel width and left/right edge alignment.
