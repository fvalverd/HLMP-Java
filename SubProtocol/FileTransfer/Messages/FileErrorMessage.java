package hlmp.SubProtocol.FileTransfer.Messages;

import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;


public class FileErrorMessage extends SafeUnicastMessage {

	private UUID fileHandlerId;

	
	public FileErrorMessage() {
		super();
		this.type = FileTransferProtocolType.FILEERRORMESSAGES;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileErrorMessage(NetUser targetNetUser, UUID fileHandlerId) {
		this();
		this.targetNetUser = targetNetUser;
		this.fileHandlerId = fileHandlerId;
	}

	public UUID getFileHandlerId() {
		return fileHandlerId;
	}

	public void setFileHandlerId(UUID fileHandlerId) {
		this.fileHandlerId = fileHandlerId;
	}
	
	@Override 
	public byte[] makePack() {
		return BitConverter.UUIDtoBytes(fileHandlerId);
	}

	@Override
	public void unPack(byte[] messagePack) {
		this.fileHandlerId = BitConverter.bytesToUUID(messagePack);
	}

	@Override
	public String toString() {
		return super.toString() + "FileErrorMessage: FileHandlerId=" + this.fileHandlerId;
	}
}