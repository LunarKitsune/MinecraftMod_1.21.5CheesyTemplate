package net.minecraft.world.level.redstone;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

public class Orientation {
    public static final StreamCodec<ByteBuf, Orientation> STREAM_CODEC = ByteBufCodecs.idMapper(Orientation::fromIndex, Orientation::getIndex);
    private static final Orientation[] ORIENTATIONS = Util.make(() -> {
        Orientation[] aorientation = new Orientation[48];
        generateContext(new Orientation(Direction.UP, Direction.NORTH, Orientation.SideBias.LEFT), aorientation);
        return aorientation;
    });
    private final Direction up;
    private final Direction front;
    private final Direction side;
    private final Orientation.SideBias sideBias;
    private final int index;
    private final List<Direction> neighbors;
    private final List<Direction> horizontalNeighbors;
    private final List<Direction> verticalNeighbors;
    private final Map<Direction, Orientation> withFront = new EnumMap<>(Direction.class);
    private final Map<Direction, Orientation> withUp = new EnumMap<>(Direction.class);
    private final Map<Orientation.SideBias, Orientation> withSideBias = new EnumMap<>(Orientation.SideBias.class);

    private Orientation(Direction pUp, Direction pFront, Orientation.SideBias pSideBias) {
        this.up = pUp;
        this.front = pFront;
        this.sideBias = pSideBias;
        this.index = generateIndex(pUp, pFront, pSideBias);
        Vec3i vec3i = pFront.getUnitVec3i().cross(pUp.getUnitVec3i());
        Direction direction = Direction.getNearest(vec3i, null);
        Objects.requireNonNull(direction);
        if (this.sideBias == Orientation.SideBias.RIGHT) {
            this.side = direction;
        } else {
            this.side = direction.getOpposite();
        }

        this.neighbors = List.of(
            this.front.getOpposite(), this.front, this.side, this.side.getOpposite(), this.up.getOpposite(), this.up
        );
        this.horizontalNeighbors = this.neighbors.stream().filter(p_363625_ -> p_363625_.getAxis() != this.up.getAxis()).toList();
        this.verticalNeighbors = this.neighbors.stream().filter(p_365283_ -> p_365283_.getAxis() == this.up.getAxis()).toList();
    }

    public static Orientation of(Direction pUp, Direction pFront, Orientation.SideBias pSideBias) {
        return ORIENTATIONS[generateIndex(pUp, pFront, pSideBias)];
    }

    public Orientation withUp(Direction pUp) {
        return this.withUp.get(pUp);
    }

    public Orientation withFront(Direction pFront) {
        return this.withFront.get(pFront);
    }

    public Orientation withFrontPreserveUp(Direction pFront) {
        return pFront.getAxis() == this.up.getAxis() ? this : this.withFront.get(pFront);
    }

    public Orientation withFrontAdjustSideBias(Direction pFront) {
        Orientation orientation = this.withFront(pFront);
        return this.front == orientation.side ? orientation.withMirror() : orientation;
    }

    public Orientation withSideBias(Orientation.SideBias pSideBias) {
        return this.withSideBias.get(pSideBias);
    }

    public Orientation withMirror() {
        return this.withSideBias(this.sideBias.getOpposite());
    }

    public Direction getFront() {
        return this.front;
    }

    public Direction getUp() {
        return this.up;
    }

    public Direction getSide() {
        return this.side;
    }

    public Orientation.SideBias getSideBias() {
        return this.sideBias;
    }

    public List<Direction> getDirections() {
        return this.neighbors;
    }

    public List<Direction> getHorizontalDirections() {
        return this.horizontalNeighbors;
    }

    public List<Direction> getVerticalDirections() {
        return this.verticalNeighbors;
    }

    @Override
    public String toString() {
        return "[up=" + this.up + ",front=" + this.front + ",sideBias=" + this.sideBias + "]";
    }

    public int getIndex() {
        return this.index;
    }

    public static Orientation fromIndex(int pIndex) {
        return ORIENTATIONS[pIndex];
    }

    public static Orientation random(RandomSource pRandom) {
        return Util.getRandom(ORIENTATIONS, pRandom);
    }

    private static Orientation generateContext(Orientation pStart, Orientation[] pOutput) {
        if (pOutput[pStart.getIndex()] != null) {
            return pOutput[pStart.getIndex()];
        } else {
            pOutput[pStart.getIndex()] = pStart;

            for (Orientation.SideBias orientation$sidebias : Orientation.SideBias.values()) {
                pStart.withSideBias
                    .put(orientation$sidebias, generateContext(new Orientation(pStart.up, pStart.front, orientation$sidebias), pOutput));
            }

            for (Direction direction1 : Direction.values()) {
                Direction direction = pStart.up;
                if (direction1 == pStart.up) {
                    direction = pStart.front.getOpposite();
                }

                if (direction1 == pStart.up.getOpposite()) {
                    direction = pStart.front;
                }

                pStart.withFront.put(direction1, generateContext(new Orientation(direction, direction1, pStart.sideBias), pOutput));
            }

            for (Direction direction2 : Direction.values()) {
                Direction direction3 = pStart.front;
                if (direction2 == pStart.front) {
                    direction3 = pStart.up.getOpposite();
                }

                if (direction2 == pStart.front.getOpposite()) {
                    direction3 = pStart.up;
                }

                pStart.withUp.put(direction2, generateContext(new Orientation(direction2, direction3, pStart.sideBias), pOutput));
            }

            return pStart;
        }
    }

    @VisibleForTesting
    protected static int generateIndex(Direction pUp, Direction pFront, Orientation.SideBias pSideBias) {
        if (pUp.getAxis() == pFront.getAxis()) {
            throw new IllegalStateException("Up-vector and front-vector can not be on the same axis");
        } else {
            int i;
            if (pUp.getAxis() == Direction.Axis.Y) {
                i = pFront.getAxis() == Direction.Axis.X ? 1 : 0;
            } else {
                i = pFront.getAxis() == Direction.Axis.Y ? 1 : 0;
            }

            int j = i << 1 | pFront.getAxisDirection().ordinal();
            return ((pUp.ordinal() << 2) + j << 1) + pSideBias.ordinal();
        }
    }

    public static enum SideBias {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        private SideBias(final String pName) {
            this.name = pName;
        }

        public Orientation.SideBias getOpposite() {
            return this == LEFT ? RIGHT : LEFT;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}