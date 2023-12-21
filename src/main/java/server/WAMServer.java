package server;

import common.WAMProtocol;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;



import static java.lang.Thread.sleep;

/**
 * Whack-A-Mole main server
 * runs one complete game between a set amount of players with a set board pattern
 * splits the process into several thread
 *
 *  main thread -> loops and waits for changes to the score to send it to the server
 *
 *  handler thread -> linked to each mole to send them out randomly to clients, the timing is random who gets it is not
 *  all players are synchronized and will get and lose the same moles
 *
 *  timer thread -> (this.run() launched as thread) keeps current server time by compare current time compaired to
 *  a time stamp made when the game started
 *
 *  listener thread -> defined in its own class (its a bit too complicated to just launch from here), creates a
 *  thread for each client connected and waits on protocol from them
 *
 * @author Christopher Asbrock
 * @author Shakeel Farooq
 *
 */
public class WAMServer implements WAMProtocol
{
    /**Time stamp for the start of the game*/
    private int game_time;

    /**the server for the server*/
    private final ServerSocket server;

    /**amount of rows the game will have*/
    private int rows;
    /**amount of columns the game will have*/
    private int cols;
    /**the max amount of players the game will have*/
    private int maxPlayers;
    /**a gradually updated current time used to run the game and send out moves*/
    private double currtime;

    /**an array of integers to hold scores, disconnected players will be null*/
    private Integer[] scores;
    /**a boolean list representing moles, true is up, false is down*/
    private boolean[] spots;
    /**an array of sockets for each player connected*/
    private final Socket[] SOCKET;
    /**an array of output streams for each player*/
    private final PrintStream[] PRINTER;

    /**helper check, if a mole was whacked it will restart the loop to start the process over*/
    private boolean[] whacked;
    /**helper check, tells the server to start*/
    protected boolean go;
    /**helper check, will break all the loops in every thread signalling the end of the game*/
    protected boolean game_over;
    /**helper check, used by listener to tell the handler when to send scores out*/
    protected boolean send_score;

    /**
     * constructor for the server class
     * initializes the information needed to run one full game of Whack-A-Mole
     *
     * @param port - the port number
     * @param rows - amount of rows the board will have
     * @param cols - amount of columns the board will have
     * @param player_number - the number of players
     * @param game_time - the total amount of time the game will run for
     * @throws IOException - if there are any I/O errors thrown
     */
    public WAMServer(int port, int rows, int cols, int player_number, int game_time) throws IOException
    {
        this.game_time = game_time;
        this.rows = rows;
        this.cols = cols;
        this.maxPlayers = player_number;

        this.server = new ServerSocket(port);

        this.spots = new boolean[rows * cols];
        this.scores = new Integer[maxPlayers];
        for (int i = 0; i < this.scores.length; i++)
            this.scores[i] = 0;

        this.go = false;
        this.game_over = false;

        this.whacked = new boolean[rows * cols];
        this.send_score = true;

        this.SOCKET = new Socket[maxPlayers];
        this.PRINTER = new PrintStream[maxPlayers];
    }

    protected synchronized boolean[]getWhacked()
    {
        return this.whacked;
    }

    protected synchronized boolean[] getSpots()
    {
        return this.spots;
    }

    public double getCurrentTime()
    {
        return this.currtime;
    }
    /**
     * A method that will be launched as a thread
     * it will simply create a start time then change the servers time based on the time elapsed
     *
     * when the times up it will change the state of the game to over
     */
    private void run()
    {
        this.go = true;
        double time = 0;
        try
        {
            while (true)
            {
                if (game_over)
                    break;

                sleep(100);
                this.currtime += (0.1);

                if (currtime >= game_time)
                {
                    this.game_over = true;
                    break;
                }
            }
        }
                catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("TIME'S UP");
    }

    /**
     * sets up a new listener thread for all clients connected
     * sends them a welcome massage containing the information needed to set up a board
     *
     * @throws IOException - if there are any I/O exceptions
     */
    private void setUpListeners() throws IOException
    {
        for (int i = 0; i < maxPlayers; i++)
        {
            System.out.println("Waiting for Player " + (i + 1) + " to connect...");
            this.SOCKET[i] = server.accept();
            this.PRINTER[i] = new PrintStream(SOCKET[i].getOutputStream());
            this.PRINTER[i].println("WELCOME" +
                    " " + rows +
                    " " + cols +
                    " " + maxPlayers +
                    " " + (i+1));

            Listener b = new Listener(this.SOCKET[i], this, i);
            b.start();

            System.out.println("Player " + (i + 1) + " Connected");
        }
    }

    /**
     * A method to sendProtocol to connected players
     *
     * @param protocol - the protocol being sent
     */
    protected synchronized void sendProtocol(String protocol)
    {
        for (int i = 0; i < this.maxPlayers; i++)
        {
            if (this.scores[i] != null)
            {
                this.PRINTER[i].println(protocol);
            }
        }
    }

    /**
     * Method used by the server to check for connected players, if all are disconnected the game will end
     *
     * @return - true if there are no more players connected, false otherwise
     */
    private boolean noPlayers()
    {
        int count = 0;

        for (int i = 0; i < this.getScores().length; i++)
        {
            if (this.getScores()[i] == null)
                count++;
        }

        if (count == this.getScores().length)
            return true;
        else
            return false;
    }

