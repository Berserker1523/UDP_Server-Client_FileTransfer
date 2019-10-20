package Cliente;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cliente {

	private final static String SERVER_ADDRESS = "localhost";
	private static InetAddress address = null;

	public static void main(String argv[]) throws Exception {
		boolean termino =false;

		DatagramSocket datagramClientSocket = new DatagramSocket();
		address = InetAddress.getByName(SERVER_ADDRESS);

		String hello = new String("H");
		byte[] buf = hello.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
		System.out.println("TO SERVER: Hello");
		datagramClientSocket.send(packet);

		packet = new DatagramPacket(buf, buf.length);
		datagramClientSocket.receive(packet);
		String received = new String(packet.getData(), 0, packet.getLength());
		System.out.println("FROM SERVER: " + received);


		while(!termino){
			buf = new byte[65000];
			packet = new DatagramPacket(buf, buf.length);
			datagramClientSocket.receive(packet);
			received = new String(packet.getData(), 0, packet.getLength());
			
			String[] split = received.split(" ");
			String serverCommand = split[0];
			String param1 = " ";
			String param2 = " ";

			if(split.length > 1){
				param1=split[1];
				if(split.length > 2){
					param2=split[2];
				}
			}
			System.out.println("SERVER COMMAND: " + serverCommand);
			System.out.println("PARAMS1: " + param1 + " PARAMS2: " +param2);


			switch(serverCommand) {
			case "F":
				saveFile(datagramClientSocket, param1, param2);
				termino = true;
				break;
			}
		}
		datagramClientSocket.close();
	}


	private static void saveFile(DatagramSocket clientSock, String nameFile, String size) throws IOException, NoSuchAlgorithmException {

		FileOutputStream fos = new FileOutputStream( new File("./downloads/"+ nameFile));
		System.out.println("START SAVE FILE");

		byte[] buffer = new byte[65535];

		int filesize = Integer.parseInt(size); // Send file size in separate msg

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		clientSock.receive(packet);
		int numberFragmentsToReceive = Integer.valueOf((new String(packet.getData())).trim());
		System.out.println("NUMBER OF PACKETS TO RECEIVE: " + numberFragmentsToReceive);

		packet = new DatagramPacket(buffer, buffer.length);
		clientSock.receive(packet);
		numberFragmentsToReceive--;

		int read = 0;
		int totalRead = 0;
		int remaining = filesize;
		while((read = packet.getLength()) > 0 && remaining > 0 && numberFragmentsToReceive > 0) {
			totalRead += read;
			remaining -= read;
			//System.out.println("READ: " + totalRead + " bytes.");
			fos.write(packet.getData(), 0, packet.getLength());
			packet = new DatagramPacket(buffer, buffer.length);
			clientSock.receive(packet);
			numberFragmentsToReceive--;
			/*System.out.println("NUMBER OF PACKETS TO RECEIVE: " + numberFragmentsToReceive);
			System.out.println((read = packet.getLength()) > 0);
			System.out.println(remaining > 0);
			System.out.println(numberFragmentsToReceive > 0);*/
		}


		System.out.println("DONE FILE TRANSFER ");

		if(read <= 0)
			System.out.println("BY: Packet length equal to 0");
		
		
		if(remaining <= 0)
			System.out.println("BY: File reamining equalt to 0");
		
		if(numberFragmentsToReceive <= 0)
			System.out.println("BY: Number of fragments to receive equal to 0");

		String received = new String("R");
		byte[] buf = received.getBytes();
		packet = new DatagramPacket(buf, buf.length, address, 4445);
		clientSock.send(packet);
		System.out.println("TO SERVER: Received");

		packet = new DatagramPacket(buffer, buffer.length);
		clientSock.receive(packet);
		String serverHash = new String(packet.getData(), 0, packet.getLength());
		System.out.println("SERVER HASH: " + serverHash);

		File receipt = new File("./downloads/"+ nameFile);
		//Use MD5 algorithm
		MessageDigest md5Digest = MessageDigest.getInstance("MD5");

		//Get the checksum
		String checksum = getFileChecksum(md5Digest, receipt);
		System.out.println("CLIENT HASH: " + checksum);

		if(checksum.equals(serverHash)){
			System.out.println("The server and client hashes are equal");

			String hashEqual = new String("E");
			buf = hashEqual.getBytes();
			packet = new DatagramPacket(buf, buf.length, address, 4445);
			clientSock.send(packet);
			System.out.println("TO SERVER: hashEqual");
		}
		else{

			System.out.println("The server and client hashes are not equal");
			String hashNotEqual = new String("W");
			buf = hashNotEqual.getBytes();
			packet = new DatagramPacket(buf, buf.length, address, 4445);
			clientSock.send(packet);
			System.out.println("TO SERVER: hashWrong");

		}

		String end = new String("X");
		buf = end.getBytes();
		packet = new DatagramPacket(buf, buf.length, address, 4445);
		clientSock.send(packet);
		System.out.println("TO SERVER: End");

		fos.close();
	}

	private static String getFileChecksum(MessageDigest digest, File file) throws IOException
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