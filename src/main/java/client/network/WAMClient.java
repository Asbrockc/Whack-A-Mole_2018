package client.network;

import common.WAMException;
import common.WAMProtocol;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The client side network interface to a Whack-a-Mole game server.
 * Each player that is playing gets its own connection to the server.
 * This class represents the controller part of the MVC pattern
 * It is used to send user actions to the server and receive protocols
 * from the server
 *
 * @author Shakeel Farooq
 * @author Chris Asbrock
 */
public class WAMClient implements WAMProtocol
{
    /** Client socket used to communicate with the server */
    private Socket clientSocket;

    /** Used to read requests from the server */
    private Scanner networkIn;

    /** Used to write responses to the server */
    private PrintStream networkOut;

    /** The number of rows for the board */
    private int rows;

    /** The number of columns for the board */
    private int cols;

    /** The total number of players playing the game */
    private int maxPlayers;

    /** The player number of this client */
    private int player;

    /** The scores for each player */
    private String[] scores;

    /** The model which keeps track of the game */
    private WAMObserver board;

    /** Used to loop for the entire length of the game */
    private boolean gameOn;

    /**
     * Called by the UI to get the number of rows for the board
     *
     * @return this.rows
     */
    public int getRows()
    {
        return this.rows;
    }

    /**
     * Called by the UI to get the number of columns for the board
     *
     * @return this.cols
     */
    public int getCols()
    {
        return this.cols;
    }

    /**
     * Hook up with a Whack-A-Mole server already running and waiting for multiple players
     * to connect.
     *
     * @param host the name of the host running the server program
     * @param port the port of teh server socket on which the server is listening
     *
     */
    public WAMClient(String host, int port, WAMObserver model)
    {
        try
        {
            this.board = model;

            //establish a connection with the server
            this.clientSocket = new Socket(host, port);

            //get input and output connections for the server
            this.networkIn = new Scanner(clientSocket.getInputStream());
            this.networkOut = new PrintStream(clientSocket.getOutputStream());

            //Check to see if the server sent anything
            if (!this.networkIn.hasNextLine())
                throw new WAMException("Lost Connection");

            String request = this.networkIn.next();

            //Check for the WELCOME protocol from the server
            if (!request.equals(WELCOME))
                throw new WAMException("Expected WELCOME from server");

            //get the arguments from the WELCOME protocol
            String[] arguments = networkIn.nextLine().split(" ");

            int rows = Integer.parseInt(arguments[1]);
            int cols = Integer.parseInt(arguments[2]);
            int players = Integer.parseInt(arguments[3]);
            int player = Integer.parseInt(arguments[4]);

            //call the welcome method
            welcome(rows, cols, players, player);

            //allows the listener to loop
            this.gameOn = true;
        }
        catch(IOException e)
        {
            System.out.println(e);
            this.board.changeErrorMessage(e.getMessage());

            //gives enough information to build the board, but it will display a network error
            //to inform the player they are not connected
            welcome(2, 2, 1, 1);
            this.gameOn = false;
        }
        catch (WAMException e)
        {
            // ^ ditto
            System.out.println(e);
            this.board.changeErrorMessage(e.getMessage());

            welcome(2, 2, 1, 1);
            this.gameOn = false;
        }
    }

    /**
     * Called when the client receives the welcome protocol from the server
     *
     * @param rows The number of rows for the board
     * @param columns The number of columns for the board
     * @param players The number of players playing
     * @param player The player number associated with this client
     */
    public void welcome(int rows, int columns, int players, int player)
    {
        this.rows = rows;
        this.cols = columns;
        //create the board with the number of rows and columns
        this.board.setBoard(this.rows, this.cols);

        this.maxPlayers = players;
        this.player = player;
        //set the board's player to this player
        this.board.setPlayer(this.player);

        //set the scores for each player to 0
        this.scores = new String[maxPlayers];
        for (int i = 0; i < this.scores.length; i++)
            this.scores[i] = "0";
    }

