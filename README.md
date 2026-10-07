# Unofficial NeoForge Port of Renice Shot

An **unofficial NeoForge port** of **Renice Shot** successfully transitions the client-side screenshot enhancement tool from Fabric to the NeoForge ecosystem. Designed to optimize and elevate how players capture in-game imagery, this port bridges the gap for users who prefer the NeoForge modding loader but want the unique rendering utility of the original platform.

## Key Features & Capabilities

The port seamlessly maintains all primary features of the original project:

* **High-Resolution Framebuffer Re-rendering**: Takes up to massive 4K screenshots (3840 × 2160) even on standard 1080p displays. It temporarily forces the engine to render the viewport at your extreme custom width and height parameters before reverting seamlessly.
* **Diverse Media Containers**: Offers direct native export pipelines for standard formats including PNG, JPG, TGA, and BMP, allowing you to bypass standard uncompressed screenshot formats.
* **Intelligent Automation**: Automatically intercepts multiple simultaneous screenshot requests requested via the screenshot API or hotkeys, sorting and executing them sequentially instead of dropping frames or freezing the render cycle.
* **Dynamic Localized File Mapping**: Handles complex automated text configurations directly inside the output pipeline, letting you save files automatically sorted by variables like `%time%` and `%world%`.
* **Advanced Error Handling & Safety**: If an active frame or buffer capture fails, the mod safely auto-restores your original display resolution and HUD configurations. Unfinished or corrupted file fragments are instantly removed to prevent local directory pollution while pushing a clear diagnostic log statement directly to your in-game chat.

## Game Configuration

When running within modpacks, you can control the capture properties natively:
* **Interactive UI Config**: If compatible configuration libraries are present within the NeoForge directory, parameters can be customized dynamically using an inside-the-game mod menu graphical layout.
* **Manual Document Controls**: Adjust your capture preferences, binding options (Default hotkey: **F9**), and custom dimensions manually by modifying the primary properties document located directly under `config/renice-shot.properties`.

If setting up a modpack or managing a client environment, let me know:
* Which **Minecraft version** is targeted?
* Are links to the active **Modrinth** or **CurseForge** listings needed?
* Are any **compatibility conflicts** with other optimization mods occurring?

Providing direct download navigation or troubleshooting steps tailored to the build is available upon request.

