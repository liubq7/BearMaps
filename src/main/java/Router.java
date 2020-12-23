import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        long stNode = g.closest(stlon, stlat);
        long destNode = g.closest(destlon, destlat);
        Map<Long, Long> edgeTo = new HashMap<>();
        Map<Long, Double> distTo = new HashMap<>();
        HashSet<Long> marked = new HashSet<>();
        PriorityQueue<Long> pq = new PriorityQueue<>(g.getNodeComparator());

        Map<Long, GraphDB.Node> nodes = g.getNodes();

        for (Long id : nodes.keySet()) {
            if (id == stNode) {
                distTo.put(id, 0.0);
            } else {
                distTo.put(id, Double.POSITIVE_INFINITY);
            }
        }
        pq.add(stNode);
        while (!pq.isEmpty()) {
            long curr = pq.remove();
            if (marked.contains(curr)) {
                continue;
            }
            marked.add(curr);
            if (curr == destNode) {
                break;
            }
            Iterable<Long> adj = g.adjacent(curr);
            for (Long next : adj) {
                double newDistTo = distTo.get(curr) + g.distance(curr, next);
                if (newDistTo < distTo.get(next)) {
                    nodes.get(next).priority = newDistTo + g.distance(next, destNode);
                    pq.add(next);
                    edgeTo.put(next, curr);
                    distTo.put(next, newDistTo);
                }
            }
        }

        List<Long> path = new LinkedList<>();
        long pointer = destNode;
        path.add(pointer);
        while (pointer != stNode) {
            pointer = edgeTo.get(pointer);
            path.add(0, pointer);
        }
        return path;
    }


    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> navDirections = new LinkedList<>();
        String currWayName;
        String prevWayName = "";
        double currBearing;
        double prevBearing = 0;
        double dist = 0;
        NavigationDirection navDirection = new NavigationDirection();

        for (int i = 0; i < route.size() - 1; i++) {
            long curr = route.get(i);
            long next = route.get(i + 1);
            currWayName = g.getWayName(curr, next);
            currBearing = g.bearing(curr, next);

            if (currWayName.equals(prevWayName)) {
                dist += g.distance(curr, next);
            } else if (i == 0) {
                navDirection.direction = 0;
                navDirection.way = currWayName;
                dist += g.distance(curr, next);
            } else {
                navDirection.distance = dist;
                NavigationDirection newNavDirection = copy(navDirection);
                navDirections.add(newNavDirection);

                dist = g.distance(curr, next);
                navDirection.way = currWayName;

                double relativeBearing = currBearing - prevBearing;
                if (relativeBearing > 180) {
                    relativeBearing -= 360;
                } else if (relativeBearing < -180) {
                    relativeBearing += 360;
                }

                if (relativeBearing < -100) {
                    navDirection.direction = 6;
                } else if (relativeBearing < -30) {
                    navDirection.direction = 5;
                } else if (relativeBearing < -15) {
                    navDirection.direction = 2;
                } else if (relativeBearing < 15) {
                    navDirection.direction = 1;
                } else if (relativeBearing < 30) {
                    navDirection.direction = 3;
                } else if (relativeBearing < 100) {
                    navDirection.direction = 4;
                } else {
                    navDirection.direction = 7;
                }
            }
            if (i == route.size() - 2) {
                navDirection.distance = dist;
                navDirections.add(navDirection);
            }
            prevWayName = currWayName;
            prevBearing = currBearing;
        }

        return navDirections;
    }

    private static NavigationDirection copy(NavigationDirection navDirection) {
        NavigationDirection newNavDirection = new NavigationDirection();
        newNavDirection.direction = navDirection.direction;
        newNavDirection.way = navDirection.way;
        newNavDirection.distance = navDirection.distance;
        return newNavDirection;
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
