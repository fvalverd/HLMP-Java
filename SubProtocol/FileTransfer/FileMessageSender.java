package hlmp.SubProtocol.FileTransfer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.Message;
import hlmp.SubProtocol.FileTransfer.Constants.FileMessageHandlerState;
import hlmp.SubProtocol.FileTransfer.Constants.FileMessageHandlerType;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.SubProtocol.FileTransfer.Messages.FileErrorMessage;
import hlmp.SubProtocol.FileTransfer.Messages.FilePartMessage;

class FileMessageSender extends FileMessageHandler {
	
	private long currentPart;

	/// <summary>
	/// Constructor parametrizado
	/// </summary>
	/// <param name="remoteNetUser">El usuario con quien se intercambia el archivo</param>
	/// <param name="remoteFileHandlerId">El id de la transferencia de archivo</param>
	/// <param name="sendMessageDelegate">Una funci�n por la que se pueda enviar un mensaje</param>
	/// <param name="fileInformation">Informaci�n del archivo</param>
	/// <param name="FileData">Datos de configuraci�n de archivos</param>
	public FileMessageSender(NetUser remoteNetUser, UUID remoteFileHandlerId, FileTransferProtocol fileTransferProtocol, FileInformation fileInformation, FileData fileData) {
		super(remoteNetUser, fileTransferProtocol, fileInformation, fileData);
		this.setFileName(fileInformation.getPath());
		this.currentPart = 0;
		this.setType(FileMessageHandlerType.UPLOAD);
		this.setId(remoteFileHandlerId);
	}

	@Override
	public void open() {
		this.loadFile();
	}

	@Override
	public void attendMessage(Message message) {
		if (message.getType() == FileTransferProtocolType.FILECOMPLETEMESSAGE) {
			this.setState(FileMessageHandlerState.COMPLETED);
			close();
		}
	}

	@Override
	public void execute() {
		this.sendPartMessage();
	}

	@Override
	public  void close() {
		try {
			randomAccessFile.close();
		} catch (Exception e) {
		}
	}

	@Override
	public int completed() {
		try {
			int percent = (int)(currentPart * 100 / this.getPartsNumber());
			if (percent > 100) {
				percent = 100;
			}
			return percent;
		} catch (Exception e) {
			return 0;
		}
	}

	private void loadFile() {
		try {
			this.file = new File(this.getFileName());
			this.randomAccessFile = new RandomAccessFile(this.file, "r");
			this.setPartsNumber(this.getPartsNumber(this.getFileInformation().getSize(), this.getPartSize()));
			this.setState(FileMessageHandlerState.ACTIVE);
		} catch (Exception e) {
			this.fileTransferProtocol.sendMessageEvent(new FileErrorMessage(this.getRemoteNetUser(), this.getId()));
			this.setState(FileMessageHandlerState.ERROR);
			this.setError(e.getMessage());
			this.close();
		}
	}

	public void sendPartMessage( ) {
		try {
			long pointer = currentPart * this.getPartSize();
			if (pointer < this.file.length()) {
				int dataSize;
				if (currentPart == this.getPartsNumber()-1) {
					dataSize = (int)(this.file.length() - this.getPartSize() * (this.getPartsNumber()-1));
				}
				else {
					dataSize = this.getPartSize();
				}
				byte[] fileData = new byte[dataSize];
				this.randomAccessFile.seek(pointer);
				int n = this.randomAccessFile.read(fileData, 0, fileData.length);
				while (n < fileData.length && n != 0) {
					int m = this.randomAccessFile.read(fileData, n, fileData.length - n);
					if (m == 0) {
						break;
					}
					else {
						n += m;
					}
				}
				this.fileTransferProtocol.sendMessageEvent(new FilePartMessage(this.getRemoteNetUser(), this.getId(), currentPart, fileData));
				currentPart++;
				this.setState(FileMessageHandlerState.ACTIVE);
			}
		} catch (Exception e) {
			this.fileTransferProtocol.sendMessageEvent(new FileErrorMessage(this.getRemoteNetUser(), this.getId()));
			this.setState(FileMessageHandlerState.ERROR);
			this.setError(e.getMessage());
			this.close();
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