package common.system;

import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;

/**
 * 複数画像を横一列に並べ、指定点を基準に拡大縮小して描画する座標ヘルパー。
 * {@link #type}のビット0は右端基準、ビット1は下端基準を表す。
 */
public class SymCoord {

	public FakeGraphics g;
	public float r, x, y;
	public int type;

	private final P size = new P(0, 0);
	private final P pos = new P(0, 0);

	public SymCoord(FakeGraphics fg, float R, float X, float Y, int t) {
		g = fg;
		r = R;
		x = X;
		y = Y;
		type = t;
	}

	/**
	 * 画像列を描画し、拡大後の全体サイズを返す。
	 * 戻り値はインスタンス内で再利用されるため、次の呼び出し後も保持する場合はコピーが必要。
	 */
	public P draw(FakeImage... fis) {
		setSize(0, 0);
		for (FakeImage f : fis) {
			size.x += f.getWidth();
			size.y = Math.max(size.y, f.getHeight());
		}
		size.times(r);
		setPos(x, y);
		if ((type & 1) > 0)
			pos.x -= size.x;
		if ((type & 2) > 0)
			pos.y -= size.y;
		for (FakeImage f : fis) {
			if (r == 1)
				g.drawImage(f, pos.x, pos.y);
			else
				g.drawImage(f, pos.x, pos.y, f.getWidth() * r, f.getHeight() * r);
			pos.x += f.getWidth() * r;
		}
		return size;
	}

	private void setPos(float x, float y) {
		pos.x = x;
		pos.y = y;
	}

	private void setSize(float x, float y) {
		size.x = x;
		size.y = y;
	}

}
