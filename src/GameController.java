import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class GameController extends Thread {

	enum PlayerType {
		AI, Human
	}

	public static boolean printBoardValues = false;

	public static boolean displayTurnTimes = true;
	public static PlayerType p1 = PlayerType.AI;
	public static PlayerType p2 = PlayerType.AI;

	public static int BOARD_DIM = 8;

	public static int SimulateMoveDelay = 0;

	public static boolean p1Turn;
	public static Dot[][] dots;
	public static HashMap<Integer, Dot> coordToDot = new HashMap<Integer, Dot>();
	public static GUI gui;
	public static Semaphore turnLock = new Semaphore(0);

	public static boolean promptUser = true; // Change to false to run without pop-up at beginning
	public static boolean changePlayers = false;

	public static boolean showThinking = true;
	public static Dot previousDot;
	public static Dot tempDot;
	public static HashMap<Edge, Edge> tempEdges;

	public static int gameNumber = 0;

	public static String selection1;
	public static String selection2;

	public static void main(String args[]) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		UIManager.put("OptionPane.messageFont", new Font(Font.DIALOG, Font.BOLD, screenSize.height / 60));
		UIManager.put("OptionPane.buttonFont", new Font(Font.DIALOG, Font.BOLD, screenSize.height / 60));
		UIManager.put("TextField.font", new Font(Font.DIALOG, Font.BOLD, screenSize.height / 60));
		UIManager.put("ToolTip.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("List.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("InternalFrame.titleFont", new Font(Font.DIALOG, Font.BOLD, screenSize.height / 60));
		UIManager.put("CheckBox.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("CheckBoxMenuItem.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("ComboBox.font", new Font(Font.DIALOG, Font.BOLD, screenSize.height / 60));
		UIManager.put("EditorPane.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("FormattedTextField.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));
		UIManager.put("Menu.font", new Font(Font.DIALOG, Font.PLAIN, screenSize.height / 60));

		gameNumber++;
		JDialog.setDefaultLookAndFeelDecorated(true);
		if (gameNumber == 1) {
			if ((args == null || args.length == 0)) {
				chooseBoardSize();
			} else if (args[0].equals("-d") || args[0].equals("--default")) {
				if (args.length > 1) { // I.e has args
					BOARD_DIM = Integer.valueOf(args[1]);
				} else {
					chooseBoardSize();
				}
				promptUser = false; // Use default settings
			} else { // Should be length if not -d
				BOARD_DIM = Integer.valueOf(args[0]);
			}
		}

		if (promptUser) {
			choosePlayers();
		}

		GameController gameController = new GameController();
		gameController.run();
	}

	public static void chooseBoardSize() {
		if (promptUser) {
			String boardSize = (String) JOptionPane.showInputDialog(null, "What size board?", "Select Board Size",
					JOptionPane.QUESTION_MESSAGE, null, null, "8");
			try {
				if (boardSize == null)
					System.exit(0);
				BOARD_DIM = Integer.parseInt(boardSize);
				if (BOARD_DIM < 5)
					throw new Throwable();
			} catch (Throwable e) {
				JOptionPane.showMessageDialog(null, "Board size must be an integer greater than 4.", "Invalid Input",
						JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
		}
	}

	public static void choosePlayers() {
		String[] selectionValues = { "Human", "Iterative Deepening Alpha Beta", "Alpha Beta",
				"Iterative Deepening Minimax", "Minimax", "Greedy", "Random" };
		String initialSelection = "Human"; // "Iterative Deepening Alpha Beta";
		selection1 = (String) JOptionPane.showInputDialog(null, "Who is Player 1?", "Select Player Types",
				JOptionPane.QUESTION_MESSAGE, null, selectionValues, initialSelection);
		if (selection1 == null)
			System.exit(0);
		selection2 = (String) JOptionPane.showInputDialog(null, "Who is Player 2?", "Select Player Types",
				JOptionPane.QUESTION_MESSAGE, null, selectionValues, initialSelection);
		if (selection2 == null)
			System.exit(0);
		else if (selection1.equals("Human"))
			p1 = PlayerType.Human;
		else {
			p1 = PlayerType.AI;
			AI.setAI1(selection1);
		}

		if (selection2.equals("Human"))
			p2 = PlayerType.Human;
		else {
			p2 = PlayerType.AI;
			AI.setAI2(selection2);
		}
	}

	public static int coordToAbsoluteIndex(int x, int y) {
		return BOARD_DIM * y + x;
	}

	public static int absoluteIndexToX(int idx) {
		return idx % BOARD_DIM;
	}

	public static int absoluteIndexToY(int idx) {
		return idx / BOARD_DIM;
	}

	public GameController() {
		// Graphical interface
		gui = new GUI();

		// Preprocessing the dots to get a useful representation for the game board.
		dots = GUI.dots;
		for (int y = 0; y < dots.length; y++) {
			for (int x = 0; x < dots[0].length; x++) {
				coordToDot.put(coordToAbsoluteIndex(x, y), dots[y][x]);
				for (Dot dot : getDotNeighborsLesserY(dots[y][x])) {
					Edge newEdge = new Edge(dot, dots[y][x]);
					dots[y][x].edges.add(newEdge);
					dot.edges.add(newEdge);
				}
				if (y == 0 || y == dots.length - 1) {
					if (x < dots.length - 1 && x != 0) {
						Edge e = new Edge(dots[y][x], dots[y][x + 1], 0.0);
						dots[y][x].edges.add(e);
						dots[y][x + 1].edges.add(e);
					}
				}
				if (x == 0 || x == dots.length - 1) {
					if (y < dots.length - 1 && y != 0) {
						Edge e = new Edge(dots[y][x], dots[y + 1][x], 0.0);
						dots[y][x].edges.add(e);
						dots[y + 1][x].edges.add(e);
					}
				}
			}
		}
	}

	// This could all be stored in Dot.
	public LinkedList<Dot> getDotNeighborsLesserY(Dot dot) {
		LinkedList<Dot> result = new LinkedList<Dot>();
		for (int r = -2; r < 0; r++) {
			for (int c = -(3 - Math.abs(r)); c <= (3 - Math.abs(r)); c += 2 * (3 - Math.abs(r))) {
				int x = dot.game_x + c;
				int y = dot.game_y + r;
				if (x >= 0 && y >= 0 && x < BOARD_DIM && y < BOARD_DIM) {
					Dot neighbor = coordToDot.get(coordToAbsoluteIndex(x, y));
					if (neighbor != null)
						result.add(neighbor);
				}
			}
		}
		return result;
	}

	public static HashMap<Edge, Edge> updateConnections(Dot dot, boolean isTemp) {
		HashMap<Edge, Edge> oldEdges = new HashMap<Edge, Edge>();
		for (Edge edge : dot.edges) {
			edge.isTemp = isTemp;
			Dot other = dot == edge.dot1 ? edge.dot2 : edge.dot1;
			if (other.color == dot.color && dot.color != GUI.NEUTRAL_COLOR) {
				// Try to make an edge, but must check if any edges are blocking this edge.
				boolean flagCanLink = true;
				for (Edge intersecting : edge.getIntersections()) {
					flagCanLink = flagCanLink && !intersecting.linked;
				}
				if (flagCanLink) {
					Edge oldEdgeValue = copyEdge(edge);
					if (!oldEdges.containsKey(edge))
						oldEdges.put(edge, oldEdgeValue);

					edge.linked = flagCanLink;
					edge.canLink = false;
					edge.weight = 0;
				}

				for (Edge intersecting : edge.getIntersections()) {
					if (edge.linked) {
						Edge oldIntersectingValue = copyEdge(intersecting);
						if (!oldEdges.containsKey(intersecting))
							oldEdges.put(intersecting, oldIntersectingValue);

						intersecting.canLink = false;
					}
				}
			}
		}
		return oldEdges;
	}

	public static Edge copyEdge(Edge e) {
		Edge newEdge = new Edge(e.dot1, e.dot2, e.weight);
		newEdge.canLink = e.canLink;
		newEdge.weight = e.weight;
		newEdge.linked = e.linked;
		return newEdge;
	}

	public void Turn(PlayerType player) {
		if (player == PlayerType.Human)
			try {
				turnLock.acquire(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		else {
			if (p1Turn)
				AI.AIMakeMove(AI.ai1, AI.depthp1, 1);
			else
				AI.AIMakeMove(AI.ai2, AI.depthp2, 2);
		}

		p1Turn = !p1Turn;
	}

	public void run() {
		GUI.winner = null;
		p1Turn = true;

		AI.openDots = new HashSet<Dot>();
		AI.bestDots = new PriorityQueue<Dot>(new Dot.DotValComp());

		for (Dot[] dotRow : dots) {
			for (Dot dot : dotRow) {
				if (!((dot.game_x == 0 || dot.game_x == BOARD_DIM - 1)
						&& (dot.game_y == 0 || dot.game_y == BOARD_DIM - 1)))
					AI.openDots.add(dot);
			}
		}

		while (true) {
			if (changePlayers) {
				choosePlayers();
				changePlayers = !changePlayers;
			}
			long startTime = System.nanoTime();
			Turn(p1);
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			if (displayTurnTimes)
				System.out.println("P1 took " + (double) (duration / 10000000) / 100 + " seconds");

			Object opts[] = { "Let's Do It", "Change Players First", "Nah, I'm Good" };
			if (AI.shortestDistance(true, 0) == 0) {
				GUI.winner = GUI.redWinText;
				int replay;
				try {
					replay = JOptionPane.showOptionDialog(GUI.frame, "Red Wins!\n\nReplay?", "Game Over",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
							new ImageIcon(ImageIO.read(GUI.class.getResource("/p1circ1.png"))), opts, opts[0]);
					if (replay == 0) {
						promptUser = false;
						main(null);
					} else if (replay == 1) {
						promptUser = true;
						main(null);
					}
					System.out.println("Red Wins");
				} catch (HeadlessException | IOException e) {
				}
				break;
			}

			if (changePlayers) {
				choosePlayers();
				changePlayers = !changePlayers;
			}
			startTime = System.nanoTime();
			Turn(p2);
			endTime = System.nanoTime();
			duration = (endTime - startTime);
			if (displayTurnTimes)
				System.out.println("P2 took " + (double) (duration / 10000000) / 100 + " seconds");
			if (AI.shortestDistance(false, 0) == 0) {
				GUI.winner = GUI.blackWinText;
				int replay;
				try {
					replay = JOptionPane.showOptionDialog(GUI.frame, "Black Wins!\n\nReplay?", "Game Over",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
							new ImageIcon(ImageIO.read(GUI.class.getResource("/p2circ1.png"))), opts, opts[0]);

					if (replay == 0) {
						promptUser = false;
						main(null);
					} else if (replay == 1) {
						promptUser = true;
						main(null);
					}
					System.out.println("Black Wins");
				} catch (HeadlessException | IOException e) {
				}
				break;
			}
		}
	}
}
