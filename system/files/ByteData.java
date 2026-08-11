package common.system.files;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import common.system.fake.FakeImage;

/**
 * バイト配列を実体とする{@link FileData}向けに、画像・ストリーム・サイズへの変換を補う。
 */
interface ByteData extends FileData {

	@Override
	byte[] getBytes();

	@Override
	default FakeImage getImg() {
		return FakeImage.read(getBytes());
	}

	@Override
	default InputStream getStream() {
		return new ByteArrayInputStream(getBytes());
	}

	@Override
	default int size() {
		return getBytes().length;
	}

}