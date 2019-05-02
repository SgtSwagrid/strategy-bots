package bots;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import games.HyperMNK;
import games.HyperMNK.HyperMNKPlayer;

/**
 * 
 * @author Adrian Shedley
 *
 */

public class C4_MCTS implements HyperMNKPlayer {
	
	private GameState gs;
	private GraphNode head = null;
	public static final int DEPTH = 7;
	
	public int turnNo = 0;
	public int iterations = 0;
	
	private class Vec2 {
		public int x, y;
		public Vec2(int x, int y) { this.x = x; this.y = y; }
	}
	
	public void takeTurn(HyperMNK game, int playerId) {
	
		// Begin minimax, load the gamestate
		gs = new GameState(game, playerId, playerId == 1 ? 2 : 1);
				
		Vec2 action = new Vec2(0, 0);
		
		if (isFirstTurn(gs))
		{
			action.x = game.getDimensions()[0] / 2;
		}
		else
		{
			// OLD MCTS action = mcts(gs);
			TreeNode tn = new TreeNode(gs);
	        
			long startTime = System.currentTimeMillis();
			
			// do iterations
			while (System.currentTimeMillis() - startTime < 2500)
			{
				tn.selectAction();
			}
	     
			action = getActions(gs).get(tn.bestChild(tn));
			
		}
		
		//game.loadState(gs.getGameState());
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		turnNo++;
		game.placePiece(action.x, action.y);
	}
	
	private Vec2 mcts(GameState state)
	{
		// set the head of the tree
		head = find(state, head, 3);
		
		if (head == null)
		{
			head = new GraphNode(state);
			populateChildren(head);
		}
		
		GraphNode current = head;
		
		long startTime = System.currentTimeMillis();
		int ii = 0;
		
		// do iterations
		while (System.currentTimeMillis() - startTime < 2500)
		{
			// check for children, if no chilrden find some
			if (current.getChildren().size() == 0)
			{
				//System.out.println("Visiting node with " + current.getVisits() + " visits and " + current.getScore());
				// check fro no visits				
				if (current.getVisits() == 0)
				{
					//System.out.println("Head has " + getActions(head.getState()).size());
					rollout(current);
					
					// reset
					current = head;
				}
				else
				{
					//System.out.println("Populate children " + ii);
					populateChildren(current);
					
					if (current.getChildren().size() != 0)
					{
						current = current.getChildren().get(0);
					}
					
					//if (isTerminal(current.getState())) System.out.println(" Score before: " + head.getScore() + "/" + head.getVisits());
					
					rollout(current);
					
					//if (isTerminal(current.getState())) System.out.println(" Score after: " + head.getScore() + "/" + head.getVisits());
					
					current = head;
				}
			}
			else
			{
				//current = maxNodeChild(current, ii);
				current = maxNodeChild(current, ii);
			}
			
			ii++;
		}
		
		System.out.println("\n Head has " + head.getScore() + "/" + head.getVisits() + " with max Id " + maxNodeIdx(head, 1));
		
		return getActions(head.getState()).get(maxNodeIdx(head, 1));
		
	}
	
	public GraphNode find(GameState s, GraphNode head, int maxDepth) {
		// Try to find the position in the tree (until a certain depth)
		if (head == null)
		{
			return null;
		}
		else
		{
			maxDepth += head.getDepth();
		}
		
		GraphNode curr = head;
		
		if (curr.getDepth() == maxDepth)
			return null; // could not find position
		for (GraphNode chil : curr.getChildren()) {
			if (chil != null) {
				if (stateEqual(chil.getState().getGameState(), s.getGameState()))
					return chil;
				GraphNode nod = find(s, chil, maxDepth);
				if (nod != null)
					return nod;
			}
		}
		return null;
	}
	
	private boolean stateEqual(int[][] s1, int[][] s2)
	{
		boolean equal = true;
		
		for (int ii = 0 ; ii < s1.length; ii++)
		{
			for (int jj = 0 ; jj < s1[0].length; jj++)
			{
				equal = (equal && s1[ii][jj] == s2[ii][jj]);
			}
		}
		
		return equal;
	}
	
