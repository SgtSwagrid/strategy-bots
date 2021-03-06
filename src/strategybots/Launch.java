package strategybots;

import strategybots.bots.SwagMNK;
import strategybots.bots.TipDots3v3;
import strategybots.bots.TipMCTS;
import strategybots.bots.legacy.SwagC4;
import strategybots.games.Amazons;
import strategybots.games.Chess;
import strategybots.games.Chomp;
import strategybots.games.Clobber;
import strategybots.games.Checkers;
import strategybots.games.Checkers.CheckersController;
import strategybots.games.DotsAndBoxes;
import strategybots.games.Reversi;
import strategybots.games.Amazons.AmazonsController;
import strategybots.games.Chess.ChessController;
import strategybots.games.Chomp.ChompController;
import strategybots.games.Clobber.ClobberController;
import strategybots.games.ConnectFour;
import strategybots.games.DotsAndBoxes.DotsController;
import strategybots.games.Pentago;
import strategybots.games.Pentago.PentagoController;
import strategybots.games.Reversi.ReversiController;
import strategybots.games.TicTacToe;
import strategybots.games.TicTacToe.TicTacToeController;;

public class Launch {
    
    private static final int AMAZONS = 1;
    private static final int CHECKERS = 2;
    private static final int CHESS = 3;
    private static final int CHOMP = 4;
    private static final int CLOBBER = 5;
    private static final int CONNECTFOUR = 6;
    private static final int DOTSANDBOXES = 7;
    private static final int PENTAGO = 8;
    private static final int REVERSI = 9;
    private static final int TICTACTOE = 10;
    
    private static final int GAME = DOTSANDBOXES;
    
    public static void main(String[] args) {
        
        switch(GAME) {
            
            case AMAZONS: new Amazons(new AmazonsController(), new AmazonsController()); break;
            case CHECKERS: new Checkers(new CheckersController(), new CheckersController()); break;
            case CHESS: new Chess(new ChessController(), new ChessController()); break;
            case CHOMP: new Chomp(new ChompController(), new ChompController()); break;
            case CLOBBER: new Clobber(new ClobberController(), new ClobberController()); break;
            case CONNECTFOUR: new ConnectFour(new SwagC4(2000), new TipMCTS(2000)); break;
            case DOTSANDBOXES: new DotsAndBoxes(5, 5, new DotsController(), new TipDots3v3()); break;
            case PENTAGO: new Pentago(new PentagoController(), new PentagoController()); break;
            case REVERSI: new Reversi(new ReversiController(), new ReversiController()); break;
            case TICTACTOE: new TicTacToe(new TicTacToeController(), new SwagMNK(2000)); break;
        }
    }
}