    /**
     * the main loop the handler will run
     * it simply runs through sending out random moves to the clients based
     * on the server time
     */
    private void runGameControl()
    {
        while (true)
        {
            if (this.game_over)
                break;

            if (this.send_score)
            {
                this.sendProtocol(SCORE + this);
                this.send_score = false;
            }

            if (this.noPlayers())
            {
                System.out.println("All Players Disconnected From Server");
                this.game_over = true;
                break;
            }

            System.out.flush();
        }
    }

    /**
     * the main handler for the server
     * creates its listeners
     * creates a timer (a single thread using this classes run method)
     * then finally keeps looping while using the servers time to send out random moves
     *
     * @throws IOException - if anything I/O exceptions happen
     */
    public void runServer() throws IOException
    {
        this.setUpListeners();

        new Thread(() -> this.run()).start();

        for (int i = 0; i < (rows * cols); i++)
        {
            new Handler(i, this).start();
        }

        try
        {
            this.runGameControl();
        }
        finally
        {
            this.go = false;
            this.calculateScores();
            this.close();
        }
    }

    /**
     * uses the scores to find the winning players
     * 1) finds the max score
     * 2) finds the players who have the max score
     * 3) sends out the protocol to inform the players who won and lost
     */
    private void calculateScores()
    {
        Integer[] total_scores = this.scores;

        ArrayList<Integer> winner = new ArrayList<>();
        int maxScore = -1000;
        for (int i = 0; i < total_scores.length; i++)
        {
            if (total_scores[i] != null)
            {
                if (total_scores[i] > maxScore)
                {
                    maxScore = total_scores[i];
                }
            }
        }

        for (int i = 0; i < total_scores.length; i++)
        {
            if (total_scores[i] != null)
            {
                if (total_scores[i] == maxScore)
                {
                    winner.add(i);
                }
            }
        }

        if (winner.size() > 1)
        {
            updatePlayers(total_scores, winner, GAME_TIED);
        }
        else
        {
            updatePlayers(total_scores, winner, GAME_WON);
        }
    }

    /**
     * Uses lists if scores to and a max score to dole out the messages to each player
     * and tells them if they won lost or tied
     *
     * @param total_scores - A reference to the array with all the scores
     * @param winner - an array list of all the winners (players with the same score)
     * @param protocol - the protocol to send,
     *                 (varies between won and tied depending on how many winners there are)
     */
    private void updatePlayers(Integer[] total_scores, ArrayList<Integer> winner, String protocol)
    {
        for (int i = 0; i < total_scores.length; i++)
        {
            if (total_scores[i] != null)
            {
                this.PRINTER[i].println(SCORE + this);
                if (winner.contains(i))
                {
                    this.PRINTER[i].println(protocol);
                    if (protocol.equals(GAME_TIED))
                        System.out.println("Player " + (i + 1) + " Tied!");
                    else
                        System.out.println("Player " + (i + 1) + " Won!");
                }
                else
                {
                    this.PRINTER[i].println(GAME_LOST);
                    System.out.println("Player " + (i + 1) + " Lost!");
                }
            }
        }
    }

    /**
     * closes all sockets, printers, and the server itself
     * handles each individually, if an error is thrown it will continue to loop
     * to try and close the rest of the sockets
     *
     * @throws IOException - if the server itself throws and error
     */
    public void close() throws IOException
    {
        for(int i = 0; i < maxPlayers; i++)
        {
            try
            {
                this.PRINTER[i].close();
                this.SOCKET[i].close();
            }
            catch (IOException e)
            {
                System.out.println(e);
            }
        }

        this.server.close();
        System.out.println("SERVER SHUTDOWN");
    }

    /**
     * prints out all scores with a space between them
     * used by the SCORE protocol to send the scores to players
     *
     * Disconnected players will send an "x" so connected players can see that the player has
     * been disconnected
     *
     * @return - protocol ready string of scores
     */
    @Override
    public String toString()
    {
        String temp = "";

        for (int i = 0; i < this.scores.length; i++)
        {
            if (this.scores[i] != null)
            {
                temp += (" " + this.scores[i]);
            }
            else
            {
                temp += (" " + "x");
            }
        }

        return temp;
    }

    /**
     * gets a copy of the scoreboard
     * used by the lister to check if the player is connected
     *
     * @return - the array of scores
     */
    public synchronized Integer[] getScores()
    {
        return this.scores;
    }

    /**
     * changes a score in the array of scores
     * synchronized, so only one listener can mess with the array at a time
     *
     * @param player - the index representing the player
     * @param change - the change to make to it
     */
    protected synchronized void changeScore(int player, Integer change)
    {
        this.scores[player] = change;
    }


    /**
     * Uses Random class to piece together a new double with two random integers
     * random int in some range + (random int between 0 and 10 / 10)
     *
     * @param min - min number
     * @param max - max number
     * @return - randomly selected number
     */
    protected double random_range(int min, int max)
    {
        return (((double)new Random().nextInt((max - min)))
                + min
                + (((double)new Random().nextInt(10) / 10)));
    }

    /**
     * create the server and starts a new Whack-A-Mole Game
     *
     * @param args - defines the port, rows, cols, players, and game time
     */
    public static void main(String[] args)
    {
        if (args.length != 5)
        {
            System.out.println("Missing Command Line Arguments");
            System.exit(-1);
        }
        else
        {
            try
            {
                WAMServer server = new WAMServer(
                        Integer.parseInt(args[0]),
                        Integer.parseInt(args[1]),
                        Integer.parseInt(args[2]),
                        Integer.parseInt(args[3]),
                        Integer.parseInt(args[4]));
                server.runServer();
            }
            catch (IOException e)
            {
                System.out.println(e);
                e.printStackTrace();
            }
            catch (NumberFormatException e)
            {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }
}
