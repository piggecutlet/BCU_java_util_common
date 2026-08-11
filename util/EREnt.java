package common.util;

import common.io.json.JsonClass;
import common.io.json.JsonClass.NoTag;
import common.system.Copable;

import org.jetbrains.annotations.Nullable;

/**
 * ランダム選択の一候補と、その倍率・抽選重みを保持する値オブジェクト。
 * {@link #copy()} は候補 ent の参照を共有し、数値だけを値コピーする。
 *
 * @param <X> 候補エンティティ型
 */
@JsonClass(noTag = NoTag.LOAD)
public class EREnt<X> implements BattleStatic, Copable<EREnt<X>> {

	@Nullable
	public X ent;
	public int multi = 100;
	public int mula = 100;
	public int share = 1;

	@Override
	public EREnt<X> copy() {
		EREnt<X> ans = new EREnt<X>();
		ans.ent = ent;
		ans.multi = multi;
		ans.mula = mula;
		ans.share = share;
		return ans;
	}

}