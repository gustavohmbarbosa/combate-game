package game.players;

import java.util.Queue;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import game.Board;
import game.feedbacks.AttackFeedback;
import game.feedbacks.DefeatFeedback;
import game.feedbacks.EqualStrengthFeedback;
import game.feedbacks.Feedback;
import game.feedbacks.LandmineFeedback;
import game.feedbacks.MoveFeedback;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;

public class YuriAlvesPlayer implements Player
{
	private String playerName = "Yuri Alves";
	private Map<String, Integer> strengthTable = new HashMap<String, Integer>();
	private Map<String, Integer> countDistribution = new HashMap<String, Integer>();
	private List<Map<String, Integer>> lastTargetPositions = new LinkedList<Map<String, Integer>>();
	private String[][] enemyPiecePositions = new String[10][10];

	@Override
	public String getPlayerName()
	{
		return this.playerName;
	}

	@Override
	public Piece[][] setup(Board board)
	{
		Piece[][] setup = new Piece[4][10];
		setup[0][0] = PieceFactory.createPiece("S", this.playerName, board);
		setup[0][1] = PieceFactory.createPiece("S", this.playerName, board);
		setup[0][2] = PieceFactory.createPiece("MJ", this.playerName, board);
		setup[0][3] = PieceFactory.createPiece("CP", this.playerName, board);
		setup[0][4] = PieceFactory.createPiece("S", this.playerName, board);
		setup[0][5] = PieceFactory.createPiece("S", this.playerName, board);
		setup[0][6] = PieceFactory.createPiece("ST", this.playerName, board);
		setup[0][7] = PieceFactory.createPiece("T", this.playerName, board);
		setup[0][8] = PieceFactory.createPiece("S", this.playerName, board);
		setup[0][9] = PieceFactory.createPiece("S", this.playerName, board);
		setup[1][0] = PieceFactory.createPiece("M", this.playerName, board);
		setup[1][1] = PieceFactory.createPiece("M", this.playerName, board);
		setup[1][2] = PieceFactory.createPiece("SG", this.playerName, board);
		setup[1][3] = PieceFactory.createPiece("MJ", this.playerName, board);
		setup[1][4] = PieceFactory.createPiece("S", this.playerName, board);
		setup[1][5] = PieceFactory.createPiece("S", this.playerName, board);
		setup[1][6] = PieceFactory.createPiece("CR", this.playerName, board);
		setup[1][7] = PieceFactory.createPiece("CP", this.playerName, board);
		setup[1][8] = PieceFactory.createPiece("T", this.playerName, board);
		setup[1][9] = PieceFactory.createPiece("CP", this.playerName, board);
		setup[2][0] = PieceFactory.createPiece("M", this.playerName, board);
		setup[2][1] = PieceFactory.createPiece("SG", this.playerName, board);
		setup[2][2] = PieceFactory.createPiece("M", this.playerName, board);
		setup[2][3] = PieceFactory.createPiece("G", this.playerName, board);
		setup[2][4] = PieceFactory.createPiece("C", this.playerName, board);
		setup[2][5] = PieceFactory.createPiece("T", this.playerName, board);
		setup[2][6] = PieceFactory.createPiece("AS", this.playerName, board);
		setup[2][7] = PieceFactory.createPiece("T", this.playerName, board);
		setup[2][8] = PieceFactory.createPiece("C", this.playerName, board);
		setup[2][9] = PieceFactory.createPiece("ST", this.playerName, board);
		setup[3][0] = PieceFactory.createPiece("PS", this.playerName, board);
		setup[3][1] = PieceFactory.createPiece("M", this.playerName, board);
		setup[3][2] = PieceFactory.createPiece("M", this.playerName, board);
		setup[3][3] = PieceFactory.createPiece("C", this.playerName, board);
		setup[3][4] = PieceFactory.createPiece("SG", this.playerName, board);
		setup[3][5] = PieceFactory.createPiece("ST", this.playerName, board);
		setup[3][6] = PieceFactory.createPiece("C", this.playerName, board);
		setup[3][7] = PieceFactory.createPiece("ST", this.playerName, board);
		setup[3][8] = PieceFactory.createPiece("SG", this.playerName, board);
		setup[3][9] = PieceFactory.createPiece("C", this.playerName, board);
		this.countDistribution.put("AS", 1);
		this.countDistribution.put("S", 8);
		this.countDistribution.put("C", 5);
		this.countDistribution.put("SG", 4);
		this.countDistribution.put("ST", 4);
		this.countDistribution.put("T", 4);
		this.countDistribution.put("CP", 3);
		this.countDistribution.put("MJ", 2);
		this.countDistribution.put("CR", 1);
		this.countDistribution.put("G", 1);
		this.countDistribution.put("PS", 1);
		this.countDistribution.put("M", 6);
		this.strengthTable.put("AS", 1);
		this.strengthTable.put("S", 2);
		this.strengthTable.put("C", 3);
		this.strengthTable.put("SG", 4);
		this.strengthTable.put("ST", 5);
		this.strengthTable.put("T", 6);
		this.strengthTable.put("CP", 7);
		this.strengthTable.put("MJ", 8);
		this.strengthTable.put("CR", 9);
		this.strengthTable.put("G", 10);
		this.strengthTable.put("PS", 0);
		this.strengthTable.put("M", 11);
		return setup;
	}

