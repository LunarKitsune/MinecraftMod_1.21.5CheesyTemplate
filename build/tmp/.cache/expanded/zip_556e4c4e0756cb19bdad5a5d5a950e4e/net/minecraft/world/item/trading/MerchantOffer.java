package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class MerchantOffer {
    public static final Codec<MerchantOffer> CODEC = RecordCodecBuilder.create(
        p_327696_ -> p_327696_.group(
                ItemCost.CODEC.fieldOf("buy").forGetter(p_328146_ -> p_328146_.baseCostA),
                ItemCost.CODEC.lenientOptionalFieldOf("buyB").forGetter(p_329936_ -> p_329936_.costB),
                ItemStack.CODEC.fieldOf("sell").forGetter(p_330911_ -> p_330911_.result),
                Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(p_329708_ -> p_329708_.uses),
                Codec.INT.lenientOptionalFieldOf("maxUses", 4).forGetter(p_334393_ -> p_334393_.maxUses),
                Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(p_334163_ -> p_334163_.rewardExp),
                Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(p_331018_ -> p_331018_.specialPriceDiff),
                Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(p_334425_ -> p_334425_.demand),
                Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", 0.0F).forGetter(p_335604_ -> p_335604_.priceMultiplier),
                Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(p_334362_ -> p_334362_.xp)
            )
            .apply(p_327696_, MerchantOffer::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffer> STREAM_CODEC = StreamCodec.of(
        MerchantOffer::writeToStream, MerchantOffer::createFromStream
    );
    private final ItemCost baseCostA;
    private final Optional<ItemCost> costB;
    private final ItemStack result;
    private int uses;
    private final int maxUses;
    private final boolean rewardExp;
    private int specialPriceDiff;
    private int demand;
    private final float priceMultiplier;
    private final int xp;

    private MerchantOffer(
        ItemCost pBaseCostA,
        Optional<ItemCost> pCostB,
        ItemStack pResult,
        int pUses,
        int pMaxUses,
        boolean pRewardExp,
        int pSpecialPriceDiff,
        int pDemand,
        float pPriceMultiplier,
        int pXp
    ) {
        this.baseCostA = pBaseCostA;
        this.costB = pCostB;
        this.result = pResult;
        this.uses = pUses;
        this.maxUses = pMaxUses;
        this.rewardExp = pRewardExp;
        this.specialPriceDiff = pSpecialPriceDiff;
        this.demand = pDemand;
        this.priceMultiplier = pPriceMultiplier;
        this.xp = pXp;
    }

    public MerchantOffer(ItemCost pBaseCostA, ItemStack pResult, int pMaxUses, int pXp, float pPriceMultiplier) {
        this(pBaseCostA, Optional.empty(), pResult, pMaxUses, pXp, pPriceMultiplier);
    }

    public MerchantOffer(ItemCost pBaseCostA, Optional<ItemCost> pCostB, ItemStack pResult, int pMaxUses, int pXp, float pPriceMultiplier) {
        this(pBaseCostA, pCostB, pResult, 0, pMaxUses, pXp, pPriceMultiplier);
    }

    public MerchantOffer(ItemCost pBaseCostA, Optional<ItemCost> pCostB, ItemStack pResult, int pUses, int pMaxUses, int pXp, float pPriceMultiplier) {
        this(pBaseCostA, pCostB, pResult, pUses, pMaxUses, pXp, pPriceMultiplier, 0);
    }

    public MerchantOffer(
        ItemCost pBaseCostA, Optional<ItemCost> pCostB, ItemStack pResult, int pUses, int pMaxUses, int pXp, float pPriceMultiplier, int pDemand
    ) {
        this(pBaseCostA, pCostB, pResult, pUses, pMaxUses, true, 0, pDemand, pPriceMultiplier, pXp);
    }

    private MerchantOffer(MerchantOffer pOther) {
        this(
            pOther.baseCostA,
            pOther.costB,
            pOther.result.copy(),
            pOther.uses,
            pOther.maxUses,
            pOther.rewardExp,
            pOther.specialPriceDiff,
            pOther.demand,
            pOther.priceMultiplier,
            pOther.xp
        );
    }

    public ItemStack getBaseCostA() {
        return this.baseCostA.itemStack();
    }

    public ItemStack getCostA() {
        return this.baseCostA.itemStack().copyWithCount(this.getModifiedCostCount(this.baseCostA));
    }

    private int getModifiedCostCount(ItemCost pItemCost) {
        int i = pItemCost.count();
        int j = Math.max(0, Mth.floor(i * this.demand * this.priceMultiplier));
        return Mth.clamp(i + j + this.specialPriceDiff, 1, pItemCost.itemStack().getMaxStackSize());
    }

    public ItemStack getCostB() {
        return this.costB.map(ItemCost::itemStack).orElse(ItemStack.EMPTY);
    }

    public ItemCost getItemCostA() {
        return this.baseCostA;
    }

    public Optional<ItemCost> getItemCostB() {
        return this.costB;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public void updateDemand() {
        this.demand = this.demand + this.uses - (this.maxUses - this.uses);
    }

    public ItemStack assemble() {
        return this.result.copy();
    }

    public int getUses() {
        return this.uses;
    }

    public void resetUses() {
        this.uses = 0;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    public void increaseUses() {
        this.uses++;
    }

    public int getDemand() {
        return this.demand;
    }

    public void addToSpecialPriceDiff(int pAdd) {
        this.specialPriceDiff += pAdd;
    }

    public void resetSpecialPriceDiff() {
        this.specialPriceDiff = 0;
    }

    public int getSpecialPriceDiff() {
        return this.specialPriceDiff;
    }

    public void setSpecialPriceDiff(int pPrice) {
        this.specialPriceDiff = pPrice;
    }

    public float getPriceMultiplier() {
        return this.priceMultiplier;
    }

    public int getXp() {
        return this.xp;
    }

    public boolean isOutOfStock() {
        return this.uses >= this.maxUses;
    }

    public void setToOutOfStock() {
        this.uses = this.maxUses;
    }

    public boolean needsRestock() {
        return this.uses > 0;
    }

    public boolean shouldRewardExp() {
        return this.rewardExp;
    }

    public boolean satisfiedBy(ItemStack pPlayerOfferA, ItemStack pPlayerOfferB) {
        if (!this.baseCostA.test(pPlayerOfferA) || pPlayerOfferA.getCount() < this.getModifiedCostCount(this.baseCostA)) {
            return false;
        } else {
            return !this.costB.isPresent()
                ? pPlayerOfferB.isEmpty()
                : this.costB.get().test(pPlayerOfferB) && pPlayerOfferB.getCount() >= this.costB.get().count();
        }
    }

    public boolean take(ItemStack pPlayerOfferA, ItemStack pPlayerOfferB) {
        if (!this.satisfiedBy(pPlayerOfferA, pPlayerOfferB)) {
            return false;
        } else {
            pPlayerOfferA.shrink(this.getCostA().getCount());
            if (!this.getCostB().isEmpty()) {
                pPlayerOfferB.shrink(this.getCostB().getCount());
            }

            return true;
        }
    }

    public MerchantOffer copy() {
        return new MerchantOffer(this);
    }

    private static void writeToStream(RegistryFriendlyByteBuf pBuffer, MerchantOffer pOffer) {
        ItemCost.STREAM_CODEC.encode(pBuffer, pOffer.getItemCostA());
        ItemStack.STREAM_CODEC.encode(pBuffer, pOffer.getResult());
        ItemCost.OPTIONAL_STREAM_CODEC.encode(pBuffer, pOffer.getItemCostB());
        pBuffer.writeBoolean(pOffer.isOutOfStock());
        pBuffer.writeInt(pOffer.getUses());
        pBuffer.writeInt(pOffer.getMaxUses());
        pBuffer.writeInt(pOffer.getXp());
        pBuffer.writeInt(pOffer.getSpecialPriceDiff());
        pBuffer.writeFloat(pOffer.getPriceMultiplier());
        pBuffer.writeInt(pOffer.getDemand());
    }

    public static MerchantOffer createFromStream(RegistryFriendlyByteBuf pBuffer) {
        ItemCost itemcost = ItemCost.STREAM_CODEC.decode(pBuffer);
        ItemStack itemstack = ItemStack.STREAM_CODEC.decode(pBuffer);
        Optional<ItemCost> optional = ItemCost.OPTIONAL_STREAM_CODEC.decode(pBuffer);
        boolean flag = pBuffer.readBoolean();
        int i = pBuffer.readInt();
        int j = pBuffer.readInt();
        int k = pBuffer.readInt();
        int l = pBuffer.readInt();
        float f = pBuffer.readFloat();
        int i1 = pBuffer.readInt();
        MerchantOffer merchantoffer = new MerchantOffer(itemcost, optional, itemstack, i, j, k, f, i1);
        if (flag) {
            merchantoffer.setToOutOfStock();
        }

        merchantoffer.setSpecialPriceDiff(l);
        return merchantoffer;
    }
}