package hlmp.SubProtocol.FileTransfer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;


public class FileInformationList {

	private HashMap<UUID, FileInformation> fileInformationCollection;


	public FileInformationList() {
		this.fileInformationCollection = new HashMap<UUID, FileInformation>();
	}

	public synchronized void add(FileInformation fileInformation) {
		if (this.fileInformationCollection.containsKey(fileInformation.getId())) {
			this.fileInformationCollection.remove(fileInformation.getId());
		}
		this.fileInformationCollection.put(fileInformation.getId(), fileInformation);
	}

	public synchronized boolean remove(UUID id)
	{
		if (this.fileInformationCollection.containsKey(id)) {
			this.fileInformationCollection.remove(id);
			return true;
		}
		return false;
	}

	public synchronized FileInformation getFileInformation(UUID id) {
		return this.fileInformationCollection.get(id);
	}

	public int size() {
		return this.fileInformationCollection.size();
	}

	public synchronized FileInformation[] toArray() {
		FileInformation[] array = new FileInformation[this.size()];
		Iterator<FileInformation> iterator = this.fileInformationCollection.values().iterator();
		int i = 0;
		while (iterator.hasNext())
		{
			array[i] = iterator.next();
			i++;
		}
		return array; 
	}
}