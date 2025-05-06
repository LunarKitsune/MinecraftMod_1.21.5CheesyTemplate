package net.minecraft.world.entity.player;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;

public class StackedContents<T> {
    public final Reference2IntOpenHashMap<T> amounts = new Reference2IntOpenHashMap<>();

    boolean hasAtLeast(T pItem, int pAmount) {
        return this.amounts.getInt(pItem) >= pAmount;
    }

    void take(T pItem, int pAmount) {
        int i = this.amounts.addTo(pItem, -pAmount);
        if (i < pAmount) {
            throw new IllegalStateException("Took " + pAmount + " items, but only had " + i);
        }
    }

    void put(T pItem, int pAmount) {
        this.amounts.addTo(pItem, pAmount);
    }

    public boolean tryPick(List<? extends StackedContents.IngredientInfo<T>> pIngredients, int pAmount, @Nullable StackedContents.Output<T> pOutput) {
        return new StackedContents.RecipePicker(pIngredients).tryPick(pAmount, pOutput);
    }

    public int tryPickAll(List<? extends StackedContents.IngredientInfo<T>> pIngredients, int pAmount, @Nullable StackedContents.Output<T> pOutput) {
        return new StackedContents.RecipePicker(pIngredients).tryPickAll(pAmount, pOutput);
    }

    public void clear() {
        this.amounts.clear();
    }

    public void account(T pItem, int pAmount) {
        this.put(pItem, pAmount);
    }

    List<T> getUniqueAvailableIngredientItems(Iterable<? extends StackedContents.IngredientInfo<T>> pIngredients) {
        List<T> list = new ArrayList<>();

        for (Entry<T> entry : Reference2IntMaps.fastIterable(this.amounts)) {
            if (entry.getIntValue() > 0 && anyIngredientMatches(pIngredients, entry.getKey())) {
                list.add(entry.getKey());
            }
        }

        return list;
    }

    private static <T> boolean anyIngredientMatches(Iterable<? extends StackedContents.IngredientInfo<T>> pIngredients, T pItem) {
        for (StackedContents.IngredientInfo<T> ingredientinfo : pIngredients) {
            if (ingredientinfo.acceptsItem(pItem)) {
                return true;
            }
        }

        return false;
    }

    @VisibleForTesting
    public int getResultUpperBound(List<? extends StackedContents.IngredientInfo<T>> pIngredients) {
        int i = Integer.MAX_VALUE;
        ObjectIterable<Entry<T>> objectiterable = Reference2IntMaps.fastIterable(this.amounts);

        label31:
        for (StackedContents.IngredientInfo<T> ingredientinfo : pIngredients) {
            int j = 0;

            for (Entry<T> entry : objectiterable) {
                int k = entry.getIntValue();
                if (k > j) {
                    if (ingredientinfo.acceptsItem(entry.getKey())) {
                        j = k;
                    }

                    if (j >= i) {
                        continue label31;
                    }
                }
            }

            i = j;
            if (j == 0) {
                break;
            }
        }

        return i;
    }

    @FunctionalInterface
    public interface IngredientInfo<T> {
        boolean acceptsItem(T pItem);
    }

    @FunctionalInterface
    public interface Output<T> {
        void accept(T pItem);
    }

    class RecipePicker {
        private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
        private final int ingredientCount;
        private final List<T> items;
        private final int itemCount;
        private final BitSet data;
        private final IntList path = new IntArrayList();

        public RecipePicker(final List<? extends StackedContents.IngredientInfo<T>> pIngredients) {
            this.ingredients = pIngredients;
            this.ingredientCount = pIngredients.size();
            this.items = StackedContents.this.getUniqueAvailableIngredientItems(pIngredients);
            this.itemCount = this.items.size();
            this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
            this.setInitialConnections();
        }

        private void setInitialConnections() {
            for (int i = 0; i < this.ingredientCount; i++) {
                StackedContents.IngredientInfo<T> ingredientinfo = (StackedContents.IngredientInfo<T>)this.ingredients.get(i);

                for (int j = 0; j < this.itemCount; j++) {
                    if (ingredientinfo.acceptsItem(this.items.get(j))) {
                        this.setConnection(j, i);
                    }
                }
            }
        }

