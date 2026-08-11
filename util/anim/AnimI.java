package common.util.anim;

import common.system.fake.FakeImage;
import common.util.Animable;
import common.util.BattleStatic;
import common.util.lang.MultiLangCont;

/**
 * アニメーション資源の共通基底。
 * 型パラメータ {@code T} は資源が提供するアニメーション種別を表し、実行時インスタンスの生成キーとして使われる。
 * コンストラクタでは継承元の {@code anim} に自分自身を設定するため、派生型は自己型 {@code A} を正しく指定する必要がある。
 */
public abstract class AnimI<A extends AnimI<A, T>, T extends Enum<T> & AnimI.AnimType<A, T>> extends Animable<A, T>
		implements BattleStatic {

	/**
	 * アニメーション種別の列挙型に、対応する資源型を結び付けるためのマーカー。
	 */
	public interface AnimType<A extends AnimI<A, T>, T extends Enum<T> & AnimType<A, T>> {

	}

	/**
	 * 種別名を現在の言語設定に従う表示名へ変換する。
	 */
	protected static String[] translate(AnimType<?, ?>... anim) {
		String[] ans = new String[anim.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = MultiLangCont.getStatic().getAnimName(anim[i]);
		return ans;
	}

	@SuppressWarnings("unchecked")
	public AnimI() {
		anim = (A) this;
	}

	public abstract void check();

	public abstract void load();

	public abstract String[] names();

	public abstract FakeImage parts(int img);

	public abstract T[] types();

}
