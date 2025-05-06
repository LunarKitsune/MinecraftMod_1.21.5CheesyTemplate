package net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadCollection {
    public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    private final List<BakedQuad> all;
    private final List<BakedQuad> unculled;
    private final List<BakedQuad> north;
    private final List<BakedQuad> south;
    private final List<BakedQuad> east;
    private final List<BakedQuad> west;
    private final List<BakedQuad> up;
    private final List<BakedQuad> down;

    QuadCollection(
        List<BakedQuad> pAll,
        List<BakedQuad> pUnculled,
        List<BakedQuad> pNorth,
        List<BakedQuad> pSouth,
        List<BakedQuad> pEast,
        List<BakedQuad> pWest,
        List<BakedQuad> pUp,
        List<BakedQuad> pDown
    ) {
        this.all = pAll;
        this.unculled = pUnculled;
        this.north = pNorth;
        this.south = pSouth;
        this.east = pEast;
        this.west = pWest;
        this.up = pUp;
        this.down = pDown;
    }

    public List<BakedQuad> getQuads(@Nullable Direction pDirection) {
        return switch (pDirection) {
            case null -> this.unculled;
            case NORTH -> this.north;
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            case UP -> this.up;
            case DOWN -> this.down;
        };
    }

    public List<BakedQuad> getAll() {
        return this.all;
    }

    public QuadCollection transform(net.minecraftforge.client.model.IQuadTransformer transformer) {
        return new QuadCollection(transformer.process(this.all), transformer.process(this.unculled),
            transformer.process(this.north), transformer.process(this.south), transformer.process(this.east),
            transformer.process(this.west), transformer.process(this.up), transformer.process(this.down));
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

        public QuadCollection.Builder addCulledFace(Direction pDirection, BakedQuad pQuad) {
            this.culledFaces.put(pDirection, pQuad);
            return this;
        }

        public QuadCollection.Builder addUnculledFace(BakedQuad pQuad) {
            this.unculledFaces.add(pQuad);
            return this;
        }

        private static QuadCollection createFromSublists(
            List<BakedQuad> pQuads, int pUnculledSize, int pNorthSize, int pSouthSize, int pEastSize, int pWestSize, int pUpSize, int pDownSize
        ) {
            int i = 0;
            int j;
            List<BakedQuad> list = pQuads.subList(i, j = i + pUnculledSize);
            List<BakedQuad> list1 = pQuads.subList(j, i = j + pNorthSize);
            int k;
            List<BakedQuad> list2 = pQuads.subList(i, k = i + pSouthSize);
            List<BakedQuad> list3 = pQuads.subList(k, i = k + pEastSize);
            int l;
            List<BakedQuad> list4 = pQuads.subList(i, l = i + pWestSize);
            List<BakedQuad> list5 = pQuads.subList(l, i = l + pUpSize);
            List<BakedQuad> list6 = pQuads.subList(i, i + pDownSize);
            return new QuadCollection(pQuads, list, list1, list2, list3, list4, list5, list6);
        }

        public QuadCollection build() {
            ImmutableList<BakedQuad> immutablelist = this.unculledFaces.build();
            if (this.culledFaces.isEmpty()) {
                return immutablelist.isEmpty()
                    ? QuadCollection.EMPTY
                    : new QuadCollection(immutablelist, immutablelist, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            } else {
                ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                builder.addAll(immutablelist);
                Collection<BakedQuad> collection = this.culledFaces.get(Direction.NORTH);
                builder.addAll(collection);
                Collection<BakedQuad> collection1 = this.culledFaces.get(Direction.SOUTH);
                builder.addAll(collection1);
                Collection<BakedQuad> collection2 = this.culledFaces.get(Direction.EAST);
                builder.addAll(collection2);
                Collection<BakedQuad> collection3 = this.culledFaces.get(Direction.WEST);
                builder.addAll(collection3);
                Collection<BakedQuad> collection4 = this.culledFaces.get(Direction.UP);
                builder.addAll(collection4);
                Collection<BakedQuad> collection5 = this.culledFaces.get(Direction.DOWN);
                builder.addAll(collection5);
                return createFromSublists(
                    builder.build(),
                    immutablelist.size(),
                    collection.size(),
                    collection1.size(),
                    collection2.size(),
                    collection3.size(),
                    collection4.size(),
                    collection5.size()
                );
            }
        }
    }
}