        public boolean tryPick(int pAmount, @Nullable StackedContents.Output<T> pOutput) {
            if (pAmount <= 0) {
                return true;
            } else {
                int i = 0;

                while (true) {
                    IntList intlist = this.tryAssigningNewItem(pAmount);
                    if (intlist == null) {
                        boolean flag = i == this.ingredientCount;
                        boolean flag1 = flag && pOutput != null;
                        this.clearAllVisited();
                        this.clearSatisfied();

                        for (int k1 = 0; k1 < this.ingredientCount; k1++) {
                            for (int l1 = 0; l1 < this.itemCount; l1++) {
                                if (this.isAssigned(l1, k1)) {
                                    this.unassign(l1, k1);
                                    StackedContents.this.put(this.items.get(l1), pAmount);
                                    if (flag1) {
                                        pOutput.accept(this.items.get(l1));
                                    }
                                    break;
                                }
                            }
                        }

                        assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();

                        return flag;
                    }

                    int j = intlist.getInt(0);
                    StackedContents.this.take(this.items.get(j), pAmount);
                    int k = intlist.size() - 1;
                    this.setSatisfied(intlist.getInt(k));
                    i++;

                    for (int l = 0; l < intlist.size() - 1; l++) {
                        if (isPathIndexItem(l)) {
                            int i1 = intlist.getInt(l);
                            int j1 = intlist.getInt(l + 1);
                            this.assign(i1, j1);
                        } else {
                            int i2 = intlist.getInt(l + 1);
                            int j2 = intlist.getInt(l);
                            this.unassign(i2, j2);
                        }
                    }
                }
            }
        }

        private static boolean isPathIndexItem(int pIndex) {
            return (pIndex & 1) == 0;
        }

        @Nullable
        private IntList tryAssigningNewItem(int pAmount) {
            this.clearAllVisited();

            for (int i = 0; i < this.itemCount; i++) {
                if (StackedContents.this.hasAtLeast(this.items.get(i), pAmount)) {
                    IntList intlist = this.findNewItemAssignmentPath(i);
                    if (intlist != null) {
                        return intlist;
                    }
                }
            }

            return null;
        }

        @Nullable
        private IntList findNewItemAssignmentPath(int pAmount) {
            this.path.clear();
            this.visitItem(pAmount);
            this.path.add(pAmount);

            while (!this.path.isEmpty()) {
                int i = this.path.size();
                if (isPathIndexItem(i - 1)) {
                    int l = this.path.getInt(i - 1);

                    for (int j1 = 0; j1 < this.ingredientCount; j1++) {
                        if (!this.hasVisitedIngredient(j1) && this.hasConnection(l, j1) && !this.isAssigned(l, j1)) {
                            this.visitIngredient(j1);
                            this.path.add(j1);
                            break;
                        }
                    }
                } else {
                    int j = this.path.getInt(i - 1);
                    if (!this.isSatisfied(j)) {
                        return this.path;
                    }

                    for (int k = 0; k < this.itemCount; k++) {
                        if (!this.hasVisitedItem(k) && this.isAssigned(k, j)) {
                            assert this.hasConnection(k, j);

                            this.visitItem(k);
                            this.path.add(k);
                            break;
                        }
                    }
                }

                int i1 = this.path.size();
                if (i1 == i) {
                    this.path.removeInt(i1 - 1);
                }
            }

            return null;
        }

        private int visitedIngredientOffset() {
            return 0;
        }

        private int visitedIngredientCount() {
            return this.ingredientCount;
        }

        private int visitedItemOffset() {
            return this.visitedIngredientOffset() + this.visitedIngredientCount();
        }

        private int visitedItemCount() {
            return this.itemCount;
        }

        private int satisfiedOffset() {
            return this.visitedItemOffset() + this.visitedItemCount();
        }

        private int satisfiedCount() {
            return this.ingredientCount;
        }

        private int connectionOffset() {
            return this.satisfiedOffset() + this.satisfiedCount();
        }

        private int connectionCount() {
            return this.ingredientCount * this.itemCount;
        }

