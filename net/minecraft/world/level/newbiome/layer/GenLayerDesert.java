package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerDesert implements AreaTransformer7 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        int[] is = new int[1];
        if (!this.checkEdge(is, center) && !this.checkEdgeStrict(is, n, e, s, w, center, 38, 37) && !this.checkEdgeStrict(is, n, e, s, w, center, 39, 37) && !this.checkEdgeStrict(is, n, e, s, w, center, 32, 5)) {
            if (center != 2 || n != 12 && e != 12 && w != 12 && s != 12) {
                if (center == 6) {
                    if (n == 2 || e == 2 || w == 2 || s == 2 || n == 30 || e == 30 || w == 30 || s == 30 || n == 12 || e == 12 || w == 12 || s == 12) {
                        return 1;
                    }

                    if (n == 21 || s == 21 || e == 21 || w == 21 || n == 168 || s == 168 || e == 168 || w == 168) {
                        return 23;
                    }
                }

                return center;
            } else {
                return 34;
            }
        } else {
            return is[0];
        }
    }

    private boolean checkEdge(int[] ids, int id) {
        if (!GenLayers.isSame(id, 3)) {
            return false;
        } else {
            ids[0] = id;
            return true;
        }
    }

    private boolean checkEdgeStrict(int[] ids, int n, int e, int s, int w, int center, int id1, int id2) {
        if (center != id1) {
            return false;
        } else {
            if (GenLayers.isSame(n, id1) && GenLayers.isSame(e, id1) && GenLayers.isSame(w, id1) && GenLayers.isSame(s, id1)) {
                ids[0] = center;
            } else {
                ids[0] = id2;
            }

            return true;
        }
    }
}
