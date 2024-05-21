import java.util.ArrayList;

/**
 * Implementation of the depth-first-search algorithm function, and some wrappers.
 */
public class DFS {

    /**
     * Wrapper function for {@link #dfs(int[][], int[][], int, int, int, boolean[][])}.
     * Get a list of boxes that are consecutively empty.
     * When a tile with no adjacent bombs is clicked, use this to also reveal adjacent blank tiles.
     * @param hints The number of adjacent bombs of every tile.
     * @param board The tile data.
     * @param startX The x index to start.
     * @param startY The y index to start.
     * @param value The value to find copies of.
     * @returns A list of recursively adjacent copies of the tile.
     * @see DFS#dfs(int[][], int[][], int, int, int, boolean[][], ArrayList)
     */
    public static ArrayList<int[]> cave(int[][] hints, int[][] board, int startX, int startY, int value) {
        ArrayList<int[]> result = new ArrayList<>();
        boolean[][] visited = new boolean[hints.length][hints[0].length];
        dfs(hints, board, startX, startY, value, visited, result);
        return result;
    }

    /**
     * Visit every tile from a starting point, and reveal adjacent identical tiles.
     * This method does not create its own result array because it is recursive and needs to remember what tiles it visited, and the results so far.
     * @param hints The number of adjacent bombs of every tile.
     * @param board The tile data.
     * @param x The x index to start.
     * @param y The y index to start.
     * @param value The tile data value to find identical pairs of.
     * @param visited An internal list of tiles that have been searched already.
     * @param result The array that will contain the results of the search.
     */
    public static void dfs(int[][] hints, int[][] board, int x, int y, int value, boolean[][] visited,
            ArrayList<int[]> result) {

        if (x < 0 || x >= hints.length || y < 0 || y >= hints[0].length || visited[x][y] || hints[x][y] != value) {
            return;
        }

        visited[x][y] = true;
        result.add(new int[] { x, y });

        // Check horizontally, vertically, and diagonally
        int[][] directions = {
                { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 },
                { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            dfs(hints, board, newX, newY, value, visited, result);
        }
    }

    /**
     * Get the neighbor tiles (if any) of a given point.
     * If a tile is in the corner, or on the edge, it has less neighbors.
     * For example:
     * <pre>
     *- - -
     *- x *
     *- * *
     * </pre>
     * In that example, x only has three neighbors, the three stars.
     * @param x The x index of the tile.
     * @param y The y index of the tile.
     * @param board The board to search.
     * @return The list of tiles that are neighbors.
     */
    public static int[][] getNeighbors(int x, int y, int[][] board) {
        // get all the surrounding coords
        ArrayList<int[]> neighbors = new ArrayList<int[]>();
        for(int i = -1; i <= 1; i++) {
            for(int j = -1; j <= 1; j++) {
                int newX = x + i;
                int newY = y + j;
                if(newX >= 0 && newX < board.length && newY >= 0 && newY < board[0].length && !(i == 0 && j == 0)) {
                    neighbors.add(new int[] {newX, newY});
                }
            }
        }

        // convert to array
        int[][] neighborsArray = new int[neighbors.size()][2];
        for(int i = 0; i < neighbors.size(); i++) {
            neighborsArray[i] = neighbors.get(i);
        }
        return neighborsArray;
    }

}