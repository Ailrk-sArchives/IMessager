package IMClient;
/*
IMClient.java - Instant Message client using UDP and TCP communication.
Text-based communication of commands.
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IMClient {
	// Protocol and System constants
	public static String serverAddress = "localhost";
	public static int TCPServerPort = 1234;					// connection to server
    public static int UDPPort = 1235;                       // udp connection.
	
	/* 	
	 * This value will need to be unique for each client you are running
	 */
	public static int TCPMessagePort;				        // port for connection between 2 clients
	
	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";

	private BufferedReader reader;							// Used for reading from standard input

    public static boolean buddyChat = false;                              //indicate the buddy convertion. 
    private String buddyTCPIP;
    private int    buddyPort;

    /*
     * decalre thread classes 
     */
    TCPMessager tcpmessager;
    UDPSender udpsender;

    // buddy list is maintained as a hash table. <userId, BuddyStatusRecord>
    private Map<String, BuddyStatusRecord> buddylist = new Hashtable<>();

	// Client state variables
    private String userId;
	private String status;

	public static void main(String []args) throws Exception {
        int p = 1248;
        if (args.length == 2 && args[0].equals("-p")){
            p = Integer.parseInt(args[1]);

            System.out.println(p);
        }
        else
            System.out.println("usage: java <class> -p port");

		IMClient client = new IMClient(p);
   		client.execute();
	}

	public IMClient(int tcpMsgPort) {
		// Initialize variables
		userId = null;
		status = null;
        TCPMessagePort = tcpMsgPort;
        tcpmessager =  new TCPMessager(this);
        udpsender   =   new UDPSender(this);
	}

	public void execute() {
		initializeThreads();

		String choice;
		reader = new BufferedReader(new InputStreamReader(System.in));

		printMenu();
		choice = getLine().toUpperCase();

		while (!choice.equals("X"))
		{
            // DEBUG
			if (choice.equals("Y"))
			{	// Must have accepted an incoming connection
				acceptConnection();
			}
			else if (choice.equals("N"))
			{	// Must have rejected an incoming connection
				rejectConnection();
			}
			else if (choice.equals("R"))				// Register
			{	registerUser();
			}
			else if (choice.equals("L"))		// Login as user id
			{	loginUser();
			}
			else if (choice.equals("A"))		// Add buddy
			{	addBuddy();
			}
			else if (choice.equals("D"))		// Delete buddy
			{	deleteBuddy();
			}
			else if (choice.equals("S"))		// Buddy list status
			{	buddyStatus();
			}
			else if (choice.equals("M"))		// Start messaging with a buddy
			{	buddyMessage();
			}
			else
				System.out.println("Invalid input!");

			printMenu();
			choice = getLine().toUpperCase();
		}
		shutdown();
	} // end execute

    /*
     * initialize threads here. We have UDPSender, UDPreceiver, TCPMessger
     * working concurrently.
     * */
	private void initializeThreads() {
        Thread tcpmessagerThread =   new Thread(tcpmessager);
        Thread udpsenderThread   =   new Thread(udpsender);

        tcpmessagerThread.start();
        udpsenderThread.start();
	}

	private void registerUser() {
		// Register user id
        String responseMsg="";
        Socket serverSocket = null;

        try {
            // TCP server socket. handle request and receive responds from server.
            serverSocket = new Socket(serverAddress, TCPServerPort); 
            DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            System.out.println("Your new user id: ");
            userId = getLine();

            outToServer.writeBytes("REG " + userId + '\n');
            responseMsg = inFromServer.readLine();

            outToServer.close();
            inFromServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // free socket 
            try {
                if (serverSocket != null) 
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // check response value;
        System.out.println(responseMsg);
        if (responseMsg.equals("200 OK\r\n"))
            status = onlineStatus;

	} // end registerBuddy

	private void loginUser() {
		// Login an existing user (no verification required - just set userId to input)
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("User id set to: "+userId + '\n');
		status = onlineStatus;
	}

	private void addBuddy(){
        // Add buddy if have current user id
        String responseMsg="";
        String buddyId;
        Socket serverSocket = null;

        // only add buddy if logined. Otherwise refuse service.
        if (status.equals(onlineStatus) && userId != null) {
            try {
                serverSocket = new Socket(serverAddress, TCPServerPort); 
                DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));


                System.out.print("Your buddy's id: ");
                buddyId = getLine().trim();

                outToServer.writeBytes("ADD " + userId + " " + buddyId+'\n');
                responseMsg = inFromServer.readLine().trim();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // free socket 
                try {
                    if (serverSocket != null) 
                        serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            System.out.println("Login pls.");
            return;
        }

        // check response value;
        System.out.println(responseMsg);
	} // end addBuddy

	private void deleteBuddy(){
        // Delete buddy if have current user id
        String responseMsg = "";
        String buddyId;
        Socket serverSocket = null;
        DataOutputStream outToServer;
        BufferedReader inFromServer;


        // only add buddy if logined. Otherwise refuse service.
        if (status.equals(onlineStatus) && userId != null) {
            try {
                serverSocket = new Socket(serverAddress, TCPServerPort); 
                outToServer = new DataOutputStream(serverSocket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

                System.out.println("Your buddy's id: ");
                buddyId = getLine();

                outToServer.writeBytes("DEL " + userId + " " + buddyId + '\n');
                buddylist.remove(buddyId);
                responseMsg = inFromServer.readLine();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // free socket 
                try {
                    if (serverSocket != null) 
                        serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Login pls. Be a buddy before add a new buddy.");
            return;
        }

        // check response value;
        System.out.println(responseMsg);
	} //end deleteBuddy

	private void buddyStatus(){
        // Print out buddy status (need to store state in instance variable that received from previous UDP message)
        Iterator<Map.Entry<String, BuddyStatusRecord>> iter;
        Map.Entry<String, BuddyStatusRecord> entry;
         
        System.out.println("My buddy list: ");
        iter = buddylist.entrySet().iterator();
        while (iter.hasNext()) {
            entry = iter.next();
            System.out.println(entry.getValue().toString());
        }
	}

    /*
     * send message require. It send a HANG? requrest and wait for buddy's Y
     * response. 
     * If success, cancel the TCPMessage thread, cancel waiting for other request then it will
     * create a BuddyRequest thread to listing to buddy's msg.
     *
     * user's q can terminate the session. And then send a BYE500 signal to inform buddy to close 
     * session. 
     */
	private void buddyMessage(){
        // Make connection to a buddy that is online
		// Must verify that they are online and should prompt to see if they accept the connection
        // Provide user ip and msg port for other to connect 
        Socket buddySocket = null;
        try{
            String buddyInviteMsg;
            String ip = "127.0.0.1";

            buddyInviteMsg = ip + " " + IMClient.TCPMessagePort + "\r\n";
            String bid;
            DataOutputStream outToBuddy;
            BufferedReader inFromBuddy;

            /*
             * Attempt pharse
             */
            System.out.println("Enter buddy id: ");
            bid = getLine().trim();

            System.out.println("Attempting to connect...");
            BuddyStatusRecord r = buddylist.get(bid);
            
            buddySocket = new Socket(r.IPaddress,  Integer.parseInt(r.buddyPort)); 
            outToBuddy = new DataOutputStream(buddySocket.getOutputStream());
            

            outToBuddy.writeBytes(buddyInviteMsg);

            IMClient.buddyChat = true;
            messageMode(buddySocket, outToBuddy);

            /*
             * Message mode. End when q pressed.
             */

        } catch (Exception e) {
            IMClient.buddyChat = false;
            e.printStackTrace();
        } finally {
            try{
                if (buddySocket != null)
                    buddySocket.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
       
	}

	private void shutdown(){
        this.status = offlineStatus;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Close down client and all threads
        tcpmessager.cancel();
        udpsender.cancel();
        System.exit(0);
	}

	private void acceptConnection(){
        // User pressed 'Y' on this side to accept connection from another user
		// Send confirmation to buddy over TCP socket
		// Enter messaging mode
        String goodByeString = "BYE500";
        String ip = "127.0.0.1";
        String buddyConfirmMsg = ip + " " + IMClient.TCPMessagePort + "\r\n";
        try {
            this.buddyTCPIP = TCPMessager.buddyTCPIP;
            this.buddyPort = TCPMessager.buddyTCPPort;
            System.out.println(buddyTCPIP + " " + buddyPort);
            Socket buddySocket = new Socket(this.buddyTCPIP, this.buddyPort);

            // get the buddy's tcpmsger socket and send Y confirm
            DataOutputStream outToBuddy = new DataOutputStream(buddySocket.getOutputStream());
            outToBuddy.writeBytes(buddyConfirmMsg);
            this.buddyChat = true; 
                
            messageMode(buddySocket, outToBuddy);
        } catch(Exception e) {
            e.printStackTrace();
        }
	}

	private void rejectConnection(){
        // User pressed 'N' on this side to decline connection from another user
		// Send no message over TCP socket then close socket
        IMClient.buddyChat = false;
        tcpmessager.cancel();
	}

	private String getLine(){
        // Read a line from standard input
		String inputLine = null;
		  try {
			  inputLine = reader.readLine();
		  }catch(IOException e){
			 System.out.println(e);
		  }
	 	 return inputLine;
	}

    /* enter message mode */
    private void messageMode(Socket buddySocket, DataOutputStream outToBuddy) {
        String goodByeString = "BYE500";

        try {
            String sentence;
            String terminator = "q";

            // write to buddy loop
            do {
                if (IMClient.buddyChat == false) break;
                System.out.print("> ");
                sentence = getLine();

                if ((sentence.trim()).equals(terminator)) {
                    IMClient.buddyChat = false;
                    break;
                }
                outToBuddy.writeBytes(sentence+'\n');

            } while (! (sentence.trim()).equals(terminator));

            /* bye! */
            outToBuddy.writeBytes(goodByeString);
            
            buddySocket.close();
            outToBuddy.close();
        } catch (Exception e) {
             
            e.printStackTrace();
        } 
    }

	private void printMenu(){
        System.out.println("\n\nSelect one of these options: ");
		System.out.println("  R - Register user id");
		System.out.println("  L - Login as user id");
		System.out.println("  A - Add buddy");
		System.out.println("  D - Delete buddy");
		System.out.println("  M - Message buddy");
		System.out.println("  S - Buddy status");
		System.out.println("  X - Exit application");
		System.out.print("Your choice: ");
	}

    /* allow UPDHanlder thread to access buddylist */
    public void updateBuddyList(String key, BuddyStatusRecord val) {
        this.buddylist.put(key, val);
    }

    public String getUserStatus() {
        return this.status;
    }
    
    public String getUserID() {
        return this.userId;
    }
}

// A record structure to keep track of each individual buddy's status
class BuddyStatusRecord
{	public String IPaddress;
	public String status;
	public String buddyId;
	public String buddyPort;

	public String toString()
	{	return buddyId+"\t"+status+"\t"+IPaddress+"\t"+buddyPort; }

	public boolean isOnline()
	{	return status.indexOf("100") >= 0; }
}

// This class implements the TCP welcome socket for other buddies to connect to.
// I have left it here as an example to show where the prompt to ask for incoming connections could come from.

/*
 * TCPMessager handle welcome request from another buddy. Only listening for req.
 * */
class TCPMessager implements Runnable
{
    private volatile boolean exit = false;
	private IMClient client;
	private ServerSocket welcomesocket;

    private Socket connection;
	private Socket buddySocket;

	private String goodByeString = "BYE500";

    public static  String buddyTCPIP;
    public static int  buddyTCPPort;

	public TCPMessager(IMClient c){
        client = c;

        try {
            welcomesocket = new ServerSocket(IMClient.TCPMessagePort);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    public void run()
	{
		// this thread starts an infinite loop looking for tcp requests.
		try
		{
			while (!exit)
			{
                String[] buddyTagList;
		    	// listen for a tcp connection request.
		    	Socket connection = welcomesocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());

                buddyTagList = reader.readLine().trim().split(" ");
                if (buddyTagList.length != 2) {
                    out.writeBytes(goodByeString);
                    connection.close();
                    continue;
                }

                buddySocket = connection;
                // prepare for connection 
                buddyTCPIP = buddyTagList[0];
                buddyTCPPort = Integer.parseInt(buddyTagList[1]);

                if (!IMClient.buddyChat) {
		    	    System.out.print("\ndo you want to accept an incoming connection (y/n)? ");
                }
                buddyRequest();
			}
	    }
		catch (Exception e){
            System.out.println(e); 
            cancel();
        }
	}


	private void buddyRequest() throws Exception {
		String buddySentence;
		BufferedReader inFromBuddy = new BufferedReader(new InputStreamReader(buddySocket.getInputStream()));

        // read from buddy loop
        try {
            do {
                buddySentence = inFromBuddy.readLine();
                if (buddySentence == null) 
                    System.out.print("\n");
                else if (buddySentence.trim().equals(goodByeString)) {
                    System.out.println("\nClosing socket connection to " + buddySocket.getInetAddress() +":"+buddySocket.getPort());
                    buddySocket.close();
                    cancel();
                    break;
                }
                else
                    if (IMClient.buddyChat) System.out.print("\nB: " + buddySentence);
                    else break;

            }while (buddySentence!=null && !buddySentence.equals(goodByeString) &&
                    !exit);


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
		    buddySocket.close();
            cancel();
        }
	}

    public void cancel(){
        IMClient.buddyChat = false;
        exit = true;
    }
    public void respawn() {
        exit = false;
    }

}


/*
 * This class keep sending GET, SET request to IMserver every 10s.
 */
class UDPSender implements Runnable {
    private volatile boolean exit = false;
    private IMClient client;
    private int UDPPort;

    private String getReq;
    private String setReq;

    private InetAddress IPAddress;
    private DatagramSocket clientSocket;

    private DatagramPacket sendPacket;
    private UDPReceiver udprecver;

    // data buffer
    byte[] sendData;

    public UDPSender(IMClient c) {
        client = c;
        UDPPort = IMClient.UDPPort;
        sendData = new byte[1024];
        try {
            // arbitary port system provide.
            IPAddress = InetAddress.getByName("localhost");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }  

    /* send GET and SET every 10s 
     * SET has no response value. 
     * */
    public void run() {
        try {
            while (!exit) {
                int localPort;
                clientSocket = new DatagramSocket();
                // construct request message. 
                getReq = "GET " + 
                        client.getUserID() ;

                // GET request
                sendData = getReq.getBytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,  UDPPort);
                clientSocket.send(sendPacket);
                setReq = "SET " + client.getUserID() + " " + 
                        client.getUserStatus() + " " + IMClient.TCPMessagePort;
                sendData = setReq.getBytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,  UDPPort);
                clientSocket.send(sendPacket);


                // after send request, sleep. give reciver a chance to use 
                // the recourse.
                localPort = clientSocket.getLocalPort();
                clientSocket.close();
                udprecver = new UDPReceiver(client, localPort); 
                Thread udprecverThread = new Thread(udprecver);
                
                clientSocket.close();
                udprecverThread.start();
                Thread.sleep(1000);
                udprecver.cancel();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            cancel();
        }
    }

    public void setStatus() {
        setReq = "SET " + client.getUserID() + " " + 
                client.getUserStatus() + " " + IMClient.TCPMessagePort;
        sendData = setReq.getBytes();
        sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,  UDPPort);
        try {
            if (!clientSocket.isClosed())
                clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void cancel(){
        exit = true;
    }
    public void respawn() {
        exit = false;
    }

}

class UDPReceiver implements Runnable{
 private volatile boolean exit = false;
    private IMClient client;

    private DatagramPacket receivePacket;

    private ArrayList<String> blist;
    private DatagramSocket serverSocket;

    // data buffer
    byte[] recvData;

    public UDPReceiver(IMClient c, int port) {
        client = c;
        try{
            serverSocket = new DatagramSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recvData = new byte[1024];
    }  

    /* send GET and SET every 10s 
     * SET has no response value. 
     * */
    public void run() {
        try {
            while (!exit) {
                
                receivePacket = new DatagramPacket(recvData, recvData.length);
                serverSocket.receive(receivePacket); 

                String[] s = new String(receivePacket.getData()).split("\n");

                for (int j=0; j < s.length-1; j++) {
                    String record = s[j];
                    String[] wordlist = record.split(" ");

                    BuddyStatusRecord r = new BuddyStatusRecord();
                    for (int i=0; i < wordlist.length; i++) {
                        switch (i) {
                            case 0:
                                r.buddyId = wordlist[i];
                                break;
                            case 1:
                                String status = wordlist[1] + wordlist[2];
                                r.status = status;
                                i = 2;
                                break;
                            case 3:
                                r.IPaddress = wordlist[3];
                                break;
                            case 4:
                                r.buddyPort = wordlist[4];
                                break;
                            default:
                                throw new RuntimeException();
                        } //switch
                    } //for wordlist
                    client.updateBuddyList(r.buddyId, r);
                } // for record
            }
        } catch (IOException e) {
            try {
                Thread.sleep(100); 
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        } finally {
            cancel();
        }
    }
   
    public void cancel(){
        if (serverSocket != null)
            serverSocket.close();
        exit = true;
    }

    public void respawn() {
        exit = false;
    }
   
}

