package common.system.fake;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import common.system.files.FileData;
import common.system.files.VFile;
import common.util.Data;

/**
 * CPU画像とOpenGL画像を同じ呼び出し側から扱うための画像抽象。
 * {@link #bimg()}と{@link #gl()}のどちらが利用可能かは描画基盤の実装に依存する。
 */
public interface FakeImage {

	/**
	 * 遅延読み込み時の画像表現や再着色処理を選択するための用途マーカー。
	 */
	enum Marker {
		BG, EDI, UNI, RECOLOR, RECOLORED
	}

	static FakeImage read(byte[] bs) {
		return read(() -> new ByteArrayInputStream(bs));
	}

	static FakeImage read(File f) {
		return Data.err(() -> ImageBuilder.builder.build(f));
	}

	static FakeImage read(FileData fd) {
		return read(fd::getStream);
	}

	static FakeImage read(Supplier<InputStream> sup) {
		return Data.err(() -> ImageBuilder.builder.build(sup));
	}

	static FakeImage read(VFile vf) {
		return read(vf.getData());
	}

	static boolean write(FakeImage img, String str, Object o) throws IOException {
		return ImageBuilder.builder.write(img, str, o);
	}

	Object bimg();

	int getHeight();

	int getRGB(int i, int j);

	FakeImage getSubimage(int i, int j, int k, int l);

	int getWidth();

	Object gl();

	boolean isValid();

	default void mark(Marker m) {
	}

	void setRGB(int i, int j, int p);

	void unload();

	FakeImage cloneImage();

	FakeGraphics getGraphics();
}
