package net.minecraft.world.level.pathfinder;

public class Path {
    private PathPoint[] heap = new PathPoint[128];
    private int size;

    public PathPoint insert(PathPoint node) {
        if (node.heapIdx >= 0) {
            throw new IllegalStateException("OW KNOWS!");
        } else {
            if (this.size == this.heap.length) {
                PathPoint[] nodes = new PathPoint[this.size << 1];
                System.arraycopy(this.heap, 0, nodes, 0, this.size);
                this.heap = nodes;
            }

            this.heap[this.size] = node;
            node.heapIdx = this.size;
            this.upHeap(this.size++);
            return node;
        }
    }

    public void clear() {
        this.size = 0;
    }

    public PathPoint peek() {
        return this.heap[0];
    }

    public PathPoint pop() {
        PathPoint node = this.heap[0];
        this.heap[0] = this.heap[--this.size];
        this.heap[this.size] = null;
        if (this.size > 0) {
            this.downHeap(0);
        }

        node.heapIdx = -1;
        return node;
    }

    public void remove(PathPoint node) {
        this.heap[node.heapIdx] = this.heap[--this.size];
        this.heap[this.size] = null;
        if (this.size > node.heapIdx) {
            if (this.heap[node.heapIdx].f < node.f) {
                this.upHeap(node.heapIdx);
            } else {
                this.downHeap(node.heapIdx);
            }
        }

        node.heapIdx = -1;
    }

    public void changeCost(PathPoint node, float weight) {
        float f = node.f;
        node.f = weight;
        if (weight < f) {
            this.upHeap(node.heapIdx);
        } else {
            this.downHeap(node.heapIdx);
        }

    }

    public int size() {
        return this.size;
    }

    private void upHeap(int index) {
        PathPoint node = this.heap[index];

        int i;
        for(float f = node.f; index > 0; index = i) {
            i = index - 1 >> 1;
            PathPoint node2 = this.heap[i];
            if (!(f < node2.f)) {
                break;
            }

            this.heap[index] = node2;
            node2.heapIdx = index;
        }

        this.heap[index] = node;
        node.heapIdx = index;
    }

    private void downHeap(int index) {
        PathPoint node = this.heap[index];
        float f = node.f;

        while(true) {
            int i = 1 + (index << 1);
            int j = i + 1;
            if (i >= this.size) {
                break;
            }

            PathPoint node2 = this.heap[i];
            float g = node2.f;
            PathPoint node3;
            float h;
            if (j >= this.size) {
                node3 = null;
                h = Float.POSITIVE_INFINITY;
            } else {
                node3 = this.heap[j];
                h = node3.f;
            }

            if (g < h) {
                if (!(g < f)) {
                    break;
                }

                this.heap[index] = node2;
                node2.heapIdx = index;
                index = i;
            } else {
                if (!(h < f)) {
                    break;
                }

                this.heap[index] = node3;
                node3.heapIdx = index;
                index = j;
            }
        }

        this.heap[index] = node;
        node.heapIdx = index;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public PathPoint[] getHeap() {
        PathPoint[] nodes = new PathPoint[this.size()];
        System.arraycopy(this.heap, 0, nodes, 0, this.size());
        return nodes;
    }
}
