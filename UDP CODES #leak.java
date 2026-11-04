

by @samitoteam in telegram
user : @jossmodz if you need contact me



//· updateEntity(...) → crea entidades remotas en el ConcurrentHashMap.
//· fromNativeSnapshot(...) → crea entidades desde el array nativo.
//· snapshot() → devuelve las entidades vivas/predichas, pero no crea nuevas.

```java
import android.os.SystemClock;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class EspEntityStore {

    private static final long ENTITY_TTL_MS = 2000;
    public static final EspEntityStore INSTANCE = new EspEntityStore();
    private static final long LOCAL_TTL_MS = 2500;

    private volatile Entity localPlayer;
    private final ConcurrentHashMap<Integer, Entity> entities = new ConcurrentHashMap<>();

    private volatile float yawRadians = Float.NaN;
    private volatile float directionX = Float.NaN;
    private volatile float directionY = Float.NaN;
    private volatile float directionZ = Float.NaN;

    private static float normalizeAngle(float f) {
        while (f > 3.141592653589793d) {
            f -= 6.2831855f;
        }
        while (f < -3.141592653589793d) {
            f += 6.2831855f;
        }
        return f;
    }

    private EspEntityStore() {
    }

    public void reset() {
        this.entities.clear();
        this.localPlayer = null;
        this.yawRadians = Float.NaN;
        this.directionX = Float.NaN;
        this.directionY = Float.NaN;
        this.directionZ = Float.NaN;
    }

    public void updateLocal(int i, final float f, float f2, final float f3) {
        float f4;
        float f5;
        float f6;

        if (validPosition(f, f2, f3)) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            Entity entity = this.localPlayer;

            if (entity != null) {
                long j = jUptimeMillis - entity.updatedAt;
                float fHorizontalDistance = horizontalDistance(entity.f214x, entity.f216z, f, f3);

                if (j <= 0 || j > 600 || fHorizontalDistance > 8.0f) {
                    f4 = 0.0f;
                    f5 = 0.0f;
                    f6 = 0.0f;
                } else {
                    float f7 = j / 1000.0f;
                    float f8 = (f - entity.f214x) / f7;
                    float f9 = (f2 - entity.f215y) / f7;
                    float f10 = (f3 - entity.f216z) / f7;

                    if (fHorizontalDistance >= 0.08f) {
                        this.yawRadians = (float) Math.atan2(f8, f10);
                    }

                    f6 = f10;
                    f4 = f8;
                    f5 = f9;
                }
            } else {
                f4 = 0.0f;
                f5 = 0.0f;
                f6 = 0.0f;
            }

            this.localPlayer = new Entity(
                    i, f, f2, f3,
                    f4, f5, f6,
                    jUptimeMillis,
                    "YOU",
                    ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION,
                    0,
                    false
            );

            this.entities.entrySet().removeIf(new Predicate() {
                @Override
                public boolean test(Object obj) {
                    return EspEntityStore.lambda$updateLocal$0(f, f3, (Map.Entry) obj);
                }
            });
        }
    }

    static /* synthetic */ boolean lambda$updateLocal$0(float f, float f2, Map.Entry entry) {
        return horizontalDistance(
                ((Entity) entry.getValue()).f214x,
                ((Entity) entry.getValue()).f216z,
                f,
                f2
        ) < 0.35f;
    }

    public int localEntityId() {
        Entity entity = this.localPlayer;
        if (entity == null) {
            return -1;
        }
        return entity.f210id;
    }

    public void updateDirection(float f, float f2, float f3) {
        float fSqrt = (float) Math.sqrt((f * f) + (f2 * f2) + (f3 * f3));

        if (!Float.isFinite(fSqrt) || fSqrt < 1.0E-4f) {
            return;
        }

        this.directionX = f / fSqrt;
        this.directionY = f2 / fSqrt;
        this.directionZ = f3 / fSqrt;
        this.yawRadians = normalizeAngle((float) Math.atan2(this.directionX, this.directionZ));
    }

    public void updateYaw(float f) {
        if (Float.isFinite(f)) {
            this.yawRadians = normalizeAngle(f);
        }
    }

    public void updateName(int i, final String str) {
        if (i <= 0 || str == null || str.length() == 0) {
            return;
        }

        this.entities.computeIfPresent(Integer.valueOf(i), new BiFunction() {
            @Override
            public Object apply(Object obj, Object obj2) {
                return EspEntityStore.lambda$updateName$1(str, (Integer) obj, (EspEntityStore.Entity) obj2);
            }
        });
    }

    static /* synthetic */ Entity lambda$updateName$1(String str, Integer num, Entity entity) {
        return new Entity(
                entity.f210id,
                entity.f214x,
                entity.f215y,
                entity.f216z,
                entity.f211vx,
                entity.f212vy,
                entity.f213vz,
                entity.updatedAt,
                str,
                entity.health,
                entity.team,
                entity.dead
        );
    }

    static /* synthetic */ Entity lambda$updateTeam$2(int i, Integer num, Entity entity) {
        return new Entity(
                entity.f210id,
                entity.f214x,
                entity.f215y,
                entity.f216z,
                entity.f211vx,
                entity.f212vy,
                entity.f213vz,
                entity.updatedAt,
                entity.name,
                entity.health,
                i,
                entity.dead
        );
    }

    public void updateTeam(int i, final int i2) {
        if (i <= 0) {
            return;
        }

        this.entities.computeIfPresent(Integer.valueOf(i), new BiFunction() {
            @Override
            public Object apply(Object obj, Object obj2) {
                return EspEntityStore.lambda$updateTeam$2(i2, (Integer) obj, (EspEntityStore.Entity) obj2);
            }
        });
    }

    public void applyDamage(int i, final int i2) {
        if (i <= 0 || i2 <= 0) {
            return;
        }

        this.entities.computeIfPresent(Integer.valueOf(i), new BiFunction() {
            @Override
            public Object apply(Object obj, Object obj2) {
                return EspEntityStore.lambda$applyDamage$3(i2, (Integer) obj, (EspEntityStore.Entity) obj2);
            }
        });
    }

    static /* synthetic */ Entity lambda$applyDamage$3(int i, Integer num, Entity entity) {
        int iMax = Math.max(
                0,
                entity.health - Math.min(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, i)
        );

        boolean z = iMax == 0;

        return new Entity(
                entity.f210id,
                entity.f214x,
                entity.f215y,
                entity.f216z,
                entity.f211vx,
                entity.f212vy,
                entity.f213vz,
                entity.updatedAt,
                entity.name,
                z ? 200 : iMax,
                entity.team,
                z
        );
    }

    public void updateEntity(final int i, final float f, final float f2, final float f3) {
        if (i <= 0 || i > 64 || !validPosition(f, f2, f3)) {
            return;
        }

        Entity entity = this.localPlayer;

        if (entity == null || horizontalDistance(entity.f214x, entity.f216z, f, f3) >= 0.35f) {
            final long jUptimeMillis = SystemClock.uptimeMillis();

            this.entities.compute(Integer.valueOf(i), new BiFunction() {
                @Override
                public Object apply(Object obj, Object obj2) {
                    return EspEntityStore.lambda$updateEntity$4(
                            jUptimeMillis,
                            i,
                            f,
                            f2,
                            f3,
                            (Integer) obj,
                            (EspEntityStore.Entity) obj2
                    );
                }
            });
        }
    }

    static /* synthetic */ Entity lambda$updateEntity$4(
            long j,
            int i,
            float f,
            float f2,
            float f3,
            Integer num,
            Entity entity
    ) {
        float f4;
        float f5;

        if (entity == null || j - entity.updatedAt > 1000) {
            return new Entity(
                    i,
                    f,
                    f2,
                    f3,
                    0.0f,
                    0.0f,
                    0.0f,
                    j,
                    "E" + i,
                    ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION,
                    0,
                    false
            );
        }

        if (horizontalDistance(entity.f214x, entity.f216z, f, f3) > 12.0f) {
            return new Entity(
                    i,
                    f,
                    f2,
                    f3,
                    0.0f,
                    0.0f,
                    0.0f,
                    j,
                    entity.name,
                    entity.health,
                    entity.team,
                    entity.dead
            );
        }

        long jMax = Math.max(8L, j - entity.updatedAt);
        float f6 = jMax / 1000.0f;
        float f7 = jMax < 80 ? 0.78f : 1.0f;
        float f8 = 1.0f - f7;

        float f9 = (((f - entity.f214x) / f6) * f7) + (entity.f211vx * f8);
        float f10 = (((f2 - entity.f215y) / f6) * f7) + (entity.f212vy * f8);
        float f11 = (f7 * ((f3 - entity.f216z) / f6)) + (f8 * entity.f213vz);

        if (Math.hypot(f9, f11) > 20.0d) {
            f5 = 0.0f;
            f4 = 0.0f;
        } else {
            f4 = f11;
            f5 = f9;
        }

        return new Entity(
                i,
                f,
                f2,
                f3,
                f5,
                f10,
                f4,
                j,
                entity.name,
                entity.health,
                entity.team,
                entity.dead
        );
    }

    public static Snapshot fromNativeSnapshot(float[] fArr) {
        Entity entity;

        if (fArr == null || fArr.length < 5) {
            return null;
        }

        long jUptimeMillis = SystemClock.uptimeMillis();
        int i = 0;

        if (Float.isFinite(fArr[0]) && Float.isFinite(fArr[2])) {
            entity = new Entity(
                    0,
                    fArr[0],
                    fArr[1],
                    fArr[2],
                    0.0f,
                    0.0f,
                    0.0f,
                    jUptimeMillis,
                    "YOU",
                    ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION,
                    0,
                    false
            );
        } else {
            entity = null;
        }

        int iMax = Math.max(i, Math.min(64, Math.round(fArr[4])));
        ArrayList arrayList = new ArrayList(iMax);

        while (i < iMax) {
            int i2 = i * 6;
            int i3 = i2 + 5;
            int i4 = i2 + 10;

            if (i4 >= fArr.length) {
                break;
            }

            ArrayList arrayList2 = arrayList;

            arrayList2.add(new Entity(
                    Math.round(fArr[i3]),
                    fArr[i2 + 6],
                    fArr[i2 + 7],
                    fArr[i2 + 8],
                    fArr[i2 + 9],
                    0.0f,
                    fArr[i4],
                    jUptimeMillis,
                    "E" + Math.round(fArr[i3]),
                    ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION,
                    0,
                    false
            ));

            i++;
            arrayList = arrayList2;
            iMax = iMax;
        }

        return new Snapshot(entity, arrayList, fArr[3], Float.NaN, Float.NaN, Float.NaN);
    }

    public Snapshot snapshot() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        ArrayList arrayList = new ArrayList();

        for (Map.Entry<Integer, Entity> entry : this.entities.entrySet()) {
            Entity value = entry.getValue();

            if (jUptimeMillis - value.updatedAt > ENTITY_TTL_MS) {
                this.entities.remove(entry.getKey(), value);
            } else {
                arrayList.add(value.predict(jUptimeMillis));
            }
        }

        Entity entity = this.localPlayer;

        if (entity != null && jUptimeMillis - entity.updatedAt > LOCAL_TTL_MS) {
            entity = null;
        }

        return new Snapshot(
                entity != null ? entity.predict(jUptimeMillis) : null,
                arrayList,
                this.yawRadians,
                this.directionX,
                this.directionY,
                this.directionZ
        );
    }

    public int entityCount() {
        return snapshot().entities.size();
    }

    private static boolean validPosition(float f, float f2, float f3) {
        return Float.isFinite(f)
                && Float.isFinite(f2)
                && Float.isFinite(f3)
                && Math.abs(f) <= 8000.0f
                && Math.abs(f3) <= 8000.0f
                && f2 >= -50.0f
                && f2 <= 500.0f
                && Math.hypot((double) f, (double) f3) >= 0.3d;
    }

    private static float horizontalDistance(float f, float f2, float f3, float f4) {
        return (float) Math.hypot(f3 - f, f4 - f2);
    }

    public static final class Entity {
        public final boolean dead;
        public final int health;

        public final int f210id;
        public final String name;
        public final int team;
        public final long updatedAt;

        public final float f211vx;
        public final float f212vy;
        public final float f213vz;

        public final float f214x;
        public final float f215y;
        public final float f216z;

        Entity(
                int i,
                float f,
                float f2,
                float f3,
                float f4,
                float f5,
                float f6,
                long j,
                String str,
                int i2,
                int i3,
                boolean z
        ) {
            this.f210id = i;
            this.f214x = f;
            this.f215y = f2;
            this.f216z = f3;
            this.f211vx = f4;
            this.f212vy = f5;
            this.f213vz = f6;
            this.updatedAt = j;
            this.name = str;
            this.health = i2;
            this.team = i3;
            this.dead = z;
        }

        Entity(
                int i,
                float f,
                float f2,
                float f3,
                float f4,
                float f5,
                float f6,
                long j,
                String str
        ) {
            this(
                    i, f, f2, f3, f4, f5, f6, j, str,
                    ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION,
                    0,
                    false
            );
        }

        Entity predict(long j) {
            float fMax = Math.max(0L, Math.min(100L, j - this.updatedAt)) / 1000.0f;

            int i = this.f210id;
            float f = this.f214x;
            float f2 = this.f211vx;
            float f3 = (f2 * fMax) + f;

            float f4 = this.f215y;
            float f5 = this.f212vy;
            float f6 = (f5 * fMax) + f4;

            float f7 = this.f216z;
            float f8 = this.f213vz;

            return new Entity(
                    i,
                    f3,
                    f6,
                    f7 + (fMax * f8),
                    f2,
                    f5,
                    f8,
                    this.updatedAt,
                    this.name,
                    this.health,
                    this.team,
                    this.dead
            );
        }
    }

    public static final class Snapshot {
        public final float directionX;
        public final float directionY;
        public final float directionZ;
        public final List<Entity> entities;
        public final Entity localPlayer;
        public final float yawRadians;

        Snapshot(
                Entity entity,
                List<Entity> list,
                float f,
                float f2,
                float f3,
                float f4
        ) {
            this.localPlayer = entity;
            this.entities = list;
            this.yawRadians = f;
            this.directionX = f2;
            this.directionY = f3;
            this.directionZ = f4;
        }

        Snapshot(Entity entity, List<Entity> list, float f) {
            this(entity, list, f, Float.NaN, Float.NaN, Float.NaN);
        }
    }
}
```

