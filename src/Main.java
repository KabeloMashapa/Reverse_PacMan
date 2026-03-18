import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    public Main() {
        initUI();
    }

    private void initUI() {
        add(new Board());
        setTitle("Ghost Eating Pac");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 420);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Main ex = new Main();
            ex.setVisible(true);
        });
    }
}