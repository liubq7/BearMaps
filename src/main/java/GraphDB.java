import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Beiqian Liu, Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */
    private Map<Long, Node> nodes = new HashMap<>();
    private Map<Long, Way> allWays = new HashMap<>();
    private KdTree kdTree = new KdTree();
    private Tries tries = new Tries();
    private Map<String, Object> locations = new HashMap<>();

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        for (long id : nodes.keySet()) {
            kdTree.insert(nodes.get(id));
        }
    }

    public Tries getTries() {
        return tries;
    }

    public void addName(String s) {
        tries.add(s);
    }

    public void addLocation(Node n) {
        String loc = n.location.toLowerCase();
        if (locations.containsKey(loc)) {
            ((ArrayList) locations.get(loc)).add(n);
        } else {
            List<Node> nodes = new ArrayList<>();
            nodes.add(n);
            locations.put(loc, nodes);
        }
    }

    public Map<String, Object> getLocations() {
        return locations;
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Iterator<Long> iterator = nodes.keySet().iterator();
        while (iterator.hasNext()) {
            long id = iterator.next();
            if (nodes.get(id).adj.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        ArrayList<Long> IDs = new ArrayList<>();
        for (long id : nodes.keySet()) {
            IDs.add(id);
        }
        return IDs;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return nodes.get(v).adj;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }
    static double distance(Node n1, Node n2) {
        return distance(n1.lon, n1.lat, n2.lon, n2.lat);
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        Node goal = new Node(lon, lat);
        return kdTree.nearest(kdTree.root, goal, kdTree.root).point.id;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return nodes.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return nodes.get(v).lat;
    }

    void addNode(Node n) {
        nodes.put(n.id, n);
    }

    void addEdge(long n1, long n2) {
        nodes.get(n1).adj.add(n2);
        nodes.get(n2).adj.add(n1);
    }

    void removeNode(Node n) {
        nodes.remove(n.id);
    }

    static class Node {
        long id;
        double lon;
        double lat;
        String location = "";
        ArrayList<Long> adj = new ArrayList<>();
        ArrayList<Long> ways = new ArrayList<>();

        double priority;

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }
        Node(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
    }

    private class nodeComparator implements Comparator<Long> {
        @Override
        public int compare(Long n1, Long n2) {
            double result = nodes.get(n1).priority - nodes.get(n2).priority;
            if (result > 0) {
                return 1;
            } else if (result < 0) {
                return -1;
            } else {
                return 0;
            }
//            return (int) (nodes.get(n1).priority - nodes.get(n2).priority);
        }
    }

    public nodeComparator getNodeComparator() {
        return new nodeComparator();
    }

    public Map<Long, Node> getNodes() {
        return nodes;
    }


    static class Way {
        long id;
        ArrayList<Long> nds;
        String name;

        Way(long id) {
            this.id = id;
            nds = new ArrayList<>();
        }
    }

    public void addWay(Way way) {
        allWays.put(way.id, way);
    }

    public String getWayName(long v, long w) {
        Node vNode = nodes.get(v);
        Node wNode = nodes.get(w);
        for (long id1 : vNode.ways) {
            for (long id2: wNode.ways) {
                if (id1 == id2) {
                    String wayName = allWays.get(id1).name;
                    if (wayName == null) {
                        return "unknown road";
                    } else {
                        return wayName;
                    }
                }
            }
        }
        return null;
    }
}
