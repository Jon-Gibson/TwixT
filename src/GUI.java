import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import java.net.URISyntaxException;
import java.net.URL;

import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GUI extends JPanel implements ComponentListener {
	public static final int BOARD_DIM = GameController.BOARD_DIM;
	public static float CIRCLE_SIZE = 40; // Default: 40
	public static float SPACING;
	public static float BOARD_WIDTH;
	public static float BOARD_HEIGHT;

	public static float SPACE_RATIO = 1.6f; // Default: 1.6
	public static float X_BORDER_SIZE = 50; // Default: 30
	public static float Y_BORDER_SIZE = 30; // Default: 30
	public static float MIN_X_PAD = 50; // Default: 30
	public static float MIN_Y_PAD = 30; // Default: 30

	public static final Color NEUTRAL_COLOR = Color.white;
	public static final Color P1_COLOR = Color.red;
	public static final Color P2_COLOR = Color.black;

	public static Dot[][] dots = new Dot[BOARD_DIM][BOARD_DIM];
	public static Shape borderSquare;
	public static JFrame frame;
	public static JPanel gamePanel;

	public static BufferedImage p1Circ;
	public static BufferedImage p2Circ;
	public static BufferedImage tempp1Circ;
	public static BufferedImage tempp2Circ;
	public static BufferedImage emptyCirc;
	public static BufferedImage blackWinText;
	public static BufferedImage redWinText;
	public static BufferedImage winner = null;

	public GUI() {
		frame = new JFrame();
		frame.setAlwaysOnTop(false);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		JMenuBar menubar = new JMenuBar();
		menubar.setPreferredSize(new Dimension(screenSize.width, screenSize.height / 30));

		JMenu settings = new JMenu("Settings");
		settings.setMnemonic(KeyEvent.VK_S);

		JMenu help = new JMenu("Help");
		help.setMnemonic(KeyEvent.VK_H);

		JMenuItem players = new JMenuItem("Change Players");
		JMenuItem thinking = new JMenuItem("Toggle Show Thinking");
		JMenuItem thinkTime = new JMenuItem("Change Thinking Time");
		JMenuItem rules = new JMenuItem("Open TwixT Rules");

		players.setMnemonic(KeyEvent.VK_C);
		thinking.setMnemonic(KeyEvent.VK_T);
		thinkTime.setMnemonic(KeyEvent.VK_H);
		rules.setMnemonic(KeyEvent.VK_R);

		players.setToolTipText("Change the player types, takes effect after the next move.");
		thinking.setToolTipText("Show AI simulating the moves it's considering");
		thinkTime.setToolTipText("Adjust the delay for each condiered move");

		players.addActionListener((ActionEvent event) -> {
			GameController.changePlayers = true;
			JOptionPane.showMessageDialog(this, "You can now change the players after the next move.", "Player Change",
					JOptionPane.INFORMATION_MESSAGE);
		});
		thinking.addActionListener((ActionEvent event) -> {
			GameController.showThinking = !GameController.showThinking;
		});
		thinkTime.addActionListener((ActionEvent event) -> {
			try {
				String newDelay = (String) JOptionPane.showInputDialog(this,
						"How many milliseconds should it take the AI to consider a move?", "Set Move Delay",
						JOptionPane.QUESTION_MESSAGE, null, null, "0");
				if (Integer.parseInt(newDelay) < 0)
					throw new Exception();
				GameController.SimulateMoveDelay = Integer.parseInt(newDelay);
				System.out.println("Changed think time to: " + newDelay);
			} catch (Throwable e) {
				JOptionPane.showMessageDialog(this, "Thinking delay needs to be a positive integer.", "Invalid Input",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		rules.addActionListener((ActionEvent event) -> {
			try {
				Desktop.getDesktop().browse(new URL("https://en.wikipedia.org/wiki/TwixT#Rules").toURI());
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		});

		settings.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));
		help.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));
		players.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));
		thinking.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));
		rules.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));
		thinkTime.setFont(new Font(null, Font.PLAIN, screenSize.height / 60));

		settings.add(players);
		settings.add(thinking);
		settings.add(thinkTime);
		help.add(rules);

		menubar.add(settings);
		menubar.add(help);
		frame.setJMenuBar(menubar);
		// setLayout(null);

		try {
			p1Circ = ImageIO.read(GUI.class.getResource("/p1circ.png"));
			p2Circ = ImageIO.read(GUI.class.getResource("/p2circ.png"));
			tempp1Circ = ImageIO.read(GUI.class.getResource("/tempp1circ.png"));
			tempp2Circ = ImageIO.read(GUI.class.getResource("tempp2circ.png"));
			emptyCirc = ImageIO.read(GUI.class.getResource("/emptycirc.png"));
			redWinText = ImageIO.read(GUI.class.getResource("/redwin.png"));
			blackWinText = ImageIO.read(GUI.class.getResource("/blackwin.png"));
			frame.setIconImage(ImageIO.read(GUI.class.getResource("/ico.png")));
		} catch (IOException e) {
			System.out.println("Couldn't find resources");
		}

		gamePanel = new JPanel();
		JPanel playersPanel = new JPanel();
		JLabel p1Img = new JLabel("Player 1: " + GameController.selection1,
				new ImageIcon(GUI.p1Circ.getScaledInstance((int) CIRCLE_SIZE, (int) CIRCLE_SIZE, (int) CIRCLE_SIZE)),
				0);
		JLabel p2Img = new JLabel("Player 2: " + GameController.selection2,
				new ImageIcon(GUI.p2Circ.getScaledInstance((int) CIRCLE_SIZE, (int) CIRCLE_SIZE, (int) CIRCLE_SIZE)),
				0);

		playersPanel.add(p1Img);
		playersPanel.add(p2Img);
		this.add(gamePanel);
		// this.add(playersPanel);
		frame.add(this);

		makeDots();
		// this.setBackground(new Color(100, 100, 100)); // Dark Mode?
		int DIM = Math.min(screenSize.height, screenSize.height) - 100;
		frame.setSize(DIM, DIM);
		frame.setTitle("TwixT AIive by Jon and Patrick");

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});

		frame.setVisible(true);
		updateSize();
		frame.addComponentListener(this);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D ga = (Graphics2D) g;
		// Black Border Square
		ga.setPaint(P2_COLOR);
		ga.setStroke(new BasicStroke(5));
		ga.draw(borderSquare);
		// Red Lines on Top
		Rectangle rectBounds = borderSquare.getBounds();
		ga.setPaint(P1_COLOR);
		ga.drawLine(rectBounds.x, rectBounds.y, rectBounds.x + rectBounds.width, rectBounds.y);
		ga.drawLine(rectBounds.x, rectBounds.y + rectBounds.height, rectBounds.x + rectBounds.width,
				rectBounds.y + rectBounds.height);

		// Draw all edges
		for (Dot[] dotRows : dots) {
			for (Dot d : dotRows) {
				for (Edge e : d.edges) {
					Dot other = e.dot1 == d ? e.dot2 : e.dot1;
					boolean isBorderEdge = (e.dot1.game_x == e.dot2.game_x) || (e.dot1.game_y == e.dot2.game_y);
					if (e.linked && !isBorderEdge) {
						if (e.isTemp)
							ga.setColor(new Color(d.color.getRed() / 255, d.color.getGreen() / 255,
									d.color.getBlue() / 255, 0.05f));
						else
							ga.setColor(d.color);
						ga.setStroke(new BasicStroke(CIRCLE_SIZE / 4));
						ga.drawLine((int) (d.x + CIRCLE_SIZE / 2), (int) (d.y + CIRCLE_SIZE / 2),
								(int) (other.x + CIRCLE_SIZE / 2), (int) (other.y + CIRCLE_SIZE / 2));
					}
				}
			}
		}
		// Draw Dots
		for (int y = 0; y < BOARD_DIM; y++) {
			for (int x = 0; x < BOARD_DIM; x++) {
				if ((x == 0 || x == BOARD_DIM - 1) && (y == 0 || y == BOARD_DIM - 1))
					continue;
				Dot dot = dots[y][x];
				this.add(dot);
				dot.setLocation((int) dot.x, (int) dot.y);
				BufferedImage image = null;
				if (dot.isTemp) {
					image = dot.color == P1_COLOR ? tempp1Circ : (dot.color == P2_COLOR ? tempp2Circ : emptyCirc);
				} else
					image = dot.color == P1_COLOR ? p1Circ : (dot.color == P2_COLOR ? p2Circ : emptyCirc);

				// if (image != emptyCirc) //Make 'pegs'
				ga.drawImage(image, (int) dot.x, (int) dot.y, (int) CIRCLE_SIZE, (int) CIRCLE_SIZE, null);
				// int innerCircSize = (int) CIRCLE_SIZE*4/8;
				// ga.drawImage(emptyCirc, (int) (dot.x + (CIRCLE_SIZE-innerCircSize)/2), (int)
				// (dot.y + (CIRCLE_SIZE-innerCircSize)/2), innerCircSize, innerCircSize, null);
				// //Draw Pegs
			}
		}
	}

	// Creates the 2D Dot array
	public void makeDots() {
		for (int y = 0; y < BOARD_DIM; y++) {
			for (int x = 0; x < BOARD_DIM; x++) {
				Dot dot = null;
				try {
					dot = new Dot(X_BORDER_SIZE + x * SPACING, Y_BORDER_SIZE + y * SPACING, CIRCLE_SIZE, NEUTRAL_COLOR,
							(JPanel) gamePanel, x, y);
				} catch (IOException e) {
					System.out.println("Couldn't make dots, might want to restart game.");
				}
				dots[y][x] = dot;
			}
		}
	}

	public void updateDots() {
		for (int y = 0; y < BOARD_DIM; y++) {
			for (int x = 0; x < BOARD_DIM; x++) {
				dots[y][x].x = X_BORDER_SIZE + x * SPACING;
				dots[y][x].y = Y_BORDER_SIZE + y * SPACING;
				dots[y][x].radius = CIRCLE_SIZE;
				dots[y][x].circle = new Ellipse2D.Float(dots[y][x].x, dots[y][x].y, CIRCLE_SIZE, CIRCLE_SIZE);
				dots[y][x].setSize((int) CIRCLE_SIZE, (int) CIRCLE_SIZE);
			}
		}
	}

	public void updateSize() {
		BOARD_HEIGHT = frame.getHeight();
		BOARD_WIDTH = frame.getWidth();
		float maxSize = Math.max(BOARD_HEIGHT, BOARD_WIDTH);
		if (BOARD_HEIGHT == maxSize) {
			Y_BORDER_SIZE = Math.max(MIN_Y_PAD, (BOARD_HEIGHT - BOARD_WIDTH) / 4); // Something is messed up here, why 4?
			X_BORDER_SIZE = MIN_X_PAD;
		} else {
			X_BORDER_SIZE = Math.max(MIN_X_PAD, (BOARD_WIDTH - BOARD_HEIGHT) / 2);
			Y_BORDER_SIZE = MIN_Y_PAD;
		}

		SPACING = (Math.min(BOARD_WIDTH - 2 * X_BORDER_SIZE, BOARD_HEIGHT - 4 * Y_BORDER_SIZE)) / BOARD_DIM;

		CIRCLE_SIZE = SPACING / SPACE_RATIO;

		borderSquare = new Rectangle2D.Double(X_BORDER_SIZE + 3 * SPACING / 4.0, Y_BORDER_SIZE + 3 * SPACING / 4.0,
				(BOARD_DIM - 2) * SPACING, (BOARD_DIM - 2) * SPACING);

		updateDots();
		repaint();
	}

	// COMPONENT EVENTS, want to use for resizing

	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent e) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				updateSize();
			}
		});

		thread.start();
	}

	public void componentShown(ComponentEvent arg0) {
	}
}