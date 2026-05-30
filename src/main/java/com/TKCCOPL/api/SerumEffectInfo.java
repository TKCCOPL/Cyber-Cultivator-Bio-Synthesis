package com.TKCCOPL.api;

/** 血清效果参数（只读） */
public record SerumEffectInfo(
    String effectId,
    int baseDuration,
    int baseAmplifier,
    double durationMultiplier,
    int activity
) {}
