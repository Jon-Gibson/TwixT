import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;

import java.io.IOException;

import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Dot extends JLabel implements MouseListener {
	public float x;
	public float y;
	public float radius;
	public Shape circle;
	public Color color;
	public int game_x;
	public int game_y;
	public LinkedList<Edge> edges = new LinkedList<Edge>();
	public double tempDistance;
	public boolean isTemp;

	public Dot(float f, float g, float circleSize, Color col, JPanel gui, int game_x, int game_y) throws IOException {
		super(new ImageIcon());
		x = f;
		y = g;
		radius = circleSize;
		color = col;
		circle = new Ellipse2D.Float(x, y, circleSize, circleSize);
		this.game_x = game_x;
		this.game_y = game_y;
		this.addMouseListener(this);
		this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	static class DotComparator implements Comparator<Dot> {
		@Override
		public int compare(Dot d1, Dot d2) {
			if (d1.tempDistance - d2.tempDistance == 0)
				return 0;

			return d1.tempDistance < d2.tempDistance ? -1 : 1;
		}
	}

	static class DotValComp implements Comparator<Dot> {
		@Override
		public int compare(Dot d1, Dot d2) {
			if (d1.tempDistance - d2.tempDistance == 0)
				return 0;

			return d1.tempDistance < d2.tempDistance ? -1 : 1;
		}
	}

	public void mouseClicked(MouseEvent e) {
		// Delete any tempDot
		if (GameController.tempDot != null) {
			GameController.tempDot.color = GUI.NEUTRAL_COLOR;
			for (Edge edge : GameController.tempEdges.keySet()) {
				Edge oldEdge = GameController.tempEdges.get(edge);
				edge.canLink = oldEdge.canLink;
				edge.linked = oldEdge.linked;
				edge.weight = oldEdge.weight;
			}
			GameController.tempDot = null;
		}

		boolean isTurn = (GameController.p1Turn && GameController.p1 == GameController.PlayerType.Human)
				|| (!GameController.p1Turn && GameController.p2 == GameController.PlayerType.Human);
		boolean isCorner = (game_x == 0 || game_x == GUI.BOARD_DIM - 1) && (game_y == 0 || game_y == GUI.BOARD_DIM - 1);
		boolean isOpponentRow = (GameController.p1Turn && (game_x == 0 || game_x == GUI.BOARD_DIM - 1))
				|| (!GameController.p1Turn && (game_y == 0 || game_y == GUI.BOARD_DIM - 1));
		if (isTurn && color == GUI.NEUTRAL_COLOR && (e.getButton() == 1) && !isCorner && !isOpponentRow) {
			color = GameController.p1Turn ? GUI.P1_COLOR : GUI.P2_COLOR;
			this.isTemp = false;
			GameController.updateConnections(this, false);
			GameController.previousDot = this;

			GameController.gui.repaint();
			GameController.turnLock.release(1);
		}

		// Temp placement on right click
		if (isTurn && color == GUI.NEUTRAL_COLOR && (e.getButton() != 1) && !isCorner && !isOpponentRow) {
			color = GameController.p1Turn ? GUI.P1_COLOR : GUI.P2_COLOR;
			this.isTemp = true;

			GameController.tempEdges = GameController.updateConnections(this, true);
			GameController.tempDot = this;

			GameController.gui.repaint();
		}
	}

	public void mouseEntered(MouseEvent e) {
		boolean isTurn = (GameController.p1Turn && GameController.p1 == GameController.PlayerType.Human)
				|| (!GameController.p1Turn && GameController.p2 == GameController.PlayerType.Human);
		boolean isCorner = (game_x == 0 || game_x == GUI.BOARD_DIM - 1) && (game_y == 0 || game_y == GUI.BOARD_DIM - 1);
		boolean isOpponentRow = (GameController.p1Turn && (game_x == 0 || game_x == GUI.BOARD_DIM - 1))
				|| (!GameController.p1Turn && (game_y == 0 || game_y == GUI.BOARD_DIM - 1));

		// Temp placement on right click
		if (isTurn && color == GUI.NEUTRAL_COLOR && (e.getButton() != 1) && !isCorner && !isOpponentRow) {
			color = GameController.p1Turn ? GUI.P1_COLOR : GUI.P2_COLOR;
			this.isTemp = true;

			GameController.tempEdges = GameController.updateConnections(this, true);
			GameController.tempDot = this;

			GameController.gui.repaint();
		}
	}

	public void mouseExited(MouseEvent e) {
		// Delete any tempDot
		if (GameController.tempDot != null) {
			GameController.tempDot.color = GUI.NEUTRAL_COLOR;
			for (Edge edge : GameController.tempEdges.keySet()) {
				Edge oldEdge = GameController.tempEdges.get(edge);
				edge.canLink = oldEdge.canLink;
				edge.linked = oldEdge.linked;
				edge.weight = oldEdge.weight;
			}
			GameController.tempDot = null;
			GameController.gui.repaint();
		}
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

}