	@Override
	public PieceAction play(Board board, Feedback myLastFeedback, Feedback enemyLastFeedback)
	{
		updateCountDistribution(myLastFeedback);
		updateCountDistribution(enemyLastFeedback);
		Piece targetPiece = null;
		Double maxValue = 0.0;
		Integer targetX = 0;
		Integer targetY = 0;
		Integer oldX = 0;
		Integer oldY = 0;
		// implement inner state update
		for (int x = 0; x < 10; x++)
		{
			for (int y = 0; y < 10; y++)
			{
				Piece currentPiece = board.getPiece(x, y);
				if
				(
					currentPiece != null &&
					currentPiece.getPlayer() != null &&
					currentPiece.getPlayer().equals(this.playerName) &&
					!currentPiece.getRepresentation().equals("PS") &&
					!currentPiece.getRepresentation().equals("M") &&
					(
						currentPiece.canMove(x+1, y) ||
						currentPiece.canMove(x-1, y) ||
						currentPiece.canMove(x, y+1) ||
						currentPiece.canMove(x, y-1)
					) &&
					(
						this.lastTargetPositions.size() < 3 ||
						x != this.lastTargetPositions.get(0).get("x") ||
						y != this.lastTargetPositions.get(0).get("y") ||
						x != this.lastTargetPositions.get(1).get("x") ||
						y != this.lastTargetPositions.get(1).get("y") ||
						x != this.lastTargetPositions.get(2).get("x") ||
						y != this.lastTargetPositions.get(2).get("y")
					)
				)
				{
		
					Boolean[][] overlayBoard = new Boolean[10][10];
					for (int i = 0; i < 10; i++)
					{
						for (int j = 0; j < 10; j++)
						{
							overlayBoard[i][j] = false;
						}
					}
					Queue<List<Map<String, Integer>>> searchSpace = new LinkedList<List<Map<String, Integer>>>();
					List<Map<String, Integer>> startSearchPath = new LinkedList<Map<String, Integer>>();
					Map<String, Integer> startSearchPathPositon = new HashMap<String, Integer>();
					startSearchPathPositon.put("x", x);
					startSearchPathPositon.put("y", y);
					startSearchPathPositon.put("distance", 0);
					startSearchPath.add(startSearchPathPositon);
					searchSpace.add(startSearchPath);
					while (true)
					{
						List<Map<String, Integer>> currentSearchPath = searchSpace.poll();
						if (currentSearchPath == null)
						{
							break;
						}
						Integer currentX = currentSearchPath.getLast().get("x");
						Integer currentY = currentSearchPath.getLast().get("y");
						Integer currentDistance = currentSearchPath.getLast().get("distance");
						overlayBoard[currentX][currentY] = true;
						if (board.getPiece(currentX, currentY) == null || board.getPiece(currentX, currentY) == currentPiece)
						{
							List<Map<String, Integer>> directions = new LinkedList<Map<String, Integer>>();
							Map<String, Integer> right = new HashMap<String, Integer>();
							Map<String, Integer> left = new HashMap<String, Integer>();
							Map<String, Integer> up = new HashMap<String, Integer>();
							Map<String, Integer> down = new HashMap<String, Integer>();
							right.put("x", currentX+1);
							right.put("y", currentY);
							left.put("x", currentX-1);
							left.put("y", currentY);
							up.put("x", currentX);
							up.put("y", currentY+1);
							down.put("x", currentX);
							down.put("y", currentY-1);
							directions.add(up);
							directions.add(down);
							directions.add(left);
							directions.add(right);
							for (Map<String, Integer> direction : directions)
							{
								if
								(
									Board.isValidPosition(direction.get("x"), direction.get("y")) &&
									(
										board.getPiece(direction.get("x"), direction.get("y")) == null ||
										board.getPiece(direction.get("x"), direction.get("y")).getPlayer() == null
									) &&
									overlayBoard[direction.get("x")][direction.get("y")] == false
								)
								{
									List<Map<String, Integer>> newSearchPath = new LinkedList<Map<String, Integer>>(currentSearchPath);
									direction.put("distance", currentDistance+1);
									newSearchPath.add(direction);
									searchSpace.add(newSearchPath);
								}
							}
						}
						else if (board.getPiece(currentX, currentY).getPlayer() == null)
						{
							Integer victoryCount = 0;
							Integer countDistributionTotal = 0;
							for (Map.Entry<String, Integer> pieceCount : this.countDistribution.entrySet())
							{
								countDistributionTotal = countDistributionTotal+pieceCount.getValue();
								if
								(
									strengthTable.get(pieceCount.getKey()) <= currentPiece.getStrength() ||
									(
										currentPiece.getRepresentation().equals("AS") &&
										pieceCount.getKey().equals("G")
									) ||
									(
										currentPiece.getRepresentation().equals("CP") &&
										pieceCount.getKey().equals("M")
									)
								)
								{
									victoryCount = victoryCount+pieceCount.getValue();
								}
							}
							Boolean kill = canKillWithPiece(currentPiece, currentX, currentY);
							if (kill != null && kill)
							{
								victoryCount = countDistributionTotal;
							}
							else if (kill != null)
							{
								victoryCount = 0;
							}
							if (2*victoryCount >= countDistributionTotal || targetPiece == null || currentPiece.getRepresentation().equals("S"))
							{
								Double newMaxValue = ((double)victoryCount/(double)countDistributionTotal)*(20.0/(double)currentSearchPath.getLast().get("distance"));
								if (newMaxValue > maxValue)
								{
									maxValue = newMaxValue;
									targetPiece = currentPiece;
									targetX = currentSearchPath.get(1).get("x");
									targetY = currentSearchPath.get(1).get("y");
									oldX = x;
									oldY = y;
								}
							}
							else
							{
								Integer defeatCount = countDistributionTotal-victoryCount;
								Double newMaxValue = ((double)defeatCount/(double)countDistributionTotal)*(20.0/(double)currentSearchPath.getLast().get("distance"));
								if (newMaxValue > maxValue)
								{
									maxValue = newMaxValue;
									Integer newTargetX = currentSearchPath.get(1).get("x");
									Integer newTargetY = currentSearchPath.get(1).get("y");
									if (newTargetX > currentX)
									{
										newTargetX = currentX-1;
									}
									else if (newTargetX < currentX)
									{
										newTargetX = currentX+1;
									}
									if (newTargetY > currentY)
									{
										newTargetY = currentY-1;
									}
									else if (newTargetY < currentY)
									{
										newTargetY = currentY+1;
									}
									if (currentPiece.canMove(newTargetX, newTargetY))
									{
										targetPiece = currentPiece;
										targetX = newTargetX;
										targetY = newTargetY;
										oldX = x;
										oldY = y;
									}
								}
							}
						}
					}
				}
			}
		}
		if (this.lastTargetPositions.size() == 3)
		{
			this.lastTargetPositions.removeFirst();
		}
		for (Map<String, Integer> currentLastTargetPosition : lastTargetPositions)
		{
			if (currentLastTargetPosition.get("x").equals(oldX) && currentLastTargetPosition.get("y").equals(oldY))
			{
				currentLastTargetPosition.put("x", targetX);
				currentLastTargetPosition.put("y", targetY);
			}
		}
		Map<String, Integer> newLastTargetPosition = new HashMap<String, Integer>();
		newLastTargetPosition.put("x", targetX);
		newLastTargetPosition.put("y", targetY);
		this.lastTargetPositions.add(newLastTargetPosition);
		PieceAction result = new PieceAction(targetPiece, targetX, targetY);
		return result;
	}

