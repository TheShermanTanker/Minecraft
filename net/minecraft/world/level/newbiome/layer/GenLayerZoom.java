package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer2;

public enum GenLayerZoom implements AreaTransformer2 {
    NORMAL,
    FUZZY {
        @Override
        protected int modeOrRandom(AreaContextTransformed<?> context, int tl, int tr, int bl, int br) {
            return context.random(tl, tr, bl, br);
        }
    };

    private static final int ZOOM_BITS = 1;
    private static final int ZOOM_MASK = 1;

    @Override
    public int getParentX(int x) {
        return x >> 1;
    }

    @Override
    public int getParentY(int y) {
        return y >> 1;
    }

    @Override
    public int applyPixel(AreaContextTransformed<?> context, Area parent, int x, int z) {
        int i = parent.get(this.getParentX(x), this.getParentY(z));
        context.initRandom((long)(x >> 1 << 1), (long)(z >> 1 << 1));
        int j = x & 1;
        int k = z & 1;
        if (j == 0 && k == 0) {
            return i;
        } else {
            int l = parent.get(this.getParentX(x), this.getParentY(z + 1));
            int m = context.random(i, l);
            if (j == 0 && k == 1) {
                return m;
            } else {
                int n = parent.get(this.getParentX(x + 1), this.getParentY(z));
                int o = context.random(i, n);
                if (j == 1 && k == 0) {
                    return o;
                } else {
                    int p = parent.get(this.getParentX(x + 1), this.getParentY(z + 1));
                    return this.modeOrRandom(context, i, n, l, p);
                }
            }
        }
    }

    protected int modeOrRandom(AreaContextTransformed<?> context, int tl, int tr, int bl, int br) {
        if (tr == bl && bl == br) {
            return tr;
        } else if (tl == tr && tl == bl) {
            return tl;
        } else if (tl == tr && tl == br) {
            return tl;
        } else if (tl == bl && tl == br) {
            return tl;
        } else if (tl == tr && bl != br) {
            return tl;
        } else if (tl == bl && tr != br) {
            return tl;
        } else if (tl == br && tr != bl) {
            return tl;
        } else if (tr == bl && tl != br) {
            return tr;
        } else if (tr == br && tl != bl) {
            return tr;
        } else {
            return bl == br && tl != tr ? bl : context.random(tl, tr, bl, br);
        }
    }
}
