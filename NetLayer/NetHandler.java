package hlmp.NetLayer;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import hlmp.NetLayer.Constants.NetHandlerState;
import hlmp.NetLayer.Constants.WifiConnectionState;
import hlmp.NetLayer.Interfaces.ResetIpHandler;
import hlmp.NetLayer.Interfaces.WifiHandler;
import hlmp.Tools.BitConverter;


public class NetHandler implements ResetIpHandler {

	private ServerSocket tcpListener;
	private NetHandler myself;
	
	/**
	 * thread de escucha para clientes TCP
	 */
	private Thread tcpListenerThread;
	/**
	 * Lista de servidores TCP
	 */
	private RemoteMachineList tcpServerList;
	/**
	 * Lista de servidores TCP que se deben cerrar al final
	 */
	private RemoteMachineList oldServerList;
	/**
	 * IpAddress de TCP
	 */
	//private InetAddress tcpAddress;
	/**
	 * thread de escucha de mensajes UDP
	 */
	private Thread udpClientThread;
	/**
	 * Socket para enviar mensajes UDP
	 */
	private DatagramSocket udpClient;
	/**
	 * Socket para recivir mensajes UDP
	 */
	private MulticastSocket udpServer;
	/**
	 * IpAddress UDP
	 */
	private InetAddress udpMulticastAddress;
	/**
	 * Cola de mensajes UDP Multicast leidos
	 */
	private NetMessageQueue udpMessageQueue;
	/**
	 * Cola de mensajes TCP leidos
	 */
	private NetMessageQueue tcpMessageQueue;
	/**
	 * Configuración de red
	 */
	private NetData netData;
	/**
	 * Handler de eventos de comunicación
	 */
	private CommHandlerI commHandler;
	/**
	 * Lock para conectar y desconectar
	 */
	private Object connectLock;
	/**
	 * estado de este objeto (un valor de NetHandlerState)
	 */
	private int netHandlerState;
	/**
	 * Manejador de la conexion de red inalambrica
	 */
	private WifiHandler wifiHandler;
	/**
	 * Thread de partida
	 */
	private Thread startThread;
	/**
	 * Control de stop
	 */
	private AtomicInteger stopPoint;
	/**
	 * Control de duplicación de Ip
	 */
	private IpHandler ipHandler;
	/**
	 * Variable de control para el cambio de IP
	 */
	private AtomicInteger iphandlerPoint;
	/**
	 * Thread de reset
	 */
	private Thread resetThread;
	/**
	 * lock para reset
	 */
	private Object resetLock;
	
	/**
	 * Constructor
	 * @param netData Los parámetros de configuración
	 * @param commHandler El comunicador que maneja los eventos generados en la red 
	 */
	public NetHandler(NetData netData, CommHandlerI commHandler, WifiHandler wifiHandler)
	{
		this.wifiHandler = wifiHandler;
		this.netData = netData;
		this.commHandler = commHandler;
		this.connectLock = new Object();
		this.resetLock = new Object();
		this.iphandlerPoint = new AtomicInteger(0);
		init();
		this.myself = this;
	}

	/**
	 * Inicializa las propiedades de la clase
	 */
	private void init()
	{
		//inicializa las listas
		udpMessageQueue = new NetMessageQueue();
		tcpMessageQueue = new NetMessageQueue();
		tcpServerList = new RemoteMachineList();
		oldServerList = new RemoteMachineList();
		//inicializa los objetos para TCP
		//tcpAddress = netData.getIpTcpListener();
		//tcpListenerThread = new TcpListenerThread(this, tcpAddress, netData.getTcpPort());
		tcpListenerThread = getListenTcpClientsThread();
		tcpListenerThread.setName("TCP NetHandler Main Thread");
		//inicializa los objetos UDP
//		udpMulticastAdress = InetAddress.getByName(netData.getIpUdpMulticast());
		udpClientThread = getListenUDPMessagesThread();
		udpClientThread.setName("UDP NetHandler Main Thread");
		//estado
		netHandlerState = NetHandlerState.INITIATED;
		startThread = getStartThread();
		startThread.setName("NetHandler Start Thread");
		stopPoint = new AtomicInteger(0);
		ipHandler = new IpHandler(netData, this, wifiHandler);
		resetThread = getResetThread();
		resetThread.setName("NetHandler Reset Thread");
	}

