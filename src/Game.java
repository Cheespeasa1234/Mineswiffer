import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Game extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    public static final int PREF_W = 617;
    public static final int PREF_H = 730;

    // game variables
    private BoardManager boardManager;
    private int[] mouseLoc;
    private int mousex, mousey;
    private boolean debug = false;
    private boolean shiftDown = false;
    
    // animation variables
    private int[][] fadeProg;
    private int[] radarAnimStartLoc;
    private int fogAnimProg = 0, fogAnimLen = 200,
        radarAnimProg = 0, radarAnimLen = 30, radarAnimIters = 3, // the radar beam itself
        radarRotAnimProg = 0, radarRotAnimLen = 360, // rotating the radar
        size = 16, sizePrev = 16, // tile size
        bombCount = 40, bombCountPrev = 40, // how many of powers to put
        radarCount = 5, radarCountPrev = 5,
        rocketCount = 3, rocketCountPrev = 3;
    
    // assets
    private Image fogImage, flagImage, radarImage, bombImage, rocketImage;
    private Color[] hintColors = {
        Color.BLACK, new Color(0, 0, 255), new Color(0, 128, 0),
        new Color(255, 0, 0), new Color(0, 0, 128), new Color(128, 0, 0),
        new Color(0, 128, 128), new Color(0, 0, 0), new Color(128, 128, 128)
    };
    
    // swing stuff
    private Color undiscoveredColor = Color.GRAY.brighter(),
        discoveredColor = new Color(200, 200, 200, 255),
        tileShadowColor = new Color(100, 100, 100, 100);
    private Font hintFont, mainFont, smallFont;
    private JButton resetButton;
    
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

        boardManager = new BoardManager(size, size, bombCount, radarCount, rocketCount);
        fadeProg = new int[size][size];

        fogImage = new ImageIcon(Game.class.getResource("fog.png")).getImage();
        flagImage = new ImageIcon(Game.class.getResource("flag.png")).getImage();
        radarImage = new ImageIcon(Game.class.getResource("radar.png")).getImage();
        bombImage = new ImageIcon(Game.class.getResource("bomb.png")).getImage();
        rocketImage = new ImageIcon(Game.class.getResource("rocket.png")).getImage();

        // if Cascadia Mono font is installed on system, use it
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        if (Arrays.asList(fonts).contains("Cascadia Mono")) {
            hintFont = new Font("Cascadia Mono", Font.BOLD, 16);
            mainFont = new Font("Cascadia Mono", Font.BOLD, 20);
            smallFont = new Font("Cascadia Mono", Font.PLAIN, 10);
        } else {
            hintFont = new Font("Arial", Font.BOLD, 16);
            mainFont = new Font("Arial", Font.BOLD, 20);
            smallFont = new Font("Arial", Font.PLAIN, 10);
        }

        JPanel configPanel = new JPanel();
        configPanel.setOpaque(false);
        
        LeviSlider sizeSlider = new LeviSlider(8, 64, -1, 8, size, true, "Size");
        sizeSlider.addChangeListener(e -> {
            size = sizeSlider.getValue();
            styleButton();
        });
        
        LeviSlider bombSlider = new LeviSlider(1, 100, -1, 10, bombCount, false, "Bombs");        
        bombSlider.addChangeListener(e -> {
            bombCount = bombSlider.getValue();
            styleButton();
        });
        
        LeviSlider radarSlider = new LeviSlider(0, 10, -1, 1, radarCount, false, "Radars");
        radarSlider.addChangeListener(e -> {
            radarCount = radarSlider.getValue();
            styleButton();
        });
        
        LeviSlider rocketSlider = new LeviSlider(0, 10, -1, 1, rocketCount, false, "Rockets");
        rocketSlider.addChangeListener(e -> {
            rocketCount = rocketSlider.getValue();
            styleButton();
        });
        
        resetButton = new JButton("Reset with changes");
        resetButton.addActionListener(e -> {
            setPrevs();
            resetButton.setFont(hintFont);
        });
        
        

        configPanel.add(sizeSlider);
        // configPanel.add(bombSlider);
        // configPanel.add(radarSlider);
        // configPanel.add(rocketSlider);
        configPanel.add(resetButton);

        // this.add(configPanel);

        fogProgressTimer.start();
    }

    // recursively configures a component.
    // if the component is a container, loop through elements and recurse.
    // else, configure it.
    public void config(JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JPanel) {
                config((JPanel) c);
            } else {
                c.setFocusable(false);
            }
        }
    }

    public boolean changesHaveBeenMade() {
        return size != sizePrev || bombCount != bombCountPrev || radarCount != radarCountPrev || rocketCount != rocketCountPrev;
    }

    public void setPrevs() {
        sizePrev = size;
        bombCountPrev = bombCount;
        radarCountPrev = radarCount;
        rocketCountPrev = rocketCount;
    }

    public void styleButton() {
        Font italicized = new Font(hintFont.getName(), Font.ITALIC, hintFont.getSize());
        resetButton.setFont(italicized);
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

        int guiY = boardManager.boardY - 110;
        int guiH = 95;

        g2.setStroke(new java.awt.BasicStroke(1));
        g2.setColor(tileShadowColor);
        g2.fillRoundRect(boardManager.boardX, guiY, boardManager.boardW - 3, guiH, 10, 10);
        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardManager.boardX, guiY, boardManager.boardW, guiH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardManager.boardX, guiY, boardManager.boardW, guiH, 10, 10);

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

        g2.setFont(smallFont);
        String[] tutorial = {
            "LMB to clear a tile, or to use a powerup",
            "RMB or LMB + Control to flag a tile",
            "If you click a bomb, you lose.",
            "If you click a radar, you see close bombs.",
            "If you click a blast, you clear a vertical row.",
        };

        for (int i = 0; i < tutorial.length; i++) {
            g2.drawString(tutorial[i], boardManager.boardX + 10, guiY + 40 + 10 * i);
        }
    }

    public void drawToolTip(Graphics2D g2) {
        // draw tooltip
        g2.setFont(hintFont);
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
        double prog = (double) radarAnimProg % (double) radarAnimLen;
        int alpha = (int) (255 * (1 - prog / (double) radarAnimLen));
        g2.setColor(new Color(0, 255, 0, alpha));
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

        RenderingHints textOnly = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        RenderingHints interpolation = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
        g2.addRenderingHints(textOnly);
        g2.addRenderingHints(interpolation);

        paintGUI(g2);

        // draw the actual board
        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardX, boardY, boardW, boardH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardX, boardY, boardW, boardH, 10, 10);

        int tileW = boardManager.tileW;
        int tileH = boardManager.tileH;

        // draw each tile
        g2.setFont(hintFont);
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
                        g2.drawString("" + bombhint, fullX + tileW / 3 + 2,
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

        paintFog(g2);
        paintRadar(g2);
        drawToolTip(g2);

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
        g2.setComposite(ac);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == KeyEvent.VK_SHIFT)
            shiftDown = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == KeyEvent.VK_R) {
            boardManager = new BoardManager(size, size, bombCount,
                    radarCount, rocketCount);
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
        } else if (c == KeyEvent.VK_SHIFT) {
            shiftDown = false;
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
            } else if (rmb || (!rmb && shiftDown)) {
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
        JFrame frame = new JFrame("Mineswiffer");
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