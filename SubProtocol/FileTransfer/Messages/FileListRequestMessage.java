package hlmp.SubProtocol.FileTransfer.Messages;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;



public class FileListRequestMessage extends SafeUnicastMessage {

	public FileListRequestMessage() {
		super();
		this.type = FileTransferProtocolType.FILELISTREQUESTMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileListRequestMessage(NetUser targetNetUser) {
		this();
		this.targetNetUser = targetNetUser;
	}

	@Override
	public byte[] makePack() {
		return new byte[0];
	}

	@Override 
	public void unPack(byte[] messagePack) {
	}

	@Override
	public String toString() {
		return super.toString() + "FileListRequestMessage:";
	}
}