	/**
	 * Levanta los servicios, Esta función es No bloqueante, levanta un thread
	 * Esta funcion siempre gatilla startNetworkingHandler o gatilla una excepcion
	 */
	public void connect()
	{
		synchronized(connectLock)
		{
			synchronized(resetLock)
			{
				if(stopPoint.compareAndSet(0, 0))
				{
					if (netHandlerState == NetHandlerState.INITIATED || netHandlerState == NetHandlerState.STOPPED)
					{
						startThread.start();
					}
				}  
			}
		}
	}

	/**
	 * Termina los servicios, Esta función es Bloqueante hasta que termine
	 * esta funcion siempre gatilla stopNetworkingHandler
	 */
	public void disconnect()
	{
//		TODO: fvalverd parametrizar los valores 0 y 1 por CONECTADO y DESCONECTADO para stopPoint
		log("NETHANDLER: disconnect...");
		if(stopPoint.compareAndSet(0, 1))
		{
			synchronized(connectLock)
			{
				try
				{
					startThread.interrupt();
					startThread.join();
				}
				catch (Exception e)
				{
					log("NETHANDLER: disconnect aborting start " + e.getMessage());
				}
				stop(false);
				stopPoint.set(0); 
			}
		} 
		log("NETHANDLER: disconnect... OK");
	}


	/**
	 * Resetea la red
	 * @return un thread que resetea la red
	 */
	private Thread getResetThread()
	{
		return new Thread(){
			public void run(){
				log("NETHANDLER: RESET...");
				synchronized(resetLock)
				{
					commHandler.resetNetworkingHandler();
					try
					{
						startThread.interrupt();
						startThread.join();
					}
					catch (InterruptedException e)
					{
					}
					catch (Exception e)
					{
						log("NETHANDLER: error on  restart startThread");
					}

					myself.stop(true);
					connect();
				}
				log("NETHANDLER: RESET... OK");
			}
		};
		
	}
	
