package com.mojang.realmsclient.util.task;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ResettingTemplateWorldTask extends ResettingWorldTask {
    private final WorldTemplate template;

    public ResettingTemplateWorldTask(WorldTemplate pTemplate, long pServerId, Component pTitle, Runnable pCallback) {
        super(pServerId, pTitle, pCallback);
        this.template = pTemplate;
    }

    @Override
    protected void sendResetRequest(RealmsClient p_167673_, long p_167674_) throws RealmsServiceException {
        p_167673_.resetWorldWithTemplate(p_167674_, this.template.id);
    }
}