package server;

import common.WAMException;
import common.WAMProtocol;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * A sub class for the WAMServer
 * defines a listener thread to run and take incoming clients protocol
 *
 * @author Christopher Asbrock
 * @author Shakeel Farooq
 */

public class Listener extends Thread implements WAMProtocol
{

    /**the scanner used to comminicate with the player*/
    private final Scanner SCANNER;

    /**the player number for this specific player*/
    private int player;

    /**A reference to the server class to link information with it*/
    private WAMServer server;

    /**
     * Constructor for the Listener
     * Each player has it's own listener
     *
     * @param socket client socket
     * @param referenceServer the server
     * @param player the player number that has this listener
     * @throws IOException if there are any I/O exceptions thrown
     */
    public Listener(Socket socket, WAMServer referenceServer, int player) throws IOException
    {
        this.server = referenceServer;
        this.player = player;

        this.SCANNER = new Scanner(socket.getInputStream());
    }

    /**
     * Listens for the client to send the WHACK protocol, then updates the player's score
     * accordingly
     * @param in The string list response from the scanner
     */
    private void handleInput(String[] in) throws WAMException
    {
        switch(in[0])
        {
            case WHACK:
                //Get the player's current score
                Integer player_score = this.server.getScores()[Integer.parseInt(in[2]) - 1];
                //Check to see if the player hasn't disconnected
                if (player_score != null)
                {
                    //Check to see if the mole is up at this mole spot
                    if (this.server.getSpots()[Integer.parseInt(in[1])])
                    {
                        //Increase the player's score by 2
                        this.server.changeScore(Integer.parseInt(in[2]) - 1, player_score + 2 );
                        this.server.getWhacked()[Integer.parseInt(in[1])] = true;
                        this.server.getSpots()[Integer.parseInt(in[1])] = false;
                        this.server.send_score = true;
                    }
                    //mole is down at this mole spot
                    else
                    {
                        //Decrease the player's score by 1
                        this.server.changeScore(Integer.parseInt(in[2]) - 1, player_score - 1 );
                        this.server.send_score = true;
                    }
                }
                break;
            default:
                System.out.println(ERROR);
                throw new WAMException("Improper Protocol");
        }
    }

    /**
     * Run method for a player's specified Listener
     * will run through and listen for requests send from one client and handle it
     * accordingly
     */
    public void run()
    {
        try
        {
            while (true)
            {
                if (this.server.game_over)
                {
                    break;
                }

                System.out.flush();
                if (this.server.go)
                {
                    if (!this.SCANNER.hasNextLine())
                        throw new WAMException("LISTENER " + (this.player + 1) +  " STOP");

                    String[] in = this.SCANNER.nextLine().split(" ");
                    this.handleInput(in);
                }
            }
        }
        catch (WAMException e)
        {
            //if anything goes wrong it will set the player as disconnected and close the listener
            this.server.changeScore(this.player, null);
            this.server.send_score = true;
            System.out.println(e.getMessage());
        }
        finally
        {
            this.close();
        }
    }

    /**
     * closes this clients scanner
     */
    public void close()
    {
        this.SCANNER.close();
        System.out.println("LISTENER " + (this.player + 1) + " SHUTDOWN");
    }


}
