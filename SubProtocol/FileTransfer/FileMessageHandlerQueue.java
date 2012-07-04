package hlmp.SubProtocol.FileTransfer;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


class FileMessageHandlerQueue {

	private ConcurrentLinkedQueue<FileMessageHandler> queue;


	public FileMessageHandlerQueue() {
		queue = new ConcurrentLinkedQueue<FileMessageHandler>();
	}

	public synchronized FileMessageHandler draw() throws InterruptedException {
		if (queue.isEmpty()) {
			return null;
		}
		return queue.poll();
	}

	public synchronized boolean contains(UUID id) {
		return queue.contains(id);
	}

	public synchronized boolean put(FileMessageHandler fileMessageHandler) {
		if (!this.contains(fileMessageHandler.getId())) {
			queue.add(fileMessageHandler);
			return true;
		}
		return false;
	}

	public synchronized int size() {
		return queue.size();
	}

	public synchronized FileMessageHandler[] toArray() {
		FileMessageHandler[] a = new FileMessageHandler[queue.size()];
		return queue.toArray(a);
	}
}