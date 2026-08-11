package common.util.pack;

import common.system.fake.FakeImage;
import common.util.anim.AnimI;
import common.util.anim.EAnimD;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;

/**
 * 背景画像の分割部品を共有して波動エフェクトを再生するアニメーション定義。
 * 画像自体は保持せず、初回読み込み時に Background の parts 参照を取得する。
 */
public class WaveAnim extends AnimI<WaveAnim, WaveAnim.WaveType> {

	public enum WaveType implements AnimI.AnimType<WaveAnim, WaveType> {
		DEF
	}

	private final Background bg;
	private final MaModel mamodel;
	private final MaAnim maanim;

	private FakeImage[] parts;

	public WaveAnim(Background BG, MaModel model, MaAnim anim) {
		bg = BG;
		mamodel = model;
		maanim = anim;
	}

	@Override
	public void check() {
		if (parts == null)
			load();
	}

	@Override
	public EAnimD<WaveType> getEAnim(WaveType t) {
		return new EAnimD<>(this, mamodel, maanim, t);
	}

	@Override
	public void load() {
		bg.check();
		parts = bg.parts;
	}

	@Override
	public String[] names() {
		return translate(WaveType.DEF);
	}

	@Override
	public FakeImage parts(int i) {
		check();
		return parts[i];
	}

	@Override
	public WaveType[] types() {
		return WaveType.values();
	}

}
