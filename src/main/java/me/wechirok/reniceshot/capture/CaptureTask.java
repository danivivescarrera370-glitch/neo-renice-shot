/*
 * MIT License
 *
 * Copyright (c) 2021 Ramid Khan
 * Copyright (c) 2026 Wechirok
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.wechirok.reniceshot.capture;

import com.mojang.blaze3d.platform.Window;
import me.wechirok.reniceshot.ReniceShot;
import me.wechirok.reniceshot.config.Config;
import me.wechirok.reniceshot.config.FileFormat;
import me.wechirok.reniceshot.event.FramebufferCaptureCallback;
import me.wechirok.reniceshot.event.ScreenshotSaveCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class CaptureTask implements Consumer<Component> {

    private static final Logger LOGGER = LogManager.getLogger(CaptureTask.class);

    private final Minecraft minecraft;
    private final Path file;
    private final boolean saveFile;
    private final FileFormat fileFormat;
    private final int width = Config.CAPTURE_WIDTH;
    private final int height = Config.CAPTURE_HEIGHT;
    private final int delay = Config.CAPTURE_DELAY;
    private final boolean hideHud = Config.HIDE_HUD;
    private final int downscale;
    private final boolean reservedFile;
    private final Consumer<Component> messageReceiver;
    private final Consumer<CaptureTask> capture;
    private int frame;
    private int previousWidth;
    private int previousHeight;
    private boolean resolutionChanged;
    private boolean hudStateCaptured;
    private boolean initialHudHidden;
    private boolean captureStarted;

    public CaptureTask(Path file) {
        this(Minecraft.getInstance(), file, Config.SAVE_FILE, Config.CAPTURE_FILE_FORMAT);
    }

    public CaptureTask(Minecraft minecraft, Path file) {
        this(minecraft, file, Config.SAVE_FILE, Config.CAPTURE_FILE_FORMAT);
    }

    public CaptureTask(Minecraft minecraft, Path file, boolean saveFile, FileFormat fileFormat) {
        this(minecraft, file, saveFile, fileFormat, 1, saveFile, ReniceShot::showMessage, null);
    }

    public CaptureTask(Minecraft minecraft, Path file, boolean saveFile, FileFormat fileFormat, int downscale,
                       boolean reservedFile, Consumer<Component> messageReceiver, Consumer<CaptureTask> capture) {
        this.minecraft = minecraft;
        this.file = file;
        this.saveFile = saveFile;
        this.fileFormat = fileFormat;
        this.downscale = downscale;
        this.reservedFile = reservedFile;
        this.messageReceiver = messageReceiver;
        this.capture = capture;
    }

    public void begin() {
        setResolution(width, height);
    }

    public Path file() {
        return file;
    }

    public Consumer<Component> messageReceiver() {
        return messageReceiver;
    }

    public void onCaptureStarted() {
        captureStarted = true;
    }

    @Override
    public void accept(Component message) {
        if (message.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("screenshot.failure")) {
            removeUnusedReservation();
        }
        if (message.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("screenshot.success")
                && contents.getArgs().length > 0
                && contents.getArgs()[0] instanceof Component fileText
                && fileText.getStyle().getClickEvent() instanceof ClickEvent.OpenFile openFile) {
            ScreenshotSaveCallback.EVENT.invoker().onSaved(Path.of(openFile.path()));
        }
        minecraft.execute(() -> messageReceiver.accept(message));
    }

    private void removeUnusedReservation() {
        if (reservedFile) {
            try {
                if (Files.exists(file) && Files.size(file) == 0) {
                    Files.deleteIfExists(file);
                }
            } catch (IOException exception) {
                LOGGER.warn("Failed to remove unused screenshot reservation {}", file, exception);
            }
        }
    }

    public void restoreState() {
        try {
            restoreHud();
        } finally {
            restoreResolution();
        }
    }

    public void discardReservedFile(Throwable cause) {
        if (!reservedFile) {
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException cleanupException) {
            cause.addSuppressed(cleanupException);
            LOGGER.warn("Failed to remove incomplete screenshot {}", file, cleanupException);
        }
    }

    public void restoreResolution() {
        if (!resolutionChanged) {
            return;
        }

        Window window = minecraft.getWindow();
        window.setWidth(previousWidth);
        window.setHeight(previousHeight);
        resolutionChanged = false;
    }

    public void setResolution(int width, int height) {
        Window window = minecraft.getWindow();
        previousWidth = window.getWidth();
        previousHeight = window.getHeight();
        resolutionChanged = true;
        window.setWidth(width);
        window.setHeight(height);
    }

    private void restoreHud() {
        if (!hudStateCaptured) {
            return;
        }

        if (minecraft.gui.hud.isHidden() != initialHudHidden) {
            minecraft.gui.hud.toggle();
        }
        hudStateCaptured = false;
    }

    public boolean onRenderTick() {
        if (frame == 0) {
            if (hideHud) {
                initialHudHidden = minecraft.gui.hud.isHidden();
                hudStateCaptured = true;
                if (!initialHudHidden) {
                    minecraft.gui.hud.toggle();
                }
            }

            frame++;
            return false;
        } else if (frame < delay) {
            frame++;
            return false;
        } else {
            if (capture != null) {
                capture.accept(this);
                if (!captureStarted) {
                    removeUnusedReservation();
                }
                return true;
            }
            Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), downscale, image -> {
                Util.ioPool().execute(() -> {
                    try (image) {
                        try {
                            FramebufferCaptureCallback.EVENT.invoker().onCapture(image);
                        } catch (RuntimeException | Error exception) {
                            discardReservedFile(exception);
                            throw exception;
                        }

                        if (saveFile) {
                            FramebufferWriter.write(image, file, fileFormat);
                            Component fileText = Component.literal(file.getFileName().toString())
                                    .withStyle(ChatFormatting.UNDERLINE)
                                    .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file)));
                            minecraft.execute(() -> messageReceiver.accept(Component.translatable("screenshot.success", fileText)));
                        }
                    } catch (IOException | RuntimeException exception) {
                        discardReservedFile(exception);
                        ReniceShot.reportFailure(exception, messageReceiver);
                    }
                });
            });
            return true;
        }
    }
}
