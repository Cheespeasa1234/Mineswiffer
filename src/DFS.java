import java.util.ArrayList;

public class DFS {

    public static ArrayList<int[]> cave(int[][] hints, int[][] board, int startX, int startY, int value) {
        ArrayList<int[]> result = new ArrayList<>();
        boolean[][] visited = new boolean[hints.length][hints[0].length];
        dfs(hints, board, startX, startY, value, visited, result);
        return result;
    }

    public static void dfs(int[][] hints, int[][] board, int x, int y, int value, boolean[][] visited,
            ArrayList<int[]> result) {

        if (x < 0 || x >= hints.length || y < 0 || y >= hints[0].length || visited[x][y] || hints[x][y] != value) {
            return;
        }

        visited[x][y] = true;
        result.add(new int[] { x, y });

        // Check horizontally and vertically adjacent tiles
        int[][] directions = {
                { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            dfs(hints, board, newX, newY, value, visited, result);
        }
    }

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
