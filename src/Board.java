import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Board extends JPanel implements ActionListener {

    private final int BLOCK_SIZE = 24;
    private final int N_BLOCKS = 15;
    private final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE;

    private final int PAC_ANIM_DELAY = 2;
    private final int PACMAN_ANIM_COUNT = 4;
    private final int PACMAN_SPEED = 6;

    private Dimension d;
    private final Color dotColor = new Color(192, 192, 0);
    private Color mazeColor;
    private boolean inGame = true;
    private boolean dying = false;

    private int pacAnimCount = PAC_ANIM_DELAY;
    private int pacAnimDir = 1;
    private int pacmanAnimPos = 0;

    private int N_GHOSTS = 6;
    private int pacsLeft, score;

    private int[] ghost_x, ghost_dx, ghost_y, ghost_dy, ghostSpeed, dx, dy;

    private int pacman_x, pacman_y, pacman_dx, pacman_dy;
    private int req_dx, req_dy, view_dx, view_dy;

    private short[] screenData;
    private Timer timer;

    private Image ghost;
    private Image pacman1, pacman2up, pacman2left, pacman2right, pacman2down;
    private Image pacman3up, pacman3left, pacman3right, pacman3down;
    private Image pacman4up, pacman4left, pacman4right, pacman4down;

    private final short levelData[] = new short[N_BLOCKS * N_BLOCKS]; // 225 elements
    private final int validSpeeds[] = {1,2,3,4,5,6,7,8};
    private int currentSpeed = 3;

    public Board() {
        setFocusable(true);
        setBackground(Color.black);
        setDoubleBuffered(true);
        loadImages();
        initVariables();
        initBoard();
    }

    private void initBoard() {
        addKeyListener(new TAdapter());
    }

    private void initVariables() {
        mazeColor = new Color(5,100,5);
        d = new Dimension(SCREEN_SIZE, SCREEN_SIZE);
        screenData = new short[N_BLOCKS * N_BLOCKS];

        ghost_x = new int[N_GHOSTS];
        ghost_dx = new int[N_GHOSTS];
        ghost_y = new int[N_GHOSTS];
        ghost_dy = new int[N_GHOSTS];
        ghostSpeed = new int[N_GHOSTS];

        dx = new int[4];
        dy = new int[4];

        timer = new Timer(40, this);
        timer.start();

        // Fill levelData with a simple maze (walls around, dots inside)
        for (int i = 0; i < N_BLOCKS; i++) {
            for (int j = 0; j < N_BLOCKS; j++) {
                if (i==0 || i==N_BLOCKS-1 || j==0 || j==N_BLOCKS-1) {
                    levelData[i * N_BLOCKS + j] = 15; // wall
                } else {
                    levelData[i * N_BLOCKS + j] = 16; // dot
                }
            }
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        initGame();
    }

    private void initGame() {
        pacsLeft = 3;
        score = 0;
        initLevel();
    }

    private void initLevel() {
        for (int i = 0; i < N_BLOCKS * N_BLOCKS; i++) {
            screenData[i] = levelData[i];
        }
        continueLevel();
    }

    private void continueLevel() {
        int dxSign = 1;
        for (int i = 0; i < N_GHOSTS; i++) {
            ghost_x[i] = BLOCK_SIZE * 4;
            ghost_y[i] = BLOCK_SIZE * 4;
            ghost_dx[i] = dxSign;
            ghost_dy[i] = 0;
            dxSign = -dxSign;

            int r = (int)(Math.random() * validSpeeds.length);
            ghostSpeed[i] = validSpeeds[r];
        }

        pacman_x = 7 * BLOCK_SIZE;
        pacman_y = 11 * BLOCK_SIZE;
        req_dx = 0;
        req_dy = 0;
        view_dx = -1;
        view_dy = 0;
        pacman_dx = 0;
        pacman_dy = 0;
        dying = false;
    }

    private void doAnim() {
        pacAnimCount--;
        if (pacAnimCount <= 0) {
            pacAnimCount = PAC_ANIM_DELAY;
            pacmanAnimPos += pacAnimDir;
            if (pacmanAnimPos == PACMAN_ANIM_COUNT - 1 || pacmanAnimPos == 0) pacAnimDir = -pacAnimDir;
        }
    }

    private void playGame(Graphics2D g2d) {
        movePacman();
        moveGhosts(g2d);
        drawPacman(g2d);
    }

    private void moveGhosts(Graphics2D g2d) {
        for (int i = 0; i < N_GHOSTS; i++) {
            if (ghost_x[i] % BLOCK_SIZE == 0 && ghost_y[i] % BLOCK_SIZE == 0) {
                int pos = ghost_x[i]/BLOCK_SIZE + N_BLOCKS*(ghost_y[i]/BLOCK_SIZE);

                if (pos < 0 || pos >= screenData.length) pos = 0; // safety

                int count = 0;
                if ((screenData[pos] & 1) == 0 && ghost_dx[i] != 1) { dx[count]= -1; dy[count]=0; count++; }
                if ((screenData[pos] & 2) == 0 && ghost_dy[i] != 1) { dx[count]= 0; dy[count]=-1; count++; }
                if ((screenData[pos] & 4) == 0 && ghost_dx[i] != -1){ dx[count]=1; dy[count]=0; count++; }
                if ((screenData[pos] & 8) == 0 && ghost_dy[i] != -1){ dx[count]=0; dy[count]=1; count++; }

                if (count == 0) { ghost_dx[i] = 0; ghost_dy[i] = 0; }
                else {
                    int r = (int)(Math.random()*count);
                    ghost_dx[i] = dx[r];
                    ghost_dy[i] = dy[r];
                }
            }

            ghost_x[i] += ghost_dx[i]*ghostSpeed[i];
            ghost_y[i] += ghost_dy[i]*ghostSpeed[i];

            // Draw ghost
            g2d.drawImage(ghost, ghost_x[i]+1, ghost_y[i]+1, this);

            // Collision with Pacman
            if (pacman_x > ghost_x[i]-12 && pacman_x < ghost_x[i]+12 &&
                    pacman_y > ghost_y[i]-12 && pacman_y < ghost_y[i]+12 &&
                    inGame) {
                dying = true;
            }
        }
    }

    private void movePacman() {
        int pos = pacman_x / BLOCK_SIZE + N_BLOCKS * (pacman_y / BLOCK_SIZE);
        if (pos < 0 || pos >= screenData.length) pos = 0;

        short ch = screenData[pos];

        if ((ch & 16) != 0) {
            screenData[pos] = (short)(ch & 15);
            score++;
        }

        // Change direction
        if (req_dx != 0 || req_dy != 0) {
            pacman_dx = req_dx;
            pacman_dy = req_dy;
            view_dx = pacman_dx;
            view_dy = pacman_dy;
        }

        pacman_x += PACMAN_SPEED*pacman_dx;
        pacman_y += PACMAN_SPEED*pacman_dy;

        // Keep Pacman inside bounds
        if (pacman_x < 0) pacman_x = 0;
        if (pacman_x > SCREEN_SIZE-BLOCK_SIZE) pacman_x = SCREEN_SIZE-BLOCK_SIZE;
        if (pacman_y < 0) pacman_y = 0;
        if (pacman_y > SCREEN_SIZE-BLOCK_SIZE) pacman_y = SCREEN_SIZE-BLOCK_SIZE;
    }

    private void drawPacman(Graphics2D g2d) {
        Image img = pacman1;
        if (view_dx == -1) img = pacman2left;
        if (view_dx == 1) img = pacman2right;
        if (view_dy == -1) img = pacman2up;
        if (view_dy == 1) img = pacman2down;
        g2d.drawImage(img, pacman_x, pacman_y, this);
    }

    private void drawMaze(Graphics2D g2d) {
        int i = 0;
        for (int y=0;y<SCREEN_SIZE;y+=BLOCK_SIZE){
            for(int x=0;x<SCREEN_SIZE;x+=BLOCK_SIZE){
                g2d.setColor(mazeColor);
                g2d.setStroke(new BasicStroke(2));
                if ((screenData[i]&1)!=0) g2d.drawLine(x,y,x,y+BLOCK_SIZE-1);
                if ((screenData[i]&2)!=0) g2d.drawLine(x,y,x+BLOCK_SIZE-1,y);
                if ((screenData[i]&4)!=0) g2d.drawLine(x+BLOCK_SIZE-1,y,x+BLOCK_SIZE-1,y+BLOCK_SIZE-1);
                if ((screenData[i]&8)!=0) g2d.drawLine(x,y+BLOCK_SIZE-1,x+BLOCK_SIZE-1,y+BLOCK_SIZE-1);
                if ((screenData[i]&16)!=0){
                    g2d.setColor(dotColor);
                    g2d.fillOval(x+11,y+11,2,2);
                }
                i++;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        doAnim();
        drawMaze(g2d);
        playGame(g2d);
        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void actionPerformed(ActionEvent e){
        repaint();
    }

    private void loadImages() {
        ghost = new ImageIcon(getClass().getResource("/Pacman_Pics/ghost.png")).getImage();
        pacman1 = new ImageIcon(getClass().getResource("/Pacman_Pics/pacman.png")).getImage();
        pacman2up = new ImageIcon(getClass().getResource("/Pacman_Pics/up1.png")).getImage();
        pacman2down = new ImageIcon(getClass().getResource("/Pacman_Pics/down1.png")).getImage();
        pacman2left = new ImageIcon(getClass().getResource("/Pacman_Pics/left1.png")).getImage();
        pacman2right = new ImageIcon(getClass().getResource("/Pacman_Pics/right1.png")).getImage();
        pacman3up = pacman2up;
        pacman3down = pacman2down;
        pacman3left = pacman2left;
        pacman3right = pacman2right;
        pacman4up = pacman2up;
        pacman4down = pacman2down;
        pacman4left = pacman2left;
        pacman4right = pacman2right;
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e){
            int key = e.getKeyCode();
            if (inGame) {
                if (key == KeyEvent.VK_LEFT) { req_dx=-1; req_dy=0; }
                if (key == KeyEvent.VK_RIGHT) { req_dx=1; req_dy=0; }
                if (key == KeyEvent.VK_UP) { req_dx=0; req_dy=-1; }
                if (key == KeyEvent.VK_DOWN) { req_dx=0; req_dy=1; }
            }
        }
    }
}