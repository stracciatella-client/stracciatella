package net.stracciatella.pathfinding.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;

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
    private static final double JUMP_GRAVITY = 0.08;
    private static final double JUMP_VERTICAL_DRAG = 0.98;
    private static final double JUMP_AIR_DRAG = 0.91;
    private static final double JUMP_BASE_VELOCITY = 0.42;
    private static final double JUMP_SPRINT_BOOST = 0.2;
    private static final double JUMP_AIR_ACCEL = 0.02;
    private static final double JUMP_FORWARD_AXIS_RATIO = 1.5;
    private static final int LEARN_MAX_GAP = 4;
    private static float currentTurnSpeed = 0.0f;
    private static float turnSpeedTarget = 0.0f;
    private static long nextTurnRetargetMs = 0;
    private static boolean flicking = false;
    private static long flickEndMs = 0;
    private static float flickTargetYaw = 0.0f;
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

    public static void start(List<MeshNode> path) {
        if (path == null || path.isEmpty()) {
            stop();
            return;
        }
        currentPath = path;
        index = 0;
        active = true;
        pauseUntilMs = 0;
        currentTurnSpeed = 0.0f;
        turnSpeedTarget = 0.0f;
        nextTurnRetargetMs = 0;
        flicking = false;
        flickEndMs = 0;
        turnPauseUntilMs = 0;
        jumpCooldownTicks = 0;
        edgeThresholdInitialized = false;
        jumpAimOffsetInitialized = false;
        alignedUntilMs = 0;
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
            }
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        updateLearning(player);
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
        double dy = targetY - player.getY();

        double distanceSq = dx * dx + dz * dz;
        double distance = Math.sqrt(distanceSq);
        boolean sharpTurn = isSharpTurnAhead();
        if (distanceSq <= ARRIVAL_RADIUS * ARRIVAL_RADIUS) {
            boolean pause = false;
            if (index + 1 < currentPath.size()) {
                int nextDy = currentPath.get(index + 1).getY() - target.getY();
                if (nextDy != 0) {
                    pause = true;
                }
            }
            index++;
            updateTargetOffset();
            if (index >= currentPath.size()) {
                stop();
                return;
            }
            if (pause) {
                schedulePause();
            }
            return;
        }

        boolean sprint = shouldSprint(distanceSq);
        JumpDecision jumpDecision = shouldJumpNow(player, target, dy, sprint, distance);
        if (jumpDecision.jump) {
            jumpCooldownTicks = CONFIG.jumpCooldownTicks;
        }
        if (jumpDecision.jump && jumpDecision.gap <= 1) {
            sprint = false;
        }

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
        float currentYaw = player.getYRot();
        float newYaw;
        float angleDelta = Math.abs(wrapDegrees(desiredYaw - currentYaw));

        boolean shouldForceFlick = sharpTurn && distanceSq <= CONFIG.turnPrepDistance * CONFIG.turnPrepDistance;
        if (!stabilizeForJump && !flicking && (angleDelta >= CONFIG.flickTriggerDeg || shouldForceFlick)) {
            float overshootAmount = randomRange(CONFIG.flickOvershootMinDeg, CONFIG.flickOvershootMaxDeg);
            float turnDir = Math.signum(wrapDegrees(desiredYaw - currentYaw));
            if (turnDir == 0.0f) {
                turnDir = 1.0f;
            }
            boolean undershoot = ThreadLocalRandom.current().nextBoolean();
            float overshoot = undershoot ? -overshootAmount * 0.6f : overshootAmount;
            flickTargetYaw = desiredYaw + (turnDir * overshoot);
            newYaw = flickTargetYaw;
            flicking = true;
            flickEndMs = System.currentTimeMillis() + randomRange(CONFIG.flickMinMs, CONFIG.flickMaxMs);
            if (sharpTurn) {
                turnPauseUntilMs = System.currentTimeMillis() + randomRange(CONFIG.turnPauseMinMs, CONFIG.turnPauseMaxMs);
            }
        } else if (flicking) {
            if (System.currentTimeMillis() >= flickEndMs) {
                flicking = false;
                float turnStep = nextTurnStep();
                newYaw = rotateToward(currentYaw, desiredYaw, turnStep);
            } else {
                newYaw = flickTargetYaw;
            }
        } else {
            float turnStep = nextTurnStep();
            newYaw = rotateToward(currentYaw, desiredYaw, turnStep);
        }

        player.setYRot(newYaw);
        player.setXRot(computePitch(dy));

        float angleDeltaAfter = Math.abs(wrapDegrees(desiredYaw - newYaw));
        if (angleDeltaAfter <= CONFIG.alignmentDeadzoneDeg) {
            currentTurnSpeed = 0.0f;
            turnSpeedTarget = 0.0f;
            nextTurnRetargetMs = System.currentTimeMillis() + CONFIG.turnJitterMinMs;
        }
        boolean facing = isFacingTarget(newYaw, desiredYaw);
        boolean jump = facing && jumpDecision.jump;
        float moveThreshold = sharpTurn ? CONFIG.turnStopThresholdDeg : CONFIG.walkTurnThresholdDeg;
        long nowMs = System.currentTimeMillis();
        if (!flicking && angleDeltaAfter <= moveThreshold) {
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
        if (jumpDecision.holdBeforeJump) {
            canMoveForward = false;
        }
        if (jumpDecision.jump && jumpDecision.gap > 1) {
            canMoveForward = true;
            sprint = true;
        }
        if (debug) {
            long now = System.currentTimeMillis();
            if (now - lastDebugMs > 200) {
                lastDebugMs = now;
                lastDebug.print(player, target, distance, angleDeltaAfter, canMoveForward, facing, jumpDecision);
            }
        }
        applyMovement(client, canMoveForward, jump, sprint && canMoveForward);
    }

    private static void applyMovement(Minecraft client, boolean forward, boolean jump, boolean sprint) {
        Options options = client.options;
        options.keyUp.setDown(forward);
        options.keyJump.setDown(jump);
        options.keySprint.setDown(sprint);
        if (client.player != null) {
            client.player.setSprinting(sprint);
        }
    }

    private static boolean shouldSprint(double distanceSq) {
        double distance = Math.sqrt(distanceSq);
        if (distance >= CONFIG.sprintDistance) {
            return true;
        }
        if (distance <= CONFIG.walkDistance) {
            return false;
        }
        double chance = randomRange(CONFIG.sprintChanceMin, CONFIG.sprintChanceMax);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static JumpDecision shouldJumpNow(LocalPlayer player, MeshNode target, double dy, boolean sprint, double distance) {
        if (jumpCooldownTicks > 0) {
            return new JumpDecision(false, 0, false);
        }
        if (!player.onGround()) {
            return new JumpDecision(false, 0, false);
        }
        int playerX = (int) Math.floor(player.getX());
        int playerZ = (int) Math.floor(player.getZ());
        double dirX = (target.getX() + 0.5) - player.getX();
        double dirZ = (target.getZ() + 0.5) - player.getZ();
        int stepX = Double.compare(dirX, 0.0);
        int stepZ = Double.compare(dirZ, 0.0);
        double absDirX = Math.abs(dirX);
        double absDirZ = Math.abs(dirZ);
        if (absDirX > absDirZ * JUMP_FORWARD_AXIS_RATIO) {
            stepZ = 0;
        } else if (absDirZ > absDirX * JUMP_FORWARD_AXIS_RATIO) {
            stepX = 0;
        }
        int dx = Math.abs(target.getX() - playerX);
        int dz = Math.abs(target.getZ() - playerZ);
        int gap = Math.max(dx, dz);
        if (gap == 0 || (stepX == 0 && stepZ == 0)) {
            return new JumpDecision(false, gap, false);
        }
        BlockPos aheadBelow = new BlockPos(playerX + stepX, (int) Math.floor(player.getY()) - 1, playerZ + stepZ);
        boolean forwardAir = player.level().getBlockState(aheadBelow).isAir();

        BlockPos landingBlock = new BlockPos(target.getX(), target.getY(), target.getZ());
        boolean landingSolid = !player.level().getBlockState(landingBlock).isAir();

        lastDebug.forwardAir = forwardAir;
        lastDebug.landingSolid = landingSolid;
        lastDebug.gap = gap;
        lastDebug.dy = dy;

        if (!landingSolid) {
            return new JumpDecision(false, gap, false);
        }
        if (dy < -0.2 && gap <= 1) {
            return new JumpDecision(false, gap, false);
        }

        boolean needsJump = dy > 0.6 || forwardAir || gap > 1;
        if (!needsJump) {
            return new JumpDecision(false, gap, false);
        }

        if ((forwardAir || dy > 0.6 || gap > 1) && shouldJumpByPhysics(player, target, sprint)) {
            return new JumpDecision(true, gap, false);
        }

        if (!edgeThresholdInitialized || currentGapForJump != gap) {
            currentGapForJump = gap;
            currentEdgeThreshold = edgeThresholdForGap(gap, sprint);
            edgeThresholdInitialized = true;
        }

        double usedThreshold = currentEdgeThreshold;
        if (gap > 1 && forwardAir) {
            usedThreshold = Math.max(CONFIG.edgeJumpForwardAirMin, currentEdgeThreshold - CONFIG.edgeJumpForwardAirBias);
        }
        lastDebug.edgeThreshold = usedThreshold;

        if (gap > 1 && forwardAir) {
            boolean axisBias = (stepX == 0) ^ (stepZ == 0);
            double edgeProgress = axisBias
                    ? edgeProgressAxis(player, stepX, stepZ)
                    : edgeProgressDirectional(player, target.getX() - player.getX(), target.getZ() - player.getZ());
            double edgeDistance = 0.5 - edgeProgress;
            if (edgeDistance <= CONFIG.edgeJumpTriggerEdge) {
                return new JumpDecision(true, gap, false);
            }
            if (edgeDistance <= CONFIG.edgeJumpHoldEdge) {
                return new JumpDecision(false, gap, true);
            }
        }
        if (gap > 1 && forwardAir && distance <= CONFIG.edgeJumpTriggerDistance) {
            return new JumpDecision(true, gap, false);
        }

        boolean atEdge;
        if (gap > 1) {
            boolean axisBias = (stepX == 0) ^ (stepZ == 0);
            if (axisBias) {
                atEdge = isAtEdge(player, stepX, stepZ, usedThreshold);
            } else {
                atEdge = isAtEdgeDirectional(player, target.getX() - player.getX(), target.getZ() - player.getZ(), usedThreshold);
            }
        } else {
            atEdge = isAtEdge(player, stepX, stepZ, currentEdgeThreshold);
        }
        if (!atEdge) {
            boolean hold = forwardAir && gap > 1 && distance <= CONFIG.edgeJumpHoldDistance;
            return new JumpDecision(false, gap, hold);
        }

        return new JumpDecision(true, gap, false);
    }

    private static boolean shouldJumpByPhysics(LocalPlayer player, MeshNode target, boolean sprint) {
        double targetX = target.getX();
        double targetZ = target.getZ();
        double targetY = target.getY() + 1.0;
        double centerX = targetX + 0.5;
        double centerZ = targetZ + 0.5;
        double dirX = centerX - player.getX();
        double dirZ = centerZ - player.getZ();
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1.0e-6) {
            return false;
        }
        dirX /= len;
        dirZ /= len;

        Vec3 velocity = player.getDeltaMovement();
        double jumpVelocity = JUMP_BASE_VELOCITY + getJumpBoost(player);
        double velX = velocity.x;
        double velY = jumpVelocity;
        double velZ = velocity.z;
        if (sprint) {
            velX += dirX * JUMP_SPRINT_BOOST;
            velZ += dirZ * JUMP_SPRINT_BOOST;
        }

        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (sprint && !player.isSprinting()) {
            speed *= 1.3;
        }
        double airAccel = speed * JUMP_AIR_ACCEL;
        double posX = player.getX();
        double posY = player.getY();
        double posZ = player.getZ();
        double minX = targetX - CONFIG.jumpLandingMargin;
        double maxX = targetX + 1.0 + CONFIG.jumpLandingMargin;
        double minZ = targetZ - CONFIG.jumpLandingMargin;
        double maxZ = targetZ + 1.0 + CONFIG.jumpLandingMargin;
        double prevY = posY;

        for (int tick = 0; tick < CONFIG.jumpSimTicks; tick++) {
            velX += dirX * airAccel;
            velZ += dirZ * airAccel;
            velX *= JUMP_AIR_DRAG;
            velY = (velY - JUMP_GRAVITY) * JUMP_VERTICAL_DRAG;
            velZ *= JUMP_AIR_DRAG;

            posX += velX;
            posY += velY;
            posZ += velZ;

            boolean withinXZ = posX >= minX && posX <= maxX && posZ >= minZ && posZ <= maxZ;
            boolean crossesY = (prevY <= targetY && posY >= targetY) || (prevY >= targetY && posY <= targetY);
            if (withinXZ && crossesY) {
                return true;
            }
            if (posY < targetY - 1.6 && velY < 0.0) {
                return false;
            }
            prevY = posY;
        }

        return false;
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

    private static boolean isAtEdge(LocalPlayer player, int stepX, int stepZ, double threshold) {
        double fracX = player.getX() - Math.floor(player.getX());
        double fracZ = player.getZ() - Math.floor(player.getZ());
        boolean edgeX = stepX > 0 ? fracX >= threshold : stepX < 0 ? fracX <= (1.0 - threshold) : true;
        boolean edgeZ = stepZ > 0 ? fracZ >= threshold : stepZ < 0 ? fracZ <= (1.0 - threshold) : true;
        return edgeX && edgeZ;
    }

    private static boolean isAtEdgeDirectional(LocalPlayer player, double dirX, double dirZ, double threshold) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len == 0.0) {
            return false;
        }
        double ux = dirX / len;
        double uz = dirZ / len;
        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        double progress = localX * ux + localZ * uz;
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
        double centerX = Math.floor(player.getX()) + 0.5;
        double centerZ = Math.floor(player.getZ()) + 0.5;
        double localX = player.getX() - centerX;
        double localZ = player.getZ() - centerZ;
        return localX * ux + localZ * uz;
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

    private static float rotateToward(float current, float target, float maxStep) {
        float delta = wrapDegrees(target - current);
        if (delta > maxStep) {
            delta = maxStep;
        } else if (delta < -maxStep) {
            delta = -maxStep;
        }
        return current + delta;
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

    private static float nextTurnStep() {
        long now = System.currentTimeMillis();
        if (now >= nextTurnRetargetMs) {
            turnSpeedTarget = randomRange(CONFIG.turnMinDeg, CONFIG.turnMaxDeg);
            int jitter = randomRange(CONFIG.turnJitterMinMs, CONFIG.turnJitterMaxMs);
            nextTurnRetargetMs = now + Math.max(10, jitter);
        }
        float delta = turnSpeedTarget - currentTurnSpeed;
        float step = Math.abs(delta) < CONFIG.turnAccel ? delta : Math.copySign(CONFIG.turnAccel, delta);
        currentTurnSpeed += step;
        return currentTurnSpeed;
    }

    private static boolean isFacingTarget(float currentYaw, float targetYaw) {
        float delta = Math.abs(wrapDegrees(targetYaw - currentYaw));
        return delta <= CONFIG.jumpFacingToleranceDeg;
    }

    private static float computePitch(double dy) {
        float jitter = randomRange(CONFIG.pitchJitterMinDeg, CONFIG.pitchJitterMaxDeg);
        if (ThreadLocalRandom.current().nextBoolean()) {
            jitter = -jitter;
        }
        if (dy < -0.4) {
            float base = (float) Math.min(45.0, 15.0 + Math.abs(dy) * 5.0);
            return base + jitter;
        }
        return jitter;
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

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void schedulePause() {
        pauseUntilMs = System.currentTimeMillis() + randomRange(CONFIG.pauseMinMs, CONFIG.pauseMaxMs);
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

    public static void setFlickTrigger(float deg) {
        CONFIG.flickTriggerDeg = deg;
        saveConfig();
    }

    public static void setFlickOvershootRange(float min, float max) {
        CONFIG.flickOvershootMinDeg = min;
        CONFIG.flickOvershootMaxDeg = max;
        saveConfig();
    }

    public static void setFlickDurationRange(int minMs, int maxMs) {
        CONFIG.flickMinMs = minMs;
        CONFIG.flickMaxMs = maxMs;
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
        public float walkTurnThresholdDeg = 25.0f;
        public float flickTriggerDeg = 70.0f;
        public float flickOvershootMinDeg = 5.0f;
        public float flickOvershootMaxDeg = 10.0f;
        public int flickMinMs = 40;
        public int flickMaxMs = 120;
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
        public int jumpCooldownTicks = 8;
        public float pitchJitterMinDeg = 0.3f;
        public float pitchJitterMaxDeg = 1.2f;
        public float jumpAimYawMinDeg = 1.5f;
        public float jumpAimYawMaxDeg = 4.0f;
        public int jumpSimTicks = 40;
        public double jumpLandingMargin = 0.3;
        public int alignmentHoldMs = 250;
        public float alignmentDeadzoneDeg = 2.5f;
        public float walkTurnMaxDeg = 60.0f;

        public void applyFrom(Config other) {
            debugEnabled = other.debugEnabled;
            turnMinDeg = other.turnMinDeg;
            turnMaxDeg = other.turnMaxDeg;
            turnAccel = other.turnAccel;
            turnJitterMinMs = other.turnJitterMinMs;
            turnJitterMaxMs = other.turnJitterMaxMs;
            jumpFacingToleranceDeg = other.jumpFacingToleranceDeg;
            walkTurnThresholdDeg = other.walkTurnThresholdDeg;
            flickTriggerDeg = other.flickTriggerDeg;
            flickOvershootMinDeg = other.flickOvershootMinDeg;
            flickOvershootMaxDeg = other.flickOvershootMaxDeg;
            flickMinMs = other.flickMinMs;
            flickMaxMs = other.flickMaxMs;
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
            edgeJumpHoldEdge = clamp(edgeJumpHoldEdge, 0.0, 0.5);
            edgeJumpTriggerEdge = clamp(edgeJumpTriggerEdge, 0.0, 0.5);
            if (jumpSimTicks < 5) {
                jumpSimTicks = 40;
            }
            if (jumpLandingMargin < 0.0) {
                jumpLandingMargin = 0.0;
            }
        }
    }

    private record JumpDecision(boolean jump, int gap, boolean holdBeforeJump) {}

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

    private static class DebugState {
        boolean forwardAir;
        boolean landingSolid;
        double edgeThreshold;
        int gap;
        double dy;

        void print(LocalPlayer player, MeshNode target, double distance, float angleDeltaAfter, boolean canMoveForward, boolean facing, JumpDecision jumpDecision) {
            System.out.println(
                    "[PathWalker] pos=" + player.blockPosition()
                            + " target=" + target.getBlockPos()
                            + " dist=" + String.format("%.2f", distance)
                            + " angleDelta=" + String.format("%.1f", angleDeltaAfter)
                            + " canMove=" + canMoveForward
                            + " facing=" + facing
                            + " gap=" + gap
                            + " dy=" + String.format("%.2f", dy)
                            + " jump=" + jumpDecision.jump
                            + " hold=" + jumpDecision.holdBeforeJump
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
