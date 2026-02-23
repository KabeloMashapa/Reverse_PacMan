import javax.swing.JFrame;
import java.awt.*;

public class Main extends JFrame{
    public Main() {

    }
    private void initUI () {
        add(new Board());
        setTitle("Ghost eating pac");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380,420);
        setLocationRelativeTo(null);

    }

    public static void main(String[]args) {
        EventQueue.invokeLater(() -> {
            Main ex = new Main();
            ex.setVisible(true);

        });
    }
}