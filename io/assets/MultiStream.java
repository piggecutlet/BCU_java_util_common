package common.io.assets;

import common.CommonStatic;
import common.pack.Context.ErrType;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 同一ファイルに対する複数の論理読取位置を、独立ストリームまたは共有{@link RandomAccessFile}で提供する。
 * 共有モードは位置キャッシュを使う同期化されていない実装で、論理サブストリームのcloseは共有ハンドルを閉じない。
 */
public class MultiStream {

	/**
	 * 指定範囲への読取りと終了だけを公開する、パック復号用の最小ストリーム契約。
	 */
	public interface ByteStream {

		void close() throws IOException;

		void read(byte[] bs, int i, int rlen) throws IOException;

	}

	/**
	 * 呼び出しごとに独立した{@link FileInputStream}を所有する実装。
	 */
	public static class TrueStream implements ByteStream {

		private final FileInputStream fis;

		public TrueStream(File f, int pos) throws IOException {
			fis = new FileInputStream(f);
			int skip = (int) fis.skip(pos);
			if (skip != pos)
				throw new IOException("failed to skip bytes");
		}

		@Override
		public void close() throws IOException {
			fis.close();
		}

		@Override
		public void read(byte[] bs, int off, int len) throws IOException {
			fis.read(bs, off, len);
		}

	}

	private interface RunExc {

		void run() throws IOException;

	}

	/**
	 * 共有ファイルハンドル上に独自の論理位置だけを持つビュー。
	 */
	private class SubStream implements ByteStream {

		private int pos;

		private SubStream(int pos) {
			this.pos = pos;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public void read(byte[] bs, int off, int len) throws IOException {
			attempt(() -> readBytes(pos, bs, off, len));
			pos += bs.length;
		}

	}

	private static final Map<File, MultiStream> MAP = new HashMap<>();

	public static ByteStream getStream(File f, int pos, boolean useRAF) throws IOException {
		if (!useRAF) {
			return new TrueStream(f, pos);
		}
		MultiStream ms = MAP.get(f);
		if (ms == null)
			MAP.put(f, ms = new MultiStream(f));
		return ms.new SubStream(pos);
	}

	private final File file;
	private RandomAccessFile raf;
	private long poscache = -1;

	private MultiStream(File f) {
		file = f;
	}

	public void close() throws IOException {
		if (raf == null)
			return;
		poscache = -1;
		raf.close();
		raf = null;
	}

	private RandomAccessFile access() throws FileNotFoundException {
		if (raf != null)
			return raf;
		poscache = -1;
		raf = new RandomAccessFile(file, "r");
		return raf;
	}

	private void attempt(RunExc r) throws IOException {
		try {
			r.run();
			return;
		} catch (IOException e) {
			CommonStatic.ctx.printErr(ErrType.INFO, "failed to read, attempted again");
			close();
		}
		r.run();
		CommonStatic.ctx.printErr(ErrType.INFO, "attempt succeed");
	}

	private void readBytes(int pos, byte[] arr, int off, int len) throws IOException {
		access();
		if (poscache == -1)
			poscache = raf.getFilePointer();
		if (pos != poscache) {
			raf.seek(pos);
			poscache = pos;
		}
		raf.read(arr, off, len);
		poscache += len;
	}

}
