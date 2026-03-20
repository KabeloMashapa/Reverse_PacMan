import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Board extends JPanel implements ActionListener {

    private final int BLOCK_SIZE  = 24;
    private final int N_BLOCKS    = 15;
    private final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE;

    private final int PAC_ANIM_DELAY    = 2;
    private final int PACMAN_ANIM_COUNT = 4;
    private final int PACMAN_SPEED      = 2;  // AI Pacman speed
    private final int GHOST_SPEED       = 3;  // Player ghost speed

    private final int TIME_LIMIT_SECONDS = 60;
    private final int TICKS_PER_SECOND   = 25; // Timer fires every 40 ms

    private final short[] levelData = {
            19, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 22, 0,
            17, 16, 15, 13, 13, 13, 13, 13, 13, 13, 13, 13, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            17, 16, 15, 13, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 21,
            25, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 28, 0
    };

    private short[] screenData;
    private int totalDots;

    // Player-controlled ghost
    private int ghost_x, ghost_y;
    private int ghost_dx, ghost_dy;
    private int req_dx,   req_dy;

    // AI-controlled Pacman
    private int pacman_x,  pacman_y;
    private int pacman_dx, pacman_dy;
    private int view_dx = -1, view_dy = 0;

    // Animation
    private int pacAnimCount   = PAC_ANIM_DELAY;
    private int pacAnimDir     = 1;
    private int pacmanAnimPos  = 0;

    // Game status
    private boolean inGame = false;
    private boolean win    = false;
    private String  endMsg = "";
    private int     ticksElapsed = 0;

    // Graphics helpers
    private Dimension d;
    private final Font smallFont = new Font("Helvetica", Font.BOLD, 14);
    private final Font bigFont   = new Font("Helvetica", Font.BOLD, 20);
    private final Color dotColor  = new Color(192, 192, 0);
    private final Color mazeColor = new Color(5, 100, 5);

    // Images
    private Image ghostImg;
    private Image pacman1;
    private Image pacman2up,    pacman3up,    pacman4up;
    private Image pacman2down,  pacman3down,  pacman4down;
    private Image pacman2left,  pacman3left,  pacman4left;
    private Image pacman2right, pacman3right, pacman4right;

    private Timer timer;

    public Board() {
        loadImages();
        initVariables();
        initBoard();
    }

    private void initBoard() {
        addKeyListener(new TAdapter());
        setFocusable(true);
        setBackground(Color.black);
    }

    private void initVariables() {
        screenData = new short[N_BLOCKS * N_BLOCKS];
        d          = new Dimension(SCREEN_SIZE, SCREEN_SIZE + 40); // extra 40px for HUD
        timer      = new Timer(40, this);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        initGame();
    }
    private void initGame() {
        totalDots = 0;
        for (int i = 0; i < screenData.length; i++) {
            screenData[i] = levelData[i];
            if ((screenData[i] & 16) != 0) totalDots++;
        }

        // Player ghost — top-left corner
        ghost_x  = 1 * BLOCK_SIZE;
        ghost_y  = 1 * BLOCK_SIZE;
        ghost_dx = 0;
        ghost_dy = 0;
        req_dx   = 0;
        req_dy   = 0;

        // AI Pacman , bottom-right area, starts moving left
        pacman_x  = 11 * BLOCK_SIZE;
        pacman_y  = 11 * BLOCK_SIZE;
        pacman_dx = -1;
        pacman_dy = 0;
        view_dx   = -1;
        view_dy   = 0;

        ticksElapsed = 0;
        inGame       = true;
        win          = false;
        endMsg       = "";
    }

    private void doAnim() {
        pacAnimCount--;
        if (pacAnimCount <= 0) {
            pacAnimCount = PAC_ANIM_DELAY;
            pacmanAnimPos += pacAnimDir;
            if (pacmanAnimPos == (PACMAN_ANIM_COUNT - 1) || pacmanAnimPos == 0) {
                pacAnimDir = -pacAnimDir;
            }
        }
    }
    private void playGame(Graphics2D g2d) {
        ticksElapsed++;

        // Time-limit check
        if (ticksElapsed >= TIME_LIMIT_SECONDS * TICKS_PER_SECOND) {
            endGame(false, "Time's up!  Pacman escaped!");
            return;
        }

        movePlayerGhost();
        moveAIPacman();

        // Win: ghost catches Pacman
        if (Math.abs(ghost_x - pacman_x) < BLOCK_SIZE - 4
                && Math.abs(ghost_y - pacman_y) < BLOCK_SIZE - 4) {
            endGame(true, "You caught Pacman!  You win!");
            return;
        }

        // Lose: all dots eaten
        int dotsLeft = countDots();
        if (dotsLeft == 0) {
            endGame(false, "Pacman ate every dot!  You lose!");
            return;
        }

        drawMaze(g2d);
        drawGhostSprite(g2d, ghost_x, ghost_y);
        drawPacman(g2d);
        drawHUD(g2d, dotsLeft);
    }
    private boolean canMove(int px,int py,int dx,int dy,int speed) {
        int nx = px + dx * speed ;
        int ny = py + dy * speed ;

        if(nx < 0 || ny < 0 || nx + BLOCK_SIZE > SCREEN_SIZE || ny + BLOCK_SIZE > SCREEN_SIZE)
            return false ;
        int [] xs = {nx,nx + BLOCK_SIZE - 1};
        int [] ys = {ny,ny + BLOCK_SIZE - 1};

        for(int cy : ys) {
            for (int cx : xs) {
                int col = cx / BLOCK_SIZE;
                int row = cy / BLOCK_SIZE;
                int idx = row * N_BLOCKS + col;
                if (idx < 0 || idx >= screenData.length) return false;
                short cell = screenData[idx];
                if (dx == 1 && (cell & 1) != 0) return false;
                if (dx == -1 && (cell & 4) != 0) return false;
                if (dy == 1 && (cell & 2) != 0) return false;
                if (dy == -1 && (cell & 8) != 0) return false;
            }

        }
        return true ;
    }

    private void movePlayerGhost() {
       if((req_dx != 0 || req_dy != 0) && canMove(ghost_x,ghost_y,req_dx,req_dy,GHOST_SPEED)) {
           ghost_dx = req_dx ;
           ghost_dy = req_dy ;
       }
       if(canMove(ghost_x,ghost_y,ghost_dx,ghost_dy,GHOST_SPEED)) {
           ghost_x += GHOST_SPEED * ghost_dx ;
           ghost_y += GHOST_SPEED * ghost_dy ;
       }
    }

    private void moveAIPacman() {
        // Recalculate direction every 8 ticks to keep movement smooth
        if (ticksElapsed % 8 == 0) {
            choosePacmanDirection();
        }
        if(canMove(pacman_x,pacman_y,pacman_dx,pacman_dy,PACMAN_SPEED)) {
            pacman_x += PACMAN_SPEED * pacman_dx ;
            pacman_y += PACMAN_SPEED * pacman_dy ;
        }
        else {
            choosePacmanDirection();
            if(canMove(pacman_x,pacman_y,pacman_dx,pacman_dy,PACMAN_SPEED)) {
                pacman_x += PACMAN_SPEED * pacman_dx ;
                pacman_y += PACMAN_SPEED * pacman_dy ;
            }
        }
        // Sync view direction for animation
        if      (pacman_dx == -1) { view_dx = -1; view_dy =  0; }
        else if (pacman_dx ==  1) { view_dx =  1; view_dy =  0; }
        else if (pacman_dy == -1) { view_dx =  0; view_dy = -1; }
        else if (pacman_dy ==  1) { view_dx =  0; view_dy =  1; }

        // Eat dot under Pacman
        int pos = (pacman_y / BLOCK_SIZE) * N_BLOCKS + (pacman_x / BLOCK_SIZE);
        if (pos >= 0 && pos < screenData.length && (screenData[pos] & 16) != 0) {
            screenData[pos] = (short) (screenData[pos] & 15);
        }
    }


    private void choosePacmanDirection() {
        final int FLEE_RADIUS = BLOCK_SIZE * 5;

        boolean fleeing = (Math.abs(pacman_x - ghost_x) + Math.abs(pacman_y - ghost_y)) < FLEE_RADIUS;

        int[] ddx = {-1,  1,  0, 0};
        int[] ddy = { 0,  0, -1, 1};

        int bestScore = Integer.MIN_VALUE;
        int bestDx    = pacman_dx;
        int bestDy    = pacman_dy;

        for (int d = 0; d < 4; d++) {

            if (ddx[d] == -pacman_dx && ddy[d] == -pacman_dy) continue;
            if(!canMove(pacman_x,pacman_y,ddx[d],ddy[d],PACMAN_SPEED)) continue;


            int nx = pacman_x + ddx[d] * BLOCK_SIZE;
            int ny = pacman_y + ddy[d] * BLOCK_SIZE;
            int score = fleeing ? Math.abs(nx - ghost_x) + Math.abs(ny - ghost_y) : -distToNearestDot(nx,ny);
            if(score > bestScore) {
                bestScore = score ;
                bestDx = ddx[d];
                bestDy = ddy[d];
            }


            if (nx < 0 || ny < 0 || nx >= SCREEN_SIZE || ny >= SCREEN_SIZE) continue;

            int score;
            if (fleeing) {
                // Higher distance from ghost = better
                score = Math.abs(nx - ghost_x) + Math.abs(ny - ghost_y);
            } else {
                // Lower distance to nearest dot = better (negate so higher = better)
                score = -distToNearestDot(nx, ny);
            }
        }
        // No valid non-reversing move found, allows reversing as last option
        if(bestScore == Integer.MIN_VALUE) {
            for(int d = 0 ; d < 4 ; d++) {
                if(canMove(pacman_x,pacman_y,ddx[d],ddy[d],PACMAN_SPEED)) {
                    bestDx = ddx[d];
                    bestDy = ddx[d];
                    break;
                }
            }
        }
        pacman_dx = bestDx;
        pacman_dy = bestDy;
    }

    private int distToNearestDot(int px, int py) {
        int best = Integer.MAX_VALUE;
        for (int row = 0; row < N_BLOCKS; row++) {
            for (int col = 0; col < N_BLOCKS; col++) {
                if ((screenData[row * N_BLOCKS + col] & 16) != 0) {
                    int dist = Math.abs(px - col * BLOCK_SIZE) + Math.abs(py - row * BLOCK_SIZE);
                    if (dist < best) best = dist;
                }
            }
        }
        return (best == Integer.MAX_VALUE) ? 0 : best;
    }

    private int countDots() {
        int n = 0;
        for (short cell : screenData) {
            if ((cell & 16) != 0) n++;
        }
        return n;
    }

    private void endGame(boolean playerWon, String message) {
        inGame = false;
        win    = playerWon;
        endMsg = message;
    }

    private void drawMaze(Graphics2D g2d) {
        int i = 0;
        for (int y = 0; y < SCREEN_SIZE; y += BLOCK_SIZE) {
            for (int x = 0; x < SCREEN_SIZE; x += BLOCK_SIZE) {
                g2d.setColor(mazeColor);
                g2d.setStroke(new BasicStroke(2));

                if ((screenData[i] & 1) != 0) // left wall
                    g2d.drawLine(x, y, x, y + BLOCK_SIZE - 1);
                if ((screenData[i] & 2) != 0) // top wall
                    g2d.drawLine(x, y, x + BLOCK_SIZE - 1, y);
                if ((screenData[i] & 4) != 0) // right wall
                    g2d.drawLine(x + BLOCK_SIZE - 1, y, x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1);
                if ((screenData[i] & 8) != 0) // bottom wall
                    g2d.drawLine(x, y + BLOCK_SIZE - 1, x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1);

                if ((screenData[i] & 16) != 0) { // dot
                    g2d.setColor(dotColor);
                    g2d.fillOval(x + 10, y + 10, 4, 4);
                }
                i++;
            }
        }
    }

    private void drawGhostSprite(Graphics2D g2d, int x, int y) {
        g2d.drawImage(ghostImg, x, y, this);
    }

    private void drawPacman(Graphics2D g2d) {
        if      (view_dx == -1) drawPacmanLeft(g2d);
        else if (view_dx ==  1) drawPacmanRight(g2d);
        else if (view_dy == -1) drawPacmanUp(g2d);
        else                    drawPacmanDown(g2d);
    }

    private void drawPacmanUp(Graphics2D g2d) {
        switch (pacmanAnimPos) {
            case 1:  g2d.drawImage(pacman2up,    pacman_x, pacman_y, this); break;
            case 2:  g2d.drawImage(pacman3up,    pacman_x, pacman_y, this); break;
            case 3:  g2d.drawImage(pacman4up,    pacman_x, pacman_y, this); break;
            default: g2d.drawImage(pacman1,      pacman_x, pacman_y, this); break;
        }
    }

    private void drawPacmanDown(Graphics2D g2d) {
        switch (pacmanAnimPos) {
            case 1:  g2d.drawImage(pacman2down,  pacman_x, pacman_y, this); break;
            case 2:  g2d.drawImage(pacman3down,  pacman_x, pacman_y, this); break;
            case 3:  g2d.drawImage(pacman4down,  pacman_x, pacman_y, this); break;
            default: g2d.drawImage(pacman1,      pacman_x, pacman_y, this); break;
        }
    }

    private void drawPacmanLeft(Graphics2D g2d) {
        switch (pacmanAnimPos) {
            case 1:  g2d.drawImage(pacman2left,  pacman_x, pacman_y, this); break;
            case 2:  g2d.drawImage(pacman3left,  pacman_x, pacman_y, this); break;
            case 3:  g2d.drawImage(pacman4left,  pacman_x, pacman_y, this); break;
            default: g2d.drawImage(pacman1,      pacman_x, pacman_y, this); break;
        }
    }

    private void drawPacmanRight(Graphics2D g2d) {
        switch (pacmanAnimPos) {
            case 1:  g2d.drawImage(pacman2right, pacman_x, pacman_y, this); break;
            case 2:  g2d.drawImage(pacman3right, pacman_x, pacman_y, this); break;
            case 3:  g2d.drawImage(pacman4right, pacman_x, pacman_y, this); break;
            default: g2d.drawImage(pacman1,      pacman_x, pacman_y, this); break;
        }
    }

    private void drawHUD(Graphics2D g2d, int dotsLeft) {
        int hudY = SCREEN_SIZE + 28;
        g2d.setFont(smallFont);
        g2d.setColor(Color.WHITE);

        g2d.drawString("Dots left: " + dotsLeft + " / " + totalDots, 8, hudY);

        int secondsLeft = Math.max(0, TIME_LIMIT_SECONDS - ticksElapsed / TICKS_PER_SECOND);
        String timeStr  = "Time: " + secondsLeft + "s";
        FontMetrics fm  = g2d.getFontMetrics();
        g2d.drawString(timeStr, SCREEN_SIZE - fm.stringWidth(timeStr) - 8, hudY);
    }

    /** Semi-transparent overlay shown on win or lose. */
    private void drawEndScreen(Graphics2D g2d) {
        drawMaze(g2d);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, SCREEN_SIZE / 2 - 50, SCREEN_SIZE, 100);

        g2d.setFont(bigFont);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(win ? Color.GREEN : Color.RED);
        g2d.drawString(endMsg, (SCREEN_SIZE - fm.stringWidth(endMsg)) / 2, SCREEN_SIZE / 2);

        String sub = "Press ENTER to play again";
        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.WHITE);
        g2d.drawString(sub, (SCREEN_SIZE - fm.stringWidth(sub)) / 2, SCREEN_SIZE / 2 + 30);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, d.width, d.height);
        doAnim();

        if (inGame) {
            playGame(g2d);
        } else {
            drawEndScreen(g2d);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void loadImages() {
        ghostImg     = new ImageIcon(getClass().getResource("/Pacman_Pics/ghost.png")).getImage();
        pacman1      = new ImageIcon(getClass().getResource("/Pacman_Pics/pacman.png")).getImage();
        pacman2up    = new ImageIcon(getClass().getResource("/Pacman_Pics/up1.png")).getImage();
        pacman3up    = new ImageIcon(getClass().getResource("/Pacman_Pics/up2.png")).getImage();
        pacman4up    = new ImageIcon(getClass().getResource("/Pacman_Pics/up3.png")).getImage();
        pacman2down  = new ImageIcon(getClass().getResource("/Pacman_Pics/down1.png")).getImage();
        pacman3down  = new ImageIcon(getClass().getResource("/Pacman_Pics/down2.png")).getImage();
        pacman4down  = new ImageIcon(getClass().getResource("/Pacman_Pics/down3.png")).getImage();
        pacman2left  = new ImageIcon(getClass().getResource("/Pacman_Pics/left1.png")).getImage();
        pacman3left  = new ImageIcon(getClass().getResource("/Pacman_Pics/left2.png")).getImage();
        pacman4left  = new ImageIcon(getClass().getResource("/Pacman_Pics/left3.png")).getImage();
        pacman2right = new ImageIcon(getClass().getResource("/Pacman_Pics/right1.png")).getImage();
        pacman3right = new ImageIcon(getClass().getResource("/Pacman_Pics/right2.png")).getImage();
        pacman4right = new ImageIcon(getClass().getResource("/Pacman_Pics/right3.png")).getImage();
    }

    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (!inGame) {
                if (key == KeyEvent.VK_ENTER) initGame(); // restart
                return;
            }

            // Player controls the ghost with arrow keys
            if      (key == KeyEvent.VK_LEFT)  { req_dx = -1; req_dy =  0; }
            else if (key == KeyEvent.VK_RIGHT)  { req_dx =  1; req_dy =  0; }
            else if (key == KeyEvent.VK_UP)     { req_dx =  0; req_dy = -1; }
            else if (key == KeyEvent.VK_DOWN)   { req_dx =  0; req_dy =  1; }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}