    /**
     * Makes a mole go up at a certain position
     *
     * @param spot the position
     */
    public void moleUp(int spot)
    {
        board.moleUp(spot);
    }

    /**
     * Makes a mole go down at a certain position
     *
     * @param spot the position
     */
    public void moleDown(int spot)
    {
        board.moleDown(spot);
    }

    /**
     * Sets the scores for each player
     *
     * @param args The scores values given by the SCORE protocol
     */
    public void setScores(String[] args)
    {
        for (int i = 1; i < args.length; i++)
        {
            scores[i - 1] = args[i];
        }
    }

    /**
     * Run the main client loop
     */
    public void run()
    {
        try
        {
            if (!this.gameOn)
                throw new WAMException("CONNECTION REFUSED");

            while (this.gameOn)
            {
                //Check to see if the client receives any protocols
                //if not, then it means that the player disconnected
                if (!this.networkIn.hasNextLine())
                    throw new WAMException("Connection Lost");

                String in = networkIn.nextLine();
                String[] request = in.split(" ");

                switch (request[0])
                {
                    case MOLE_UP:
                        this.board.changeStatus(WAMObserver.Status.PLAYING);
                        this.board.updateScoreBoard(this.toString());
                        this.moleUp(Integer.parseInt(request[1]));
                        break;
                    case MOLE_DOWN:
                        this.board.updateScoreBoard(this.toString());
                        this.moleDown(Integer.parseInt(request[1]));
                        break;
                    case SCORE:
                        this.board.changeStatus(WAMObserver.Status.PLAYING);
                        this.setScores(request);
                        this.board.updateScoreBoard(this.toString());
                        this.board.updateScore();
                        break;
                    case GAME_WON:
                        this.board.updateScoreBoard(this.toString());
                        this.board.changeStatus(WAMObserver.Status.WON);
                        gameOn = false;
                        break;
                    case GAME_LOST:
                        this.board.updateScoreBoard(this.toString());
                        this.board.changeStatus(WAMObserver.Status.LOST);
                        gameOn = false;
                        break;
                    case GAME_TIED:
                        this.board.updateScoreBoard(this.toString());
                        this.board.changeStatus(WAMObserver.Status.TIED);
                        gameOn = false;
                        break;
                    case ERROR:
                        this.board.updateScoreBoard("");
                        this.sendError(in);
                        throw new WAMException("AN ERROR HAS OCCURRED");
                    default:
                        this.sendError("ERROR - Improper protocol");
                        throw new WAMException("ERROR - Improper protocol");
                }
            }
        }
        catch (WAMException e)
        {
            this.board.changeErrorMessage(e.getMessage());
            this.board.changeStatus(WAMObserver.Status.ERROR);
            System.out.println(e.getMessage());
            System.out.println("GAME OVER");
        }
    }

    private void sendError(String message)
    {
        this.board.changeErrorMessage(message);
        this.board.changeStatus(WAMObserver.Status.ERROR);
    }

    /**
     * Method that sends the WHACK protocol to the server
     *
     * @param spot the mole spot that the player whacked
     */
    public void sendWhack(int spot)
    {
        this.networkOut.println(WHACK + " " + spot + " " + this.player);
    }