//conseguir/crear una entidad remota:

```java
EspEntityStore.INSTANCE.updateEntity(id, x, y, z);
EspEntityStore.Snapshot snap = EspEntityStore.INSTANCE.snapshot();
```

//desde el snapshot nativo:

```java
EspEntityStore.Snapshot snap = EspEntityStore.fromNativeSnapshot(floatArray);
```











//AUTO FREEZE
//SCOOPE FREEZE
//FREEZE WHEN FREEZE 
//AND FREEZE NORMAL





import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

public final class FreezeController {

    private static final long FIRE_HOLD_MS = 700L;
    private static final long SCOPE_HOLD_MS = 1800L;

    private static final int CMD_STOP_ALL = 103;
    private static final int CMD_FIRE_START = 104;
    private static final int CMD_FIRE_STOP = 105;
    private static final int CMD_SCOPE_START = 137;
    private static final int CMD_SCOPE_STOP = 138;

    private static volatile boolean aimLagEnabled;
    private static volatile boolean autoFreezeEnabled;

    private static final ConcurrentHashMap<Integer, Long> candidateLastLogAt = new ConcurrentHashMap<>();

    private static volatile long firePulseEndsAt;
    private static volatile boolean firing;

    private static volatile long lastCandidateLogAt;
    private static volatile long lastFeedLogAt;
    private static volatile long lastFreezeLogAt;
    private static volatile long lastPoseLogAt;

