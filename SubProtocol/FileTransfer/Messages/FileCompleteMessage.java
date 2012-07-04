package hlmp.SubProtocol.FileTransfer.Messages;

import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;


public class FileCompleteMessage extends SafeUnicastMessage {
	

	private UUID fileHandlerId;


	public FileCompleteMessage() {
		super();
		this.type = FileTransferProtocolType.FILECOMPLETEMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileCompleteMessage(NetUser targetNetUser, UUID fileHandlerId) {
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
		return super.toString() + "FileCompleteMessage: " + " FileHandlerId=" + this.fileHandlerId;
	}
}