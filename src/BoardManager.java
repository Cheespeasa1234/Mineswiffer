import java.util.ArrayList;

public class BoardManager {
    public final int BOARD_EMPTY = 0;
    public final int BOARD_BOMB = 1;
    public final int BOARD_RADAR = 2;
    public final int BOARD_ROCKET = 3;
    public final int STEPPED_ON_EMPTY = 0;
    public final int STEPPED_ON_BOMB = 1;
    public final int STEPPED_ON_RADAR = 2;
    public final int STEPPED_ON_ROCKET = 3;


    public int[][] board; // contents of board - 0 - empty, 1 - bomb, 2 - radar
    public boolean[][] discovered;
    public int[][] flags; // 0 - no flag, 1 - flagged, 2 - question mark
    public int[][] bombHints; // n bombs
    public int[][] powerHints; // UNUSED - has powerup in radius
    public int bombCount, radarCount, rocketCount, flagCount = 0, discoveredCount = 0;

    public int boardX = 10, boardW = 600, boardY = 120, boardH = 600, gap = 5, w, h, tileW, tileH;
    public boolean gameOver = false, clickedYet = false, won = false;

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
        } else
            throw new IllegalArgumentException("val is inval(id)");
    }

    public boolean opensCave(int x, int y, int[][] boardb) {
        ArrayList<int[]> cave = DFS.cave(bombHints, boardb, x, y, 0);
        System.out.println("Cave size: " + cave.size());
        return cave.size() >= 5;
    }

    public int stepOnTile(int x, int y) {

        // if first click, make this the first click
        if (!clickedYet) {
            clickedYet = true;
            createBoard(x, y);
        }
        
        // if dead, dont do anything
        if (gameOver || flags[x][y] == 1)
            return STEPPED_ON_EMPTY;

        // if you clicked on a bomb that is not flagged, you lose
        if (board[x][y] == BOARD_BOMB && flags[x][y] == 0) {
            gameOver();
            return STEPPED_ON_BOMB;

        // if you clicked on a discovered powerup
        } else if (board[x][y] == BOARD_RADAR && discovered[x][y]) {
            int temp = board[x][y];
            board[x][y] = BOARD_EMPTY;
            return temp;
        
        } else if (board[x][y] == BOARD_ROCKET && discovered[x][y]) {
            int temp = board[x][y];
            board[x][y] = BOARD_EMPTY;

            for(int i = x; i < board.length; i++) {
                if (board[i][y] == BOARD_BOMB && flags[i][y] == 0) {
                    flags[i][y] = 1;
                    break;
                } else {
                    discovered[i][y] = true;
                }
            }

            for(int i = x; i > -1; i--) {
                if (board[i][y] == BOARD_BOMB) {
                    flags[i][y] = 1;
                    break;
                } else {
                    discovered[i][y] = true;
                }
            }
    
            return temp;
        

        // if clicked on a clear tile, open a cave
        } else if (bombHints[x][y] == 0) {
            ArrayList<int[]> cave = DFS.cave(bombHints, board, x, y, 0);
            for (int[] coord : cave) {
                discovered[coord[0]][coord[1]] = true;
                for (int[] neighborCoord : DFS.getNeighbors(coord[0], coord[1], board)) {
                    if(!discovered[neighborCoord[0]][neighborCoord[1]])
                        discoveredCount++;
                    discovered[neighborCoord[0]][neighborCoord[1]] = true;
                }
            }
        }

        discovered[x][y] = true;
        return STEPPED_ON_EMPTY;
    }

    public void flagTile(int x, int y) {

        System.out.println("flagging tile " + x + ", " + y);

        // make sure its not discovered
        if (!clickedYet || discovered[x][y])
            return;

        // flag the tile
        if (flags[x][y] == 0) {
            flags[x][y] = 1;
            flagCount++;
        } else if (flags[x][y] == 1) {
            flags[x][y] = 0;
            flagCount--;
        } else if (flags[x][y] == 2) {
            flags[x][y] = 0;
        }

        // check if all bombs are flagged
        boolean allClear = true;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                int tile = board[i][j];
                if (tile != BOARD_BOMB) continue;
                if (flags[i][j] != 1) {
                    allClear = false;
                };
            }
        }

        if (allClear) {
            won = true;
            gameOver();
        }

    }

    public void randomFill(int[][] board, int count, int val, int[] dontFill) {
        for (int i = 0; i < count; i++) { // for each time to spawn a val
            int x = (int) (Math.random() * board.length);
            int y = (int) (Math.random() * board[0].length);

            // if on top of dontFill, try again
            if (dontFill != null && x == dontFill[0] && y == dontFill[1]) {
                i--;
                continue;
            }

            // if on top of another val, try again
            if (board[x][y] == val) {
                i--;
                continue;
            }

            // get distance to dontfill
            double distLim = 2.0;
            double dist = distLim + 1.0;
            if (dontFill != null) {
                double dx = x - dontFill[0];
                double dy = y - dontFill[1];
                dist = Math.sqrt(dx * dx + dy * dy);
            }

            // if too close to dontFill, try again
            if (dist < distLim) {
                i--;
                continue;
            }

            // set the tile
            board[x][y] = val;

        }
    }

    public void gameOver() {
        // set everything to discovered
        gameOver = true;
        for (int i = 0; i < discovered.length; i++) {
            for (int j = 0; j < discovered[0].length; j++) {
                discovered[i][j] = true;
            }
        }
    }

    public void createBoard(int firstClickX, int firstClickY) {
        randomFill(board, bombCount, BOARD_BOMB, new int[] { firstClickX, firstClickY });
        randomFill(board, bombCount / 7, BOARD_RADAR, null);
        randomFill(board, 2, BOARD_ROCKET, null);

        bombHints = createHints(board, BOARD_BOMB);
    }
    
    public BoardManager(int w, int h, int bombCount, int radarCount, int rocketCount) {
        
        this.w = w;
        this.h = h;
        this.bombCount = bombCount;
        this.radarCount = radarCount;
        this.rocketCount = rocketCount;
        
        board = new int[w][h];
        discovered = new boolean[w][h];
        flags = new int[w][h];

        tileW = boardW / h - gap;
        tileH = boardH / w - gap;
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

    public String[] getToolTip(int x, int y) {
        String content = "Empty Tile";
        if (board[x][y] == BOARD_BOMB) {
            content = "Bomb";
        } else if (board[x][y] == BOARD_RADAR) {
            content = "Radar";
        } else if (board[x][y] == BOARD_ROCKET) {
            content = "Rocket";
        }

        String hint = "0 bombs nearby";
        if (bombHints[x][y] == 1) {
            hint = "1 bomb nearby";
        } else if (bombHints[x][y] > 1) {
            hint = bombHints[x][y] + " bombs nearby";
        }

        String disc = discovered[x][y] ? "Discovered" : "Undiscovered";
        String flag = flags[x][y] == 1 ? "Flagged" : flags[x][y] == 2 ? "Unknown" : "No Flag";

        return new String[] { content, hint, disc + ", " + flag };
    }
}