    private static volatile boolean manualFreezeEnabled;

    private static volatile long rewrittenPoseCount;

    private static volatile boolean scopeFreezeEnabled;
    private static volatile long scopePulseEndsAt;
    private static volatile boolean scoping;

    private static boolean isValidCommandId(int commandId) {
        return commandId > 0 && commandId <= 65535;
    }

    private FreezeController() {
    }

    public static void resetRuntime() {
        firing = false;
        scoping = false;
        firePulseEndsAt = 0L;
        scopePulseEndsAt = 0L;
        lastFreezeLogAt = 0L;
        lastPoseLogAt = 0L;
        lastCandidateLogAt = 0L;
        lastFeedLogAt = 0L;
        rewrittenPoseCount = 0L;
        candidateLastLogAt.clear();
    }

    public static void setManualFreeze(boolean enabled) {
        manualFreezeEnabled = enabled;
        NetworkLog.state("Freeze thủ công=" + enabled);
    }

    public static boolean isManualFreeze() {
        return manualFreezeEnabled;
    }

    public static void setAutoFreeze(boolean enabled) {
        autoFreezeEnabled = enabled;
        NetworkLog.state("Auto Freeze=" + enabled);
    }

    public static void setAimLag(boolean enabled) {
        aimLagEnabled = enabled;
        NetworkLog.state("Aim Lag=" + enabled);
    }

