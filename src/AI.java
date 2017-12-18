import java.awt.Color;
import java.awt.HeadlessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class AI {

	enum AIType {
		Random, GreedyEdge, BasicMinimax, TaunterBasicMinimax, IterativeDeepening, AlphaBeta, IterativeDeepeningAlphaBeta
	};

	public static AIType ai1 = AIType.IterativeDeepeningAlphaBeta; // Changed by player selection
	public static AIType ai2 = AIType.IterativeDeepeningAlphaBeta; // Changed by player selection

	public static final double MINDOUBLE = -1000000000;
	public static final double EDGE_NOISE = 0.05; // Default 0.05
	public static double MIDDLE_BIAS = 0.0; // Will favor middle if != 0

	public static int depthp1 = 3; // Changed by player selection
	public static int depthp2 = 3; // Changed by player selection
	public static int think_time = 100;
	public static boolean iterativeDeepeningTaunt = false;

	public static double iterativeDeepeningTimeout = 86400000; // milliseconds
	public static int curTurn = 1; // 1 for player 1, 2 for player 2

	public static boolean useRandomness = true;
	public static boolean useBestFirstHeuristic = true;

	public static HashSet<Dot> openDots;
	public static PriorityQueue<Dot> bestDots;

	public static void selectDepth(boolean player1) {
		String depthInput;
		if (player1)
			depthInput = (String) JOptionPane.showInputDialog(null, "What depth should Player 1 search?",
					"Select Depth", JOptionPane.QUESTION_MESSAGE, null, null, "4");
		else
			depthInput = (String) JOptionPane.showInputDialog(null, "What depth should Player 2 search?",
					"Select Depth", JOptionPane.QUESTION_MESSAGE, null, null, "4");

		try {
			if (Integer.parseInt(depthInput) < 1)
				throw new Throwable();
			if (player1) {
				depthp1 = Integer.valueOf(depthInput);
			} else {
				depthp2 = Integer.valueOf(depthInput);
			}
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(null, "Depth must be a positive integer.\nSet to 4 by default.",
					"Invalid Input", JOptionPane.ERROR_MESSAGE);
			if (player1) {
				depthp1 = 4;
			} else {
				depthp2 = 4;
			}
		}
	}

	public static void setAI1(String selection) {
		switch (selection) {
		case "Random":
			ai1 = AIType.Random;
			break;
		case "Greedy":
			ai1 = AIType.GreedyEdge;
			break;
		case "Minimax":
			ai1 = AIType.BasicMinimax;
			selectDepth(true);
			break;
		case "Iterative Deepening Minimax":
			ai1 = AIType.IterativeDeepening;
			selectDepth(true);
			break;
		case "Alpha Beta":
			ai1 = AIType.AlphaBeta;
			selectDepth(true);
			break;
		case "Iterative Deepening Alpha Beta":
			ai1 = AIType.IterativeDeepeningAlphaBeta;
			selectDepth(true);
			break;
		}
	}

	public static void setAI2(String selection) {
		switch (selection) {
		case "Random":
			ai2 = AIType.Random;
			break;
		case "Greedy":
			ai2 = AIType.GreedyEdge;
			break;
		case "Minimax":
			ai2 = AIType.BasicMinimax;
			selectDepth(false);
			break;
		case "Iterative Deepening Minimax":
			ai2 = AIType.IterativeDeepening;
			selectDepth(false);
			break;
		case "Alpha Beta":
			ai2 = AIType.AlphaBeta;
			selectDepth(false);
			break;
		case "Iterative Deepening Alpha Beta":
			ai2 = AIType.IterativeDeepeningAlphaBeta;
			selectDepth(false);
			break;
		}
	}

	public static Dot AIMakeMove(AIType ai, int depth, int playerNum) {
		curTurn = playerNum;
		try {
			Thread.sleep(think_time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Dot choiceDot = null;

		switch (ai) {
		case Random:
			choiceDot = randAI();
			break;
		case GreedyEdge:
			choiceDot = greedyAI();
			break;
		case BasicMinimax:
			DoubleDot dd = minimaxAI(depth, true, false, -1);
			choiceDot = dd.dot;
			break;
		case TaunterBasicMinimax:
			dd = minimaxAI(depth, true, true, -1);
			choiceDot = dd.dot;
			break;
		case IterativeDeepening:
			choiceDot = iterativeDeepeningMinimaxAI(playerNum);
			break;
		case AlphaBeta:
			DoubleDot d = alphaBetaAI(depth, true, MINDOUBLE, -MINDOUBLE, false, -1, null);
			choiceDot = d.dot;
			break;
		case IterativeDeepeningAlphaBeta:
			choiceDot = iterativeDeepeningAlphaBetaAI(playerNum);

		}
		// If one player can't find a move IDK what to do, usually it means a tie, so
		// good enough?
		if (choiceDot == null) {
			Object opts[] = { "Let's Do It", "Change Players First", "Nah, I'm Good" };
			// GUI.winner = GUI.noWinText;
			int replay;
			try {
				replay = JOptionPane.showOptionDialog(GUI.frame, "It's a Tie!\n\nReplay?", "Game Over",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						new ImageIcon(ImageIO.read(GUI.class.getResource("/emptycirc1.png"))), opts, opts[0]);

				if (replay == 0) {
					GameController.promptUser = false;
					GameController.main(null);
				} else if (replay == 1) {
					GameController.promptUser = true;
					GameController.main(null);
				}
			} catch (HeadlessException | IOException e) {
			}
			System.exit(0);
		}
		choiceDot.isTemp = false;
		choiceDot.color = GameController.p1Turn ? GUI.P1_COLOR : GUI.P2_COLOR;
		GameController.updateConnections(choiceDot, false);
		// previousDot = choiceDot;
		GameController.gui.repaint();

		openDots.remove(choiceDot);
		return choiceDot;
	}

	private static Dot iterativeDeepeningMinimaxAI(int player) {
		int currentDepth = 1;
		Dot choiceDot = null;
		long timeout = System.nanoTime() + (long) ((double) 1000000000 * iterativeDeepeningTimeout);
		while ((currentDepth < depthp1 + 1 && player == 1) || (currentDepth < depthp2 + 1 && player == 2)) {
			// There's no hope of getting the next depth, since in theory assuming timing is
			// similar if we've spent over 1/BOARD_DIM^2 of our time, there's no hope.
			if (((long) ((double) 1000000000 * iterativeDeepeningTimeout) + System.nanoTime()
					- timeout) > (long) ((double) 1000000000 * iterativeDeepeningTimeout)
							/ (GameController.BOARD_DIM * GameController.BOARD_DIM / 3))
				return choiceDot;

			DoubleDot dd = minimaxAI(currentDepth, true, iterativeDeepeningTaunt, timeout);
			if (dd == null)
				return choiceDot;

			System.out.println("Board value using iterative deepening at depth " + currentDepth + ": " + dd.number
					+ " calculated in "
					+ (((long) ((double) 1000000000 * iterativeDeepeningTimeout) - timeout + (System.nanoTime()))
							/ 1000000)
					+ " milliseconds");

			if ((dd.number <= MINDOUBLE / 2 || dd.number >= -MINDOUBLE / 2) && choiceDot != null)
				return choiceDot;

			choiceDot = dd.dot;
			currentDepth++;
		}
		return choiceDot;
	}

	private static Dot iterativeDeepeningAlphaBetaAI(int player) {
		int currentDepth = 1;
		Dot choiceDot = null;
		long timeout = System.nanoTime() + (long) ((double) 1000000000 * iterativeDeepeningTimeout);
		while ((currentDepth < depthp1 + 1 && player == 1) || (currentDepth < depthp2 + 1 && player == 2)) {
			// There's no hope of getting the next depth, since in theory assuming timing is
			// similar if we've spent over 1/(BOARD_DIM^2/2) of our time, there's no hope.
			if (((long) ((double) 1000000000 * iterativeDeepeningTimeout) + System.nanoTime()
					- timeout) > (long) ((double) 1000000000 * iterativeDeepeningTimeout)
							/ (GameController.BOARD_DIM * GameController.BOARD_DIM / 3))
				return choiceDot;

			DoubleDot dd = alphaBetaAI(currentDepth, true, MINDOUBLE, -MINDOUBLE, iterativeDeepeningTaunt, timeout,
					useBestFirstHeuristic ? choiceDot : null);
			if (dd == null)
				return choiceDot;

			choiceDot = dd.dot;
			// If we know it's a loss or win, just make the best move we can.
			if ((dd.number <= MINDOUBLE / 2 || dd.number >= -MINDOUBLE / 2) && choiceDot != null)
				return choiceDot;

			System.out.println("Board value using iterative deepening at depth " + currentDepth + ": " + dd.number
					+ " calculated in "
					+ (((long) ((double) 1000000000 * iterativeDeepeningTimeout) - timeout + (System.nanoTime()))
							/ 1000000)
					+ " milliseconds");
			currentDepth++;
		}
		return choiceDot;
	}

	public static boolean taskComplete = false;

	// Returns null if there is a timeout.
	public static DoubleDot alphaBetaAI(int depth, boolean ismax, double alpha, double beta, boolean shortCircuitOnWin,
			long timeout, Dot bestguess) {
		// Timeout == -1 means no timeout.
		if (timeout != -1 && System.nanoTime() > timeout)
			return null;

		if (depth == 0)
			return new DoubleDot(computeBoardVal(EDGE_NOISE)
					+ (useRandomness ? (ThreadLocalRandom.current().nextDouble() - 0.5) : 0), null);

		double curVal = computeBoardVal(0);
		if (curVal == (ismax ? -1 : 1) * MINDOUBLE)
			return new DoubleDot(curVal, null);

		Dot bestDot = null;
		double minmaxBoardVal = ismax ? MINDOUBLE : -MINDOUBLE;

		LinkedList<Dot> dotList = new LinkedList<Dot>();
		if (bestguess != null) {
			dotList.add(bestguess);
		}

		for (Dot[] dotArray : GameController.dots) {
			for (Dot dot : dotArray) {
				if (dot != bestguess)
					dotList.add(dot);
			}
		}

		for (Dot dot : dotList) {
			if (dot.color != GUI.NEUTRAL_COLOR
					|| (((GameController.p1Turn && ismax) || (!GameController.p1Turn && !ismax))
							&& (dot.game_x == 0 || dot.game_x == GameController.BOARD_DIM - 1))
					|| (((!GameController.p1Turn && ismax) || (GameController.p1Turn && !ismax))
							&& (dot.game_y == 0 || dot.game_y == GameController.BOARD_DIM - 1)))
				continue;

			// update board
			dot.isTemp = true;
			Color olddotcolor = dot.color;
			dot.color = (GameController.p1Turn && ismax) || (!GameController.p1Turn && !ismax) ? GUI.P1_COLOR
					: GUI.P2_COLOR;
			HashMap<Edge, Edge> oldEdges = GameController.updateConnections(dot, true);

			DoubleDot newdd = alphaBetaAI(depth - 1, !ismax, alpha, beta, shortCircuitOnWin, timeout, bestguess);
			double newBoardVal = 0;
			if (newdd != null) {
				// Means we haven't timed out.
				newBoardVal = newdd.number;

				// Print out top level game values
				if (((depth == AI.depthp2 && curTurn == 2) || (depth == AI.depthp1 && curTurn == 1))
						&& GameController.printBoardValues)
					System.out.println(
							"Board value of playing at (" + dot.game_x + "," + dot.game_y + "): " + newBoardVal);

				if ((minmaxBoardVal <= newBoardVal && ismax) || (minmaxBoardVal >= newBoardVal && !ismax)) {
					minmaxBoardVal = newBoardVal;
					bestDot = dot;
				}
			}

			if (GameController.showThinking) {
				GameController.gui.repaint();
				try {
					Thread.sleep(GameController.SimulateMoveDelay);
				} catch (InterruptedException e1) {
				}
			}

			// restore board
			dot.color = olddotcolor;
			for (Edge e : oldEdges.keySet()) {
				Edge oldEdge = oldEdges.get(e);
				e.canLink = oldEdge.canLink;
				e.linked = oldEdge.linked;
				e.weight = oldEdge.weight;
			}

			if (newdd == null)
				// propagate timeout
				return null;

			if (ismax) {
				alpha = Math.max(newBoardVal, alpha);
			} else {
				beta = Math.min(newBoardVal, beta);
			}

			if (beta <= alpha) {
				return new DoubleDot(newBoardVal, dot);
			}

			// System.out.println("Value: "+ newBoardVal+",
			// "+shortCircuitOnWin+((newBoardVal > -0.99*MINDOUBLE && ismax) || (newBoardVal
			// < 0.99*MINDOUBLE && !ismax)) );
			if ((shortCircuitOnWin)
					&& ((newBoardVal > -0.99 * MINDOUBLE && ismax) || (newBoardVal < 0.99 * MINDOUBLE && !ismax))) {
				// if(ismax)System.out.println(newBoardVal + ": win at (" + dot.game_x + "," +
				// dot.game_y + ")");
				return new DoubleDot(newBoardVal, dot);

			}
		}
		return new DoubleDot(minmaxBoardVal, bestDot);
	}

	// Returns null if there is a timeout.
	public static DoubleDot minimaxAI(int depth, boolean ismax, boolean shortCircuitOnWin, long timeout) {
		// Timeout == -1 means no timeout.
		if (timeout != -1 && System.nanoTime() > timeout)
			return null;

		if (depth == 0)
			return new DoubleDot(computeBoardVal(EDGE_NOISE)
					+ (useRandomness ? (ThreadLocalRandom.current().nextDouble() - 0.5) : 0), null);

		double curVal = computeBoardVal(0);
		if (curVal == (ismax ? -1 : 1) * MINDOUBLE)
			return new DoubleDot(curVal, null);

		Dot bestDot = null;
		double minmaxBoardVal = ismax ? MINDOUBLE : -MINDOUBLE;
		for (Dot[] dotArray : GameController.dots) {
			for (Dot dot : dotArray) {
				if (dot.color != GUI.NEUTRAL_COLOR
						|| (((GameController.p1Turn && ismax) || (!GameController.p1Turn && !ismax))
								&& (dot.game_x == 0 || dot.game_x == GameController.BOARD_DIM - 1))
						|| (((!GameController.p1Turn && ismax) || (GameController.p1Turn && !ismax))
								&& (dot.game_y == 0 || dot.game_y == GameController.BOARD_DIM - 1)))
					continue;

				// update board
				dot.isTemp = true;
				Color olddotcolor = dot.color;
				dot.color = (GameController.p1Turn && ismax) || (!GameController.p1Turn && !ismax) ? GUI.P1_COLOR
						: GUI.P2_COLOR;
				HashMap<Edge, Edge> oldEdges = GameController.updateConnections(dot, true);

				DoubleDot newdd = minimaxAI(depth - 1, !ismax, shortCircuitOnWin, timeout);
				double newBoardVal = 0;
				if (newdd != null) {
					// Means we haven't timed out.
					newBoardVal = newdd.number;

					// Print out top level game values
					if (((depth == AI.depthp2 && curTurn == 2) || (depth == AI.depthp1 && curTurn == 1))
							&& GameController.printBoardValues)
						System.out.println(
								"Board value of playing at (" + dot.game_x + "," + dot.game_y + "): " + newBoardVal);

					if ((minmaxBoardVal <= newBoardVal && ismax) || (minmaxBoardVal >= newBoardVal && !ismax)) {
						minmaxBoardVal = newBoardVal;
						bestDot = dot;
					}
				}

				if (GameController.showThinking) {
					GameController.gui.repaint();
					try {
						Thread.sleep(GameController.SimulateMoveDelay);
					} catch (InterruptedException e1) {
					}
				}

				// restore board
				dot.color = olddotcolor;
				for (Edge e : oldEdges.keySet()) {
					Edge oldEdge = oldEdges.get(e);
					e.canLink = oldEdge.canLink;
					e.linked = oldEdge.linked;
					e.weight = oldEdge.weight;
				}

				if (newdd == null)
					// propogate timeout
					return null;

				// System.out.println("Value: "+ newBoardVal+",
				// "+shortCircuitOnWin+((newBoardVal > -0.99*MINDOUBLE && ismax) || (newBoardVal
				// < 0.99*MINDOUBLE && !ismax)) );
				if ((shortCircuitOnWin)
						&& ((newBoardVal > -0.99 * MINDOUBLE && ismax) || (newBoardVal < 0.99 * MINDOUBLE && !ismax))) {
					// if(ismax)System.out.println(newBoardVal + ": win at (" + dot.game_x + "," +
					// dot.game_y + ")");
					return new DoubleDot(newBoardVal, dot);

				}
			}
		}
		return new DoubleDot(minmaxBoardVal, bestDot);
	}

	public static Dot randAI() {
		Random rand = new Random();
		HashSet<Dot> openDotsCopy = new HashSet<Dot>(openDots);
		Dot dot = null;
		boolean wrong_edge = false;

		while (!openDotsCopy.isEmpty() && (dot == null || dot.color != GUI.NEUTRAL_COLOR || wrong_edge)) {
			openDotsCopy.remove(dot); // That one didn't work for me, don't consider it again
			if (openDotsCopy.size() == 0)
				break;
			Dot[] dots = openDotsCopy.toArray(new Dot[openDotsCopy.size()]);
			dot = dots[rand.nextInt(dots.length)];
			wrong_edge = (GameController.p1Turn && (dot.game_x == 0 || dot.game_x == GameController.BOARD_DIM - 1))
					|| (!GameController.p1Turn && (dot.game_y == 0 || dot.game_y == GameController.BOARD_DIM - 1));
		}
		if (dot == null || wrong_edge || dot.color != GUI.NEUTRAL_COLOR)
			dot = null;

		return dot;
	}

	public static Dot greedyAI() {
		Dot bestDot = null;
		double maxBoardVal = MINDOUBLE;
		for (Dot[] dotArray : GameController.dots) {
			for (Dot dot : dotArray) {
				if (dot.color != GUI.NEUTRAL_COLOR
						|| (GameController.p1Turn && (dot.game_x == 0 || dot.game_x == GameController.BOARD_DIM - 1))
						|| (!GameController.p1Turn && (dot.game_y == 0 || dot.game_y == GameController.BOARD_DIM - 1)))
					continue;

				// update board
				Color olddotcolor = dot.color;
				dot.color = GameController.p1Turn ? GUI.P1_COLOR : GUI.P2_COLOR;
				HashMap<Edge, Edge> oldEdges = GameController.updateConnections(dot, true);

				double newBoardVal = computeBoardVal(EDGE_NOISE)
						+ (useRandomness ? (ThreadLocalRandom.current().nextDouble() - 0.5) : 0);
				if (maxBoardVal <= newBoardVal) {
					// Tie goes to the middle
					if (maxBoardVal == newBoardVal && bestDot != null && MIDDLE_BIAS != 0) {
						System.out.println(dot.game_x - (GameController.BOARD_DIM - 1) / 2 + "x y"
								+ (dot.game_y - (GameController.BOARD_DIM - 1) / 2));
						System.out.println(((dot.game_x - (GameController.BOARD_DIM - 1) / 2)) + "x y"
								+ (dot.game_y - (GameController.BOARD_DIM - 1) / 2));
						double newMid = Math.abs(dot.game_x - (GameController.BOARD_DIM - 1) / 2)
								+ Math.abs(dot.game_y - (GameController.BOARD_DIM - 1) / 2);
						double oldMid = Math.abs((bestDot.game_x - (GameController.BOARD_DIM - 1) / 2))
								+ (Math.abs(bestDot.game_y - (GameController.BOARD_DIM - 1) / 2));
						System.out.println("Old dist from center at " + bestDot.game_x + "," + bestDot.game_y + ": "
								+ oldMid + "New dist from center at " + dot.game_x + "," + dot.game_y + ": " + newMid);
						if (newMid <= oldMid) {
							maxBoardVal = newBoardVal;
							bestDot = dot;
						}
					} else {
						maxBoardVal = newBoardVal;
						bestDot = dot;
					}
				}

				// restore board
				dot.color = olddotcolor;
				for (Edge e : oldEdges.keySet()) {
					Edge oldEdge = oldEdges.get(e);
					e.canLink = oldEdge.canLink;
					e.linked = oldEdge.linked;
					e.weight = oldEdge.weight;
				}

			}
		}
		System.out.println("Path length difference: " + Math.round(maxBoardVal));
		return bestDot;
	}

	public static double shortestDistance(boolean isp1, double edge_noise) {
		Dot start = isp1 ? GameController.coordToDot.get(GameController.coordToAbsoluteIndex(1, 0))
				: GameController.coordToDot.get(GameController.coordToAbsoluteIndex(0, 1));
		PriorityQueue<Dot> frontier = new PriorityQueue<Dot>(1000, new Dot.DotComparator());
		ArrayList<Dot> visited = new ArrayList<Dot>();
		HashMap<Dot, Dot> backp = new HashMap<Dot, Dot>();
		frontier.add(start);
		start.tempDistance = 0.0;
		backp.put(start, null);
		while (frontier.size() != 0) {
			Dot current = frontier.poll();
			visited.add(current);
			// We made it to the other side
			if ((isp1 && current.game_y == GameController.BOARD_DIM - 1)
					|| (!isp1 && current.game_x == GameController.BOARD_DIM - 1)) {
				Dot p = current;
				while (p != null) {
					// System.out.print("(" + p.game_x + "," + p.game_y + "),");
					p = backp.get(p);
				}
				// System.out.print("\n Gives distance of " + current.tempDistance);
				return current.tempDistance;
			}

			for (Edge e : current.edges) {
				boolean isP1Edge = (e.dot1.color == GUI.P1_COLOR && e.dot2.color == GUI.P1_COLOR);
				boolean isP2Edge = (e.dot1.color == GUI.P2_COLOR && e.dot2.color == GUI.P2_COLOR);
				boolean isOwnEdge = ((isp1 && isP1Edge) || (!isp1 && isP2Edge)) && e.linked;
				if (!(e.canLink || isOwnEdge)
						|| ((e.dot1.color == GUI.P1_COLOR || e.dot2.color == GUI.P1_COLOR) && !isp1)
						|| (e.dot1.color == GUI.P2_COLOR || e.dot2.color == GUI.P2_COLOR) && isp1)
					continue;
				Dot otherDot = e.dot1 == current ? e.dot2 : e.dot1;

				if ((isp1 && (otherDot.game_x == 0 || otherDot.game_x == GameController.BOARD_DIM - 1))
						|| (!isp1 && (otherDot.game_y == 0 || otherDot.game_y == GameController.BOARD_DIM - 1)))
					continue;

				// double middle_bias =
				// MIDDLE_BIAS*(Math.abs(otherDot.game_x-(GameController.BOARD_DIM-1)/2)+Math.abs(otherDot.game_y-(GameController.BOARD_DIM-1)/2));
				// System.out.println(middle_bias);
				boolean onOwnBoundary = current.game_y == 0
						&& (isp1 && (otherDot.game_y == 0 || otherDot.game_y == GameController.BOARD_DIM - 1))
						|| current.game_x == 0
								&& (!isp1 && (otherDot.game_x == 0 || otherDot.game_x == GameController.BOARD_DIM - 1));
				double weight = (e.linked || onOwnBoundary) ? 0.0 : e.weight;
				double otherDotDistance = current.tempDistance + weight;
				if (frontier.contains(otherDot)) {
					if (otherDotDistance < otherDot.tempDistance) {
						otherDot.tempDistance = otherDotDistance;
						frontier.remove(otherDot);
						frontier.add(otherDot);
						backp.put(otherDot, current);
					}
				} else if (!visited.contains(otherDot)) {
					otherDot.tempDistance = otherDotDistance;
					frontier.add(otherDot);
					backp.put(otherDot, current);
				}

			}
		}
		// Can't find a path :(
		// String player = isp1 ? "Player 1" : "Player 2";
		// System.out.println(player +" cannot win.");
		return -MINDOUBLE + (useRandomness ? ((ThreadLocalRandom.current().nextDouble() - 0.5)) : 0);
	}

	public static double computeBoardVal(double edge_noise) {
		return shortestDistance(!GameController.p1Turn, edge_noise)
				- shortestDistance(GameController.p1Turn, edge_noise);
	}
}
