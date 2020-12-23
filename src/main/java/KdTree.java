public class KdTree {
    public KdNode root = null;
    private int size;

    public class KdNode {
        public GraphDB.Node point;
        private KdNode leftChild;
        private KdNode rightChild;
        private int level;

        private KdNode(GraphDB.Node n, int lev) {
            point = n;
            level = lev;
        }
    }

    public void insert(GraphDB.Node n) {
        root = insertHelper(root, n, 0);
    }

    private KdNode insertHelper(KdNode root, GraphDB.Node n, int lev) {
        if (root == null) {
            size += 1;
            return new KdNode(n, lev);
        }

        if (compare(root, n) > 0) {
            root.leftChild = insertHelper(root.leftChild, n, lev + 1);
        } else {
            root.rightChild = insertHelper(root.rightChild, n, lev + 1);
        }
        return root;
    }

    private double compare(KdNode k, GraphDB.Node n) {
        if (k.level % 2 == 0) {
            return k.point.lon - n.lon;
        } else {
            return k.point.lat - n.lat;
        }
    }

    public KdNode nearest(KdNode k, GraphDB.Node goal, KdNode best) {
        KdNode goodSide;
        KdNode badSide;
        if (k == null) {
            return best;
        }
        if (GraphDB.distance(k.point, goal) < GraphDB.distance(best.point, goal)) {
            best = k;
        }
        if (compare(k, goal) > 0) {
            goodSide = k.leftChild;
            badSide = k.rightChild;
        } else {
            goodSide = k.rightChild;
            badSide = k.leftChild;
        }
        best = nearest(goodSide, goal, best);
        if (Math.abs(compare(k, goal)) < GraphDB.distance(best.point, goal)) {
            best = nearest(badSide, goal, best);
        }
        return best;
    }

}
