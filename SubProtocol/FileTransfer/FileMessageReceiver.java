package hlmp.SubProtocol.FileTransfer;

import java.io.File;
import java.io.RandomAccessFile;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.Message;
import hlmp.SubProtocol.FileTransfer.Constants.FileMessageHandlerState;
import hlmp.SubProtocol.FileTransfer.Constants.FileMessageHandlerType;
import hlmp.SubProtocol.FileTransfer.Constants.FilePartStatus;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.SubProtocol.FileTransfer.Messages.FileCompleteMessage;
import hlmp.SubProtocol.FileTransfer.Messages.FileErrorMessage;
import hlmp.SubProtocol.FileTransfer.Messages.FilePartMessage;
import hlmp.SubProtocol.FileTransfer.Messages.FileRequestMessage;


class FileMessageReceiver extends FileMessageHandler {
	
	private FilePartIndicator[] parts;
	private int partsLoaded;
	private String downloadDir;

	/// <summary>
	/// Constructor Parametrizado
	/// </summary>
	/// <param name="remoteNetUser">El usuario remoto con el cual se intercambiar� el archivo</param>
	/// <param name="sendMessageDelegate">Un m�todo con el cual se puedan env�ar mensajes a la MANET</param>
	/// <param name="fileInformation">La informaci�n del archivo</param>
	/// <param name="FileData">Los par�metros de configuraci�n</param>
	public FileMessageReceiver(NetUser remoteNetUser, FileTransferProtocol fileTransferProtocol, FileInformation fileInformation, FileData fileData) {
		super(remoteNetUser, fileTransferProtocol, fileInformation, fileData);
		this.setType(FileMessageHandlerType.DOWNLOAD);
		this.downloadDir = fileData.getDownloadDir();
	}

	@Override
	public void open() {
		this.createFile();
		FileRequestMessage fileRequestMessage = new FileRequestMessage(this.getRemoteNetUser(), this.getFileInformation().getId(), this.getId());
		this.fileTransferProtocol.sendMessageEvent(fileRequestMessage);
	}

	@Override
	public void attendMessage(Message message) {
		if (message.getType() == FileTransferProtocolType.FILEPARTMESSAGE) {
			FilePartMessage filePartMessage = (FilePartMessage)message;
			receivePartMessage(filePartMessage);
		}
	}

	public void execute() {
	}

	@Override
	public void close() {
		try {
			this.randomAccessFile.close();
		} catch (Exception e) {
		} 
	}

	@Override
	public int completed() {
		try {
			int percent = (int)(partsLoaded * 100 / this.getPartsNumber());
			if (percent > 100) {
				percent = 100;
			}
			return percent;
		} catch (Exception e) {
			return 0;
		}
	}

	private synchronized void createFile() {
		try {
			this.setPartsNumber(this.getPartsNumber(this.getFileInformation().getSize(), this.getPartSize()));
			parts = new FilePartIndicator[(int)this.getPartsNumber()];
			for (int i = 0; i < parts.length; i++) {
				parts[i] = new FilePartIndicator();
			}

			//crea e inicializa el archivo temporal
			// TODO: Esto lo debe hacer el sistema operativo
			boolean exists = true;
			int j = 0;
			while (exists) {
				this.setFileName(downloadDir + "/" + this.getRemoteNetUser().getName() + "." + j + "." + this.getFileInformation().getName());
				this.file = new File(this.getFileName());
				exists = this.file.exists();
				j++;
			}
			long currentPart = 0;
			long pointer = currentPart * this.getPartSize();
			this.randomAccessFile = new RandomAccessFile(this.file, "rw");
			while (pointer < this.getFileInformation().getSize()) {
				int dataSize;
				if (currentPart == this.getPartsNumber()-1) {
					dataSize = (int)(this.getFileInformation().getSize() - this.getPartSize() * (this.getPartsNumber()-1));
				}
				else {
					dataSize = this.getPartSize();
				}
				byte[] fileData = new byte[dataSize];
				this.randomAccessFile.seek(pointer);
				this.randomAccessFile.write(fileData);
				currentPart++;
				pointer = currentPart * this.getPartSize();
			}
			partsLoaded = 0;
			if (this.getFileInformation().getSize() <= 0) {
				this.setState(FileMessageHandlerState.COMPLETED);
			}
			else {
				this.setState(FileMessageHandlerState.OPEN);
			}
		} catch (Exception e) {
			this.setState(FileMessageHandlerState.ERROR);
			this.setError(e.getMessage());
			this.close();
		} 
	}

	private synchronized void receivePartMessage(FilePartMessage message) {
		try {
			long pointer = message.getPartId() * this.getPartSize();
			if (pointer < this.getFileInformation().getSize() && parts[(int)message.getPartId()].getStatus() == FilePartStatus.NOTRECEIVED) {
				this.randomAccessFile.seek(pointer);
				this.randomAccessFile.write(message.getFilePart());
				parts[(int)message.getPartId()].setStatus(FilePartStatus.RECEIVED);
				partsLoaded++;
			}

			boolean completed = true;
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].getStatus() == FilePartStatus.NOTRECEIVED) {
					completed = false;
					break;
				}
			}
			if (completed) {
				this.fileTransferProtocol.sendMessageEvent(new FileCompleteMessage(this.getRemoteNetUser(), this.getId()));
				this.setState(FileMessageHandlerState.COMPLETED);
			}
			else {
				this.setState(FileMessageHandlerState.ACTIVE);
			}
		} catch (Exception e) {
			this.fileTransferProtocol.sendMessageEvent(new FileErrorMessage(this.getRemoteNetUser(), this.getId()));
			this.setState(FileMessageHandlerState.ERROR);
			this.setError(e.getMessage());
			close();
		}
	}

	private long getPartsNumber(long fileSize, int partSize) {
		long partsNumber = 0;

		long pointer = 0;
		while (pointer < fileSize) {
			partsNumber++;
			pointer += partSize;
		}
		return partsNumber;
	}
}