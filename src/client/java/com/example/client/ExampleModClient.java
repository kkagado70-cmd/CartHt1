package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    public static boolean isEnabled = true;
    public static boolean smoothAim = true;
    public static double aimSpeed = 28.0;
    public static boolean autoShoot = true;
    public static boolean swapBack = true;
    public static int keybind = GLFW.GLFW_KEY_UNKNOWN;

    private static KeyMapping guiKeyBinding;

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
        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.arsenal.gui",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.arsenal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        while (guiKeyBinding.consumeClick()) {
            mc.setScreen(new ArsenalScreen());
        }

        if (!isEnabled) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        boolean isHoldingRail = isRail(mainHand);

        if (isHoldingRail && stage == Stage.IDLE) {
            if (mc.hitResult instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                initiateCombo(mc, hitResult);
            }
        }

        if (stage != Stage.IDLE) {
            if (tickTimer > 0) {
                tickTimer--;
                return;
            }
            processStateTransition(mc);
        }
    }

    private boolean isRail(ItemStack stack) {
        return stack.is(Items.RAIL) || stack.is(Items.POWERED_RAIL) || stack.is(Items.DETECTOR_RAIL) || stack.is(Items.ACTIVATOR_RAIL);
    }

    private int findItemSlot(Minecraft mc, Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private int findCrossbowSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) return i;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.CROSSBOW)) return i;
        }
        return -1;
    }

    private int findRailSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (isRail(mc.player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private void initiateCombo(Minecraft mc, BlockHitResult hitResult) {
        int cartSlot = findItemSlot(mc, Items.TNT_MINECART);
        int flintSlot = findItemSlot(mc, Items.FLINT_AND_STEEL);
        int xbowSlot = findCrossbowSlot(mc);

        if (cartSlot == -1 || flintSlot == -1 || xbowSlot == -1) {
            return;
        }

        this.targetBlockHit = hitResult;
        this.previousSlot = mc.player.getInventory().selected;
        this.stage = Stage.PLACE_RAIL;
        this.tickTimer = 0;
    }

    private void processStateTransition(Minecraft mc) {
        if (targetBlockHit == null) {
            reset(mc, true);
            return;
        }

        BlockPos groundPos = targetBlockHit.getBlockPos();
        Direction clickedFace = targetBlockHit.getDirection();
        BlockPos railPos = groundPos.relative(clickedFace);

        BlockHitResult railHit = new BlockHitResult(targetBlockHit.getLocation(), clickedFace, groundPos, false);
        Vec3 cartCenter = new Vec3(railPos.getX() + 0.5, railPos.getY() + 0.1, railPos.getZ() + 0.5);
        BlockHitResult cartHit = new BlockHitResult(cartCenter, Direction.UP, railPos, false);

        Direction toPlayer = mc.player.getDirection().getOpposite();
        BlockHitResult fireHit;
        boolean isElevated = groundPos.getY() >= mc.player.getBlockY() + 1;

        if (isElevated) {
            fireHit = new BlockHitResult(new Vec3(groundPos.getX() + 0.5, groundPos.getY() + 0.5, groundPos.getZ() + 0.5), toPlayer, groundPos, false);
        } else {
            BlockPos fireBase = groundPos.relative(toPlayer);
            fireHit = new BlockHitResult(new Vec3(fireBase.getX() + 0.5, fireBase.getY() + 1.0, fireBase.getZ() + 0.5), Direction.UP, fireBase, false);
        }

        switch (stage) {
            case PLACE_RAIL:
                int railSlot = findRailSlot(mc);
                if (railSlot != -1) {
                    mc.player.getInventory().selected = railSlot;
                    dispatchInteraction(mc, railHit);
                }
                stage = Stage.PLACE_CART;
                tickTimer = ACTION_DELAY_TICKS;
                break;

            case PLACE_CART:
                int cartSlot = findItemSlot(mc, Items.TNT_MINECART);
                if (cartSlot != -1) {
                    mc.player.getInventory().selected = cartSlot;
                    dispatchInteraction(mc, cartHit);
                }
                stage = Stage.LIGHT_FIRE;
                tickTimer = ACTION_DELAY_TICKS;
                break;

            case LIGHT_FIRE:
                int flintSlot = findItemSlot(mc, Items.FLINT_AND_STEEL);
                if (flintSlot != -1) {
                    mc.player.getInventory().selected = flintSlot;
                    dispatchInteraction(mc, fireHit);
                }
                computeAimKinematics(mc, railPos);
                stage = Stage.SMOOTH_AIMING;
                tickTimer = 0;
                break;

            case SMOOTH_AIMING:
                int xbowSlot = findCrossbowSlot(mc);
                if (xbowSlot != -1) {
                    mc.player.getInventory().selected = xbowSlot;
                    if (smoothAim) {
                        boolean aligned = stepKinematicRotation(mc, targetYaw, targetPitch);
                        if (aligned) {
                            stage = Stage.DISCHARGE;
                            tickTimer = 1;
                        }
                    } else {
                        applyCameraOrientation(mc, targetYaw, targetPitch);
                        stage = Stage.DISCHARGE;
                        tickTimer = 1;
                    }
                } else {
                    reset(mc, true);
                }
                break;

            case DISCHARGE:
                if (autoShoot) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                reset(mc, swapBack);
                break;

            default:
                reset(mc, false);
                break;
        }
    }

    private void computeAimKinematics(Minecraft mc, BlockPos railPos) {
        Vec3 eyePos = mc.player.getEyePosition();
        double yOffset = (eyePos.y < railPos.getY() + 0.2) ? 0.35 : 0.15;
        Vec3 target = new Vec3(railPos.getX() + 0.5, railPos.getY() + yOffset, railPos.getZ() + 0.5);

        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        this.targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        this.targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    private boolean stepKinematicRotation(Minecraft mc, float destYaw, float destPitch) {
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDelta = normalizeAngle(destYaw - currentYaw);
        float pitchDelta = destPitch - currentPitch;

        float stepYaw = (float) (yawDelta * DAMPING_FACTOR);
        float stepPitch = (float) (pitchDelta * DAMPING_FACTOR);

        stepYaw = (float) Math.max(-aimSpeed, Math.min(aimSpeed, stepYaw));
        stepPitch = (float) Math.max(-aimSpeed, Math.min(aimSpeed, stepPitch));

        if (Math.abs(yawDelta) <= 1.2f && Math.abs(pitchDelta) <= 1.2f) {
            applyCameraOrientation(mc, destYaw, destPitch);
            return true;
        }

        applyCameraOrientation(mc, currentYaw + stepYaw, currentPitch + stepPitch);
        return false;
    }

    private void applyCameraOrientation(Minecraft mc, float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.yHeadRot = yaw;
        mc.player.yHeadRotO = yaw;
    }

    private float normalizeAngle(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private void dispatchInteraction(Minecraft mc, BlockHitResult hitResult) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void reset(Minecraft mc, boolean restoreSlot) {
        if (restoreSlot && previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = previousSlot;
        }
        this.stage = Stage.IDLE;
        this.targetBlockHit = null;
        this.previousSlot = -1;
        this.tickTimer = 0;
    }
	}
