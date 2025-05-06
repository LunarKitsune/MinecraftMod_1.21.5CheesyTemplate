package net.minecraft.world.level.pathfinder;

import net.minecraft.network.FriendlyByteBuf;

public class Target extends Node {
    private float bestHeuristic = Float.MAX_VALUE;
    private Node bestNode;
    private boolean reached;

    public Target(Node pNode) {
        super(pNode.x, pNode.y, pNode.z);
    }

    public Target(int p_77498_, int p_77499_, int p_77500_) {
        super(p_77498_, p_77499_, p_77500_);
    }

    public void updateBest(float pHeuristic, Node pNode) {
        if (pHeuristic < this.bestHeuristic) {
            this.bestHeuristic = pHeuristic;
            this.bestNode = pNode;
        }
    }

    public Node getBestNode() {
        return this.bestNode;
    }

    public void setReached() {
        this.reached = true;
    }

    public boolean isReached() {
        return this.reached;
    }

    public static Target createFromStream(FriendlyByteBuf pBuffer) {
        Target target = new Target(pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt());
        readContents(pBuffer, target);
        return target;
    }
}