	/**
	 * Se gatilla para levantar los servicios
	 * Si ocurre un error se arroja
	 * @return un thread que levanta los servicios
	 */
    private Thread getStartThread(){
    	return new Thread(){

			@Override
			public void run() {
				try
	            {
	                log("NETHANDLER: start netHandler...");
	                netHandlerState = NetHandlerState.STARTING;
	                
	                Boolean connect = false;
	                int timeOutIpChange = 0;
	                while (!connect) {
	                    try {
	                    	log("NETHANDLER: connect adhoc...");
	                        wifiHandler.connect();
	                        connect = true;
	                        log("NETHANDLER: connect adhoc... OK");
	                    }
	                    catch (Exception e) {
	                    	e.printStackTrace();
	                        log("NETHANDLER: connect adhoc... FAILED! " + e.getMessage());
	                        timeOutIpChange++;
	                        if (timeOutIpChange > netData.getWaitForStart()) {
	                            throw new Exception("timeout, adhoc can't start");
	                        }
	                        Thread.sleep(netData.getWaitTimeStart());
	                    }
	                }
	                
	                while (wifiHandler.getConnectionState() == WifiConnectionState.STOP) {
	                	log("NETHANDLER: waiting request to AdHoc...");
	                	Thread.sleep(1000);
	                	log("NETHANDLER: waiting request to AdHoc...");
	                }
	                
	                // Wait for connect
	                while (wifiHandler.getConnectionState() != WifiConnectionState.CONNECTED) {
	                	log("wifiHandler.state=" + wifiHandler.getConnectionState());
	                	if (wifiHandler.getConnectionState() == WifiConnectionState.FAILED ||
	                			wifiHandler.getConnectionState() == WifiConnectionState.STOP) {
	                		netHandlerState = NetHandlerState.STOPFORCED;
	                		throw new Exception("adhoc Failed!");
	                	}
	                	Thread.sleep(1000);
	                	log("wifiHandler.state=" + wifiHandler.getConnectionState());
	                }
	                log("wifiHandler.state=" + wifiHandler.getConnectionState());
	                netData.setIpTcpListener(wifiHandler.getInetAddress());
	                
	                log("NETHANDLER: start wifi... OK");
	                
	                
	                log("NETHANDLER: start strong DAD");
	                ipHandler.startStrongDAD();
	                
	                this.startTCP();
	                this.startUDP();                
	                
	                log("NETHANDLER: start weak DAD");
	                ipHandler.chageToWeakDAD();
	                
	                commHandler.startNetworkingHandler();
	                netHandlerState = NetHandlerState.STARTED;
	                log("NETHANDLER: start netHandler... OK");
	                log("NETHANDLER: We are so connected, welcome to HLMP !!!");
	            }
	            catch (InterruptedException e) {
	            	return;
	            }
	            catch (Exception e) {
	            	log("NETHANDLER: start netHandler... failed! " + e.getMessage());
	                commHandler.errorNetworkingHandler(e);
	            }
			}

			private void startTCP() throws Exception {
				log("NETHANDLER: start TCP...");
                Boolean tcpChange = false;
                int timeOutTcpChange = 0;
                while (!tcpChange) {
                    try {
                        log("NETHANDLER: start TCP listener... " + netData.getIpTcpListener().getHostAddress() + ":" + netData.getTcpPort());
                        tcpListener = new ServerSocket();
                        tcpListener.setReuseAddress(true);
                        tcpListener.bind(new InetSocketAddress(netData.getIpTcpListener(),netData.getTcpPort()));
                        tcpChange = true;
                        log("NETHANDLER: start TCP listener... OK");
                    }
                    catch (Exception e) {
                    	e.printStackTrace();
                    	log("NETHANDLER: start TCP listener... failed! " + e.getMessage());
                        timeOutTcpChange++;
                        if (timeOutTcpChange > netData.getWaitForStart()) {
                            throw new Exception("timeout, para levantar servicio TCP");
                        }
                        else {
                            Thread.sleep(netData.getWaitTimeStart());
                        }
                    }
                }
                tcpListenerThread.start();
                log("NETHANDLER: start TCP... OK");				
			}
			
			private void startUDP() throws Exception {
				log("NETHANDLER: start UDP...");
                log("NETHANDLER: start UDP... " + netData.getIpUdpMulticast() + ":" + netData.getUdpPort());
                udpMulticastAddress = InetAddress.getByName(netData.getIpUdpMulticast());
                udpServer = new MulticastSocket(netData.getUdpPort());
                udpServer.setBroadcast(true);
                udpServer.setReuseAddress(true);
                udpClient = new DatagramSocket();
                udpClient.setBroadcast(true);
                udpClient.setReuseAddress(true);
                udpServer.joinGroup(udpMulticastAddress);
                udpClientThread.start();
                log("NETHANDLER: start UDP... OK");				
			}
    		
    	};
    }

