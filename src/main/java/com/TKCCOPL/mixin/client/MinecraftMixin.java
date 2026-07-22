package com.TKCCOPL.mixin.client;

import com.TKCCOPL.client.s02.S02DetectionClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将 S-02 的私有目标加入当前客户端的原版发光判断。
 * <p>
 * 只扩展返回值，不修改实体同步数据，因此其他客户端不会看到该轮廓；目标颜色、
 * 穿墙效果和实体渲染层均继续由原版 {@link Minecraft} 渲染管线处理。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void cybercultivator$showS02TargetOutline(Entity entity,
                                                       CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && S02DetectionClientState.shouldAppearGlowing(entity)) {
            callback.setReturnValue(true);
        }
    }
}
