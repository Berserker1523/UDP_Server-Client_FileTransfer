package Servidor;
import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Server {

	public static PrintWriter fileOut = null;

	public final static int SERVER_PORT = 4445;
	private static ServerThread[] threads;
	private final static int MAX_CONNECTIONS = 25;
	public static Integer numberConnections = 0;
	public final static String FILE_DIR = "./files/";
	public final static String LOGS_DIR = "./logs/";

	public static File fileToSend = null;
	public static int simultaneousClients = 25;
	public static Integer succesfullFilesSent = 0;

	public static void main(String args[]) throws Exception {
		DatagramSocket socket = new DatagramSocket(SERVER_PORT);
		serverProtocol(socket);
		while(true){
			if(succesfullFilesSent == simultaneousClients){
				fileOut.close();
				succesfullFilesSent = 0;
				numberConnections = 0;
				System.out.println();
				serverProtocol(socket);
			}
		}//end_updateCycle
	}//end_main

	public static void addSuccesfullFilesSent(){
		synchronized(succesfullFilesSent){
			succesfullFilesSent++;
		}
	}
	
	public static void addNumberConnections(){
		synchronized(numberConnections){
			numberConnections++;
		}
		
	}

	public static void serverProtocol(DatagramSocket socket) throws Exception{
		DateFormat fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date date = new Date();
		System.out.println("LogFile: " + fileName.format(date));

		fileOut= new PrintWriter(new FileWriter(new File(LOGS_DIR + fileName.format(date) + ".txt")), false);
		fileOut.println("Date: " + new SimpleDateFormat("yyyy/MM/dd").format(date) +
				"time: " + new SimpleDateFormat("HH:mm:ss").format(date));

		BufferedReader serverIn = new BufferedReader(new InputStreamReader(System.in));

		/*
		 * Choose file to send
		 */
		Path dir = Paths.get(Server.FILE_DIR);
		DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir);

		//File dir = new File(Server.FILE_DIR);
		File[] fileNames = new File[2];
		System.out.println("Choose a file to send to users: ");
		int fileCounter = 1;
		for (Path path : dirStream) {
			File actualFile = path.toFile();
			fileNames[fileCounter-1] = actualFile;
			System.out.println(fileCounter + "." + actualFile.getName() + " " + actualFile.length());
			fileCounter++;
		}
		while(true){
			System.out.println("Range: 1 - 2");
			int chosenFile = Integer.parseInt(serverIn.readLine());
			if(chosenFile==1 || chosenFile ==2){
				fileToSend = fileNames[chosenFile -1];
				System.out.println("FileChosen: " + fileToSend.getName() + "\n");
				fileOut.println("FileChosen: " + fileToSend.getName() + "size: " + fileToSend.length());
				break;
			}
			else{
				System.out.println("Please choice a valid number of file");
			}

		}


		/**
		 * Choose how many clients the file should be sent simultaneously
		 */
		while(true){
			System.out.println("Write how many clients the file should be sent simultaneously: ");
			System.out.println("Min: 1 - Max: " + MAX_CONNECTIONS);
			int choice = Integer.parseInt(serverIn.readLine());
			if(choice <= MAX_CONNECTIONS && choice >= 1){
				simultaneousClients = choice;
				System.out.println("simultaneousClients: " + simultaneousClients + "\n");
				break;
			}
			else{
				System.out.println("Please choice a valid number of clients");
			}
		}


		/*
		 * Server threads initialization
		 */
		System.out.println("Server threads initialization");
		threads = new ServerThread[simultaneousClients];

		for(int i=0; i<threads.length; i++){
			threads[i] = threads[i] = new ServerThread(i, socket);
			threads[i].start();
		}

		/*
		 * Connection attendance
		 */
		while (numberConnections<simultaneousClients) {

		}//end_while

		for (int i = 0; i < threads.length; i++) {
			threads[i].sendFile = true;
		}

	}
	
	/*public static void forwardPacket(DatagramPacket packet){
		for(int i=0; i<threads.length; i++){
			threads[i].clientAdress = clientAdress;
			threads[i].port = port;
			addNumberConnections();
		}
	}*/
}
