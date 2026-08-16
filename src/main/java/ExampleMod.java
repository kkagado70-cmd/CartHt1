package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RailItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ClientModInitializer {

    public static boolean isEnabled = true;
    public static boolean smoothAim = true;
    public static double aimSpeed = 28.0;
    public static boolean autoShoot = true;
    public static boolean swapBack = true;
    public static int keybind = GLFW.GLFW_KEY_UNKNOWN;

    private static KeyBinding guiKeyBinding;

    public enum Stage {
        IDLE,
        PLACE_RAIL,
        PLACE_CART,
        LIGHT_FIRE,
        SMOOTH_AIMING,
        DISCHARGE
    }

    private static final double DAMPING_FACTOR = 0.35;
    private static final int ACTION_DELAY_TICKS = 1;

    private Stage stage = Stage.IDLE;
    private int tickTimer = 0;
    private BlockHitResult targetBlockHit = null;
    private int previousSlot = -1;

    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;

    @Override
    public void onInitializeClient() {
        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arsenal.gui",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.arsenal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        while (guiKeyBinding.wasPressed()) {
            client.setScreen(new ArsenalScreen());
        }

        if (!isEnabled) return;

        ItemStack mainHand = client.player.getMainHandStack();
        boolean isHoldingRail = isRail(mainHand);

        if (isHoldingRail && stage == Stage.IDLE) {
            if (client.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                initiateCombo(client, hitResult);
            }
        }

        if (stage != Stage.IDLE) {
            if (tickTimer > 0) {
                tickTimer--;
                return;
            }
            processStateTransition(client);
        }
    }

    private boolean isRail(ItemStack stack) {
        return stack.getItem() instanceof RailItem;
    }

    private int findItemSlot(MinecraftClient client, Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findCrossbowSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) return i;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.CROSSBOW)) return i;
        }
        return -1;
    }

    private int findRailSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            if (isRail(client.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private void initiateCombo(MinecraftClient client, BlockHitResult hitResult) {
        int cartSlot = findItemSlot(client, Items.TNT_MINECART);
        int flintSlot = findItemSlot(client, Items.FLINT_AND_STEEL);
        int xbowSlot = findCrossbowSlot(client);

        if (cartSlot == -1 || flintSlot == -1 || xbowSlot == -1) {
            return;
        }

        this.targetBlockHit = hitResult;
        this.previousSlot = client.player.getInventory().selectedSlot;
        this.stage = Stage.PLACE_RAIL;
        this.tickTimer = 0;
    }

    private void processStateTransition(MinecraftClient client) {
        if (targetBlockHit == null) {
            reset(client, true);
            return;
        }

        BlockPos groundPos = targetBlockHit.getBlockPos();
        Direction clickedFace = targetBlockHit.getSide();
        BlockPos railPos = groundPos.offset(clickedFace);

        BlockHitResult railHit = new BlockHitResult(targetBlockHit.getPos(), clickedFace, groundPos, false);
        Vec3d cartCenter = new Vec3d(railPos.getX() + 0.5, railPos.getY() + 0.1, railPos.getZ() + 0.5);
        BlockHitResult cartHit = new BlockHitResult(cartCenter, Direction.UP, railPos, false);

        Direction toPlayer = client.player.getHorizontalFacing().getOpposite();
        BlockHitResult fireHit;
        boolean isElevated = groundPos.getY() >= client.player.getBlockY() + 1;

        if (isElevated) {
            fireHit = new BlockHitResult(new Vec3d(groundPos.getX() + 0.5, groundPos.getY() + 0.5, groundPos.getZ() + 0.5), toPlayer, groundPos, false);
        } else {
            BlockPos fireBase = groundPos.offset(toPlayer);
            fireHit = new BlockHitResult(new Vec3d(fireBase.getX() + 0.5, fireBase.getY() + 1.0, fireBase.getZ() + 0.5), Direction.UP, fireBase, false);
        }

        switch (stage) {
            case PLACE_RAIL:
                int railSlot = findRailSlot(client);
                if (railSlot != -1) {
                    client.player.getInventory().selectedSlot = railSlot;
                    dispatchInteraction(client, railHit);
                }
                stage = Stage.PLACE_CART;
                tickTimer = ACTION_DELAY_TICKS;
                break;

            case PLACE_CART:
                int cartSlot = findItemSlot(client, Items.TNT_MINECART);
                if (cartSlot != -1) {
                    client.player.getInventory().selectedSlot = cartSlot;
                    dispatchInteraction(client, cartHit);
                }
                stage = Stage.LIGHT_FIRE;
                tickTimer = ACTION_DELAY_TICKS;
                break;

            case LIGHT_FIRE:
                int flintSlot = findItemSlot(client, Items.FLINT_AND_STEEL);
                if (flintSlot != -1) {
                    client.player.getInventory().selectedSlot = flintSlot;
                    dispatchInteraction(client, fireHit);
                }
                computeAimKinematics(client, railPos);
                stage = Stage.SMOOTH_AIMING;
                tickTimer = 0;
                break;

            case SMOOTH_AIMING:
                int xbowSlot = findCrossbowSlot(client);
                if (xbowSlot != -1) {
                    client.player.getInventory().selectedSlot = xbowSlot;
                    if (smoothAim) {
                        boolean aligned = stepKinematicRotation(client, targetYaw, targetPitch);
                        if (aligned) {
                            stage = Stage.DISCHARGE;
                            tickTimer = 1;
                        }
                    } else {
                        applyCameraOrientation(client, targetYaw, targetPitch);
                        stage = Stage.DISCHARGE;
                        tickTimer = 1;
                    }
                } else {
                    reset(client, true);
                }
                break;

            case DISCHARGE:
                if (autoShoot) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
                reset(client, swapBack);
                break;

            default:
                reset(client, false);
                break;
        }
    }

    private void computeAimKinematics(MinecraftClient client, BlockPos railPos) {
        Vec3d eyePos = client.player.getEyePos();
        double yOffset = (eyePos.y < railPos.getY() + 0.2) ? 0.35 : 0.15;
        Vec3d target = new Vec3d(railPos.getX() + 0.5, railPos.getY() + yOffset, railPos.getZ() + 0.5);

        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        this.targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        this.targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    private boolean stepKinematicRotation(MinecraftClient client, float destYaw, float destPitch) {
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float yawDelta = normalizeAngle(destYaw - currentYaw);
        float pitchDelta = destPitch - currentPitch;

        float stepYaw = (float) (yawDelta * DAMPING_FACTOR);
        float stepPitch = (float) (pitchDelta * DAMPING_FACTOR);

        stepYaw = (float) Math.max(-aimSpeed, Math.min(aimSpeed, stepYaw));
        stepPitch = (float) Math.max(-aimSpeed, Math.min(aimSpeed, stepPitch));

        if (Math.abs(yawDelta) <= 1.2f && Math.abs(pitchDelta) <= 1.2f) {
            applyCameraOrientation(client, destYaw, destPitch);
            return true;
        }

        applyCameraOrientation(client, currentYaw + stepYaw, currentPitch + stepPitch);
        return false;
    }

    private void applyCameraOrientation(MinecraftClient client, float yaw, float pitch) {
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.prevYaw = yaw;
        client.player.prevPitch = pitch;
        client.player.prevHeadYaw = yaw;
        client.player.headYaw = yaw;
    }

    private float normalizeAngle(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private void dispatchInteraction(MinecraftClient client, BlockHitResult hitResult) {
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private void reset(MinecraftClient client, boolean restoreSlot) {
        if (restoreSlot && previousSlot != -1 && client.player != null) {
            client.player.getInventory().selectedSlot = previousSlot;
        }
        this.stage = Stage.IDLE;
        this.targetBlockHit = null;
        this.previousSlot = -1;
        this.tickTimer = 0;
    }
}
