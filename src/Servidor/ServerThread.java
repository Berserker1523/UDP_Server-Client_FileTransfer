package Servidor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;

public class ServerThread extends Thread {
	private int id;
	private DatagramSocket socket;

	public InetAddress clientAddress;
	public int port;

	public boolean hello = false;
	public boolean sendFile = false;
	public boolean end = false;
	private long startFileTransferTime = 0;
	private long endFileTransferTime = 0;

	public ServerThread(int i, DatagramSocket socket) {
		this.id = i;
		this.socket = socket;
	}


	public void run() {
		try {	
			DatagramPacket packet;
			String clientSentence = "";

			while(true){

				if(!hello){
					byte[] buf = new byte[65535];
					packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
					clientSentence = new String(packet.getData(), 0, packet.getLength());

					if (!(clientSentence.length()>0)) {
						break;
					}
					else {
						String[] splitBySpace = clientSentence.split(" ");
						String command = splitBySpace[0];
						String param = "";
						if (splitBySpace.length != 1){ 
							param = splitBySpace[1];
						}

						log("IN: " + command + " " + param);

						if(command.equals("H")){
							log("Connection");
							Server.fileOut.println("Connection to client: " + id);

							String helloResponse = new String("H");
							buf = helloResponse.getBytes();

							clientAddress = packet.getAddress();
							log("CLIENT ADDRESS: " + clientAddress);
							port = packet.getPort();
							log("PORT: " + port);

							packet = new DatagramPacket(buf, buf.length, clientAddress, port);
							socket.send(packet);

							hello = true;
							Server.addNumberConnections();
						}
					}
				}
				else if(sendFile == true){
					writeFile2Client(Server.fileToSend);

					while(true){
						
						byte[] buf = new byte[65535];
						packet = new DatagramPacket(buf, buf.length);
						socket.receive(packet);
						clientAddress = packet.getAddress();
						log("CLIENT ADDRESS: " + clientAddress);
						port = packet.getPort();
						log("PORT: " + port);
						clientSentence = new String(packet.getData(), 0, packet.getLength());

						if (!(clientSentence.length()>0)) {
							break;
						}
						else {
							String[] splitBySpace = clientSentence.split(" ");
							String command = splitBySpace[0];
							String param = "";
							if (splitBySpace.length != 1){ 
								param = splitBySpace[1];
							}

							log("IN: " + command + " " + param);

							if(command.equals("R")){
								endFileTransferTime = System.currentTimeMillis();
								long transferTime = (endFileTransferTime - startFileTransferTime);
								log("The client has received the file");
								log("Transfer time: "  + transferTime + "ms");
								Server.fileOut.println("File sent succesfully to client: " + id);
								Server.fileOut.println("Transfer time of " + id + ": " + transferTime + "ms");

								File sent = new File(Server.fileToSend.getAbsolutePath());
								//Use MD5 algorithm
								MessageDigest md5Digest = MessageDigest.getInstance("MD5");

								//Get the hash
								String hash = getFileHash(md5Digest, sent);
								log("Server hash: " + hash);

								//The client hash must have the same length
								String responseHash = new String(hash);
								buf = responseHash.getBytes();
								packet = new DatagramPacket(buf, buf.length, clientAddress, port);
								socket.send(packet);
							}
							else if(command.equals("E")){
								log("Hash is equal to the received file");
								Server.fileOut.println("File integrity maintained: " + id);

							}
							else if(command.equals("W")){
								log("Hash is NOT equal to the received file");
								Server.fileOut.println("File integrity was not maintained: " + id);
							}
							else if(command.equals("X")){
								log("Ending File Transfer");
								Server.addSuccesfullFilesSent();
								break;
							}
						}
					}//break while receive requests
					break;
				}//else if -> sendFile

			}//end_while

		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	public void writeFile2Client(File file2send){
		log("Sending file to client");
		try {
			String[] fileName = file2send.getName().split("\\.");
			String sendingFileName = fileName[0] + id + "." + fileName[1];

			String fileResponse = new String("F " + sendingFileName + " " + file2send.length());
			byte[] buf = fileResponse.getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, clientAddress, port);
			socket.send(packet);
			
			int bufferSize = 2000;
			
			String numberFragmentsToSend = "" + file2send.length()/bufferSize;
			log("FRAGMENTS TO SEND: " + numberFragmentsToSend);
			buf = numberFragmentsToSend.getBytes();
			packet = new DatagramPacket(buf, buf.length, clientAddress, port);
			socket.send(packet);

			FileInputStream fis = new FileInputStream(file2send);

			int count;
			byte[] buffer = new byte[bufferSize];
			startFileTransferTime = System.currentTimeMillis();
			while ((count=fis.read(buffer)) > 0) {

				packet = new DatagramPacket(buffer, count, clientAddress, port);
				socket.send(packet);
			}

			String endFile = new String("");
			buf = endFile.getBytes();
			packet = new DatagramPacket(buf, buf.length, clientAddress, port);
			socket.send(packet);

			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void log(String wat) {
		System.out.println(id + ": " + wat);
	}

	private static String getFileHash(MessageDigest digest, File file) throws IOException
	{
		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		};

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//This bytes[] has bytes in decimal format;
		//Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		//return complete hash
		return sb.toString();
	}
}