	private void updateCountDistribution(Feedback feedback)
	{
		if (feedback == null)
		{
			return;
		}

		String eliminatedPieceCode = null;

		if (feedback instanceof AttackFeedback)
		{
			AttackFeedback af = (AttackFeedback)feedback;
			if (af.attacker.getPlayer().equals(this.playerName))
			{
				eliminatedPieceCode = af.defender.getRepresentation();
				enemyPiecePositions[af.toX][af.toY] = null;
			}
			else if (af.defender.getPlayer().equals(this.playerName))
			{
				storeEnemyPiecePosition(af.toX, af.toY, af.defender.getRepresentation());
			}
		}
		else if (feedback instanceof MoveFeedback)
		{
			MoveFeedback moveFeedback = (MoveFeedback)feedback;
			updateEnemyPiecePosition(moveFeedback.fromX, moveFeedback.fromY, moveFeedback.toX, moveFeedback.toY);
		}

		else if (feedback instanceof DefeatFeedback)
		{
			DefeatFeedback df = (DefeatFeedback)feedback;
			if (!df.attacker.getPlayer().equals(this.playerName))
			{
				eliminatedPieceCode = df.attacker.getRepresentation();
				enemyPiecePositions[df.toX][df.toY] = null;
			}
			else if (!df.defender.getPlayer().equals(this.playerName))
			{
				storeEnemyPiecePosition(df.toX, df.toY, df.defender.getRepresentation());
			}
		}

		else if (feedback instanceof LandmineFeedback)
		{
			LandmineFeedback lf = (LandmineFeedback)feedback;
			if (!lf.getVictim().getPlayer().equals(this.playerName))
			{
				eliminatedPieceCode = lf.getVictim().getRepresentation();
			}
		}

		else if (feedback instanceof EqualStrengthFeedback)
		{
			EqualStrengthFeedback ef = (EqualStrengthFeedback)feedback;
			if (!ef.attacker.getPlayer().equals(this.playerName))
			{
				eliminatedPieceCode = ef.attacker.getRepresentation();
			}
			else if (!ef.defender.getPlayer().equals(this.playerName))
			{
				eliminatedPieceCode = ef.defender.getRepresentation();
			}
		}

		if (eliminatedPieceCode != null && countDistribution.containsKey(eliminatedPieceCode))
		{
			int newCount = countDistribution.get(eliminatedPieceCode)-1;
			if (newCount > 0)
			{
				countDistribution.put(eliminatedPieceCode, newCount);

			}
			else
			{
				countDistribution.remove(eliminatedPieceCode);
			}
			
		}
	}

	private void storeEnemyPiecePosition(int x, int y, String pieceRepresentation) {
		this.enemyPiecePositions[x][y] = pieceRepresentation;
		System.out.println("Peça do inimigo descoberta = " + enemyPiecePositions[x][y]);
	}

	private void updateEnemyPiecePosition(int fromX, int fromY, int toX, int toY) {
		
			this.enemyPiecePositions[toX][toY] = this.enemyPiecePositions[fromX][fromY];

			this.enemyPiecePositions[fromX][fromY] = null;
	}
	

	private Boolean canKillWithPiece(Piece attackingPiece, int targetX, int targetY) {
		if (this.enemyPiecePositions[targetX][targetY] != null) {
			int enemyStrength = strengthTable.get(enemyPiecePositions[targetX][targetY]);
			return attackingPiece.getStrength() > enemyStrength;
		}
		return null; 
	}

}