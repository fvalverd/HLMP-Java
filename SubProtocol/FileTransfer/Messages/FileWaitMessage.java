package hlmp.SubProtocol.FileTransfer.Messages;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;

import java.util.UUID;


public class FileWaitMessage extends SafeUnicastMessage {
	
	private UUID fileHandlerId;

	
	public FileWaitMessage() {
		super();
		this.type = FileTransferProtocolType.FILEWAITMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileWaitMessage(NetUser targetNetUser, UUID fileHandlerId) {
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
		return BitConverter.UUIDtoBytes(this.fileHandlerId);
	}

	@Override
	public void unPack(byte[] messagePack) {
		this.fileHandlerId = BitConverter.bytesToUUID(messagePack);
	}

	@Override
	public String toString() {
		return super.toString() + "FileWaitMessage: FileHandlerId=" + this.fileHandlerId;
	}
}