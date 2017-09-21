import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class GUI extends Frame {
	private static final long serialVersionUID = 1L; //Idk what this is but it suppresses eclipse warning
	
	public static final int BOARD_SIZE = 24;
	public static final float CIRCLE_SIZE = 10;
	public static final float X_BORDER_SIZE = 20;
	public static final float Y_BORDER_SIZE = 80;
	public static final float SPACING = 40;
	public static final Paint NEUTRAL_COLOR = Color.gray;
	
	public static final int BOARD_WIDTH =(int) (BOARD_SIZE * SPACING + X_BORDER_SIZE);
	public static final int BOARD_HEIGHT =(int) (BOARD_SIZE * SPACING + Y_BORDER_SIZE);
	public Shape[] circles = new Shape[BOARD_SIZE*BOARD_SIZE];
	Shape borderSquare = new Rectangle2D.Double(X_BORDER_SIZE+SPACING/2.0, Y_BORDER_SIZE+SPACING/2.0,(BOARD_SIZE-2)*SPACING, (BOARD_SIZE-2)*SPACING);

	public void paint(Graphics g) {
		Graphics2D ga = (Graphics2D)g;
		ga.setPaint(NEUTRAL_COLOR);
		for (float y=0; y<BOARD_SIZE; y++) {
			for (float x=0; x<BOARD_SIZE; x++) {
				Shape circle = new Ellipse2D.Float(X_BORDER_SIZE+x*SPACING, Y_BORDER_SIZE+y*SPACING, CIRCLE_SIZE, CIRCLE_SIZE);
				int index = (int)(x+y*BOARD_SIZE);
				circles[index] = circle;		
				ga.draw(circles[index]);
				ga.fill(circles[index]);
			}
		}
		ga.setPaint(Color.red);
		ga.draw(borderSquare);
	}

	public static void main(String args[]) {
		Frame frame = new GUI();
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				System.exit(0);
			}
		});
		frame.setSize(BOARD_WIDTH, BOARD_HEIGHT);
		frame.setVisible(true);
	}
}