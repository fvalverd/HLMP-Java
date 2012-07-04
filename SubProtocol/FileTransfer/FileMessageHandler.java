package hlmp.SubProtocol.FileTransfer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.Message;
import hlmp.SubProtocol.FileTransfer.Constants.FileMessageHandlerState;


abstract class FileMessageHandler {
	private UUID id;
	private NetUser remoteNetUser;
	private FileInformation fileInformation;
	private int partSize;
	private long partsNumber;
	private String fileName;
	private int state;
	private String error;
	private int timeOut;
	private int maxTimeOut;
	private int type;
	protected File file;
	protected RandomAccessFile randomAccessFile;
	public FileTransferProtocol fileTransferProtocol;
	
	
	public FileMessageHandler(NetUser remoteNetUser, FileTransferProtocol fileTransferProtocol, FileInformation fileInformation, FileData fileData) {
		this.id = UUID.randomUUID();
		this.remoteNetUser = remoteNetUser;
		this.fileTransferProtocol = fileTransferProtocol;
		this.fileInformation = fileInformation;
		this.partSize = fileData.getPartSize();
		this.state = FileMessageHandlerState.WAITING;
		this.maxTimeOut = fileData.getFileTimeOut();
		resetTimeOut();
	}
	
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public NetUser getRemoteNetUser() {
		return remoteNetUser;
	}

	public void setRemoteNetUser(NetUser remoteNetUser) {
		this.remoteNetUser = remoteNetUser;
	}

	public FileInformation getFileInformation() {
		return fileInformation;
	}

	public void setFileInformation(FileInformation fileInformation) {
		this.fileInformation = fileInformation;
	}

	public int getPartSize() {
		return partSize;
	}

	public void setPartSize(int partSize) {
		this.partSize = partSize;
	}

	public long getPartsNumber() {
		return partsNumber;
	}

	public void setPartsNumber(long partsNumber) {
		this.partsNumber = partsNumber;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public int getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}

	public int getMaxTimeOut() {
		return maxTimeOut;
	}

	public void setMaxTimeOut(int maxTimeOut) {
		this.maxTimeOut = maxTimeOut;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	
	public void waitUp(int n) {
		this.timeOut = (this.timeOut + n);
		if (this.timeOut > this.maxTimeOut) {
			this.resetTimeOut();
		}
	}

	public void resetTimeOut() {
		this.timeOut = this.maxTimeOut;
	}


	public abstract void close();
	public abstract void open();
	public abstract void attendMessage(Message message);
	public abstract void execute();
	public abstract int completed();
}
