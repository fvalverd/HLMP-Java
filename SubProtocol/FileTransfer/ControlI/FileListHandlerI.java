package hlmp.SubProtocol.FileTransfer.ControlI;

import hlmp.CommLayer.NetUser;
import hlmp.SubProtocol.FileTransfer.FileInformationList;


public interface FileListHandlerI {
	void addFileList(NetUser netUser, FileInformationList fileList);
	void removeFileList(NetUser netUser);
}