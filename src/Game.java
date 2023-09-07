import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Game extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    public static final int PREF_W = 617;
    public static final int PREF_H = 710;

    // game variables
    private BoardManager boardManager;
    private int[] mouseLoc;
    private boolean debug = false;
    private int mousex;
    private int mousey;

    // animation variables
    private int[][] fadeProg;
    private int fogAnimProg = 0, fogAnimLen = 200;
    private int radarAnimProg = 0, radarAnimLen = 30, radarAnimIters = 3; // the radar beam itself
    private int[] radarAnimStartLoc;
    private int radarRotAnimProg = 0, radarRotAnimLen = 360;

    // assets
    private Image fogImage, flagImage, radarImage, bombImage, rocketImage;
    private Color[] hintColors = {
            Color.BLACK, new Color(0, 0, 255), new Color(0, 128, 0),
            new Color(255, 0, 0), new Color(0, 0, 128), new Color(128, 0, 0),
            new Color(0, 128, 128), new Color(0, 0, 0), new Color(128, 128, 128)
    };

    private Color undiscoveredColor = Color.GRAY.brighter();
    private Color discoveredColor = new Color(200, 200, 200, 255);
    private Color tileShadowColor = new Color(100, 100, 100, 100);
    private Font hintFont = new Font("Cascadia Mono", Font.BOLD, 14);
    private Font mainFont = new Font("Cascadia Mono", Font.BOLD, 20);

    private Timer fogProgressTimer = new Timer(1000 / 30, e -> {
        radarRotAnimProg += 2;
        radarRotAnimProg %= radarRotAnimLen;

        fogAnimProg++;
        fogAnimProg %= fogAnimLen;

        if (radarAnimProg > 0 && radarAnimProg < radarAnimLen * radarAnimIters) {
            radarAnimProg++;
        
            
        } else {
            radarAnimProg = 0;
        }

        for (int i = 0; i < fadeProg.length; i++) {
            for (int j = 0; j < fadeProg[0].length; j++) {
                // if its fadeProg is != 0, then it is fading
                if (boardManager.isDiscovered(i, j)) {
                    fadeProg[i][j] = Math.min(fadeProg[i][j] + 8, fogAnimLen);
                }
            }
        }
        repaint();
    });

    public Game() {
        this.setFocusable(true);
        this.setBackground(Color.WHITE);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);

        int w = 16, h = 16;
        boardManager = new BoardManager(w, h, 40, 8, 2);

        fadeProg = new int[w][h];

        fogImage = new ImageIcon(Game.class.getResource("fog.png")).getImage();
        flagImage = new ImageIcon(Game.class.getResource("flag.png")).getImage();
        radarImage = new ImageIcon(Game.class.getResource("radar.png")).getImage();
        bombImage = new ImageIcon(Game.class.getResource("bomb.png")).getImage();
        rocketImage = new ImageIcon(Game.class.getResource("rocket.png")).getImage();

        fogProgressTimer.start();
    }

    public void paintFog(Graphics2D g2) {
        int numRows = boardManager.w;
        int numCols = boardManager.h;
        int tileSize = boardManager.boardW / numCols;
        int numCircles = 15; // Number of fog circles per tile

        Random rand = new Random();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                // if not discovered and the fade animation hasn't yet increased to fogAnimLen
                if (fadeProg[i][j] < fogAnimLen) {
                    drawFogTile(g2, i, j, tileSize, numCircles, rand);
                }
            }
        }
    }

    private void drawFogTile(Graphics2D g2, int i, int j, int tileSize, int numCircles, Random rand) {
        for (int circleIndex = 1; circleIndex < numCircles + 1; circleIndex++) {
            // Use Random to create a different seed for each fog circle
            long seed = circleIndex * 1000000000 + i * 100000 + j * 1000;
            rand.setSeed(seed);

            // Calculate the position of the fog circle based on animation progress
            double angle = 2 * Math.PI * fogAnimProg / fogAnimLen + rand.nextDouble() * 2 * Math.PI;
            boolean nextToDiscovered = false;

            int add = (nextToDiscovered ? 0 : tileSize / 2);

            int radius = rand.nextInt(tileSize / 2) + 2 + fadeProg[i][j] + add;
            int x = j * tileSize + tileSize / 2 + (int) (Math.cos(angle) * radius * rand.nextDouble())
                    + rand.nextInt(tileSize / 2) - tileSize / 4 + tileSize / 2;
            int y = i * tileSize + tileSize / 2 + (int) (Math.sin(angle) * radius * rand.nextDouble())
                    + rand.nextInt(tileSize / 2) - tileSize / 4 + boardManager.boardY;

            // Draw a transparent fog circle

            float transparency = 1 - (float) fadeProg[i][j] / fogAnimLen;
            float transparencyMod = 1f;
            if (mouseLoc != null) {
                // use x, y, mousex, mousey functions to change x and y to move away from the
                // mouse the closer the mouse is
                int dx = x - mousex;
                int dy = y - mousey;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double distFactor = 1 - dist / 100;
                if (distFactor < 0)
                    distFactor = 0;
                // x += dx * distFactor;
                // y += dy * distFactor;

                transparencyMod = (float) (1 - distFactor);
            }
            if (boardManager.flags[i][j] > 0) {
                transparencyMod = 0;
            }

            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency * transparencyMod / 3);
            g2.setComposite(ac);
            drawFogCircle(g2, x, y, (int) ((double) tileSize * 0.9d));
        }
    }

    private void drawFogCircle(Graphics2D g2, int x, int y, int radius) {
        // draw the fog image
        g2.drawImage(fogImage, x - radius, y - radius, radius * 2, radius * 2, null);
    }

    public void paintGUI(Graphics2D g2) {

        int guiY = boardManager.boardY - 90;
        int guiH = 75;

        g2.setStroke(new java.awt.BasicStroke(1));
        g2.setColor(tileShadowColor);
        g2.fillRoundRect(boardManager.boardX, guiY, boardManager.boardW - 3, 75, 10, 10);
        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardManager.boardX, guiY, boardManager.boardW, guiH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardManager.boardX, guiY, boardManager.boardW, 75, 10, 10);

        // draw the minesweeper text
        g2.setFont(hintFont);
        g2.drawString("Flagged: " + boardManager.flagCount + "/" + boardManager.bombCount, boardManager.boardX + 10,
                guiY + 20);

        // draw the main text
        g2.setFont(mainFont);
        String txt = "";
        double pct = boardManager.discoveredCount / (double) (boardManager.w * boardManager.h - boardManager.bombCount);
        String pctxt = "" + (int) (pct * 100);
        pctxt = pctxt.substring(0, Math.min(pctxt.length(), 3));
        if (!boardManager.gameOver && !boardManager.won) {
            txt = "Clearing Mines - " + pctxt + "% of map cleared";
        } else if (!boardManager.won) {
            txt = "Game Over :( - Press r to restart";
        } else if (boardManager.won) {
            txt = "You Won! :) - Press r to restart";
        }
        int len = g2.getFontMetrics().stringWidth(txt);
        g2.drawString(txt, boardManager.boardX + boardManager.boardW - len - 10, guiY + 20);

        // draw tooltip
        if (debug && mouseLoc != null && boardManager.clickedYet) {
            g2.setFont(hintFont);
            g2.setColor(tileShadowColor);
            g2.fillRoundRect(mousex - 50, mousey - 50, 100, 50, 10, 10);
            g2.setColor(Color.GRAY);
            g2.fillRoundRect(mousex - 50, mousey - 50, 100, 50, 10, 10);
            g2.setColor(Color.BLACK);
            String[] tooltip = boardManager.getToolTip(mouseLoc[0], mouseLoc[1]);
            for (int i = 0; i < tooltip.length; i++) {
                g2.drawString(tooltip[i], mousex - 40, mousey - 40 + 15 * i);
            }
        }
    }

    public double getRadius(int animProg) {
        double prog = (double) animProg % (double) radarAnimLen;
        return Math.pow(prog / (double) radarAnimLen, 2) * 100;
    }

    public void paintRadar(Graphics2D g2) {
        if (radarAnimProg == 0)
            return;

        double radius = getRadius(radarAnimProg);
        g2.setColor(Color.GREEN);
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawOval(radarAnimStartLoc[0] - (int) radius, radarAnimStartLoc[1] - (int) radius, (int) radius * 2,
                (int) radius * 2);
    }

    public void paintComponent(Graphics g) {

        int boardX = boardManager.boardX;
        int boardY = boardManager.boardY;
        int boardW = boardManager.boardW;
        int boardH = boardManager.boardH;
        int gap = boardManager.gap; // gap between each tile. each tile should leave gap/2 margin on each side.

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(hintFont);

        g2.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        // draw the actual board
        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardX, boardY, boardW, boardH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardX, boardY, boardW, boardH, 10, 10);

        int tileW = boardManager.tileW;
        int tileH = boardManager.tileH;

        // draw each tile
        for (int x = 0; x < boardManager.h; x++) {
            for (int y = 0; y < boardManager.w; y++) {
                int fullX = boardX + (gap) + y * (tileW + gap);
                int fullY = boardY + (gap) + x * (tileH + gap);

                // draw the tile box
                g2.setColor(tileShadowColor);
                g2.fillRoundRect(fullX, fullY, tileW, tileH, 10, 10);

                g2.setColor(boardManager.isDiscovered(x, y) ? discoveredColor : undiscoveredColor);
                g2.fillRoundRect(fullX, boardY + gap + x * (tileH + gap), tileW, tileH, 10, 10);

                // if it is discovered, draw the content
                if (boardManager.isDiscovered(x, y)) {

                    // draw the content of the tile (bombs, powerups etc)
                    int tile = boardManager.getTile(x, y);
                    if (tile == boardManager.BOARD_BOMB) {
                        g2.drawImage(bombImage, fullX, boardY + gap + x * (tileH + gap), tileW, tileH, null);
                    } else if (tile == boardManager.BOARD_RADAR) {

                        int tileCenterX = fullX + tileW / 2;
                        int tileCenterY = boardY + gap + x * (tileH + gap) + tileH / 2;
                        double startRot = new Random(tileCenterX * tileCenterY).nextDouble() * 360;
                        g2.rotate(Math.toRadians(startRot + radarRotAnimProg), tileCenterX, tileCenterY);
                        g2.drawImage(radarImage, fullX, boardY + gap + x * (tileH + gap), tileW, tileH, null);
                        g2.rotate(-Math.toRadians(startRot + radarRotAnimProg), tileCenterX, tileCenterY);

                    } else if (tile == boardManager.BOARD_ROCKET) {
                        int off = 10;
                        g2.drawImage(rocketImage, fullX - off, boardY + gap + x * (tileH + gap), tileW + off * 2, tileH, null);
                    }

                    // draw the number
                    int bombhint = boardManager.getHint(x, y, boardManager.BOARD_BOMB);
                    if (bombhint > 0) {
                        g2.setColor(
                                (tile == boardManager.BOARD_RADAR || tile == boardManager.BOARD_ROCKET) ? Color.WHITE : hintColors[bombhint-1]);
                        g2.drawString("" + bombhint, fullX + tileW / 3,
                                boardY + gap + x * (tileH + gap) + ((float) tileH / 1.5f));
                    }
                }

                // draw the flag
                if (boardManager.flags[x][y] == 1) {
                    g2.drawImage(flagImage, fullX, boardY + gap + x * (tileH + gap), tileW, tileH, null);

                    // draw the gameover format
                    if (boardManager.gameOver && boardManager.getTile(x, y) != boardManager.BOARD_BOMB) {
                        g2.setColor(Color.RED.darker());
                        g2.setStroke(new java.awt.BasicStroke(3));
                        // draw X
                        g2.drawLine(fullX, boardY + gap + x * (tileH + gap), fullX + tileW,
                                boardY + gap + x * (tileH + gap) + tileH);
                        g2.drawLine(fullX + tileW, boardY + gap + x * (tileH + gap), fullX,
                                boardY + gap + x * (tileH + gap) + tileH);
                    }
                }

                // draw the radar animation callout
                if (boardManager.flags[x][y] == 0 && radarAnimStartLoc != null
                        && boardManager.getTile(x, y) == boardManager.BOARD_BOMB) {
                    double radius = getRadius(radarAnimProg);
                    boolean closeToRing = Math.abs(fullX + tileW / 2 - radarAnimStartLoc[0]) < radius
                            && Math.abs(boardY + gap + x * (tileH + gap) + tileH / 2 - radarAnimStartLoc[1]) < radius;
                    if(closeToRing) {
                        g2.drawImage(bombImage, fullX, boardY + gap + x * (tileH + gap), tileW, tileH, null);
                    }
                }

            }
        }

        // mouse hover highlight
        if (mouseLoc != null) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            g2.setColor(new Color(100, 100, 0, 100));
            g2.fillRoundRect(boardX + y * (tileW + gap) + gap, boardY + gap + x * (tileH + gap), tileW, tileH, 10, 10);
        }

        // draw the debug cave
        if (debug && mouseLoc != null && boardManager.clickedYet) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            if (boardManager.getHint(x, y, boardManager.BOARD_BOMB) == 0
                    && boardManager.getTile(x, y) != boardManager.BOARD_BOMB) {
                ArrayList<int[]> cave = DFS.cave(boardManager.bombHints, boardManager.board, x, y, 0);
                for (int[] coord : cave) {
                    g2.setColor(new Color(100, 100, 0, 100));
                    g2.fillRoundRect(boardX + gap + coord[1] * (tileW + gap), boardY + gap + coord[0] * (tileH + gap),
                            tileW, tileH, 10, 10);
                }
            }
        }

        paintGUI(g2);
        paintFog(g2);
        paintRadar(g2);
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == KeyEvent.VK_R) {
            boardManager = new BoardManager(boardManager.w, boardManager.h, boardManager.bombCount,
                    boardManager.radarCount, boardManager.rocketCount);
            mouseLoc = null;
            debug = false;
            mousex = 0;
            mousey = 0;

            // animation variables
            fadeProg = new int[boardManager.w][boardManager.h];
            fogAnimProg = 0;
            fogAnimLen = 200;
            radarAnimProg = 0;
            radarAnimLen = 30;
            radarAnimIters = 3;
            radarAnimStartLoc = null;
        } else if (c == KeyEvent.VK_D) {
            debug = !debug;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseLoc = getMousePos(e);
    }

    public int[] getMousePos(MouseEvent e) {
        mousex = e.getX();
        mousey = e.getY();
        // get the board entry that the mouse is over
        int boardX = boardManager.boardX;
        int boardY = boardManager.boardY;
        int boardW = boardManager.boardW;
        int boardH = boardManager.boardH;

        int tileW = boardW / boardManager.h;
        int tileH = boardH / boardManager.w;

        if (mousex >= boardX && mousex <= boardX + boardW - 5 && mousey >= boardY && mousey <= boardY + boardH - 5) {
            int x = (mousey - boardY) / tileH;
            int y = (mousex - boardX) / tileW;
            return new int[] { x, y };
        } else {
            return null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseLoc = getMousePos(e);
        boolean rmb = e.getButton() == MouseEvent.BUTTON3 || e.isControlDown();
        if (mouseLoc != null) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            if (!rmb) {
                int ret = boardManager.stepOnTile(x, y);

                if (ret == boardManager.STEPPED_ON_RADAR) {
                    radarAnimProg = 1;
                    radarAnimStartLoc = new int[] { e.getX(), e.getY() };
                } else if (ret == boardManager.STEPPED_ON_ROCKET) {
                }
            } else if (rmb) {
                boardManager.flagTile(x, y);
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /* METHODS FOR CREATING JFRAME AND JPANEL */

    public Dimension getPreferredSize() {
        return new Dimension(PREF_W, PREF_H);
    }

    private static JPanel gamePanel;

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("You're Mother");
        gamePanel = new Game();

        frame.getContentPane().add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}