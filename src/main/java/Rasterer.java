import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private static double[] depthDPP = new double[8];
    private static final double ROOT_LRLON = MapServer.ROOT_LRLON, ROOT_ULLON = MapServer.ROOT_ULLON,
            ROOT_LRLAT = MapServer.ROOT_LRLAT, ROOT_ULLAT = MapServer.ROOT_ULLAT;

    public Rasterer() {
        double LonDPP = (ROOT_LRLON - ROOT_ULLON) / MapServer.TILE_SIZE;
        for (int i = 0; i < depthDPP.length; i++) {
            depthDPP[i] = LonDPP;
            LonDPP /= 2;
        }
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
//        System.out.println(params);
        Map<String, Object> results = new HashMap<>();

        double requestedULLon = params.get("ullon");
        double requestedULLat = params.get("ullat");
        double requestedLRLon = params.get("lrlon");
        double requestedLRLat = params.get("lrlat");

        if (requestedLRLon <= ROOT_ULLON || requestedLRLat >= ROOT_ULLAT
                || requestedULLon >= ROOT_LRLON || requestedULLat <= ROOT_LRLAT
                || requestedULLon >= requestedLRLon || requestedLRLat >= requestedULLat) {
            results.put("render_grid", null);
            results.put("raster_ul_lon", 0);
            results.put("raster_ul_lat", 0);
            results.put("raster_lr_lon", 0);
            results.put("raster_lr_lat", 0);
            results.put("depth", 0);
            results.put("query_success", false);
            return results;
        }

        double requestedLonDPP = (requestedLRLon - requestedULLon) / params.get("w");
        int depth = getDepth(requestedLonDPP);
        int n = (int) Math.pow(2, depth);
        double intervalX = (ROOT_LRLON - ROOT_ULLON) / Math.pow(2, depth);
        double intervalY = (ROOT_ULLAT - ROOT_LRLAT) / Math.pow(2, depth);

        int xUL = (int) ((requestedULLon - ROOT_ULLON) / intervalX);
        int xLR = n - 1 - (int) ((ROOT_LRLON - requestedLRLon) / intervalX);
        int yUL = (int) ((ROOT_ULLAT - requestedULLat) / intervalY);
        int yLR = n - 1 - (int) ((requestedLRLat - ROOT_LRLAT) / intervalY);
        if (xUL < 0) {
            xUL = 0;
        }
        if (xLR > Math.pow(2, depth) - 1) {
            xLR = (int) Math.pow(2, depth) - 1;
        }
        if (yUL < 0) {
            yUL = 0;
        }
        if (yLR > Math.pow(2, depth) - 1) {
            yLR = (int) Math.pow(2, depth) - 1;
        }

        double ULLon = ROOT_ULLON + intervalX * xUL;
        double ULLat = ROOT_ULLAT - intervalY * yUL;
        double LRLon = ROOT_ULLON + intervalX * (xLR + 1);
        double LRLat = ROOT_ULLAT - intervalY * (yLR + 1);

        String[][] renderGrid = new String[yLR - yUL + 1][xLR - xUL + 1];
        for (int j = 0; j < yLR - yUL + 1; j++) {
            for (int i = 0; i < xLR - xUL + 1; i++) {
                int x = xUL + i;
                int y = yUL + j;
                renderGrid[j][i] = "d" + depth + "_x" + x + "_y" + y + ".png";
//                System.out.println(renderGrid[j][i]);
            }
        }
        results.put("render_grid", renderGrid);
        results.put("raster_ul_lon", ULLon);
        results.put("raster_ul_lat", ULLat);
        results.put("raster_lr_lon", LRLon);
        results.put("raster_lr_lat", LRLat);
        results.put("depth", depth);
        results.put("query_success", true);

        return results;
    }

    private int getDepth(double requestedLonDPP) {
        int depth = 0;
        while (depthDPP[depth] > requestedLonDPP && depth < depthDPP.length - 1) {
            depth += 1;
        }
        return depth;
    }

}
