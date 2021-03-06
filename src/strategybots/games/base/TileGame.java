package strategybots.games.base;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import strategybots.games.graphics.Colour;
import strategybots.games.graphics.Texture;
import strategybots.games.graphics.Tile;
import strategybots.games.graphics.Window;

/**
 * Abstract supertype for abstract board games using a basic grid board.<br>
 * Takes care of turn order progression, the game board and piece movement.
 * 
 * @author Alec Dorrington
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class TileGame extends Game {
    
    private static final long serialVersionUID = -1764178362241848211L;

    /** Colour used for selected pieces. */
    private static Colour HIGHLIGHT_COLOUR = Colour.rgb(46, 213, 115);
    
    /** The window in which the board resides. */
    private final Window window;
    
    /** The board on which the game is played - manages tile layout. */
    private final Board board;
    
    /** Grid of pieces indexed by position. */
    private final Piece[][] boardPieces;
    /** Set of pieces indexed by owner. */
    private final Set<Piece>[] playerPieces;
    
    /** The dimensions of the board. */
    private final int width, height;
    
    /** Title of the window. */
    private String title;
    
    /**
     * Constructs a new grid game of the given dimensions and players.
     * @param width the number of tiles wide the board is.
     * @param height the number of tiles high the board is.
     * @param title the title of the window in which the board is to reside.
     * @param players the players (in turn order) participating in this game.
     */
    protected TileGame(int width, int height, String title, Player... players) {
        
        //Load the players.
        super(players);
        
        //Create the game board.
        board = new Board(width, height, title);
        boardPieces = new Piece[width][height];
        window = board.getWindow();
        
        //Setup click listeners on board for controllers.
        getBoard().addListenerToAll((x, y) -> {
            if(getCurrentPlayer() instanceof Controller && isRunning()) {
                //Trigger the click listener for the current player.
                ((Controller)getCurrentPlayer()).onTileClicked(
                        TileGame.this, getCurrentPlayerId(), x, y);
            }
        });
        
        playerPieces = new Set[players.length];
        for(int i = 0; i < players.length; i++) {
            playerPieces[i] = new HashSet<>();
        }
        
        //Set the dimensions.
        this.width = width;
        this.height = height;
        
        this.title = title;
        
        //Start the game.
        start();
    }
    
    /**
     * @return the width of the game board.
     */
    public int getWidth() { return width; }
    
    /**
     * @return the height of the game board.
     */
    public int getHeight() { return height; }
    
    /**
     * @param action to perform for each grid position.
     */
    public void forEachPosition(BiConsumer<Integer, Integer> action) {
        
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                action.accept(x, y);
            }
        }
    }
    
    /**
     * @return the window in which the game board exists.
     */
    public Window getWindow() { return window; }
    
    /**
     * @return the board upon which the game is being played.
     */
    public Board getBoard() { return board; }
    
    @Override
    public boolean isRunning() {
        return super.isRunning() && board.getWindow().isOpen();
    }
    
    /**
     * Finds the piece at a position on the board, if on exists.
     * @param x the x position to check.
     * @param y the y position to check.
     * @return the piece at the given position, if there is one.
     */
    public Optional<Piece> getPieceInst(int x, int y) {
        return Optional.ofNullable(boardPieces[x][y]);
    }
    
    /**
     * Returns a set of all the pieces owned by a particular player.
     * @param owner_id the ID of the player whose pieces to return.
     * @return all the pieces owned by this player.
     */
    protected Set<Piece> getPieces(int ownerId) {
        return playerPieces[ownerId-1];
    }
    
    @Override
    protected void preTurn() {
        //Set the title to indicate the players' turn.
        window.setTitle(title + " - Current Turn: "
                + getPlayerName(getCurrentPlayerId()));
    }
    
    @Override
    protected void onFinish() {
        
        //Display the winner of the game.
        if(getWinner().isPresent()) {
            window.setTitle(title + " - Winner: " + getPlayerName(getWinnerId()));
            
        //Set the title to reflect a draw.
        } else {
            getBoard().getWindow().setTitle(title + " - Draw");
        }
    }
    
    /**
     * @param x the x position to check.
     * @param y the y position to check.
     * @return whether this position is within the bounds of the board.
     */
    protected boolean inBounds(int x, int y) {
        return x>=0 && y>=0 && x<getWidth() && y<getHeight();
    }
    
    /**
     * Changes the background colour given to selected pieces.
     * @param colour
     */
    protected void setHighlightColour(Colour colour) {
        HIGHLIGHT_COLOUR = colour;
    }
    
    /**
     * @param title the new title of the window.
     */
    protected void setTitle(String title) {
        window.setTitle(title);
        this.title = title;
    }
    
    /**
     * Supertype for pieces which are used on a chess board.
     * @author Alec Dorrington
     * @param <G> the game to which this piece belongs.
     */
    protected abstract class Piece extends Tile {
        
        private static final long serialVersionUID = 9077509164516940403L;

        /** The owner of this piece. */
        private Player<?> owner;
        
        /** The ID of the owner of this piece. */
        private int ownerId;
        
        /** The board position of this piece. */
        private int x, y;
        
        /**
         * Constructs a new piece of a particular owner at a particular position.
         * @param board the chessboard on which this piece resides.
         * @param owner the owner of this piece.
         * @param ownerId the ID of the owner of this piece.
         * @param x the x position of this piece.
         * @param y the y position of this piece.
         */
        protected Piece(int ownerId, int x, int y, String texture) {
            
            super(board.getWindow());
            
            //Set the owner of this piece.
            owner = getPlayer(ownerId);
            this.ownerId = ownerId;
            
            //Update the position of this piece.
            setBoardPos(x, y);
            
            //Match the size of the piece to the grid size of the board.
            setColour(Colour.WHITE);
            setTexture(Texture.getTexture(texture));
        }
        
        /**
         * Sets the position of this piece.<br>
         * Captures (removes) the piece which was previously at this location if there is one.
         * @param piece the piece for which to set the position.
         * @param x the new x position.
         * @param y the new y position.
         */
        public void setBoardPos(int x, int y) {
            
            //Capture a piece if this piece is moving on top of it.
            if(getPieceInst(x, y).isPresent())
                getPieceInst(x, y).get().delete();
            
            //Remove the piece from its previous board index.
            if(boardPieces[getCol()][getRow()] == this)
                boardPieces[getCol()][getRow()] = null;
            
            //Add the piece to its new board index.
            boardPieces[x][y] = this;
            playerPieces[getOwnerId()-1].add(this);
            
            //Set the graphical position of the piece.
            board.setPosition(this, x, y);
            
            this.x = x;
            this.y = y;
        }
        
        /**
         * Removes this piece from the board.
         */
        public void delete() {
            
            //Remove the piece from the board pieces array.
            if(boardPieces[getCol()][getRow()] == this)
                boardPieces[getCol()][getRow()] = null;
            
            //Remove the piece from the player pieces set.
            playerPieces[getOwnerId() - 1].remove(this);
            
            //Remove this tile from the renderer.
            destroy();
        }
        
        /**
         * @return the player to whom this piece belongs.
         */
        protected Player<?> getOwner() { return owner; }
        
        /**
         * @return the ID of the player to whom this piece belongs.
         */
        public int getOwnerId() { return ownerId; }
        
        /**
         * @return the current x position of this piece on the board.
         */
        public int getCol() { return x; }
        
        /**
         * @return the current y position of this piece on the board.
         */
        public int getRow() { return y; }
        
        /**
         * Called by the game to move a piece to a particular position.<br>
         * Implementations of this method should provide their own move validation.<br>
         * @param x_to the x position to move to.
         * @param y_to the y position to move to.
         * @return whether the move was valid and successful.
         */
        public abstract boolean movePiece(int x_to, int y_to);
        
        /**
         * Determines whether such a move would be valid.<br>
         * @param x_to the x position to move to.
         * @param y_to the y position to move to.
         * @return whether the move is valid.
         */
        public boolean validateMove(int x_to, int y_to) { return false; }
    }
    
    /**
     * Abstract supertype of player implementations for use in inserting human players.
     * @author Alec Dorrington
     * @param <G> the game which this player plays.
     */
    protected static abstract class Controller<G extends TileGame> implements Player<G> {
        
        /** The display name of this player. */
        private String name = "Controller";
        
        /** The piece currently selected by the mouse, if there is any. */
        private Optional<Piece> selected = Optional.empty();
        
        /**
         * Constructs a new Controller with the default name of "Controller".
         */
        public Controller() {}
        
        /** 
         * Constructs a new Controller with the given name.
         * @param name the display name of this controller.
         */
        public Controller(String name) { this.name = name; }
        
        /**
         * Called once each time the board is left clicked by the mouse.<br>
         * Is only called by clicks made within this players turn.
         * @param game the game being played.
         * @param playerId the ID of this player.
         * @param x the x position of the tile that was clicked.
         * @param y the y position of the tile that was clicked.
         */
        protected abstract void onTileClicked(G game, int playerId, int x, int y);
        
        @Override
        public void takeTurn(G game, int playerId) {
            //Wait until the turn is complete before returning control to the game.
            //Actual logic is handled asynchronously by listeners set up in init().
            while(!game.turnDone() && game.getWindow().isOpen()) {}
        }
        
        /**
         * Selects the given piece. This piece will be highlighted.
         *  @param game the game being played.
         * @param piece the piece to store as selected.
         */
        protected void selectPiece(G game, Piece piece) {
            //Select this piece.
            selected = Optional.of(piece);
            game.getBoard().resetColours();
            game.getBoard().setColour(piece.getCol(), piece.getRow(), HIGHLIGHT_COLOUR);
        }
        
        /**
         * Unselects the currently selected piece.
         * @param game the game being played.
         */
        protected void unselectPiece(G game) {
            selected = Optional.empty();
            game.getBoard().resetColours();
        }
        
        /**
         * @return the currently selected piece, if there is any.
         */
        protected Optional<Piece> getSelected() { return selected; }
        
        @Override
        public String getName() { return name; }
    }
}