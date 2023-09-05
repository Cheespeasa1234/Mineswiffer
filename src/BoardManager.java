import java.awt.Image;
import java.util.ArrayList;
import java.util.Random;

public class BoardManager {
    public int[][] board;
    public boolean[][] discovered;
    public int[][] flags; // 0 - no flag, 1 - flagged, 2 - question mark
    public int[][] bombHints;
    public int[][] powerHints;
    public int bombCount = 40;
    public int flagCount = 0;

    public final int BOARD_EMPTY = 0;
    public final int BOARD_BOMB = 1;
    public final int BOARD_RADAR = 2;
    
    public int boardX = 10;
    public int boardW = 600;
    public int boardY = 100;
    public int boardH = 600;
    public int gap = 5;

    public int w, h;

    public boolean isDiscovered(int x, int y) {
        return discovered[x][y];
    }

    public int getTile(int x, int y) {
        return board[x][y];
    }

    public int getHint(int x, int y, int val) {
        if (val == BOARD_BOMB) {
            return bombHints[x][y];
        } else if (val == BOARD_RADAR) {
            return powerHints[x][y];
        } else throw new IllegalArgumentException("val is inval(id)");
    }

    public final int STEPPED_ON_RADAR = 1;
    public final int STEPPED_ON_BOMB = 2;
    public final int STEPPED_ON_EMPTY = 3;
    public int stepOnTile(int x, int y) {
        if (board[x][y] == BOARD_BOMB) {
            gameOver();
            return 2;
        } else if (discovered[x][y] && board[x][y] == BOARD_RADAR) {
            board[x][y] = BOARD_EMPTY;
            return 1;
        } else if (bombHints[x][y] == 0) {
            ArrayList<int[]> cave = DFS.cave(bombHints, board, x, y, 0);
            for (int[] coord : cave) {
                discovered[coord[0]][coord[1]] = true;
                for (int[] neighborCoord : DFS.getNeighbors(coord[0], coord[1], board)) {
                    discovered[neighborCoord[0]][neighborCoord[1]] = true;
                }
            }
        }
        
        discovered[x][y] = true;
        return 3;
    }

    public void flagTile(int x, int y) {

        // make sure its not discovered
        if (discovered[x][y])
            return;

        // if there is a flag, remove it
        if (flags[x][y] == 1) {
            flags[x][y] = 0;
            flagCount--;
        } else {
            flags[x][y] = 1;
            flagCount++;
        }

    }

    public void randomFill(int[][] board, int count, int val) {
        for (int i = 0; i < count; i++) {
            int x = 0, y = 0;
            do {
                x = (int) (Math.random() * board.length);
                y = (int) (Math.random() * board[0].length);
            } while(board[x][y] == val);
            
            board[x][y] = val;
        }
    }

    public void gameOver() {
        // set everything to discovered
        for (int i = 0; i < discovered.length; i++) {
            for (int j = 0; j < discovered[0].length; j++) {
                discovered[i][j] = true;
            }
        }
    }

    public BoardManager(int w, int h) {

        this.w = w;
        this.h = h;

        board = new int[w][h];
        discovered = new boolean[w][h];
        randomFill(board, bombCount, BOARD_BOMB);
        randomFill(board, bombCount / 4, BOARD_RADAR);
        board[0][0] = 0;

        bombHints = createHints(board, BOARD_BOMB);
        powerHints = createHints(board, BOARD_RADAR);
        flags = new int[w][h];

    }

    public int[][] createHints(int[][] board, int value) {
        int[][] hints = new int[board.length][board[0].length];
        for (int i = 0; i < hints.length; i++) {
            for (int j = 0; j < hints[0].length; j++) {
                if (board[i][j] == value)
                    continue;
                int[][] neighborCoords = DFS.getNeighbors(i, j, board);
                int explosiveNeighbors = 0;
                for (int[] neighborCoord : neighborCoords) {
                    if (board[neighborCoord[0]][neighborCoord[1]] == value) {
                        explosiveNeighbors++;
                    }
                }
                hints[i][j] = explosiveNeighbors;
            }
        }
        return hints;
    }
}