    /**
     * Se gatilla para terminar los servicios
     * Si ocurre algun error se informa en informationNetworkingHandler, no se detiene ejecución
     */
    private void stop(boolean reset){
    	log("NETHANDLER: stop netHandler...");
        netHandlerState = NetHandlerState.STOPPING;
        try
        {
            log("NETHANDLER: stop DAD...");
        	ipHandler.stop();
            log("NETHANDLER: stop DAD... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: stop DAD... failed! " + e.getMessage());
        }
        try
        {
            log("NETHANDLER: stop communication...");
        	commHandler.stopNetworkingHandler();
            log("NETHANDLER: stop communication... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: stop communication... failed! " + e.getMessage());
        }
        
        // Close UDP  
        try
        {
            log("NETHANDLER: drop multicast suscription...");
            udpServer.leaveGroup(udpMulticastAddress);
            log("NETHANDLER: drop multicast suscription... OK");
        }
        catch (Exception e)
        {
            log("NETHANDLER: drop multicast suscription... failed! " + e.getMessage());
        }
        try
        {
        	log("NETHANDLER: shutdown UDP client...");
            udpClient.close();
            log("NETHANDLER: shutdown UDP client... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: shutdown UDP client... failed! " + e.getMessage());
        }
        try
        {

        	log("NETHANDLER: stop UDP thread...");
            udpClientThread.interrupt();
            try
        	{
            	log("NETHANDLER: close UDP socket...");
				udpServer.close();
				log("NETHANDLER: close UDP socket... OK");
	        }
	        catch (Exception e)
	        {
	        	log("NETHANDLER: stop UDP thread... failed! " + e.getMessage());
	        }
            udpClientThread.join();
            log("NETHANDLER: stop UDP thread... OK");
            
        }
        catch (Exception e)
        {
        	log("NETHANDLER: stop UDP thread... failed! " + e.getMessage());
        }

        // Close TCP
        try
        {
        	log("NETHANDLER: kill TCP links...");
            RemoteMachine[] serverRemoteMachines = tcpServerList.toObjectArray();
            for (int i = 0; i < serverRemoteMachines.length; i++)
            {
                try
                {
                	log("NETHANDLER: kill TCP link... " + serverRemoteMachines[i].getIp().getHostAddress());
                    killRemoteMachine(serverRemoteMachines[i]);
                    log("NETHANDLER: kill TCP link... OK");
                }
                catch (Exception e)
                {
                	log("NETHANDLER: kill TCP link... failed! " + e.getMessage());
                }
            }
            log("NETHANDLER: kill TCP links... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: kill TCP links... failed! " + e.getMessage());
        }
        
        try
        {
        	log("NETHANDLER: kill TCP links..." + "(TCP is hard to kill)");
            RemoteMachine[] serverRemoteMachines = oldServerList.toObjectArray();
            for (int i = 0; i < serverRemoteMachines.length; i++)
            {
                try
                {
                	log("NETHANDLER: kill TCP link... " + serverRemoteMachines[i].getIp().getHostAddress());
                    killRemoteMachine(serverRemoteMachines[i]);
                    log("NETHANDLER: kill TCP link... OK");
                }
                catch (Exception e)
                {
                	log("NETHANDLER: kill TCP link... failed! " + e.getMessage());
                }
            }
            log("NETHANDLER: kill TCP links... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: kill TCP links... failed! " + e.getMessage());
        }
        
        try
        {
        	log("NETHANDLER: stop TCP listener...");
            tcpListener.close();
            log("NETHANDLER: stop TCP listener... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: stop TCP listener... failed! " + e.getMessage());
        }
        try
        {
        	log("NETHANDLER: stop TCP thread...");
            tcpListenerThread.interrupt();
            tcpListenerThread.join();
            log("NETHANDLER: stop TCP thread... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: stop TCP thread... failed! " + e.getMessage());
        }
        
        
        // Restart Objects
        try
        {
        	log("NETHANDLER: restart objects...");
            init();
            log("NETHANDLER: restart... OK");
        }
        catch (Exception e)
        {
        	log("NETHANDLER: initialation of objects... failed! " + e.getMessage());
        }
        netHandlerState = NetHandlerState.STOPPED;
        if(!reset)
        {
        	try
        	{
        		log("NETHANDLER: stop adhoc...");
        		wifiHandler.disconnect();
        		log("NETHANDLER: stop adhoc... OK");
        	}
        	catch (Exception e)
        	{
        		log("NETHANDLER: stop adhoc... failed!" + e.getMessage());
        	}
        }
        log("NETHANDLER: stop netHandler... OK");
        log("NETHANDLER: bye bye!");
    }

    /**
     * Envia el mensaje por TCP al usuario indicado
     * @param netMessage mensage a enviar
     * @param ip direccion del usuario
     * @return true si se envio correctamente, false si no
     */
	public boolean sendTcpMessage(NetMessage netMessage, InetAddress ip){
		try
		{
			RemoteMachine remoteMachine = tcpServerList.getRemoteMachine(ip); 
			if (remoteMachine != null)
			{
				try
				{
					remoteMachine.sendNetMessage(netMessage, netData.getTimeOutWriteTCP());
				}
				catch (Exception e)
				{
					log("TCP WARNING 1: send failed " + e.getMessage());
					if (remoteMachine.getFails() >= netData.getSendFailsToDisconnect())
					{
						disconnectFrom(remoteMachine);
					}
					return false;
				}
				return true;
			}
			else
			{
				throw new Exception("There is no TCP link with that remote machine");
			}
		}
//		catch (InterruptedException e)
//		{
//			throw e;
//		}
		catch (Exception e)
		{
			log("TCP WARNING 2: send failed " + e.getMessage());
			return false;
		}
	}