	private int maxNodeIdx(GraphNode gn, int iteration)
	{
		int num = gn.getChildren().size();
		float[] UCB1 = new float[num];
		
		for (int ii = 0 ; ii < num ; ii++)
		{
			UCB1[ii] = UCB1(gn.getChildren().get(ii), iteration);
		}
		
		float maxUCB1 = 0;
		int maxIdx = 0;
		
		for (int ii = 0 ; ii < num ; ii++)
		{
			if (UCB1[ii] > maxUCB1)
			{
				maxIdx = ii;
				maxUCB1 = UCB1[ii];
			}
		}
		
		return maxIdx;
	}
	
	private GraphNode maxNodeChild(GraphNode gn, int iteration)
	{
		return gn.getChildren().get(maxNodeIdx(gn, iteration));
	}

	private float UCB1(GraphNode gn, int iteration)
	{
		float multiplier = 1;//gn.getDepth() % 2 == 0 ? 1.0f : -1.0f ;
		//System.out.println("Dep[th " + multiplier);
		return (float)(multiplier * gn.getScore()/(gn.getVisits()+0.00001) + 2.5 * Math.sqrt((Math.log(gn.getParent().getVisits()))/(gn.getVisits()+0.00001)));
	}
	
	private void populateChildren(GraphNode gn)
	{
		GameState state = gn.getState();
		ArrayList<Vec2> actions = getActions(state);
		
		//System.out.println("list size is " + actions.size());
		
		for (Vec2 act : actions)
		{
			GraphNode child = new GraphNode(getResult(state, act), gn);
			gn.addChild(child);
		}
	}
	
	private void rollout(GraphNode gn)
	{
		GameState currentState = new GameState(gn.getState());
		
		while (!isTerminal(currentState))
		{
			Random rand = new Random();
			ArrayList<Vec2> actions = getActions(currentState);
			currentState = getResult(currentState, actions.get(rand.nextInt(actions.size())));
		}
		
		int winner = checkWin(currentState);
		int score = 0;
		
		if (winner == gn.getState().getMe())
		{
			score = 1;
		}
		else if (winner == gn.getState().getOp())
		{
			score = 0;
		}
		
		// backpropogate 
		do
		{
			gn.addScore(score);
			gn.addVisit();
			
			//score = -score; // NEGAMAX
			
			gn = gn.getParent();
			
		} while  (gn != null);
	}

	private ArrayList<Vec2> getActions(GameState state)
	{
		int[][] board = state.getGameState();
		ArrayList<Vec2> actions = new ArrayList<Vec2>();
		
		for (int ii = 0 ; ii < state.getWidth(); ii++)
		{
			if (board[ii][state.getHeight() - 1] == 0)
			{
				int placeHeight = 0;
				
				for(int y = 0; y < state.getHeight(); y++) 
				{
					if(board[ii][y] == 0) 
					{
						placeHeight = y;
						break;
					}
				}

				actions.add(new Vec2(ii, placeHeight));
			}
		}
		
		/*if (actions.size() == 1)
		{
			for (int yy = state.getHeight() - 1; yy >= 0; yy--)
			{
				for (int xx = 0 ; xx < state.getWidth(); xx++)
				{
				
					System.out.print(state.getGameState()[xx][yy]);
				}
				System.out.println();
			}
		}*/
		return actions;
	}
	
	private GameState getResult(GameState state, Vec2 action)
	{
		GameState finalState = new GameState(state);
		
		finalState.setGameState(action.x, action.y, finalState.getPlayer());
		finalState.swapPlayer();
		
		return finalState;
	}
	
	private boolean isTerminal(GameState state)
	{
		boolean terminal = false;
		int winner = checkWin(state);
		
		if (winner == state.getMe())
		{
			terminal = true;
		}
		else if (winner == state.getOp())
		{
			terminal = true;
		}
		else if (getActions(state).size() == 0)
		{
			terminal = true;
		}
		
		return terminal;
	}
	
	private int checkWin(GameState state)
	{
		int width = state.getWidth(), height = state.getHeight(), target = state.getGame().getTarget();
		List<List<int[]>> streaks = findStreaks(state, target);
		
		for(List<int[]> streak : streaks) {
			
			int player = state.getGameState()[streak.get(0)[0]][streak.get(0)[1]];
			boolean win = true;
			
			for(int[] pos : streak.subList(1, streak.size())) {
				
				win &= 0 <= pos[0] && pos[0] < width &&
					   0 <= pos[1] && pos[1] < height &&
					state.getGameState()[pos[0]][pos[1]] == player;
			}
			
			if(win) {
				return player;
			}
		}
		return 0;
	}
	
