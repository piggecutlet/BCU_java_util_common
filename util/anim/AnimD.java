package common.util.anim;

import common.system.fake.FakeImage;

import java.util.List;

/**
 * スプライト分割、部品階層、種別ごとのタイムラインを束ねるデータ駆動アニメーション資源。
 * 画像は必要になるまで読み込まず、{@link #check()} を入口として派生型の {@link #load()} に委譲する。
 */
public abstract class AnimD<A extends AnimD<A, T>, T extends Enum<T> & AnimI.AnimType<A, T>> extends AnimI<A, T> {

	public ImgCut imgcut;
	public MaModel mamodel;
	public T[] types;
	public MaAnim[] anims;
	public FakeImage[] parts;

	public boolean mismatch;

	protected final String str;
	protected boolean loaded = false;

	public AnimD(String st) {
		str = st;
	}

	@Override
	public void check() {
		if (!loaded)
			load();
	}

	@Override
	public EAnimD<T> getEAnim(T t) {
		check();
		if (mamodel == null)
			return null;
		MaAnim anim = getMaAnim(t);
		return anim == null ? null : new EAnimD<T>(this, mamodel, anim, t);
	}

	public final MaAnim getMaAnim(T t) {
		for (int i = 0; i < types.length; i++)
			if (types[i] == t)
				return anims[i];
		return null;
	}

	public abstract FakeImage getNum();

	public final int len(T t) {
		check();
		return getMaAnim(t).max + 1;
	}

	@Override
	public abstract void load();

	public abstract boolean cantLoadAll(AnimU.ImageKeeper.AnimationType type);

	public abstract List<String> collectInvalidAnimation(AnimU.ImageKeeper.AnimationType type);

	@Override
	public final String[] names() {
		check();
		return translate(types);
	}

	@Override
	public FakeImage parts(int i) {
		check();
		if (i < 0 || i >= parts.length)
			return null;
		return parts[i];
	}

	/**
	 * モデル部品の旧インデックスから新インデックスへの対応を、親参照と全タイムラインの対象参照へ反映する。
	 * 配列の範囲と全参照の整合性は呼び出し側の責務。
	 */
	public void reorderModel(int[] inds) {
		for (int[] ints : mamodel.parts)
			if (ints != null && ints[0] >= 0)
				ints[0] = inds[ints[0]];
		for (MaAnim ma : anims)
			for (Part part : ma.parts)
				part.ints[0] = inds[part.ints[0]];
	}

	public void revert() {
		mamodel.revert();
		for (MaAnim ma : anims)
			if (ma != null)
				ma.revert();
	}

	@Override
	public final T[] types() {
		check();
		return types;
	}

	public void unload() {
		if (parts != null) {
			for (int i = 0; i < parts.length; i++) {
				if (parts[i] != null) {
					parts[i].unload();
					parts[i] = null;
				}
			}
		}
		parts = null;
		loaded = false;
	}

	/**
	 * 読み込んだ各データを相互参照と照合し、範囲外参照や不正値を利用可能な値へ補正する。
	 * 検査だけではなくモデルとタイムラインを破壊的に変更する。
	 */
	public void validate() {
		check();
		mamodel.check(this);
		for (MaAnim ma : anims) {
			for (Part p : ma.parts) {
				p.check(this);
				p.validate();
			}
			ma.validate();
		}
	}

}