    public static void setScopeFreeze(boolean enabled) {
        scopeFreezeEnabled = enabled;
        NetworkLog.state("Scope Freeze=" + enabled);
    }

    public static void observeCommand(int commandId, byte[] body, boolean serverToClient) {
        if (!isValidCommandId(commandId)) {
            return;
        }

        int bodyLength = body == null ? 0 : body.length;
        long now = SystemClock.elapsedRealtime();

        if (!serverToClient) {
            if (commandId == CMD_SCOPE_START) {
                scoping = true;
                scopePulseEndsAt = SCOPE_HOLD_MS + now;
            } else if (commandId == CMD_SCOPE_STOP) {
                scoping = false;
                scopePulseEndsAt = 0L;
            } else {
                switch (commandId) {
                    case CMD_STOP_ALL:
                        firing = false;
                        scoping = false;
                        firePulseEndsAt = 0L;
                        scopePulseEndsAt = 0L;
                        break;

                    case CMD_FIRE_START:
                        firing = true;
                        firePulseEndsAt = FIRE_HOLD_MS + now;
                        break;

                    case CMD_FIRE_STOP:
                        firing = false;
                        firePulseEndsAt = 0L;
                        break;
                }
            }

            NetworkLog.state(
                    "Lag state cmd=" + commandId +
                    " " + (serverToClient ? "S2C" : "C2S") +
                    " body=" + bodyLength +
                    " fire=" + isFireActive(now) +
                    " scope=" + isScopeActive(now)
            );
            return;
        }

        if (aimLagEnabled) {
            logCandidate(commandId, bodyLength, serverToClient, now);
        }
    }

