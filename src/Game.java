import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
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

    private BoardManager boardManager;
    private int[] mouseLoc;
    private boolean debug = false;

    // anim
    private int[][] fadeProg;
    private int fogAnimProg = 0;
    private int fogAnimLen = 200;
    private Image fogImage;

    private Color[] hintColors = {
            Color.BLACK,
            new Color(0, 0, 255),
            new Color(0, 128, 0),
            new Color(255, 0, 0),
            new Color(0, 0, 128),
            new Color(128, 0, 0),
            new Color(0, 128, 128),
            new Color(0, 0, 0),
            new Color(128, 128, 128)
    };

    private Color undiscoveredColor = Color.GRAY.brighter();
    private Color discoveredColor = new Color(200, 200, 200, 255);
    private Color tileShadowColor = new Color(100, 100, 100, 100);
    private Font hintFont = new Font("Cascadia Mono", Font.BOLD, 14);

    private Timer fogProgressTimer = new Timer(1000 / 30, e -> {
        fogAnimProg++;
        if (fogAnimProg > fogAnimLen)
            fogAnimProg = 0;

        for(int i = 0; i < fadeProg.length; i++) {
            for(int j = 0; j < fadeProg[0].length; j++) {
                // if its fadeProg is != 0, then it is fading
                if(boardManager.isDiscovered(i, j)) {
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
        boardManager = new BoardManager(w, h);

        fadeProg = new int[w][h];
        File fogImageFile = new File("src/fog.png");
        System.out.println(fogImageFile.getAbsolutePath() + " << path");
        fogImage = new ImageIcon(fogImageFile.getPath()).getImage();

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
                    // lower the transparency as the fadeProg increases
                    float transparency = (1 - (float) fadeProg[i][j] / fogAnimLen) / 3;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
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

        g2.setColor(tileShadowColor);
        g2.fillRoundRect(boardManager.boardX + 3, guiY + 3, boardManager.boardW - 3, 75, 10, 10);
        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardManager.boardX, guiY, boardManager.boardW - 3, guiH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardManager.boardX, guiY, boardManager.boardW - 3, 75, 10, 10);

        // draw the minesweeper text
        g2.drawString("Flagged: 0/" + boardManager.bombCount, boardManager.boardX + 10, guiY + 20);
    }

    public void paintComponent(Graphics g) {

        int boardX = boardManager.boardX;
        int boardY = boardManager.boardY;
        int boardW = boardManager.boardW;
        int boardH = boardManager.boardH;
        int gap = boardManager.gap;

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(hintFont);

        g2.setColor(Color.GRAY);
        g2.fillRoundRect(boardX, boardY, boardW - 3, boardH - 3, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(boardX, boardY, boardW - 3, boardH - 3, 10, 10);

        int tileW = boardW / boardManager.w - gap;
        int tileH = boardH / boardManager.h - gap;
        for (int x = 0; x < boardManager.h; x++) {
            for (int y = 0; y < boardManager.w; y++) {
                int fullX = boardX + gap + y * (tileW + gap);
                int fullY = boardY + gap + x * (tileH + gap);
                g2.setColor(tileShadowColor);
                g2.fillRoundRect(fullX + 3, fullY + 3, tileW, tileH, 10, 10);
                if (boardManager.isDiscovered(x, y))
                    g2.setColor(discoveredColor);
                else
                    g2.setColor(undiscoveredColor);
                g2.fillRoundRect(fullX, boardY + gap + x * (tileH + gap), tileW, tileH, 10, 10);
                if (boardManager.isDiscovered(x, y)) {
                    if (boardManager.getTile(x, y) == boardManager.BOARD_BOMB) {
                        g2.setColor(Color.RED);
                        g2.fillOval(fullX, boardY + gap + x * (tileH + gap), tileW, tileH);
                    } else if (boardManager.getTile(x, y) == boardManager.BOARD_RADAR) {
                        g2.setColor(Color.GREEN);
                        g2.fillOval(fullX, boardY + gap + x * (tileH + gap), tileW, tileH);
                    }
                    int bombhint = boardManager.getHint(x, y, boardManager.BOARD_BOMB);
                    int powerhint = boardManager.getHint(x, y, boardManager.BOARD_RADAR);
                    g2.setColor(hintColors[bombhint]);
                    if (bombhint > 0) {
                        g2.drawString("" + boardManager.getHint(x, y, boardManager.BOARD_BOMB), fullX + tileW / 4,
                                boardY + gap + x * (tileH + gap) + ((float) tileH / 1.5f));
                    }
                    
                    if (powerhint > 0) {
                        g2.setColor(Color.YELLOW);
                        g2.drawString("!", fullX + ((float) tileW / 1.5f), boardY + gap + x * (tileH + gap) + ((float) tileH / 1.5f));
                    }
                        
                } else if (false && boardManager.flags[x][y] == 1) {
                    g2.setColor(Color.RED);
                    g2.fillOval(fullX, boardY + gap + x * (tileH + gap), tileW, tileH);
                }
            }
        }

        if (mouseLoc != null) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            g2.setColor(new Color(100, 100, 0, 100));
            g2.fillRoundRect(boardX + gap + y * (tileW + gap), boardY + gap + x * (tileH + gap), tileW, tileH, 10, 10);
        }
        if (debug && mouseLoc != null) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            if (boardManager.getHint(x, y, boardManager.BOARD_BOMB) == 0 && boardManager.getTile(x, y) != boardManager.BOARD_BOMB) {
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
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
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
        int mouseX = e.getX();
        int mouseY = e.getY();
        // get the board entry that the mouse is over
        int boardX = boardManager.boardX;
        int boardY = boardManager.boardY;
        int boardW = boardManager.boardW;
        int boardH = boardManager.boardH;

        int tileW = boardW / boardManager.h;
        int tileH = boardH / boardManager.w;

        if (mouseX >= boardX && mouseX <= boardX + boardW - 5 && mouseY >= boardY && mouseY <= boardY + boardH - 5) {
            int x = (mouseY - boardY) / tileH;
            int y = (mouseX - boardX) / tileW;
            return new int[] { x, y };
        } else {
            return null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseLoc = getMousePos(e);
        if (mouseLoc != null) {
            int x = mouseLoc[0];
            int y = mouseLoc[1];
            if (e.getButton() == MouseEvent.BUTTON1) {
                boardManager.stepOnTile(x, y);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
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

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("You're Mother");
        JPanel gamePanel = new Game();

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
