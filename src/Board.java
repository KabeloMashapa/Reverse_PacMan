import javax.swing.*;
import java.awt.*;
import java.awt.Toolkit;
import java.awt.Dimension;

public class Board extends JPanel {
    private Dimension d ;
    private final Font smallFont = new Font("Helvetica",Font.BOLD,14);
    private Image ii ;
    private final Color dotColor = new Color(192,192,0);
    private Color mazeColor;
    private boolean inGame = true ;
    private boolean dying = false ;
    private final int Block_Size = 24 ;
    private final int N_BLOCKS = 15 ;
    private final int SCREEN_SIZE = N_BLOCKS * Block_Size ;
    private final int PAC_ANIM_DELAY = 2 ;
    private final int PACMAN_ANIM_COUNT = 4 ;
    private final int PACMAN_SPEED = 6 ;

    private int pacAnimCount = PAC_ANIM_DELAY ;
    private int pacAnimDir = 1 ;
    private int pacmanAnimPos = 0 ;
    private int N_GHOSTS = 6 ;
    private int MAX_GHOST ;
    private int pacsleft,score;
    private int[] ghost_x ,ghost_dx, ghost_y ,ghost_dy , ghostSpeed,dx,dy;

    private int pacman_x , pacman_y, pacman_dx, pacman_dy ;
    private int req_dx , req_dy , view_dx ,view_dy ;
    private final short levelData[] = {
            19,26,26,26,18,18,18,18,18,18,18,22,21,0,0,
            0,17,16,15,13,13,21,14,13,15,14,15,2,7,22,33,
            12,12,35,35,13,14,14,13,15,15,13,13,10,10,10,
            10,10,14,16,16,16,16,16,17,17,17,18,18,18,13,13,
            13,17,17,16,16,18,18,19,19,19,10,10,10,18,18,18,
            12,12,12,14,14,14,16,16,16,23,23,22,27,24,23,24
    };
    private final int validSpeeds[] = {1,2,3,4,5,6,7,8};
    private final int maxSpeed = 6 ;
    private int currentSpeed = 4 ;
    private short[]  screenData ;
    private Timer timer ;

    public Board() {
        initVariables();
        initBoard();

    }
    private void initBoard() {
        setFocusable(true);
        setBackground(Color.black);
    }
    private void initVariables() {
        screenData = new short[N_GHOSTS * N_GHOSTS] ;
        mazeColor = new Color(5,100,5);
        d = new Dimension(400,400);
        ghost_x = new int[MAX_GHOST] ;
        ghost_dx = new int [MAX_GHOST];
        ghost_y = new int[MAX_GHOST];
        ghost_dy = new int[MAX_GHOST];
        ghostSpeed = new int[MAX_GHOST];
        dx = new int[4];
        dy = new int[4];

    }
    @Override
    public void addNotify() {
        super.addNotify();
        initGame();
    }
    private void initGame(){
        pacsleft = 3 ;
        score = 0 ;
        initLevel();
        N_GHOSTS = 6 ;
        currentSpeed = 3 ;
    }
    private void initLevel() {
        int i ;
        for(i = 0 ; i < N_GHOSTS*N_GHOSTS;i++) {
            screenData[i] = levelData[i];
        }
    }
    private void drawMaze(Graphics2D g2d) {
        short i = 0 ;
        int x,y ;
        for(y = 0 ; y < SCREEN_SIZE;y+= Block_Size) {
            for(x=0 ; x < SCREEN_SIZE ; x+= Block_Size) {
                g2d.setColor(mazeColor);
                g2d.setStroke(new BasicStroke(2));
                if((screenData[i] & 1) != 0) {
                    g2d.drawLine(x,y,x,y + Block_Size-1);
                }
                if((screenData[i] & 2) != 0 ) {
                    g2d.drawLine(x,y,x+Block_Size-1,y);
                }
                if((screenData[i] & 4) != 0) {
                    g2d.drawLine(x + Block_Size-1,y,x,y+Block_Size-1);
                }
                if((screenData[i] & 8) != 0) {
                    g2d.drawLine(x ,y + Block_Size-1,x+Block_Size-1,y+Block_Size-1);
                }
                if((screenData[i] & 16) != 0) {
                    g2d.setColor(dotColor);
                    g2d.drawLine(x+ 11,y + 11,2,2);
                }
                i++;
            }

        }
    }
    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        doDrawing(g);

    }
    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g ;
        g2d.setColor(Color.black);
        g2d.fillRect(0,0,d.width,d.height);
        drawMaze(g2d);
        g2d.drawImage(ii,5,5,this);
        Toolkit.getDefaultToolkit().sync();
        g2d.dispose();

    }


}