	/**
	 * Envía un mensaje TCP a todas las máquinas remotas visibles
	 * Si ocurre algun error se arroja
	 * @param message el mensaje a envíar
	 */
	public void sendTcpMessage(NetMessage message) throws InterruptedException{
		RemoteMachine[] serverRemoteMachines = tcpServerList.toObjectArray();
		for (int i = 0; i < serverRemoteMachines.length; i++)
		{
			sendTcpMessage(message, serverRemoteMachines[i].getIp());
		}
	}

	/**
	 * Envia un mensaje UDP a todas las maquinas remotas visibles
	 * Si ocurre un error se informa
	 * @param message El mensaje a envíar
	 */
	public boolean sendUdpMessage(NetMessage message){
		try
		{
			byte[] lenght =  BitConverter.intToByteArray(message.getSize());
			byte[] netByteMessage = new byte[4 + message.getSize()];
			System.arraycopy(lenght, 0, netByteMessage, 0, 4);
			System.arraycopy(message.getBody(), 0, netByteMessage, 4, message.getSize());
			DatagramPacket packet = new DatagramPacket(netByteMessage, netByteMessage.length, udpMulticastAddress, netData.getUdpPort());
			packet.setLength(netByteMessage.length);
			udpClient.send(packet);
			return true;
		}
		//        catch (ThreadAbortException e)
		//        {
		//            throw e;
		//        }
		catch (Exception e)
		{
			log("UDP WARNING: send failed " + e.getMessage());
			return false;
		}
	}

	/**
	 * Envia un mensaje UDP a todas las maquinas remotas visibles
	 * Si ocurre un error se informa
	 * @param message El mensaje a envíar
	 * @param ip la direccion IP a la cual enviar el mensaje
	 */
	public boolean sendUdpMessage(NetMessage message, InetAddress ip){
		try
		{
			byte[] lenght = BitConverter.intToByteArray(message.getSize());
			byte[] netByteMessage = new byte[4 + message.getSize()];
			System.arraycopy(lenght, 0, netByteMessage, 0, 4);
			System.arraycopy(message.getBody(), 0, netByteMessage, 4, message.getSize());
			
			DatagramPacket packet = new DatagramPacket(netByteMessage, netByteMessage.length, ip, netData.getUdpPort());
			packet.setLength(netByteMessage.length);
			udpClient.send(packet);
			return true;
		}
		//        catch (ThreadAbortException e)
		//        {
		//            throw e;
		//        }
		//        catch (SocketException e)
		//        {
		//            debug("UDP WARNING: send failed ErrorCode=" + e.ErrorCode);
		//            return false;
		//        }
		catch (Exception e)
		{
			log("UDP WARNING: send failed " + e.getMessage());
			return false;
		}
	}

	/**
	 * Se conecta a una máquina remota por TCP para enviarle mensajes posteriormente
	 * Si ocurre un error se notifica en informationNetworkingHandler
	 * @param serverIp La dirección IP de la máquina remota, debe ser un String
	 */
	public void connectTo(String serverIp)
	{
		connectToAsync(serverIp);
	}