        private int residualOffset() {
            return this.connectionOffset() + this.connectionCount();
        }

        private int residualCount() {
            return this.ingredientCount * this.itemCount;
        }

        private boolean isSatisfied(int pStackingIndex) {
            return this.data.get(this.getSatisfiedIndex(pStackingIndex));
        }

        private void setSatisfied(int pStackingIndex) {
            this.data.set(this.getSatisfiedIndex(pStackingIndex));
        }

        private int getSatisfiedIndex(int pStackingIndex) {
            assert pStackingIndex >= 0 && pStackingIndex < this.ingredientCount;

            return this.satisfiedOffset() + pStackingIndex;
        }

        private void clearSatisfied() {
            this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
        }

        private void setConnection(int pItemIndex, int pIngredientIndex) {
            this.data.set(this.getConnectionIndex(pItemIndex, pIngredientIndex));
        }

        private boolean hasConnection(int pItemIndex, int pIngredientIndex) {
            return this.data.get(this.getConnectionIndex(pItemIndex, pIngredientIndex));
        }

        private int getConnectionIndex(int pItemIndex, int pIngredientIndex) {
            assert pItemIndex >= 0 && pItemIndex < this.itemCount;

            assert pIngredientIndex >= 0 && pIngredientIndex < this.ingredientCount;

            return this.connectionOffset() + pItemIndex * this.ingredientCount + pIngredientIndex;
        }

        private boolean isAssigned(int pItemIndex, int pIngredientIndex) {
            return this.data.get(this.getResidualIndex(pItemIndex, pIngredientIndex));
        }

        private void assign(int pItemIndex, int pIngredientIndex) {
            int i = this.getResidualIndex(pItemIndex, pIngredientIndex);

            assert !this.data.get(i);

            this.data.set(i);
        }

        private void unassign(int pItemIndex, int pIngredientIndex) {
            int i = this.getResidualIndex(pItemIndex, pIngredientIndex);

            assert this.data.get(i);

            this.data.clear(i);
        }

        private int getResidualIndex(int pItemIndex, int pIngredientIndex) {
            assert pItemIndex >= 0 && pItemIndex < this.itemCount;

            assert pIngredientIndex >= 0 && pIngredientIndex < this.ingredientCount;

            return this.residualOffset() + pItemIndex * this.ingredientCount + pIngredientIndex;
        }

        private void visitIngredient(int pIngredientIndex) {
            this.data.set(this.getVisitedIngredientIndex(pIngredientIndex));
        }

        private boolean hasVisitedIngredient(int pIngredientIndex) {
            return this.data.get(this.getVisitedIngredientIndex(pIngredientIndex));
        }

        private int getVisitedIngredientIndex(int pIngredientIndex) {
            assert pIngredientIndex >= 0 && pIngredientIndex < this.ingredientCount;

            return this.visitedIngredientOffset() + pIngredientIndex;
        }

        private void visitItem(int pItemIndex) {
            this.data.set(this.getVisitiedItemIndex(pItemIndex));
        }

        private boolean hasVisitedItem(int pItemIndex) {
            return this.data.get(this.getVisitiedItemIndex(pItemIndex));
        }

        private int getVisitiedItemIndex(int pItemIndex) {
            assert pItemIndex >= 0 && pItemIndex < this.itemCount;

            return this.visitedItemOffset() + pItemIndex;
        }

        private void clearAllVisited() {
            this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
            this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
        }

        private void clearRange(int pOffset, int pCount) {
            this.data.clear(pOffset, pOffset + pCount);
        }

        public int tryPickAll(int pAmount, @Nullable StackedContents.Output<T> pOutput) {
            int i = 0;
            int j = Math.min(pAmount, StackedContents.this.getResultUpperBound(this.ingredients)) + 1;

            while (true) {
                int k = (i + j) / 2;
                if (this.tryPick(k, null)) {
                    if (j - i <= 1) {
                        if (k > 0) {
                            this.tryPick(k, pOutput);
                        }

                        return k;
                    }

                    i = k;
                } else {
                    j = k;
                }
            }
        }
    }
}