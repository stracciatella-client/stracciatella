package net.stracciatella.pathfinding.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class PathWalker {

    public static final Config CONFIG = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "pathwalker.json";
    private static List<MeshNode> currentPath = Collections.emptyList();
    private static int index = 0;
    private static boolean active = false;
    private static long pauseUntilMs = 0;
    private static double targetOffsetX = 0.0;
    private static double targetOffsetZ = 0.0;
    private static final double ARRIVAL_RADIUS = 0.18;
    private static final double BRAKE_RADIUS = 0.6;
    private static final double ARRIVAL_MARGIN = 0.05;
    private static final double JUMP_FORWARD_AXIS_RATIO = 1.5;
    private static final int LEARN_MAX_GAP = 4;
    private static final int JUMP_SIM_HOLD_TICKS = 2;
    private static float aimYaw;
    private static float aimYawVelocity;
    private static float aimYawAccel;
    private static float aimPitch;
    private static long turnPauseUntilMs = 0;
    private static int jumpCooldownTicks = 0;
    private static int currentGapForJump = 0;
    private static double currentEdgeThreshold = 0.0;
    private static boolean edgeThresholdInitialized = false;
    private static boolean jumpAimOffsetInitialized = false;
    private static float jumpAimYawOffsetDeg = 0.0f;
    private static boolean debug = false;
    private static long lastDebugMs = 0;
    private static DebugState lastDebug = new DebugState();
    private static long alignedUntilMs = 0;
    private static boolean learningEnabled = false;
    private static boolean lastLearnOnGround = false;
    private static double lastLearnX = 0.0;
    private static double lastLearnZ = 0.0;
    private static final LearnStats[] learnStatsByGap = initLearnStats();
    private static final CalibrationState CALIBRATION = new CalibrationState();
    private static double lastDistance = -1.0;
    private static int offCourseTicks = 0;

    public static void start(List<MeshNode> path) {
        if (path == null || path.isEmpty()) {
            stop();
            return;
        }
        currentPath = path;
        index = 0;
        active = true;
        pauseUntilMs = 0;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            aimYaw = player.getYRot();
            aimPitch = player.getXRot();
        } else {
            aimYaw = 0.0f;
            aimPitch = 0.0f;
        }

        aimYawVelocity = 0.0f;
        aimYawAccel = 0.0f;
        turnPauseUntilMs = 0;
        jumpCooldownTicks = 0;
        edgeThresholdInitialized = false;
        jumpAimOffsetInitialized = false;
        alignedUntilMs = 0;
        lastDistance = -1.0;
        offCourseTicks = 0;
        updateTargetOffset();
    }

    public static void loadConfig() {
        Path configPath = getConfigPath();
        if (configPath == null) {
            return;
        }
        if (!Files.exists(configPath)) {
            saveConfig();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) {
                CONFIG.applyFrom(loaded);
                CONFIG.normalize();
                debug = CONFIG.debugEnabled;
                saveConfig();
            }
        } catch (IOException ignored) {
            // ignore and keep defaults
        }
    }

    public static void saveConfig() {
        Path configPath = getConfigPath();
        if (configPath == null) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException ignored) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(CONFIG, writer);
        } catch (IOException ignored) {
            // ignore
        }
    }

    public static void stop() {
        active = false;
        currentPath = Collections.emptyList();
        index = 0;
        pauseUntilMs = 0;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player != null) {
            applyMovement(client, false, false, false);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(Minecraft client) {
        if (!active) {
            LocalPlayer idlePlayer = client.player;
            if (idlePlayer != null) {
                updateLearning(idlePlayer);
                updateCalibration(client, idlePlayer);
            }
            return;
        }
        LocalPlayer player = client.getInstance().player;
        if (player == null) {
            return;
        }
        updateLearning(player);
        updateCalibration(client, player);
        if (jumpCooldownTicks > 0) {
            jumpCooldownTicks--;
        }
        if (System.currentTimeMillis() < pauseUntilMs || System.currentTimeMillis() < turnPauseUntilMs) {
            applyMovement(client, false, false, false);
            return;
        }
        if (index >= currentPath.size()) {
            stop();
            return;
        }

        MeshNode target = currentPath.get(index);
        double targetX = target.getX() + 0.5 + targetOffsetX;
        double targetY = target.getY() + 1.0;
        double targetZ = target.getZ() + 0.5 + targetOffsetZ;

        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double dy = targetY - player.getEyeY();

        double distanceSq = dx * dx + dz * dz;
        double distance = Math.sqrt(distanceSq);
        boolean sharpTurn = isSharpTurnAhead();
        if (hasReachedNode(player, target, distanceSq)) {
            index++;
            updateTargetOffset();
            if (index >= currentPath.size()) {
                stop();
                return;
            }
            return;
        }

        boolean sprint = true;
        JumpDecision jumpDecision = shouldJumpNow(player, target, dy, sprint, distance);

        boolean stabilizeForJump = jumpDecision.gap > 1;
        if (stabilizeForJump && !jumpAimOffsetInitialized) {
            jumpAimYawOffsetDeg = randomRange(CONFIG.jumpAimYawMinDeg, CONFIG.jumpAimYawMaxDeg);
            if (ThreadLocalRandom.current().nextBoolean()) {
                jumpAimYawOffsetDeg = -jumpAimYawOffsetDeg;
            }
            jumpAimOffsetInitialized = true;
        }
        if (!stabilizeForJump) {
            jumpAimOffsetInitialized = false;
        }
        double aimDx = dx;
        double aimDz = dz;
        if (stabilizeForJump) {
            double baseX = target.getX() + 0.5;
            double baseZ = target.getZ() + 0.5;
            aimDx = baseX - player.getX();
            aimDz = baseZ - player.getZ();
        }

        float desiredYaw = (float) (Math.toDegrees(Math.atan2(-aimDx, aimDz)));
        if (stabilizeForJump) {
            desiredYaw += jumpAimYawOffsetDeg;
        }
        float newYaw = updateAim(desiredYaw);
        float desiredPitch = computeDesiredPitch(dy, distance);
        float newPitch = updatePitch(desiredPitch);

        player.setYRot(newYaw);
        player.setXRot(newPitch);

        float angleDeltaAfter = Math.abs(wrapDegrees(desiredYaw - newYaw));
        boolean facing = isFacingTarget(newYaw, desiredYaw);
        boolean jumpFacing = facing;
        if (!jumpFacing && jumpDecision.jump) {
            if (jumpDecision.gap > 1) {
                float gapTolerance = CONFIG.jumpFacingToleranceDeg + CONFIG.jumpFacingExtraGapDeg;
                jumpFacing = Math.abs(wrapDegrees(desiredYaw - newYaw)) <= gapTolerance;
            } else if (jumpDecision.gap <= 1) {
                // For step-up jumps (gap 0-1), be more lenient with facing
                jumpFacing = Math.abs(wrapDegrees(desiredYaw - newYaw)) <= 45.0f;
            }
        }
        boolean jump = jumpFacing && jumpDecision.jump;
        if (jump) {
            jumpCooldownTicks = CONFIG.jumpCooldownTicks;
        }
        float moveThreshold = sharpTurn ? CONFIG.turnStopThresholdDeg : CONFIG.walkTurnThresholdDeg;
        long nowMs = System.currentTimeMillis();
        if (angleDeltaAfter <= moveThreshold) {
            alignedUntilMs = nowMs + CONFIG.alignmentHoldMs;
        }
        boolean canMoveForward = angleDeltaAfter <= moveThreshold || nowMs < alignedUntilMs;
        if (angleDeltaAfter >= CONFIG.walkTurnMaxDeg) {
            canMoveForward = false;
        }
        if (distance <= BRAKE_RADIUS && shouldBrakeForNextTurn()) {
            canMoveForward = false;
        }
        if (isMovingAway(player, targetX, targetZ)) {
            canMoveForward = false;
        }
        if (jumpDecision.holdBeforeJump && jumpDecision.gap <= 2) {
            canMoveForward = false;
        }

        // Check if we're about to overshoot due to velocity at a turn/edge
        // Stop if we're making a turn near an edge with momentum
        // BUT: Don't prevent movement if we're positioned and ready to jump (jumpFacing is true)
        if (jumpDecision.gap > 1 && !jumpDecision.jump && !jumpFacing) {
            Vec3 velocity = player.getDeltaMovement();
            double forwardVel = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            // Calculate if velocity will carry us past the target
            double vx = velocity.x;
            double vz = velocity.z;
            double projectedX = player.getX() + vx * 3; // project 3 ticks ahead
            double projectedZ = player.getZ() + vz * 3;
            double projectedDist = Math.sqrt(
                (targetX - projectedX) * (targetX - projectedX) +
                (targetZ - projectedZ) * (targetZ - projectedZ)
            );

            // If we'll overshoot or if turning sharply with speed near edge
            if ((projectedDist > distance && forwardVel > 0.15 && distance < 1.5) ||
                (forwardVel > 0.2 && angleDeltaAfter > 20.0f && distance < 2.0)) {
                canMoveForward = false;
            }
        }

        if (jumpDecision.jump && jumpDecision.gap > 1 && !jump) {
            canMoveForward = false;
        }

        // Check if we need to brake with backwards movement to counter momentum
        boolean shouldBrake = false;
        Vec3 velocity = player.getDeltaMovement();
        double vx = velocity.x;
        double vz = velocity.z;
        double forwardVel = Math.sqrt(vx * vx + vz * vz);

        // Only brake in specific situations to prevent overshooting or sliding off edges
        if (forwardVel > 0.1) {
            // Project position ahead based on velocity
            int ticksAhead = player.onGround() ? 4 : 6;
            double projectedX = player.getX() + vx * ticksAhead;
            double projectedZ = player.getZ() + vz * ticksAhead;
            double projectedDist = Math.sqrt(
                (targetX - projectedX) * (targetX - projectedX) +
                (targetZ - projectedZ) * (targetZ - projectedZ)
            );

            boolean willOvershoot = projectedDist > distance;

            // Calculate if velocity is taking us away from target (already past it)
            double dirToTargetX = targetX - player.getX();
            double dirToTargetZ = targetZ - player.getZ();
            double dirLen = Math.sqrt(dirToTargetX * dirToTargetX + dirToTargetZ * dirToTargetZ);
            boolean movingAwayFromTarget = false;
            if (dirLen > 0.001) {
                dirToTargetX /= dirLen;
                dirToTargetZ /= dirLen;
                double velocityDotTarget = vx * dirToTargetX + vz * dirToTargetZ;
                movingAwayFromTarget = velocityDotTarget < -0.1; // Negative means moving away
            }

            // CASE 1: Already past target and still moving away - brake immediately
            if (movingAwayFromTarget && forwardVel > 0.15) {
                shouldBrake = true;
                canMoveForward = false;
            }

            // CASE 2: Parkour turn with momentum - stopped for turning but momentum carries us off
            // Only applies to parkour gaps where there's a risk of falling
            if (jumpDecision.gap > 1 && !canMoveForward && willOvershoot && distance < 2.0 && forwardVel > 0.15) {
                shouldBrake = true;
            }

            // CASE 3: Very close to target and would overshoot completely
            if (willOvershoot && distance < 0.8 && forwardVel > 0.2) {
                shouldBrake = true;
                canMoveForward = false;
            }

            // CASE 4: Landing prediction - just landed with high velocity and would overshoot
            if (player.onGround() && willOvershoot && forwardVel > 0.25 && distance < 1.5) {
                shouldBrake = true;
                canMoveForward = false;
            }

            // CASE 5: Gap jump lateral drift prevention.
            // When preparing for a gap jump (gap > 1), lateral velocity (perpendicular to the jump
            // direction) can carry the player off the takeoff platform in the wrong axis before the
            // jump edge-trigger fires. This happens when the player arrives at the takeoff node from
            // a different direction than the jump, leaving them with cross-velocity they need to shed.
            // Stop forward movement so ground friction decays the lateral component safely.
            if (jumpDecision.gap > 1 && !jumpDecision.jump && player.onGround()) {
                double jux = targetX - player.getX();
                double juz = targetZ - player.getZ();
                double jlen = Math.sqrt(jux * jux + juz * juz);
                if (jlen > 0.001) {
                    jux /= jlen;
                    juz /= jlen;
                    double fwdComp = vx * jux + vz * juz;
                    double latX = vx - fwdComp * jux;
                    double latZ = vz - fwdComp * juz;
                    double latSq = latX * latX + latZ * latZ;
                    if (latSq > 0.005) { // lateral velocity > ~0.07 blocks/tick
                        canMoveForward = false;
                        if (debug) {
                            System.out.println("[PathWalker] gap-jump lateral brake: latVel=" + fmt(Math.sqrt(latSq)) + " gap=" + jumpDecision.gap);
                        }
                    }
                }
            }
        }

        // For long-range gap jumps (>= 3), maintain sprint momentum during the approach phase.
        // The edge-distance trigger system fires the jump at the correct position.
        // Stopping to "prepare" kills the sprint speed needed to clear the gap.
        if (!jump && jumpDecision.gap >= 3 && player.onGround() && facing) {
            canMoveForward = true;
            shouldBrake = false;
            sprint = true;
        }

        // If jump is committed, override any braking or movement suppression that occurred above.
        // For long-range gaps this preserves the sprint speed needed to clear the gap.
        // For step-up jumps this ensures the player moves into the block while jumping.
        if (jump) {
            canMoveForward = true;
            shouldBrake = false;
            if (jumpDecision.gap > 1) {
                sprint = true;
            }
        }

        if (debug) {
            long now = System.currentTimeMillis();
            if (now - lastDebugMs > 200) {
                lastDebugMs = now;
                lastDebug.print(player, target, distance, angleDeltaAfter, canMoveForward, facing, jumpDecision, sprint);
            }
            logFallDiagnostics(player, target, dy, distance, jumpDecision);
            if (jumpDecision.jump && !jump) {
                System.out.println(
                        "[PathWalker] jump blocked: angleDelta=" + fmt(Math.abs(wrapDegrees(desiredYaw - newYaw)))
                                + " facingTol=" + fmt(CONFIG.jumpFacingToleranceDeg)
                                + " gapTol=" + fmt(CONFIG.jumpFacingToleranceDeg + CONFIG.jumpFacingExtraGapDeg)
                                + " gap=" + jumpDecision.gap
                );
            }
        }

        // Check off-course AFTER we've calculated movement state
        if (shouldCancelOffCourse(distance, player, target, canMoveForward, shouldBrake)) {
            if (debug) {
                System.out.println("[PathWalker] Off course - stopping pathwalking");
            }
            stop();
            return;
        }

        applyMovement(client, canMoveForward, shouldBrake, jump, sprint);
    }

    private static float updateAim(float targetYaw) {
        float delta = wrapDegrees(targetYaw - aimYaw);
        aimYawAccel = delta * CONFIG.turnAccel - aimYawVelocity * CONFIG.turnFriction;
        aimYawVelocity += aimYawAccel;
        aimYaw += aimYawVelocity;
        return aimYaw;
    }

    private static float updatePitch(float targetPitch) {
        // Use exponential smoothing instead of spring physics to avoid oscillation
        // Smoothly interpolate towards target with heavy damping for human-like movement
        float delta = targetPitch - aimPitch;
        if (Math.abs(delta) < 0.5f) {
            // Close enough, just set it to avoid micro-adjustments
            aimPitch = targetPitch;
            return aimPitch;
        }
        // Move a fraction of the distance each tick (exponential smoothing)
        aimPitch += delta * 0.15f;
        // Clamp to valid Minecraft range
        aimPitch = Math.max(-90.0f, Math.min(90.0f, aimPitch));
        return aimPitch;
    }

    private static void logFallDiagnostics(LocalPlayer player, MeshNode target, double dy, double distance, JumpDecision jumpDecision) {
        Vec3 velocity = player.getDeltaMovement();
        if (!player.onGround() && velocity.y < -0.08 && distance > 0.8) {
            System.out.println(
                    "[PathWalker] fall? velY=" + fmt(velocity.y)
                            + " dy=" + fmt(dy)
                            + " dist=" + fmt(distance)
                            + " jump=" + jumpDecision.jump
                            + " gap=" + jumpDecision.gap
                            + " target=" + target.getBlockPos()
            );
        }
    }

    private static boolean hasReachedNode(LocalPlayer player, MeshNode target, double distanceSq) {
        if (distanceSq <= ARRIVAL_RADIUS * ARRIVAL_RADIUS) {
            return true;
        }
        double px = player.getX();
        double pz = player.getZ();
        double minX = target.getX() - ARRIVAL_MARGIN;
        double maxX = target.getX() + 1.0 + ARRIVAL_MARGIN;
        double minZ = target.getZ() - ARRIVAL_MARGIN;
        double maxZ = target.getZ() + 1.0 + ARRIVAL_MARGIN;
        if (px < minX || px > maxX || pz < minZ || pz > maxZ) {
            return false;
        }
        double py = player.getY();
        double minY = target.getY() - 0.25;
        double maxY = target.getY() + 1.75;
        return py >= minY && py <= maxY;
    }

    private static boolean shouldCancelOffCourse(double distance, LocalPlayer player, MeshNode target, boolean canMoveForward, boolean shouldBrake) {
        // Don't count as off-course if we're actively braking or stopped for turning
        // These are intentional stops, not being stuck
        if (shouldBrake || !canMoveForward) {
            // Reset the counter but don't increment
            offCourseTicks = Math.max(0, offCourseTicks - 2);
            lastDistance = distance;
            return false;
        }

        if (distance > CONFIG.offCourseDistance) {
            // Only count as off-course if distance is significantly increasing
            if (lastDistance >= 0.0 && distance > lastDistance + 0.1) {
                offCourseTicks++;
            } else {
                offCourseTicks = Math.max(0, offCourseTicks - 1);
            }
        } else {
            offCourseTicks = 0;
        }
        lastDistance = distance;
        if (offCourseTicks >= CONFIG.offCourseTicks) {
            if (debug) {
                System.out.println(
                        "[PathWalker] cancel: off course dist=" + fmt(distance)
                                + " target=" + target.getBlockPos()
                                + " pos=" + player.blockPosition()
                );
            }
            return true;
        }
        return false;
    }

    private static void applyMovement(Minecraft client, boolean forward, boolean jump, boolean sprint) {
        applyMovement(client, forward, false, jump, sprint);
    }

    private static void applyMovement(Minecraft client, boolean forward, boolean backward, boolean jump, boolean sprint) {
        Options options = client.options;
        options.keyUp.setDown(forward);
        options.keyDown.setDown(backward);
        options.keyJump.setDown(jump);
        options.keySprint.setDown(sprint);
        if (client.player != null) {
            client.player.setSprinting(sprint);
        }
    }

    private static JumpDecision shouldJumpNow(LocalPlayer player, MeshNode target, double dy, boolean sprint, double distance) {
        boolean jump = false;
        boolean hold = false;
        int decidedGap = 0;
        String reason = "none";
        boolean forwardAir = false;
        boolean landingSolid = true;
        double edgeThreshold = 0.0;

        decide: {
            if (jumpCooldownTicks > 0) {
                reason = "cooldown";
                break decide;
            }
            if (!player.onGround()) {
                reason = "airborne";
                break decide;
            }

            int playerX = (int) Math.floor(player.getX());
            int playerZ = (int) Math.floor(player.getZ());
            double dirX = (target.getX() + 0.5) - player.getX();
            double dirZ = (target.getZ() + 0.5) - player.getZ();
            int stepX = Double.compare(dirX, 0.0);
            int stepZ = Double.compare(dirZ, 0.0);
            if (Math.abs(dirX) > Math.abs(dirZ) * JUMP_FORWARD_AXIS_RATIO) {
                stepZ = 0;
            } else if (Math.abs(dirZ) > Math.abs(dirX) * JUMP_FORWARD_AXIS_RATIO) {
                stepX = 0;
            }
            int gap = Math.max(Math.abs(target.getX() - playerX), Math.abs(target.getZ() - playerZ));
            decidedGap = gap;
            double rawDistance = Math.sqrt(dirX * dirX + dirZ * dirZ);
            double feetToTargetDy = target.getY() - player.getY();

            if (gap == 0) {
                if (feetToTargetDy >= 0.5) {
                    jump = true;
                    reason = "step-up-same-block";
                } else {
                    reason = "same-block-flat";
                }
                break decide;
            }

            if (stepX == 0 && stepZ == 0) {
                landingSolid = false;
                reason = "no-direction";
                break decide;
            }

            int baseY = (int) Math.floor(player.getY()) - 1;
            if (gap > 1) {
                for (int i = 1; i <= gap; i++) {
                    if (player.level().getBlockState(new BlockPos(playerX + stepX * i, baseY, playerZ + stepZ * i)).isAir()) {
                        forwardAir = true;
                        break;
                    }
                }
            } else {
                forwardAir = player.level().getBlockState(new BlockPos(playerX + stepX, baseY, playerZ + stepZ)).isAir();
            }

            landingSolid = !player.level().getBlockState(new BlockPos(target.getX(), target.getY(), target.getZ())).isAir();

            if (!landingSolid) {
                reason = "landing-not-solid";
                break decide;
            }

            if (debug) {
                System.out.println(String.format(Locale.US, "[PathWalker] Jump check: gap=%d, feetToTargetDy=%.2f, playerY=%.2f, targetY=%d, distance=%.2f",
                    gap, feetToTargetDy, player.getY(), target.getY(), distance));
            }

            if (feetToTargetDy < -0.5 && gap <= 1) {
                reason = "drop-no-jump";
                break decide;
            }

            boolean blockInFront = false;
            if (gap == 1) {
                BlockPos frontPos = new BlockPos(playerX + stepX, (int) Math.floor(player.getY() + 0.01), playerZ + stepZ);
                blockInFront = !player.level().getBlockState(frontPos).isAir();
            }

            // Step-up: target is 1 block higher or there's a block face at foot level
            // Only jump when close to the block face; rawDistance avoids target-offset interfering.
            if (gap <= 1 && (feetToTargetDy >= 0.5 || blockInFront) && distance <= CONFIG.stepUpJumpDistance) {
                if (rawDistance <= 0.95) {
                    jump = true;
                    reason = "step-up";
                } else {
                    reason = "step-up-too-far";
                }
                break decide;
            }

            boolean needsJump = feetToTargetDy > 0.5 || blockInFront || forwardAir || gap > 1;
            if (!needsJump) {
                reason = "no-jump-needed";
                break decide;
            }

            if (!edgeThresholdInitialized || currentGapForJump != gap) {
                currentGapForJump = gap;
                currentEdgeThreshold = edgeThresholdForGap(gap, sprint);
                edgeThresholdInitialized = true;
            }
            double usedThreshold = (gap > 1 && forwardAir)
                    ? Math.max(CONFIG.edgeJumpForwardAirMin, currentEdgeThreshold - CONFIG.edgeJumpForwardAirBias)
                    : currentEdgeThreshold;
            edgeThreshold = usedThreshold;

            // For gap >= 3 or large distances, skip simulation — braking kills sprint velocity.
            boolean longRangeJump = gap >= 3 || distance >= 2.5;
            if (!longRangeJump) {
                JumpDecision simDecision = decideJumpBySimulation(player, target, sprint, gap);
                if (simDecision != null) {
                    jump = simDecision.jump;
                    hold = simDecision.holdBeforeJump;
                    reason = simDecision.reason;
                    break decide;
                }
            }

            // Gaps with air ahead: use edge-distance triggers (handles both long- and short-range).
            if (gap > 1 && forwardAir) {
                JumpDecision edgeDecision = decideJumpByEdgeDistance(player, target, gap, dy, distance, stepX, stepZ, longRangeJump);
                jump = edgeDecision.jump;
                hold = edgeDecision.holdBeforeJump;
                reason = edgeDecision.reason;
                break decide;
            }

            // Fallback: block-fraction position check
            boolean axisBias = (stepX == 0) ^ (stepZ == 0);
            boolean atEdge = (gap > 1)
                    ? (axisBias
                        ? isAtEdge(player, stepX, stepZ, usedThreshold)
                        : isAtEdgeDirectional(player, target.getX() - player.getX(), target.getZ() - player.getZ(), usedThreshold))
                    : isAtEdge(player, stepX, stepZ, currentEdgeThreshold);
            if (atEdge) {
                jump = true;
                reason = "edge-position";
            } else if (forwardAir && gap > 1 && distance <= CONFIG.edgeJumpHoldDistance) {
                hold = true;
                reason = "edge-hold";
            } else {
                reason = "edge-not-reached";
            }
        }

        lastDebug.gap = decidedGap;
        lastDebug.dy = dy;
        lastDebug.forwardAir = forwardAir;
        lastDebug.landingSolid = landingSolid;
        lastDebug.edgeThreshold = edgeThreshold;

        if (debug) {
            System.out.println(String.format(Locale.US, "[PathWalker] Jump result: reason=%s jump=%b hold=%b gap=%d dist=%.2f",
                reason, jump, hold, decidedGap, distance));
        }

        return new JumpDecision(jump, decidedGap, hold, reason);
    }

    private static JumpDecision decideJumpByEdgeDistance(
            LocalPlayer player, MeshNode target, int gap, double dy, double distance,
            int stepX, int stepZ, boolean longRangeJump) {
        boolean axisBias = (stepX == 0) ^ (stepZ == 0);
        double edgeProgressDir = edgeProgressDirectional(player, target.getX() - player.getX(), target.getZ() - player.getZ());
        double edgeProgressAxis = axisBias ? edgeProgressAxis(player, stepX, stepZ) : edgeProgressAxisDiagonal(player, stepX, stepZ);
        double edgeDistanceDir = 0.5 - edgeProgressDir;
        double edgeDistanceAxis = 0.5 - edgeProgressAxis;
        double edgeDistance = Math.min(edgeDistanceDir, edgeDistanceAxis);
        Vec3 velocity = player.getDeltaMovement();
        double projectedDir = projectedProgressDirectional(velocity, target.getX() - player.getX(), target.getZ() - player.getZ());
        double projectedAxis = axisBias
                ? (stepX != 0 ? velocity.x * Math.signum(stepX) : velocity.z * Math.signum(stepZ))
                : projectedProgressAxisDiagonal(velocity, stepX, stepZ);
        double nextEdgeDistance = 0.5 - Math.max(edgeProgressDir + projectedDir, edgeProgressAxis + projectedAxis);
        double projectedForward = Math.max(projectedDir, projectedAxis);
        double dynamicTrigger = CONFIG.edgeJumpTriggerEdge;
        if (projectedForward > 0.0) {
            dynamicTrigger = Math.max(dynamicTrigger, Math.min(0.5, projectedForward * 2.0));
        }

        if (debug && System.currentTimeMillis() - lastDebugMs > 200) {
            System.out.println(
                    "[PathWalker] edgeDist=" + String.format("%.3f", edgeDistance)
                            + " edgeAxis=" + String.format("%.3f", edgeDistanceAxis)
                            + " edgeDir=" + String.format("%.3f", edgeDistanceDir)
                            + " nextEdgeDist=" + String.format("%.3f", nextEdgeDistance)
                            + " dynTrigger=" + String.format("%.3f", dynamicTrigger)
                            + " dist=" + String.format("%.2f", distance)
                            + " dy=" + String.format("%.2f", dy)
                            + " gap=" + gap
            );
        }

        if (longRangeJump) {
            // Use collision-based edge detection for all long-range jumps (like Meteor Client's
            // parkour module). Shrink the player's bounding box slightly and check if there
            // is still ground below. When there isn't, the player is at the very last frame
            // before falling off — the optimal moment for maximum jump distance.
            // This is more reliable than calculated edge distances because the gap value can
            // change as the player crosses block boundaries mid-approach.
            double currentSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            boolean atEdge = isAtCollisionEdge(player);
            if (debug) {
                if (atEdge) {
                    System.out.println(String.format(Locale.US,
                            "[PathWalker] Jump decision: collision-edge FIRE edgeDist=%.3f speed=%.3f gap=%d dist=%.2f",
                            edgeDistance, currentSpeed, gap, distance));
                } else if (System.currentTimeMillis() - lastDebugMs > 200) {
                    System.out.println(String.format(Locale.US,
                            "[PathWalker] Jump decision: collision-edge HOLD edgeDist=%.3f speed=%.3f gap=%d dist=%.2f",
                            edgeDistance, currentSpeed, gap, distance));
                }
            }
            return new JumpDecision(atEdge, gap, false, atEdge ? "collision-edge:fire" : "collision-edge:hold");
        }

        // Trigger jump if at or past the edge trigger threshold
        String shortFireReason = null;
        if (edgeDistance <= CONFIG.edgeJumpTriggerEdge) shortFireReason = "edgeTrigger";
        else if (edgeDistance <= dynamicTrigger) shortFireReason = "dynTrigger";
        else if (nextEdgeDistance <= 0.0) shortFireReason = "nextEdge<=0";
        else if (nextEdgeDistance <= CONFIG.edgeJumpTriggerEdge) shortFireReason = "nextEdgeTrigger";
        else if (distance <= CONFIG.edgeJumpTriggerDistance) shortFireReason = "distTrigger";
        if (shortFireReason != null) {
            if (debug) {
                System.out.println(String.format(Locale.US,
                        "[PathWalker] Jump decision: short-range FIRE reason=%s edgeDist=%.3f nextEdge=%.3f dynTrig=%.3f gap=%d dist=%.2f",
                        shortFireReason, edgeDistance, nextEdgeDistance, dynamicTrigger, gap, distance));
            }
            return new JumpDecision(true, gap, false, "short-range:" + shortFireReason);
        }

        // Hold position when approaching the edge but not yet at the trigger
        if (dy >= -0.2 && (edgeDistance <= CONFIG.edgeJumpHoldEdge || nextEdgeDistance <= CONFIG.edgeJumpHoldEdge)) {
            if (debug && System.currentTimeMillis() - lastDebugMs > 200) {
                System.out.println(String.format(Locale.US,
                        "[PathWalker] Jump decision: hold edgeDist=%.3f nextEdge=%.3f gap=%d dist=%.2f",
                        edgeDistance, nextEdgeDistance, gap, distance));
            }
            return new JumpDecision(false, gap, true, "edge-hold");
        }

        if (debug && System.currentTimeMillis() - lastDebugMs > 200) {
            System.out.println(String.format(Locale.US,
                    "[PathWalker] Jump decision: waiting edgeDist=%.3f nextEdge=%.3f dynTrig=%.3f gap=%d dist=%.2f",
                    edgeDistance, nextEdgeDistance, dynamicTrigger, gap, distance));
        }
        return new JumpDecision(false, gap, false, "edge-waiting");
    }

    private static JumpDecision decideJumpBySimulation(LocalPlayer player, MeshNode target, boolean sprint, int gap) {
        SimulationResult now = simulateJumpLanding(player, target, sprint, 0);
        if (now.lands) {
            return new JumpDecision(true, gap, false, "simulation-fire");
        }
        SimulationResult later = simulateJumpLanding(player, target, sprint, JUMP_SIM_HOLD_TICKS);
        if (later.lands) {
            return new JumpDecision(false, gap, true, "simulation-hold");
        }
        return null;
    }

    private static SimulationResult simulateJumpLanding(LocalPlayer player, MeshNode target, boolean sprint, int preTicks) {
        double targetX = target.getX();
        double targetZ = target.getZ();
        double targetY = target.getY() + 1.0;
        double centerX = targetX + 0.5;
        double centerZ = targetZ + 0.5;
        double dirX = centerX - player.getX();
        double dirZ = centerZ - player.getZ();
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1.0e-6) {
            return new SimulationResult(false);
        }
        dirX /= len;
        dirZ /= len;

        PhysicsSnapshot physics = PhysicsSnapshot.from(player, sprint);
        Vec3 velocity = player.getDeltaMovement();
        double velX = velocity.x;
        double velY = velocity.y;
        double velZ = velocity.z;
        double posX = player.getX();
        double posY = player.getY();
        double posZ = player.getZ();

        if (preTicks > 0) {
            for (int i = 0; i < preTicks; i++) {
                double forward = velX * dirX + velZ * dirZ;
                double sideX = velX - forward * dirX;
                double sideZ = velZ - forward * dirZ;
                forward = moveToward(forward, physics.maxSpeed, physics.groundAccel);
                velX = sideX + forward * dirX;
                velZ = sideZ + forward * dirZ;
                velY = 0.0;
                posX += velX;
                posZ += velZ;
            }
        }

        double jumpVelocity = physics.jumpVelocity + getJumpBoost(player);
        velY = jumpVelocity;
        if (sprint) {
            velX += dirX * physics.sprintJumpBoost;
            velZ += dirZ * physics.sprintJumpBoost;
        }

        double minX = targetX - CONFIG.jumpLandingMargin;
        double maxX = targetX + 1.0 + CONFIG.jumpLandingMargin;
        double minZ = targetZ - CONFIG.jumpLandingMargin;
        double maxZ = targetZ + 1.0 + CONFIG.jumpLandingMargin;
        double prevY = posY;

        for (int tick = 0; tick < CONFIG.jumpSimTicks; tick++) {
            velX += dirX * physics.airAccel;
            velZ += dirZ * physics.airAccel;
            velX *= physics.airDrag;
            velY = (velY - physics.gravity) * physics.verticalDrag;
            velZ *= physics.airDrag;

            posX += velX;
            posY += velY;
            posZ += velZ;

            boolean withinXZ = posX >= minX && posX <= maxX && posZ >= minZ && posZ <= maxZ;
            boolean crossesY = (prevY <= targetY && posY >= targetY) || (prevY >= targetY && posY <= targetY);
            if (withinXZ && crossesY) {
                return new SimulationResult(true);
            }
            if (posY < targetY - 1.6 && velY < 0.0) {
                return new SimulationResult(false);
            }
            prevY = posY;
        }

        return new SimulationResult(false);
    }

    private static double moveToward(double value, double target, double step) {
        if (step <= 0.0) {
            return value;
        }
        double delta = target - value;
        if (Math.abs(delta) <= step) {
            return target;
        }
        return value + Math.copySign(step, delta);
    }

    private static double getJumpBoost(LocalPlayer player) {
        MobEffectInstance effect = player.getEffect(MobEffects.JUMP_BOOST);
        if (effect == null) {
            return 0.0;
        }
        return 0.1 * (effect.getAmplifier() + 1);
    }

    private static void updateLearning(LocalPlayer player) {
        boolean onGround = player.onGround();
        if (learningEnabled && !active) {
            if (lastLearnOnGround && !onGround) {
                Vec3 velocity = player.getDeltaMovement();
                if (velocity.y > 0.0) {
                    LearnSample sample = sampleJumpForLearning(player);
                    if (sample != null) {
                        recordLearnSample(sample);
                    }
                }
            }
        }
        lastLearnOnGround = onGround;
        lastLearnX = player.getX();
        lastLearnZ = player.getZ();
    }

    private static LearnSample sampleJumpForLearning(LocalPlayer player) {
        double dirX = player.getX() - lastLearnX;
        double dirZ = player.getZ() - lastLearnZ;
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1.0e-4) {
            double yawRad = Math.toRadians(player.getYRot());
            dirX = -Math.sin(yawRad);
            dirZ = Math.cos(yawRad);
            len = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (len < 1.0e-6) {
                return null;
            }
        }
        dirX /= len;
        dirZ /= len;

        int stepX = Double.compare(dirX, 0.0);
        int stepZ = Double.compare(dirZ, 0.0);
        if (stepX == 0 && stepZ == 0) {
            return null;
        }

        int playerX = (int) Math.floor(player.getX());
        int playerZ = (int) Math.floor(player.getZ());
        int baseY = (int) Math.floor(player.getY()) - 1;

        BlockPos ahead = new BlockPos(playerX + stepX, baseY, playerZ + stepZ);
        if (!player.level().getBlockState(ahead).isAir()) {
            return null;
        }

        int gap = 0;
        for (int i = 1; i <= LEARN_MAX_GAP; i++) {
            BlockPos pos = new BlockPos(playerX + stepX * i, baseY, playerZ + stepZ * i);
            if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()) {
                gap = i;
                break;
            }
        }
        if (gap == 0) {
            return null;
        }

        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        double progress = localX * dirX + localZ * dirZ;
        if (progress < 0.0) {
            return null;
        }
        double threshold = Math.min(1.0, Math.max(0.0, progress / 0.5));
        return new LearnSample(gap, threshold);
    }

    private static void recordLearnSample(LearnSample sample) {
        if (sample.gap <= 0 || sample.gap > LEARN_MAX_GAP) {
            return;
        }
        LearnStats stats = learnStatsByGap[sample.gap];
        if (stats == null) {
            return;
        }
        stats.add(sample.threshold);
        boolean updated = applyLearningToConfig(sample.gap, stats);
        if (updated) {
            edgeThresholdInitialized = false;
            saveConfig();
        }
    }

    private static boolean applyLearningToConfig(int gap, LearnStats stats) {
        if (stats.count < 4) {
            return false;
        }
        double band = Math.max(0.02, stats.std() * 1.2);
        double min = clamp(stats.mean - band, 0.0, 1.0);
        double max = clamp(stats.mean + band, 0.0, 1.0);
        if (gap == 1) {
            CONFIG.edgeJumpShortMin = min;
            CONFIG.edgeJumpShortMax = max;
            return true;
        }
        if (gap == 2) {
            CONFIG.edgeJumpMidMin = min;
            CONFIG.edgeJumpMidMax = max;
            return true;
        }
        if (gap == 3) {
            CONFIG.edgeJumpMin = min;
            CONFIG.edgeJumpMax = max;
            LearnStats gap4 = learnStatsByGap.length > 4 ? learnStatsByGap[4] : null;
            if (gap4 != null && gap4.count >= 4) {
                double scale = clamp(gap4.mean - stats.mean, 0.0, 0.2);
                CONFIG.edgeJumpScale = scale;
            }
            return true;
        }
        if (gap == 4) {
            LearnStats gap3 = learnStatsByGap.length > 3 ? learnStatsByGap[3] : null;
            if (gap3 != null && gap3.count >= 4) {
                double scale = clamp(stats.mean - gap3.mean, 0.0, 0.2);
                CONFIG.edgeJumpScale = scale;
                return true;
            }
        }
        return false;
    }

    private static LearnStats[] initLearnStats() {
        LearnStats[] stats = new LearnStats[LEARN_MAX_GAP + 1];
        for (int i = 0; i < stats.length; i++) {
            stats[i] = new LearnStats();
        }
        return stats;
    }

    public static void setLearning(boolean enabled) {
        learningEnabled = enabled;
        for (LearnStats stats : learnStatsByGap) {
            if (stats != null) {
                stats.reset();
            }
        }
    }

    public static void startCalibration() {
        CALIBRATION.start();
    }

    public static CalibrationReport stopCalibration() {
        return CALIBRATION.stopAndApply();
    }

    public static boolean isCalibrationActive() {
        return CALIBRATION.enabled;
    }

    public static String getCalibrationSummary() {
        return CALIBRATION.summary();
    }

    private static void updateCalibration(Minecraft client, LocalPlayer player) {
        if (!CALIBRATION.enabled) {
            return;
        }
        if (active) {
            return;
        }
        CALIBRATION.update(client, player);
    }

    private static boolean isAtCollisionEdge(LocalPlayer player) {
        // Collision-based edge detection inspired by Meteor Client's parkour module.
        // Shrink the player's bounding box by a tiny amount and shift it down 0.5 blocks.
        // If there are no block collisions in this adjusted box, the player's feet are at
        // the very edge of the block — the last possible frame to jump for maximum distance.
        AABB box = player.getBoundingBox();
        AABB shrunk = box.deflate(0.001, 0.0, 0.001).move(0.0, -0.5, 0.0);
        return player.level().noCollision(player, shrunk);
    }

    private static boolean isAtEdge(LocalPlayer player, int stepX, int stepZ, double threshold) {
        double fracX = player.getX() - Math.floor(player.getX());
        double fracZ = player.getZ() - Math.floor(player.getZ());
        boolean edgeX = stepX > 0 ? fracX >= threshold : stepX < 0 ? fracX <= (1.0 - threshold) : true;
        boolean edgeZ = stepZ > 0 ? fracZ >= threshold : stepZ < 0 ? fracZ <= (1.0 - threshold) : true;
        return edgeX && edgeZ;
    }

    private static boolean isAtEdgeDirectional(LocalPlayer player, double dirX, double dirZ, double threshold) {
        double progress = edgeProgressDirectional(player, dirX, dirZ);
        double required = 0.5 * threshold;
        return progress >= required;
    }

    private static double edgeProgressDirectional(LocalPlayer player, double dirX, double dirZ) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len == 0.0) {
            return 0.0;
        }
        double ux = dirX / len;
        double uz = dirZ / len;
        double totalDist = directionalEdgeDistance(ux, uz);
        if (totalDist <= 1.0e-6) {
            return 0.0;
        }
        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        double progress = localX * ux + localZ * uz;
        return progress * (0.5 / totalDist);
    }

    private static double projectedProgressDirectional(Vec3 velocity, double dirX, double dirZ) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len == 0.0) {
            return 0.0;
        }
        double ux = dirX / len;
        double uz = dirZ / len;
        double totalDist = directionalEdgeDistance(ux, uz);
        if (totalDist <= 1.0e-6) {
            return 0.0;
        }
        return (velocity.x * ux + velocity.z * uz) * (0.5 / totalDist);
    }

    private static double directionalEdgeDistance(double ux, double uz) {
        double absUx = Math.abs(ux);
        double absUz = Math.abs(uz);
        double edgeX = absUx > 1.0e-6 ? 0.5 / absUx : Double.POSITIVE_INFINITY;
        double edgeZ = absUz > 1.0e-6 ? 0.5 / absUz : Double.POSITIVE_INFINITY;
        return Math.min(edgeX, edgeZ);
    }

    private static double edgeProgressAxis(LocalPlayer player, int stepX, int stepZ) {
        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        if (stepX != 0) {
            return localX * Math.signum(stepX);
        }
        if (stepZ != 0) {
            return localZ * Math.signum(stepZ);
        }
        return 0.0;
    }

    private static double edgeProgressAxisDiagonal(LocalPlayer player, int stepX, int stepZ) {
        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        double progressX = localX * Math.signum(stepX);
        double progressZ = localZ * Math.signum(stepZ);
        return Math.max(progressX, progressZ);
    }

    private static double projectedProgressAxisDiagonal(Vec3 velocity, int stepX, int stepZ) {
        double progressX = velocity.x * Math.signum(stepX);
        double progressZ = velocity.z * Math.signum(stepZ);
        return Math.max(progressX, progressZ);
    }

    private static double edgeThresholdForGap(int gap, boolean sprint) {
        double min = CONFIG.edgeJumpMin + Math.max(0, gap - 3) * CONFIG.edgeJumpScale;
        double max = CONFIG.edgeJumpMax + Math.max(0, gap - 3) * CONFIG.edgeJumpScale;
        if (gap <= 1) {
            min = CONFIG.edgeJumpShortMin;
            max = CONFIG.edgeJumpShortMax;
        } else if (gap == 2) {
            min = CONFIG.edgeJumpMidMin;
            max = CONFIG.edgeJumpMidMax;
        }
        if (sprint) {
            min = Math.min(min + CONFIG.edgeJumpSprintBias, 0.99);
            max = Math.min(max + CONFIG.edgeJumpSprintBias, 1.0);
        }
        min = Math.min(min, 0.98);
        max = Math.min(max, 1.0);
        return randomRange(min, max);
    }

    private static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private static boolean isFacingTarget(float currentYaw, float targetYaw) {
        float delta = Math.abs(wrapDegrees(targetYaw - currentYaw));
        return delta <= CONFIG.jumpFacingToleranceDeg;
    }

    private static float computeDesiredPitch(double dy, double distance) {
        // Keep pitch more neutral for human-like movement
        // Only look significantly up/down for large vertical differences
        if (distance > 0.1 && Math.abs(dy) > 1.0) {
            // dy = targetY - eyeY, so positive dy means look up (negative pitch in MC)
            float pitch = (float) -Math.toDegrees(Math.atan(dy / distance));
            // Clamp pitch to reasonable range for walking
            return Math.max(-15.0f, Math.min(15.0f, pitch));
        }
        return 0f;
    }

    private static void updateTargetOffset() {
        if (!active || index >= currentPath.size()) {
            targetOffsetX = 0.0;
            targetOffsetZ = 0.0;
            return;
        }
        edgeThresholdInitialized = false;
        jumpAimOffsetInitialized = false;
        if (shouldSuppressOffsetForCurrentNode()) {
            targetOffsetX = 0.0;
            targetOffsetZ = 0.0;
            return;
        }
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        double radius = randomRange(CONFIG.offsetMin, CONFIG.offsetMax);
        targetOffsetX = Math.cos(angle) * radius;
        targetOffsetZ = Math.sin(angle) * radius;
    }

    private static boolean shouldSuppressOffsetForCurrentNode() {
        MeshNode current = currentPath.get(index);
        if (index == 0 || index + 1 >= currentPath.size()) {
            return false;
        }
        MeshNode prev = currentPath.get(index - 1);
        MeshNode next = currentPath.get(index + 1);
        int dxPrev = current.getX() - prev.getX();
        int dzPrev = current.getZ() - prev.getZ();
        int dxNext = next.getX() - current.getX();
        int dzNext = next.getZ() - current.getZ();
        if (Math.abs(dxNext) <= 1 && Math.abs(dzNext) <= 1) {
            return true;
        }
        double angle = angleBetween(dxPrev, dzPrev, dxNext, dzNext);
        return angle >= 45.0;
    }

    private static boolean shouldBrakeForNextTurn() {
        if (index + 1 >= currentPath.size()) {
            return false;
        }
        MeshNode current = currentPath.get(index);
        MeshNode next = currentPath.get(index + 1);
        int dx = next.getX() - current.getX();
        int dz = next.getZ() - current.getZ();
        return Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
    }

    private static boolean isMovingAway(LocalPlayer player, double targetX, double targetZ) {
        double vx = player.getX() - player.xo;
        double vz = player.getZ() - player.zo;
        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double dot = vx * dx + vz * dz;
        return dot < -0.0005;
    }

    private static boolean isSharpTurnAhead() {
        if (index == 0 || index + 1 >= currentPath.size()) {
            return false;
        }
        MeshNode prev = currentPath.get(index - 1);
        MeshNode current = currentPath.get(index);
        MeshNode next = currentPath.get(index + 1);
        int dxPrev = current.getX() - prev.getX();
        int dzPrev = current.getZ() - prev.getZ();
        int dxNext = next.getX() - current.getX();
        int dzNext = next.getZ() - current.getZ();
        double angle = angleBetween(dxPrev, dzPrev, dxNext, dzNext);
        return angle >= CONFIG.sharpTurnDeg;
    }

    private static double angleBetween(int ax, int az, int bx, int bz) {
        double aLen = Math.sqrt(ax * ax + az * az);
        double bLen = Math.sqrt(bx * bx + bz * bz);
        if (aLen == 0 || bLen == 0) {
            return 0.0;
        }
        double dot = ax * bx + az * bz;
        double cos = dot / (aLen * bLen);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return Math.toDegrees(Math.acos(cos));
    }

    private static float randomRange(float min, float max) {
        if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        return (float) (min + (max - min) * ThreadLocalRandom.current().nextDouble());
    }

    private static double randomRange(double min, double max) {
        if (max < min) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    private static int randomRange(int min, int max) {
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static double forwardSpeed(Vec3 velocity, float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        return velocity.x * dirX + velocity.z * dirZ;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    public static void setTurnRange(float min, float max) {
        CONFIG.turnMinDeg = min;
        CONFIG.turnMaxDeg = max;
        saveConfig();
    }

    public static void setOffsetRange(double min, double max) {
        CONFIG.offsetMin = min;
        CONFIG.offsetMax = max;
        saveConfig();
    }

    public static void setPauseRange(int min, int max) {
        CONFIG.pauseMinMs = min;
        CONFIG.pauseMaxMs = max;
        saveConfig();
    }

    public static void setDistanceThresholds(double walkDistance, double sprintDistance) {
        CONFIG.walkDistance = walkDistance;
        CONFIG.sprintDistance = sprintDistance;
        saveConfig();
    }

    public static void setSprintChanceRange(double min, double max) {
        CONFIG.sprintChanceMin = min;
        CONFIG.sprintChanceMax = max;
        saveConfig();
    }

    public static void setEdgeJumpRange(double min, double max) {
        CONFIG.edgeJumpMin = min;
        CONFIG.edgeJumpMax = max;
        saveConfig();
    }

    public static void setTurnAccel(float accel) {
        CONFIG.turnAccel = accel;
        saveConfig();
    }

    public static void setTurnJitterRange(int minMs, int maxMs) {
        CONFIG.turnJitterMinMs = minMs;
        CONFIG.turnJitterMaxMs = maxMs;
        saveConfig();
    }

    public static void setJumpTolerance(float deg) {
        CONFIG.jumpFacingToleranceDeg = deg;
        saveConfig();
    }

    public static void setWalkTurnThreshold(float deg) {
        CONFIG.walkTurnThresholdDeg = deg;
        saveConfig();
    }

    public static void setSharpTurnDeg(float deg) {
        CONFIG.sharpTurnDeg = deg;
        saveConfig();
    }

    public static void setTurnPrepDistance(float distance) {
        CONFIG.turnPrepDistance = distance;
        saveConfig();
    }

    public static void setTurnStopThreshold(float deg) {
        CONFIG.turnStopThresholdDeg = deg;
        saveConfig();
    }

    public static void setTurnPauseRange(int minMs, int maxMs) {
        CONFIG.turnPauseMinMs = minMs;
        CONFIG.turnPauseMaxMs = maxMs;
        saveConfig();
    }

    public static void setEdgeJumpScale(double scale) {
        CONFIG.edgeJumpScale = scale;
        saveConfig();
    }

    public static void setEdgeJumpShortRange(double min, double max) {
        CONFIG.edgeJumpShortMin = min;
        CONFIG.edgeJumpShortMax = max;
        saveConfig();
    }

    public static void setEdgeJumpMidRange(double min, double max) {
        CONFIG.edgeJumpMidMin = min;
        CONFIG.edgeJumpMidMax = max;
        saveConfig();
    }

    public static void setEdgeJumpSprintBias(double bias) {
        CONFIG.edgeJumpSprintBias = bias;
        saveConfig();
    }

    public static void setJumpCooldownTicks(int ticks) {
        CONFIG.jumpCooldownTicks = ticks;
        saveConfig();
    }

    public static void setPitchJitterRange(float min, float max) {
        CONFIG.pitchJitterMinDeg = min;
        CONFIG.pitchJitterMaxDeg = max;
        saveConfig();
    }

    public static void setJumpAimYawRange(float min, float max) {
        CONFIG.jumpAimYawMinDeg = min;
        CONFIG.jumpAimYawMaxDeg = max;
        saveConfig();
    }

    public static void setEdgeJumpForwardAirBias(double bias) {
        CONFIG.edgeJumpForwardAirBias = bias;
        saveConfig();
    }

    public static void setEdgeJumpForwardAirMin(double min) {
        CONFIG.edgeJumpForwardAirMin = min;
        saveConfig();
    }

    public static void setEdgeJumpHoldDistance(double distance) {
        CONFIG.edgeJumpHoldDistance = distance;
        saveConfig();
    }

    public static void setEdgeJumpTriggerDistance(double distance) {
        CONFIG.edgeJumpTriggerDistance = distance;
        saveConfig();
    }

    public static void setEdgeJumpHoldEdge(double distance) {
        CONFIG.edgeJumpHoldEdge = clamp(distance, 0.0, 0.5);
        saveConfig();
    }

    public static void setEdgeJumpTriggerEdge(double distance) {
        CONFIG.edgeJumpTriggerEdge = clamp(distance, 0.0, 0.5);
        saveConfig();
    }

    public static void setJumpSimTicks(int ticks) {
        CONFIG.jumpSimTicks = Math.max(5, ticks);
        saveConfig();
    }

    public static void setJumpLandingMargin(double margin) {
        CONFIG.jumpLandingMargin = Math.max(0.0, margin);
        saveConfig();
    }

    public static void setAlignmentHoldMs(int ms) {
        CONFIG.alignmentHoldMs = ms;
        saveConfig();
    }

    public static void setAlignmentDeadzoneDeg(float deg) {
        CONFIG.alignmentDeadzoneDeg = deg;
        saveConfig();
    }

    public static void setWalkTurnMaxDeg(float deg) {
        CONFIG.walkTurnMaxDeg = deg;
        saveConfig();
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;
        CONFIG.debugEnabled = enabled;
        saveConfig();
    }

    public static class Config {
        public boolean debugEnabled = false;
        public float turnMinDeg = 6.0f;
        public float turnMaxDeg = 12.0f;
        public float turnAccel = 0.8f;
        public int turnJitterMinMs = 120;
        public int turnJitterMaxMs = 260;
        public float jumpFacingToleranceDeg = 18.0f;
        public float jumpFacingExtraGapDeg = 18.0f;
        public float walkTurnThresholdDeg = 25.0f;
        public float sharpTurnDeg = 60.0f;
        public float turnPrepDistance = 0.8f;
        public float turnStopThresholdDeg = 12.0f;
        public int turnPauseMinMs = 80;
        public int turnPauseMaxMs = 180;
        public double offsetMin = 0.05;
        public double offsetMax = 0.25;
        public int pauseMinMs = 150;
        public int pauseMaxMs = 350;
        public double walkDistance = 1.5;
        public double sprintDistance = 4.0;
        public double sprintChanceMin = 0.35;
        public double sprintChanceMax = 0.7;
        public double edgeJumpMin = 0.9;
        public double edgeJumpMax = 1.0;
        public double edgeJumpScale = 0.02;
        public double edgeJumpShortMin = 0.92;
        public double edgeJumpShortMax = 1.0;
        public double edgeJumpMidMin = 0.9;
        public double edgeJumpMidMax = 0.98;
        public double edgeJumpSprintBias = 0.02;
        public double edgeJumpForwardAirBias = 0.08;
        public double edgeJumpForwardAirMin = 0.75;
        public double edgeJumpHoldDistance = 1.2;
        public double edgeJumpTriggerDistance = 1.0;
        public double edgeJumpHoldEdge = 0.22;
        public double edgeJumpTriggerEdge = 0.08;
        public int jumpCooldownTicks = 4;
        public float pitchJitterMinDeg = 0.3f;
        public float pitchJitterMaxDeg = 1.2f;
        public float jumpAimYawMinDeg = 1.5f;
        public float jumpAimYawMaxDeg = 4.0f;
        public int jumpSimTicks = 40;
        public double jumpLandingMargin = 0.3;
        public int alignmentHoldMs = 250;
        public float alignmentDeadzoneDeg = 2.5f;
        public float walkTurnMaxDeg = 60.0f;
        public double offCourseDistance = 3.5;
        public int offCourseTicks = 10;
        public double stepUpJumpDistance = 2.0;
        public boolean physicsCalibrated = false;
        public double physicsGravity = 0.08;
        public double physicsVerticalDrag = 0.98;
        public double physicsAirDrag = 0.91;
        public double physicsJumpVelocity = 0.42;
        public double physicsSprintJumpBoost = 0.2;
        public double physicsAirAccelFactor = 0.02;
        public double physicsMaxWalkSpeedFactor = 1.0;
        public double physicsMaxSprintSpeedFactor = 1.3;
        public double physicsGroundAccelFactorWalk = 1.0;
        public double physicsGroundAccelFactorSprint = 1.3;

        // Friction values are tuned for critical damping (fastest response without overshooting).
        // The formula is friction = 2 * sqrt(acceleration).
        public float turnFriction = 1.8f; // 2 * sqrt(0.8) approx 1.79
        public float pitchAccel = 0.2f;
        public float pitchFriction = 0.9f; // 2 * sqrt(0.2) approx 0.89

        public void applyFrom(Config other) {
            debugEnabled = other.debugEnabled;
            turnMinDeg = other.turnMinDeg;
            turnMaxDeg = other.turnMaxDeg;
            turnAccel = other.turnAccel;
            turnJitterMinMs = other.turnJitterMinMs;
            turnJitterMaxMs = other.turnJitterMaxMs;
            jumpFacingToleranceDeg = other.jumpFacingToleranceDeg;
            jumpFacingExtraGapDeg = other.jumpFacingExtraGapDeg;
            walkTurnThresholdDeg = other.walkTurnThresholdDeg;
            sharpTurnDeg = other.sharpTurnDeg;
            turnPrepDistance = other.turnPrepDistance;
            turnStopThresholdDeg = other.turnStopThresholdDeg;
            turnPauseMinMs = other.turnPauseMinMs;
            turnPauseMaxMs = other.turnPauseMaxMs;
            offsetMin = other.offsetMin;
            offsetMax = other.offsetMax;
            pauseMinMs = other.pauseMinMs;
            pauseMaxMs = other.pauseMaxMs;
            walkDistance = other.walkDistance;
            sprintDistance = other.sprintDistance;
            sprintChanceMin = other.sprintChanceMin;
            sprintChanceMax = other.sprintChanceMax;
            edgeJumpMin = other.edgeJumpMin;
            edgeJumpMax = other.edgeJumpMax;
            edgeJumpScale = other.edgeJumpScale;
            edgeJumpShortMin = other.edgeJumpShortMin;
            edgeJumpShortMax = other.edgeJumpShortMax;
            edgeJumpMidMin = other.edgeJumpMidMin;
            edgeJumpMidMax = other.edgeJumpMidMax;
            edgeJumpSprintBias = other.edgeJumpSprintBias;
            edgeJumpForwardAirBias = other.edgeJumpForwardAirBias;
            edgeJumpForwardAirMin = other.edgeJumpForwardAirMin;
            edgeJumpHoldDistance = other.edgeJumpHoldDistance;
            edgeJumpTriggerDistance = other.edgeJumpTriggerDistance;
            edgeJumpHoldEdge = other.edgeJumpHoldEdge;
            edgeJumpTriggerEdge = other.edgeJumpTriggerEdge;
            jumpCooldownTicks = other.jumpCooldownTicks;
            pitchJitterMinDeg = other.pitchJitterMinDeg;
            pitchJitterMaxDeg = other.pitchJitterMaxDeg;
            jumpAimYawMinDeg = other.jumpAimYawMinDeg;
            jumpAimYawMaxDeg = other.jumpAimYawMaxDeg;
            jumpSimTicks = other.jumpSimTicks;
            jumpLandingMargin = other.jumpLandingMargin;
            alignmentHoldMs = other.alignmentHoldMs;
            alignmentDeadzoneDeg = other.alignmentDeadzoneDeg;
            walkTurnMaxDeg = other.walkTurnMaxDeg;
            offCourseDistance = other.offCourseDistance;
            offCourseTicks = other.offCourseTicks;
            stepUpJumpDistance = other.stepUpJumpDistance;
            physicsCalibrated = other.physicsCalibrated;
            physicsGravity = other.physicsGravity;
            physicsVerticalDrag = other.physicsVerticalDrag;
            physicsAirDrag = other.physicsAirDrag;
            physicsJumpVelocity = other.physicsJumpVelocity;
            physicsSprintJumpBoost = other.physicsSprintJumpBoost;
            physicsAirAccelFactor = other.physicsAirAccelFactor;
            physicsMaxWalkSpeedFactor = other.physicsMaxWalkSpeedFactor;
            physicsMaxSprintSpeedFactor = other.physicsMaxSprintSpeedFactor;
            physicsGroundAccelFactorWalk = other.physicsGroundAccelFactorWalk;
            physicsGroundAccelFactorSprint = other.physicsGroundAccelFactorSprint;
            turnFriction = other.turnFriction;
            pitchAccel = other.pitchAccel;
            pitchFriction = other.pitchFriction;
        }

        public void normalize() {
            if (edgeJumpTriggerDistance <= 0.0) {
                edgeJumpTriggerDistance = 1.0;
            }
            if (edgeJumpHoldDistance <= 0.0) {
                edgeJumpHoldDistance = 1.2;
            }
            if (edgeJumpForwardAirMin <= 0.0) {
                edgeJumpForwardAirMin = 0.75;
            }
            if (edgeJumpForwardAirBias < 0.0) {
                edgeJumpForwardAirBias = 0.0;
            }
            if (walkTurnMaxDeg <= 0.0) {
                walkTurnMaxDeg = 60.0f;
            }
            if (alignmentHoldMs < 0) {
                alignmentHoldMs = 0;
            }
            if (alignmentDeadzoneDeg < 0.0f) {
                alignmentDeadzoneDeg = 0.0f;
            }
            if (jumpFacingExtraGapDeg < 0.0f) {
                jumpFacingExtraGapDeg = 0.0f;
            }
            if (offCourseDistance <= 0.0) {
                offCourseDistance = 3.5;
            }
            if (offCourseTicks <= 0) {
                offCourseTicks = 10;
            }
            if (stepUpJumpDistance <= 0.0) {
                stepUpJumpDistance = 1.4;
            }
            edgeJumpHoldEdge = clamp(edgeJumpHoldEdge, 0.0, 0.5);
            edgeJumpTriggerEdge = clamp(edgeJumpTriggerEdge, 0.0, 0.5);
            if (jumpSimTicks < 5) {
                jumpSimTicks = 40;
            }
            if (jumpLandingMargin < 0.0) {
                jumpLandingMargin = 0.0;
            }
            if (physicsGravity <= 0.0) {
                physicsGravity = 0.08;
            }
            physicsVerticalDrag = clamp(physicsVerticalDrag, 0.5, 0.999);
            physicsAirDrag = clamp(physicsAirDrag, 0.5, 0.999);
            if (physicsJumpVelocity <= 0.0) {
                physicsJumpVelocity = 0.42;
            }
            if (physicsSprintJumpBoost < 0.0) {
                physicsSprintJumpBoost = 0.2;
            }
            if (physicsAirAccelFactor <= 0.0) {
                physicsAirAccelFactor = 0.02;
            }
            if (physicsMaxWalkSpeedFactor <= 0.0) {
                physicsMaxWalkSpeedFactor = 1.0;
            }
            if (physicsMaxSprintSpeedFactor <= 0.0) {
                physicsMaxSprintSpeedFactor = 1.3;
            }
            if (physicsGroundAccelFactorWalk <= 0.0) {
                physicsGroundAccelFactorWalk = 1.0;
            }
            if (physicsGroundAccelFactorSprint <= 0.0) {
                physicsGroundAccelFactorSprint = 1.3;
            }
            if (turnFriction <= 0.0f) {
                turnFriction = 1.8f;
            }
            if (pitchAccel <= 0.0f) {
                pitchAccel = 0.6f;
            }
            if (pitchFriction <= 0.0f) {
                pitchFriction = 1.0f;
            }
        }
    }

    private record JumpDecision(boolean jump, int gap, boolean holdBeforeJump, String reason) {}

    private record SimulationResult(boolean lands) {}

    private record PhysicsSnapshot(
            double gravity,
            double verticalDrag,
            double airDrag,
            double jumpVelocity,
            double sprintJumpBoost,
            double airAccel,
            double maxSpeed,
            double groundAccel
    ) {
        static PhysicsSnapshot from(LocalPlayer player, boolean sprint) {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double maxWalk = speed * CONFIG.physicsMaxWalkSpeedFactor;
            double maxSprint = speed * CONFIG.physicsMaxSprintSpeedFactor;
            double maxSpeed = sprint ? maxSprint : maxWalk;
            double airAccel = speed * CONFIG.physicsAirAccelFactor;
            double groundAccel = speed * (sprint ? CONFIG.physicsGroundAccelFactorSprint : CONFIG.physicsGroundAccelFactorWalk);
            return new PhysicsSnapshot(
                    CONFIG.physicsGravity,
                    CONFIG.physicsVerticalDrag,
                    CONFIG.physicsAirDrag,
                    CONFIG.physicsJumpVelocity,
                    CONFIG.physicsSprintJumpBoost,
                    airAccel,
                    maxSpeed,
                    groundAccel
            );
        }
    }

    private record LearnSample(int gap, double threshold) {}

    private static class LearnStats {
        int count;
        double mean;
        double m2;

        void add(double value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
        }

        double std() {
            if (count < 2) {
                return 0.0;
            }
            return Math.sqrt(m2 / (count - 1));
        }

        void reset() {
            count = 0;
            mean = 0.0;
            m2 = 0.0;
        }
    }

    public record CalibrationReport(boolean applied, String summary) {}

    private static class RunningMean {
        int count;
        double mean;

        void add(double value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
        }
    }

    private static class Regression {
        int count;
        double sumX;
        double sumY;
        double sumXX;
        double sumXY;

        void add(double x, double y) {
            count++;
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }

        double meanX() {
            return count == 0 ? 0.0 : sumX / count;
        }

        double meanY() {
            return count == 0 ? 0.0 : sumY / count;
        }

        double slope() {
            double denom = count * sumXX - sumX * sumX;
            if (count < 2 || Math.abs(denom) < 1.0e-9) {
                return 0.0;
            }
            return (count * sumXY - sumX * sumY) / denom;
        }

        double intercept() {
            if (count == 0) {
                return 0.0;
            }
            return meanY() - slope() * meanX();
        }
    }

    private static class CalibrationState {
        private static final int MIN_AIR_SAMPLES = 10;
        private static final int MIN_JUMP_SAMPLES = 3;

        boolean enabled;
        private boolean initialized;
        private boolean lastOnGround;
        private boolean lastForwardDown;
        private float lastYaw;
        private double lastVy;
        private double lastForwardSpeed;

        private final Regression verticalReg = new Regression();
        private final Regression airNoInputReg = new Regression();
        private final Regression airInputReg = new Regression();
        private final RunningMean jumpVelocity = new RunningMean();
        private final RunningMean groundAccelWalk = new RunningMean();
        private final RunningMean groundAccelSprint = new RunningMean();
        private final RunningMean speedWalk = new RunningMean();
        private final RunningMean speedSprint = new RunningMean();
        private final RunningMean speedAir = new RunningMean();
        private double maxWalkSpeed;
        private double maxSprintSpeed;

        void start() {
            enabled = true;
            initialized = false;
            lastOnGround = false;
            lastForwardDown = false;
            lastYaw = 0.0f;
            lastVy = 0.0;
            lastForwardSpeed = 0.0;
            verticalReg.count = 0;
            verticalReg.sumX = 0.0;
            verticalReg.sumY = 0.0;
            verticalReg.sumXX = 0.0;
            verticalReg.sumXY = 0.0;
            airNoInputReg.count = 0;
            airNoInputReg.sumX = 0.0;
            airNoInputReg.sumY = 0.0;
            airNoInputReg.sumXX = 0.0;
            airNoInputReg.sumXY = 0.0;
            airInputReg.count = 0;
            airInputReg.sumX = 0.0;
            airInputReg.sumY = 0.0;
            airInputReg.sumXX = 0.0;
            airInputReg.sumXY = 0.0;
            jumpVelocity.count = 0;
            jumpVelocity.mean = 0.0;
            groundAccelWalk.count = 0;
            groundAccelWalk.mean = 0.0;
            groundAccelSprint.count = 0;
            groundAccelSprint.mean = 0.0;
            speedWalk.count = 0;
            speedWalk.mean = 0.0;
            speedSprint.count = 0;
            speedSprint.mean = 0.0;
            speedAir.count = 0;
            speedAir.mean = 0.0;
            maxWalkSpeed = 0.0;
            maxSprintSpeed = 0.0;
        }

        CalibrationReport stopAndApply() {
            enabled = false;
            boolean applied = applyToConfig();
            return new CalibrationReport(applied, summary());
        }

        void update(Minecraft client, LocalPlayer player) {
            Options options = client.options;
            boolean forwardDown = options.keyUp.isDown();
            boolean sprintDown = options.keySprint.isDown();
            boolean onGround = player.onGround();
            float yaw = player.getYRot();
            Vec3 velocity = player.getDeltaMovement();
            double movementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double forwardSpeed = forwardSpeed(velocity, yaw);

            if (onGround && forwardDown && forwardSpeed >= 0.0) {
                if (sprintDown) {
                    maxSprintSpeed = Math.max(maxSprintSpeed, forwardSpeed);
                    speedSprint.add(movementSpeed);
                } else {
                    maxWalkSpeed = Math.max(maxWalkSpeed, forwardSpeed);
                    speedWalk.add(movementSpeed);
                }
            }

            if (initialized) {
                if (lastOnGround && !onGround && velocity.y > 0.0) {
                    jumpVelocity.add(velocity.y);
                }
                boolean yawStable = Math.abs(wrapDegrees(yaw - lastYaw)) <= 5.0f;
                if (!onGround && yawStable && forwardSpeed >= 0.0 && lastForwardSpeed >= 0.0) {
                    if (lastForwardDown && forwardDown) {
                        airInputReg.add(lastForwardSpeed, forwardSpeed);
                        speedAir.add(movementSpeed);
                    } else if (!forwardDown) {
                        airNoInputReg.add(lastForwardSpeed, forwardSpeed);
                        speedAir.add(movementSpeed);
                    }
                }
                if (lastOnGround && onGround && lastForwardDown && forwardDown) {
                    double accel = forwardSpeed - lastForwardSpeed;
                    if (accel > 0.0) {
                        if (sprintDown) {
                            groundAccelSprint.add(accel);
                        } else {
                            groundAccelWalk.add(accel);
                        }
                    }
                }
                if (!lastOnGround && !onGround) {
                    verticalReg.add(lastVy, velocity.y);
                }
            }

            initialized = true;
            lastOnGround = onGround;
            lastForwardDown = forwardDown;
            lastYaw = yaw;
            lastVy = velocity.y;
            lastForwardSpeed = forwardSpeed;
        }

        boolean applyToConfig() {
            boolean updated = false;
            double airDrag = estimateAirDrag();
            if (airDrag > 0.0 && airDrag < 1.0) {
                CONFIG.physicsAirDrag = airDrag;
                updated = true;
            }
            if (verticalReg.count >= MIN_AIR_SAMPLES) {
                double verticalDrag = clamp(verticalReg.slope(), 0.5, 0.999);
                double gravity = 0.0;
                if (verticalDrag > 0.0) {
                    gravity = -verticalReg.intercept() / verticalDrag;
                }
                if (gravity > 0.0) {
                    CONFIG.physicsGravity = clamp(gravity, 0.01, 0.2);
                    CONFIG.physicsVerticalDrag = verticalDrag;
                    updated = true;
                }
            }
            if (jumpVelocity.count >= MIN_JUMP_SAMPLES) {
                CONFIG.physicsJumpVelocity = clamp(jumpVelocity.mean, 0.2, 0.8);
                updated = true;
            }

            if (airInputReg.count >= MIN_AIR_SAMPLES && airDrag > 0.0) {
                double accel = airInputReg.meanY() / airDrag - airInputReg.meanX();
                double speedBase = speedAir.count > 0 ? speedAir.mean : 0.0;
                if (speedBase > 0.0) {
                    CONFIG.physicsAirAccelFactor = clamp(accel / speedBase, 0.001, 0.2);
                    updated = true;
                }
            }

            if (maxWalkSpeed > 0.0 && speedWalk.count > 0) {
                CONFIG.physicsMaxWalkSpeedFactor = clamp(maxWalkSpeed / speedWalk.mean, 0.2, 4.0);
                updated = true;
            }
            if (maxSprintSpeed > 0.0 && speedSprint.count > 0) {
                CONFIG.physicsMaxSprintSpeedFactor = clamp(maxSprintSpeed / speedSprint.mean, 0.2, 5.0);
                updated = true;
            }

            if (groundAccelWalk.count > 0 && speedWalk.count > 0) {
                CONFIG.physicsGroundAccelFactorWalk = clamp(groundAccelWalk.mean / speedWalk.mean, 0.001, 4.0);
                updated = true;
            }
            if (groundAccelSprint.count > 0 && speedSprint.count > 0) {
                CONFIG.physicsGroundAccelFactorSprint = clamp(groundAccelSprint.mean / speedSprint.mean, 0.001, 5.0);
                updated = true;
            }

            CONFIG.physicsCalibrated = updated;
            if (updated) {
                saveConfig();
            }
            return updated;
        }

        double estimateAirDrag() {
            Regression reg = airNoInputReg.count >= MIN_AIR_SAMPLES ? airNoInputReg : airInputReg;
            if (reg.count < MIN_AIR_SAMPLES) {
                return CONFIG.physicsAirDrag;
            }
            return reg.slope();
        }

        String summary() {
            return "airSamples=" + verticalReg.count
                    + " jumpSamples=" + jumpVelocity.count
                    + " airInput=" + airInputReg.count
                    + " airNoInput=" + airNoInputReg.count
                    + " maxWalk=" + fmt(maxWalkSpeed)
                    + " maxSprint=" + fmt(maxSprintSpeed)
                    + " calibrated=" + CONFIG.physicsCalibrated;
        }
    }

    private static class DebugState {
        boolean forwardAir;
        boolean landingSolid;
        double edgeThreshold;
        int gap;
        double dy;

        void print(LocalPlayer player, MeshNode target, double distance, float angleDeltaAfter, boolean canMoveForward, boolean facing, JumpDecision jumpDecision, boolean sprint) {
            Vec3 vel = player.getDeltaMovement();
            double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            System.out.println(
                    "[PathWalker] pos=" + player.blockPosition()
                            + " target=" + target.getBlockPos()
                            + " dist=" + String.format("%.2f", distance)
                            + " angleDelta=" + String.format("%.1f", angleDeltaAfter)
                            + " canMove=" + canMoveForward
                            + " facing=" + facing
                            + " sprint=" + sprint
                            + " isSprinting=" + player.isSprinting()
                            + " speed=" + String.format("%.3f", speed)
                            + " gap=" + gap
                            + " dy=" + String.format("%.2f", dy)
                            + " jump=" + jumpDecision.jump
                            + " hold=" + jumpDecision.holdBeforeJump
                            + " reason=" + jumpDecision.reason
                            + " forwardAir=" + forwardAir
                            + " landingSolid=" + landingSolid
                            + " edgeTh=" + String.format("%.2f", edgeThreshold)
            );
        }
    }

    private static Path getConfigPath() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameDirectory == null) {
            return null;
        }
        return client.gameDirectory.toPath().resolve("config").resolve("stracciatella").resolve(CONFIG_FILE);
    }
}