	/**
	 * Se conecta a una máquina remota por TCP para enviarle mensajes posteriormente
	 * Si ocurre un error se notifica en informationNetworkingHandler
	 * @param o La dirección IP de la máquina remota en formato IPAddress
	 */
	private void connectToAsync(Object o)
	{
		try
		{
			log("TCP: connection...");
			InetAddress serverIp = null;
			if (o.getClass().equals(InetAddress.class))
			{
				serverIp = (InetAddress) o;
			}
			else
			{
				serverIp = InetAddress.getByName((String) o);
			}

			Socket tcpClient = new Socket();
			tcpClient.setSoTimeout(0);

			try
			{
				InetSocketAddress isa = new InetSocketAddress(serverIp, netData.getTcpPort());
				tcpClient.connect(isa, netData.getTcpConnectTimeOut());
			}
			catch(SocketTimeoutException x)
			{
				log("TCP: connection... time out!");
				return;
			}

			ListenTCPMessagesThread clientThread = new ListenTCPMessagesThread(this);
			clientThread.setName("ListenTCPMessagesThread_" + serverIp.getHostAddress());
			RemoteMachine remoteMachine = new RemoteMachine(serverIp, tcpClient, clientThread);
			clientThread.setRemoteMachine(remoteMachine);
			clientThread.start();
			
			RemoteMachine oldRemoteMachine = tcpServerList.getRemoteMachine(serverIp);
			if (oldRemoteMachine != null)
			{
				tcpServerList.remove(oldRemoteMachine);
			}
			tcpServerList.add(serverIp, remoteMachine);
			log("TCP: connection... OK");
		}
		catch (Exception e)
		{
			log("TCP: connection... failed! " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Desconecta los servicios TCP asociados a una maquina remota
	 * @param machineIp la Ip de la maquina a desconectar debe ser un String
	 */
	public void disconnectFrom(InetAddress machineIp)
	{
		//disconnectFromAsync(machineIp);
		killRemoteMachine(machineIp);
	}

	/**
	 * Desconecta los servicios TCP asociados a una maquina remota
	 * @param machine la maquina a desconectar debe ser un String
	 */
	public void disconnectFrom(RemoteMachine machine)
	{
		//disconnectFromAsync(machine);
		killRemoteMachine(machine);
	}

	/**
	 * Desconecta los servicios TCP asociados a una maquina remota
	 * @param o La ip de la maquina remota a desconectar en formato de String o la maquina remota
	 */
	@SuppressWarnings("unused")
	private void disconnectFromAsync(Object o)
	{
		try
		{
			log("TCP: disconnection...");
			if(o.getClass().equals(InetAddress.class))
			{
				InetAddress machineIp = (InetAddress)o;
				RemoteMachine machine = tcpServerList.getRemoteMachine(machineIp);
				log("TCP: old list queue");
				tcpServerList.remove(machine);
				oldServerList.add(machine.getIp(), machine);
			}
			else if (o.getClass().equals(RemoteMachine.class))
			{
				RemoteMachine machine = (RemoteMachine)o;
				log("TCP: old list queue");
				tcpServerList.remove(machine);
				oldServerList.add(machine.getIp(), machine);
			}
			log("TCP: disconnection... OK");
		}
//		catch (InterruptedException e)
//		{
//			throw e;
//		}
		catch (Exception e)
		{
			log("TCP: disconnection... failed! " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Desconecta los servicios TCP asociados a una maquina remota
	 * @param o La ip de la maquina remota a desconectar en formato de String o la maquina remota
	 */
	private void killRemoteMachine(Object o)
	{
		try
		{
			log("TCP: kill...");
			if(o.getClass().equals(InetAddress.class))
			{
				InetAddress machineIp = (InetAddress)o;
				RemoteMachine machine = tcpServerList.getRemoteMachine(machineIp);
				log("TCP: close machine");
				machine.close();
				log("TCP: drop from queue");
				tcpServerList.remove(machine);
			}
			else if (o.getClass().equals(RemoteMachine.class))
			{
				RemoteMachine machine = (RemoteMachine)o;
				log("TCP: close machine");
				machine.close();
				log("TCP: drop from queue");
				tcpServerList.remove(machine);
			}
			log("TCP: kill... OK");
		}
//		catch (InterruptedException e)
//		{
//			throw e;
//		}
		catch (Exception e)
		{
			log("TCP: kill... failed! " + e.getMessage());
		}
	}

	/**
	 * Agrega mensajes TCP recibidos a la cola
	 * @param message el mensaje recibido
	 */
	public void addTCPMessages(NetMessage message)
	{
			// TODO: FVALVERD PARAMETRIZAR el 50
			if (tcpMessageQueue.size() < 50)
			{
				tcpMessageQueue.put(message);
			}
			else
			{
				log("TCP WARNING: TCP message dropped");
			}
	} 

	/**
	 * Los datos de red
	 * @return
	 */
	public NetData getNetData()
	{
		return netData;
	}

	/**
	 * Lista de maquinas de la red adhoc que son directamente visibles para esta máquina.
	 * Se posee una conexión TCP directa con cada una de ellas.
	 * @return
	 */
	public RemoteMachineList getTcpServerList() {
		return tcpServerList;
	}

	/**
	 * Cola de mensajes UDP que ha recibido esta máquina
	 * @return
	 */
	public NetMessageQueue getUdpMessageQueue() {
		return udpMessageQueue;
	}

	/**
	 * Cola de mensajes TCP que ha recibido esta máquina
	 * @return
	 */
	public NetMessageQueue getTcpMessageQueue() {
		return tcpMessageQueue;
	}

	/**
	 * Registra una ip externa para el chequeo de ip duplicada
	 * @param ip la ip a registrar
	 */
	public void registerIp(String ip)
	{
		ipHandler.put(ip);
	}

	
	private void log(String text) {
		commHandler.informationNetworkingHandler(text);
	}
	
	public void resetIp() {
		if (iphandlerPoint.compareAndSet(0, 1)) {
			try {
				resetThread.start();
			}
			catch (Exception e) {
			}
			iphandlerPoint.set(0);
		}
	}
	
	private Thread getListenTcpClientsThread(){
		return new Thread(){
			@Override
			public void run() {
				try {
		            while (true) {
		            	log("TCP: accepting client ...");
		                Socket tcpClient = tcpListener.accept();
		                tcpClient.setSoTimeout(0);
		                log("TCP: new client detected");
		                
		                InetAddress ip = tcpClient.getInetAddress();
		                
		                ListenTCPMessagesThread clientThread = new ListenTCPMessagesThread(myself);
		                clientThread.setName("ListenTCPMessages_" + ip.getHostAddress());
		                RemoteMachine remoteMachine = new RemoteMachine(ip, tcpClient, clientThread);
		                clientThread.setRemoteMachine(remoteMachine);
		                clientThread.start();
		                
		                RemoteMachine oldRemoteMachine = tcpServerList.getRemoteMachine(ip);
		                if (oldRemoteMachine != null)
		                {
		                    tcpServerList.remove(oldRemoteMachine);
		                }
		                tcpServerList.add(ip, remoteMachine);
		                log("TCP: new client connected");
		            }
		        }
		        catch (SocketException e) {
		            return;
		        }
		        catch (Exception e) {
		        	// TODO: FVALVERD reiniciar la conexion
		        	log("TCP WARNING: TCP listener has stopped!! " + e.getMessage());
		        }
				
			}
		};
	}
	
	private Thread getListenUDPMessagesThread(){
		return new Thread(){

			@Override
			public void run() {
				try
	            {
	                while (true)
	                {
	                	byte[] buf = new byte[1000];
	                	DatagramPacket packet = new DatagramPacket(buf, buf.length);
	                	udpServer.receive(packet);
	                    byte[] buffer = packet.getData();
	                    if (buffer.length > 4)
	                    {
	                    	byte[] arraySize = new byte[4];
	                    	System.arraycopy(buffer, 0, arraySize, 0, 4);
	                    	int size = BitConverter.byteArrayToInt(arraySize);
	                        if (buffer.length >= 4 + size)
	                        {
	                            byte[] body = new byte[size];
	                            System.arraycopy(buffer, 4, body, 0, size);
	                            NetMessage message = new NetMessage(body);
	                            udpMessageQueue.put(message);
	                        }else{
	                        	// agregado por NM
		                    	log("UDP WARNING: upd receive wrong message, bad length");
	                        }
	                    }else{
	                    	// agregado por NM
	                    	log("UDP WARNING: upd receive wrong message, lenght less than 4");
	                    }
	                }
	            }
	            catch (Exception e)
	            {
	            	if (!this.isInterrupted()) {
	            		log("UDP WARNING: udp client has stopped!!! " + e.getMessage());
	            	}
	            }
			}
	
		};
	}

	public void informationNetworkingHandler(String message){
		log("NETHANDLER: " + message);
	}
}