	private List<List<int[]>> findStreaks(GameState state, int target)
	{
		int width = state.getWidth(), height = state.getHeight();
		List<List<int[]>> streaks = new LinkedList<>();
				
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				
				if(state.getGameState()[x][y] != 0) {
					
					List<int[]> streak;
					
					streak = new ArrayList<>(target);
					for(int i = 0; i < target; i++)
						streak.add(new int[] {x + i, y});
					streaks.add(streak);
					
					streak = new ArrayList<>(target);
					for(int i = 0; i < target; i++)
						streak.add(new int[] {x + i, y + i});
					streaks.add(streak);
					
					streak = new ArrayList<>(target);
					for(int i = 0; i < target; i++)
						streak.add(new int[] {x, y + i});
					streaks.add(streak);
					
					streak = new ArrayList<>(target);
					for(int i = 0; i < target; i++)
						streak.add(new int[] {x - i, y + i});
					streaks.add(streak);
					
				} else continue;
			}
		}
		
		return streaks;
	}
	
	private boolean isFirstTurn(GameState state)
	{
		boolean isFirst = true;
		
		for (int ii = 0 ; ii < state.getWidth(); ii++)
		{
			if (state.getGameState()[ii][0] != 0)
			{
				isFirst = false;
			}
		}
		
		return isFirst;
	}

	@Override 
	public void init(HyperMNK game, int playerId)
	{
		gs = new GameState(game, playerId, playerId == 1 ? 2 : 1);
	}
	
	
	private class GraphNode
	{
		private GameState gs;
		private int visits = 0;
		private int score = 0;
		
		private GraphNode parent = null;
		private ArrayList<GraphNode> children;
		
		public GraphNode(GameState state)
		{
			gs = state;
			children = new ArrayList<GraphNode>();
		}
		
		public GraphNode(GameState state, GraphNode parent)
		{
			this(state);
			this.parent = parent;
		}
		
		public ArrayList<GraphNode> getChildren()
		{
			return children;
		}
		
		public void addChild(GraphNode gn)
		{
			children.add(gn);
		}
		
		public GraphNode getParent()
		{
			return parent;
		}
		
		public GameState getState()
		{
			return gs;
		}
		
		public int getScore()
		{
			return score;
		}
		
		public void addScore(int score)
		{
			this.score += score;
		}
		
		public int getVisits()
		{
			return visits;
		}
		
		public void addVisit()
		{
			visits++;
		}
		
		public int getDepth()
		{
			int depth = 0;
			GraphNode cur = parent;
			while (cur != null)
			{
				depth++;
				cur = cur.parent;
			}
			
			return depth;
		}
	}
	
	private class GameState {

		private int currentPlayer = 0;
		private int me, opponent;
		
		private int gameState[][];
		private HyperMNK game;
		
		private int width, height;
		
		public GameState(HyperMNK game, int me, int op)
		{
			this.game = game;
			this.me = me;
			this.opponent = op;
			
			this.currentPlayer = me;
			
			width = game.getDimensions()[0];
			height = game.getDimensions()[1];
			
			gameState = new int[width][height];
			
			populateBoard(game);
		}
		
		public GameState(GameState state)
		{
			this.game = state.getGame();
			this.me = state.me;
			this.opponent = state.opponent;
			
			width = state.getWidth();
			height = state.getHeight();
			
			this.currentPlayer = state.currentPlayer;
			this.gameState = deepCopy(state.getGameState());
		}
		
		// Load the game from the game object into some data structure of state
		private void populateBoard(HyperMNK game)
		{
			for (int xx = 0 ; xx < width; xx++)
			{
				for (int yy = 0; yy < height; yy++)
				{
					gameState[xx][yy] = (int)game.getPiece(xx, yy);
				}
			}
		}
		
		public int getPlayer()
		{
			return currentPlayer;
		}
		
		public void swapPlayer()
		{
			if (currentPlayer == me)
			{
				currentPlayer = opponent;
			}
			else
			{
				currentPlayer = me;
			}
		}
		
		public int[][] getGameState()
		{
			return gameState;
		}
		
		public void setGameState(int x, int y, int player)
		{
			gameState[x][y] = player;
		}
		
		public HyperMNK getGame()
		{
			return game;
		}
		
		public int getMe()
		{
			return me;
		}
		
		public int getOp()
		{
			return opponent;
		}
		
		private int[][] deepCopy(int[][] A)
		{
			int[][] copy = new int[A.length][A[0].length];
			
			for (int ii = 0 ; ii < A.length ; ii++)
			{
				for (int jj = 0; jj < A[0].length; jj++)
				{
					copy[ii][jj] = A[ii][jj];
				}
			}
			
			return copy;
		}
		
		public int getWidth() { return width; }
		public int getHeight() { return height; }
	}
	
	private class TreeNode {
	    final Random r = new Random();
	    static final double epsilon = 1e-6;

	    TreeNode[] children;
	    double nVisits, totValue;
	    GameState state;
	    
	    public TreeNode(GameState st)
	    {
	    	state = st;
	    }
	    
	    public void selectAction() {
	        List<TreeNode> visited = new LinkedList<TreeNode>();
	        TreeNode cur = this;
	        visited.add(this);
	        while (!cur.isLeaf() && cur.arity() != 0) {
	            cur = cur.select();
	            // System.out.println("Adding: " + cur);
	            visited.add(cur);
	        }
	        
	        cur.expand();
	        
	        TreeNode newNode = cur.select();
	        visited.add(newNode);
	        double value = rollOut(newNode);
	        
	        for (TreeNode node : visited) {
	            // would need extra logic for n-player game
	            // System.out.println(node);
	            node.updateStats(value);
	        }
	    }

	    public void expand() {
			
	    	ArrayList<Vec2> actions = getActions(state);
			children = new TreeNode[actions.size()];
			
	        for (int i=0; i < actions.size(); i++) {
				TreeNode child = new TreeNode(getResult(state, actions.get(i)));
				children[i] = child;
			}

	    }

	    private TreeNode select() {
	        TreeNode selected = null;
	        double bestValue = Double.MIN_VALUE;
	        for (TreeNode c : children) {
	            double uctValue =
	                    c.totValue / (c.nVisits + epsilon) +
	                            Math.sqrt(Math.log(nVisits+1) / (c.nVisits + epsilon)) +
	                            r.nextDouble() * epsilon;
	            // small random number to break ties randomly in unexpanded nodes
	            // System.out.println("UCT value = " + uctValue);
	            if (uctValue > bestValue) {
	                selected = c;
	                bestValue = uctValue;
	            }
	        }
	        // System.out.println("Returning: " + selected);
	        return selected;
	    }

	    public int bestChild(TreeNode head)
	    {
	    	TreeNode selected = null;
	    	int selectedId = 0, counter = 0;
	    	
	        double bestValue = Double.MIN_VALUE;
	        for (TreeNode c : head.children) {
	            
	        	double uctValue = c.totValue / (c.nVisits + epsilon) +
	                            Math.sqrt(Math.log(nVisits+1) / (c.nVisits + epsilon)) + r.nextDouble() * epsilon;
	            // small random number to break ties randomly in unexpanded nodes
	            // System.out.println("UCT value = " + uctValue);
	            if (uctValue > bestValue) {
	                selected = c;
	                selectedId = counter;
	                bestValue = uctValue;
	            }
	            counter++;
	        }
	        // System.out.println("Returning: " + selected);
	        return selectedId;
	    }
	    
	    public boolean isLeaf() {
	        return children == null;
	    }

	    public double rollOut(TreeNode tn) {
	        
			GameState currentState = new GameState(tn.getState());
			
			while (!isTerminal(currentState))
			{
				Random rand = new Random();
				ArrayList<Vec2> actions = getActions(currentState);
				currentState = getResult(currentState, actions.get(rand.nextInt(actions.size())));
			}
			
			int winner = checkWin(currentState);
			int score = 0;
			
			if (winner == tn.getState().getMe())
			{
				score = 1;
			}
			else if (winner == tn.getState().getOp())
			{
				score = -1;
			}

	        return score;
	    }

	    public void updateStats(double value) {
	        nVisits++;
	        totValue += value;
	    }

	    public int arity() {
	        return children == null ? 0 : children.length;
	    }
	    
	    public GameState getState()
	    {
	    	return state;
	    }
	}
}