    public static void observePoseCommand(String poseInfo, int commandId) {
        long now = SystemClock.elapsedRealtime();

        if (now - lastPoseLogAt < 1000) {
            return;
        }

        lastPoseLogAt = now;

        boolean fireActive = isFireActive(now);
        boolean scopeActive = isScopeActive(now);

        boolean autoFreezeActive = aimLagEnabled && (
                (autoFreezeEnabled && fireActive) ||
                (scopeFreezeEnabled && scopeActive)
        );

        boolean freezeActive = manualFreezeEnabled || autoFreezeActive;

        NetworkLog.state(
                "Lag pose C2S cmd=" + commandId +
                " freeze=" + freezeActive +
                " manual=" + manualFreezeEnabled +
                " aim=" + aimLagEnabled +
                " auto=" + autoFreezeEnabled +
                " fire=" + fireActive +
                " scopeFlag=" + scopeFreezeEnabled +
                " scoping=" + scopeActive +
                " " + poseInfo
        );
    }

    public static boolean isFreezeActive() {
        long now = SystemClock.elapsedRealtime();

        return manualFreezeEnabled || (
                aimLagEnabled && (
                        (autoFreezeEnabled && isFireActive(now)) ||
                        (scopeFreezeEnabled && isScopeActive(now))
                )
        );
    }

    public static void recordDownlinkRewrite(String info, int commandId, int entityCount) {
        if (entityCount <= 0) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long total = rewrittenPoseCount + 1L;
        rewrittenPoseCount = total;

        if (now - lastFreezeLogAt >= 1000) {
            lastFreezeLogAt = now;
            NetworkLog.state(
                    "Freeze rewrite S2C cmd=" + commandId +
                    " entities=" + entityCount +
                    " total=" + total +
                    " " + info
            );
        }
    }

    public static void recordDownlinkFeed(
            String info,
            int commandId,
            int entityCount,
            int liveCount,
            int heldCount,
            boolean active
    ) {
        if (entityCount <= 0) {
            return;
        }

        if (!manualFreezeEnabled && !aimLagEnabled) {
            return;
        }

        long now = SystemClock.elapsedRealtime();

        if (now - lastFeedLogAt < 1000) {
            return;
        }

        lastFeedLogAt = now;

        NetworkLog.state(
                "Freeze feed S2C cmd=" + commandId +
                " entities=" + entityCount +
                " live=" + liveCount +
                " held=" + heldCount +
                " active=" + active +
                " fire=" + isFireActive(now) +
                " scope=" + isScopeActive(now) +
                " " + info
        );
    }

    private static boolean isFireActive(long now) {
        return firing || now < firePulseEndsAt;
    }

    private static boolean isScopeActive(long now) {
        return scoping || now < scopePulseEndsAt;
    }

    private static void logCandidate(
            int commandId,
            int bodyLength,
            boolean serverToClient,
            long now
    ) {
        if (commandId == 2001 || commandId == 2012) {
            return;
        }

        if ((commandId >= 101 && commandId <= 105) ||
                commandId == CMD_SCOPE_START ||
                commandId == CMD_SCOPE_STOP) {
            return;
        }

        Long lastLogAt = candidateLastLogAt.get(commandId);

        boolean canLogCommand = lastLogAt == null || now - lastLogAt >= 2500;
        boolean canLogGlobal = now - lastCandidateLogAt >= 200;

        if (canLogCommand && canLogGlobal) {
            candidateLastLogAt.put(commandId, now);
            lastCandidateLogAt = now;

            NetworkLog.state(
                    "Aim candidate " + (serverToClient ? "S2C" : "C2S") +
                    " cmd=" + commandId +
                    " body=" + bodyLength
            );
        }
    }
}






by @samitoteam in telegram
user : @jossmodz if you need contact me