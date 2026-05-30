package com.TKCCOPL.api;

/** 冷凝器状态（只读快照） */
public record CondenserInfo(
    int progress, int maxProgress,
    int stock, int maxStock,
    boolean isFull
) {}
