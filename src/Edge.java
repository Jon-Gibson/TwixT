import java.util.LinkedList;

public class Edge {
	public Dot dot1;
	public Dot dot2;
	public boolean linked = false;
	public boolean canLink = true;
	public double weight;
	public boolean isTemp;

	public Edge(Dot dot1, Dot dot2) {
		this.dot1 = dot1; // lower y
		this.dot2 = dot2; // higher y
		this.weight = 1.0;
	}

	public Edge(Dot dot1, Dot dot2, double weight) {
		this.dot1 = dot1; // lower y
		this.dot2 = dot2; // higher y
		this.weight = weight;
	}

	public LinkedList<Edge> getIntersections() {
		LinkedList<Edge> edgeIntersections = new LinkedList<Edge>();
		int lowx = Math.min(this.dot1.game_x, this.dot2.game_x);
		int highx = Math.max(this.dot1.game_x, this.dot2.game_x);
		int lowy = Math.min(this.dot1.game_y, this.dot2.game_y);
		int highy = Math.max(this.dot1.game_y, this.dot2.game_y);

		for (int x = Math.max(0, lowx - 2); x <= Math.min(GameController.BOARD_DIM - 1, highx + 2); x++) {
			for (int y = Math.max(0, lowy - 2); y <= Math.min(GameController.BOARD_DIM - 1, highy + 2); y++) {
				Dot d = GameController.coordToDot.get(GameController.coordToAbsoluteIndex(x, y));
				for (Edge edge : d.edges) {
					// Handle case when one endpoint is shared (or same edge), as these should not
					// count as intersections.
					if (edge.dot1 == this.dot1 || edge.dot2 == this.dot1 || edge.dot1 == this.dot2
							|| edge.dot2 == this.dot2)
						continue;

					int x1 = this.dot1.game_x;
					int x2 = this.dot2.game_x;
					int x3 = edge.dot1.game_x;
					int x4 = edge.dot2.game_x;
					int y1 = this.dot1.game_y;
					int y2 = this.dot2.game_y;
					int y3 = edge.dot1.game_y;
					int y4 = edge.dot2.game_y;
					if (ccw(x1, y1, x3, y3, x4, y4) != ccw(x2, y2, x3, y3, x4, y4)
							&& ccw(x1, y1, x2, y2, x3, y3) != ccw(x1, y1, x2, y2, x4, y4)
							&& !edgeIntersections.contains(edge))
						edgeIntersections.add(edge);
				}
			}
		}

		return edgeIntersections;
	}

	private boolean ccw(int x1, int y1, int x2, int y2, int x3, int y3) {
		return (y3 - y1) * (x2 - x1) > (y2 - y1) * (x3 - x1);
	}
}