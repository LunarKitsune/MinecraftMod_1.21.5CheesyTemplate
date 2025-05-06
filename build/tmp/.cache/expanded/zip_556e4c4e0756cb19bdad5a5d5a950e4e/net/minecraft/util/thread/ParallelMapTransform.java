package net.minecraft.util.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.Mth;

public class ParallelMapTransform {
    private static final int DEFAULT_TASKS_PER_THREAD = 16;

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> pInputs, BiFunction<K, U, V> pOperation, int pMaxTasksPerBatch, Executor pExecutor) {
        int i = pInputs.size();
        if (i == 0) {
            return CompletableFuture.completedFuture(Map.of());
        } else if (i == 1) {
            Entry<K, U> entry = pInputs.entrySet().iterator().next();
            K k = entry.getKey();
            U u = entry.getValue();
            return CompletableFuture.supplyAsync(() -> {
                V v = pOperation.apply(k, u);
                return v != null ? Map.of(k, v) : Map.of();
            }, pExecutor);
        } else {
            ParallelMapTransform.SplitterBase<K, U, V> splitterbase = (ParallelMapTransform.SplitterBase<K, U, V>)(i <= pMaxTasksPerBatch
                ? new ParallelMapTransform.SingleTaskSplitter<>(pOperation, i)
                : new ParallelMapTransform.BatchedTaskSplitter<>(pOperation, i, pMaxTasksPerBatch));
            return splitterbase.scheduleTasks(pInputs, pExecutor);
        }
    }

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> pInputs, BiFunction<K, U, V> pOperation, Executor pExecutor) {
        int i = Util.maxAllowedExecutorThreads() * 16;
        return schedule(pInputs, pOperation, i, pExecutor);
    }

    static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
        private final Map<K, V> result;
        private final int batchSize;
        private final int firstUndersizedBatchIndex;

        BatchedTaskSplitter(BiFunction<K, U, V> p_397810_, int p_391927_, int p_394050_) {
            super(p_397810_, p_391927_, p_394050_);
            this.result = new HashMap<>(p_391927_);
            this.batchSize = Mth.positiveCeilDiv(p_391927_, p_394050_);
            int i = this.batchSize * p_394050_;
            int j = i - p_391927_;
            this.firstUndersizedBatchIndex = p_394050_ - j;

            assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= p_394050_;
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_395039_, int p_391190_, int p_393559_, Executor p_393890_) {
            int i = p_393559_ - p_391190_;

            assert i == this.batchSize || i == this.batchSize - 1;

            return CompletableFuture.runAsync(createTask(this.result, p_391190_, p_393559_, p_395039_), p_393890_);
        }

        @Override
        protected int batchSize(int p_396019_) {
            return p_396019_ < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
        }

        private static <K, U, V> Runnable createTask(Map<K, V> pResult, int pLastScheduledIndex, int pCurrentIndex, ParallelMapTransform.Container<K, U, V> pContainer) {
            return () -> {
                for (int i = pLastScheduledIndex; i < pCurrentIndex; i++) {
                    pContainer.applyOperation(i);
                }

                synchronized (pResult) {
                    for (int j = pLastScheduledIndex; j < pCurrentIndex; j++) {
                        pContainer.copyOut(j, pResult);
                    }
                }
            };
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_396406_, ParallelMapTransform.Container<K, U, V> p_397157_) {
            Map<K, V> map = this.result;
            return p_396406_.thenApply(p_391758_ -> map);
        }
    }

    record Container<K, U, V>(BiFunction<K, U, V> operation, Object[] keys, Object[] values) {
        public Container(BiFunction<K, U, V> pOperation, int pSize) {
            this(pOperation, new Object[pSize], new Object[pSize]);
        }

        public void put(int pIndex, K pKey, U pValue) {
            this.keys[pIndex] = pKey;
            this.values[pIndex] = pValue;
        }

        @Nullable
        private K key(int pIndex) {
            return (K)this.keys[pIndex];
        }

        @Nullable
        private V output(int pIndex) {
            return (V)this.values[pIndex];
        }

        @Nullable
        private U input(int pIndex) {
            return (U)this.values[pIndex];
        }

        public void applyOperation(int pIndex) {
            this.values[pIndex] = this.operation.apply(this.key(pIndex), this.input(pIndex));
        }

        public void copyOut(int pIndex, Map<K, V> pOutputMap) {
            V v = this.output(pIndex);
            if (v != null) {
                K k = this.key(pIndex);
                pOutputMap.put(k, v);
            }
        }

        public int size() {
            return this.keys.length;
        }
    }

    static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
        SingleTaskSplitter(BiFunction<K, U, V> pOperation, int pSize) {
            super(pOperation, pSize, pSize);
        }

        @Override
        protected int batchSize(int p_397895_) {
            return 1;
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_394262_, int p_393896_, int p_396012_, Executor p_392458_) {
            assert p_393896_ + 1 == p_396012_;

            return CompletableFuture.runAsync(() -> p_394262_.applyOperation(p_393896_), p_392458_);
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_391498_, ParallelMapTransform.Container<K, U, V> p_397732_) {
            return p_391498_.thenApply(p_391357_ -> {
                Map<K, V> map = new HashMap<>(p_397732_.size());

                for (int i = 0; i < p_397732_.size(); i++) {
                    p_397732_.copyOut(i, map);
                }

                return map;
            });
        }
    }

    abstract static class SplitterBase<K, U, V> {
        private int lastScheduledIndex;
        private int currentIndex;
        private final CompletableFuture<?>[] tasks;
        private int batchIndex;
        private final ParallelMapTransform.Container<K, U, V> container;

        SplitterBase(BiFunction<K, U, V> pOperation, int pContainerSize, int pNumBatches) {
            this.container = new ParallelMapTransform.Container<>(pOperation, pContainerSize);
            this.tasks = new CompletableFuture[pNumBatches];
        }

        private int pendingBatchSize() {
            return this.currentIndex - this.lastScheduledIndex;
        }

        public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> pInputs, Executor pExecutor) {
            pInputs.forEach((p_392446_, p_396440_) -> {
                this.container.put(this.currentIndex++, (K)p_392446_, (U)p_396440_);
                if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
                    this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, pExecutor);
                    this.lastScheduledIndex = this.currentIndex;
                }
            });

            assert this.currentIndex == this.container.size();

            assert this.lastScheduledIndex == this.currentIndex;

            assert this.batchIndex == this.tasks.length;

            return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
        }

        protected abstract int batchSize(int pBatchIndex);

        protected abstract CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> pContainer, int pLastScheduledIndex, int pCurrentIndex, Executor pExecutor);

        protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> pFuture, ParallelMapTransform.Container<K, U, V> pContainer);
    }
}