    /**
     * Close the client connection.
     * Called at the end of the game
     */
    public void close()
    {
        this.gameOn = false;

        /*make sure the socket was opened before tyring to close it*/
        if (this.clientSocket != null)
        {
            try
            {
                this.networkIn.close();
                this.networkOut.close();
                this.clientSocket.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            System.out.println("Socket Closed");
        }
    }

    /**
     * Create a new thread and have it start the client run method
     */
    public void start()
    {
        new Thread(() -> this.run()).start();
    }

    /**
     * Helper method for toString
     *
     * toString creates a string representing a players place, name and score
     *
     * Returns an int value for a string extracted from from the current scores
     * and "x" presents a player who was disconnected from the server
     *
     * @param a - the score of a player as a String
     * @return - a parsed int, unless there was an x present then it will return a
     * very low number to ensure the player sorts to the bottom of the list
     */
    private int intValue(String a)
    {
        //if there was an x the player was disconnected, but the game will still
        //continue with the remaining players
        if (a.equals("x"))
            return -5000;

        int b = Integer.parseInt(a);
        return b;
    }

    /**
     * Helper method for toString method
     * Creates the places for each player in the leader board
     *
     * @param place the integer place
     * @return a string that represents the place of a player
     */
    private String ordinalIndicator (int place)
    {
        String strPlace = ("  " + String.valueOf((place + 1)));
        //switch statement affixes the correct ordinal indicator for places
        switch(place)
        {
            case 0:
                strPlace += "st-\t";
                break;
            case 1:
                strPlace += "nd-\t";
                break;
            case 2:
                strPlace += "rd-\t";
                break;
            default:
                strPlace += "th-\t";
        }

        return strPlace;
    }

    /**
     * Helper function that creates a leader board type string for the scoreboard to show
     * It keeps track of the players in the lead (players in the same score will show the same place)
     * It also shows players who were disconnected
     *
     * @param place The string that represents what place the players are in
     *              (created from the place method)
     * @param playerNum Integer that is looped through to access the player scores
     *
     * @return the leader board string
     */
    private String strLeaderBoard(String place, int playerNum)
    {
        String leaderBoard = "";

        //Check to see if the player has disconnected
        if (!this.scores[playerNum].equals("x"))
        {
            String check = (place) + ((playerNum == (player - 1)) ? "SCORE\t" : "Player ");
            leaderBoard += check;
            leaderBoard += check.contains("SCORE") ? ("") : ((playerNum + 1) + "\t");
            leaderBoard += (":\t" + this.scores[playerNum] + "\n");
        }
        else
        {
            //if they have disconnected it will note it in the text area
            leaderBoard += ("    Player " + (playerNum + 1) + " Disconnected" + "\n");
        }

        return leaderBoard;
    }

    /**
     * Evaluates the current scores orders them based on place and creates a string to display in the gui
     * showing each players scores, current place (1st, 2nd,...), and also notes if other players have
     * been disconnected from the server.
     *
     * @return - computed String value of scores
     */
    public String toString()
    {
        if (this.scores.length == 0)
            return "";

        ArrayList<String> list = new ArrayList<>();

        //grab current scores and throw them into the array list
        for (String score : this.scores)
            list.add(score);

        //sorts the list, converts each String to an int and sorts by score
        list.sort((String a, String b) ->
        {
            return this.intValue(b) - this.intValue(a);
        });

        String ordered_String = "";

        //boolean array corresponding to the player score array
        boolean[] marked = new boolean[maxPlayers];
        //The place of the player in the leader board(1st, 2nd, 3rd, etc.)
        String place = "";
        String previous = "";

        //goes through the scores and constructs a string by comparing it to the ordered list
        for (int i = 0; i < this.scores.length; i++)
        {
            for (int j = 0; j < this.scores.length; j++)
            {
                //go through each of the score values until the highest score is found
                if(this.scores[j].equals(list.get(0)))
                {
                    //Check to make sure this player's spot hasn't been accounted for
                    if (!marked[j])
                    {
                        //Check to see if this player's score is not the same as the previous player's score
                        if (!previous.equals(list.get(0)))
                        {
                            place = ordinalIndicator(i);
                        }
                        //create the leader board string
                        ordered_String += strLeaderBoard(place, j);

                        //pop the top spot off the list to move onto the next places score
                        previous = list.remove(0);

                        //mark this players spot as accounted for
                        marked[j] = true;
                        break;
                    }
                }
            }
        }

        return ordered_String;
    }


}
